package pl.mojeroboty.app

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.Currency
import java.util.Locale

data class Job(
    val id: String,
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val cents: Long,
    val status: String = "PLANNED",
    val clientId: String? = null,
    val address: String = "",
    val notes: String = ""
)

data class Client(
    val id: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val archived: Boolean = false
)

data class Payment(
    val id: String,
    val jobId: String,
    val cents: Long,
    val paidAt: LocalDate,
    val method: String = "Gotówka",
    val notes: String = ""
)

data class Snapshot(
    val jobs: List<Job> = emptyList(),
    val clients: List<Client> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val loaded: Boolean = false
) {
    fun paid(job: Job): Long = payments.filter { it.jobId == job.id }.sumOf { it.cents }
    fun remaining(job: Job): Long = job.cents - paid(job)
    fun clientName(job: Job): String = clients.firstOrNull { it.id == job.clientId }?.name ?: ""
}

val STATUS = linkedMapOf(
    "PLANNED" to "Zaplanowane",
    "IN_PROGRESS" to "W trakcie",
    "DONE" to "Zrobione",
    "CANCELLED" to "Anulowane"
)

fun money(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pl-PL"))
    .apply { currency = Currency.getInstance("EUR") }
    .format(BigDecimal.valueOf(cents, 2))

fun parseEuro(raw: String): Long {
    val normalized = raw.trim().replace(" ", "").replace("\u00a0", "").replace(',', '.')
    require(Regex("\\d+(\\.\\d{1,2})?").matches(normalized)) { "Podaj poprawną kwotę w euro." }
    return BigDecimal(normalized).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
}

fun intersects(job: Job, from: LocalDate, to: LocalDate): Boolean =
    !job.start.isAfter(to) && !job.end.isBefore(from)

fun days(job: Job): Long = ChronoUnit.DAYS.between(job.start, job.end) + 1

fun monday(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
fun sunday(date: LocalDate): LocalDate = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

data class Segment(
    val job: Job,
    val offset: Int,
    val span: Int,
    val lane: Int,
    val startsBefore: Boolean,
    val endsAfter: Boolean
)

/** Stable interval packing: overlapping calendar ranges never occupy the same lane. */
fun segments(jobs: List<Job>, weekStart: LocalDate): List<Segment> {
    val last = weekStart.plusDays(6)
    val occupied = mutableListOf<MutableSet<Int>>()
    return jobs.asSequence()
        .filter { it.status != "CANCELLED" && intersects(it, weekStart, last) }
        .sortedWith(compareBy<Job> { it.start }.thenByDescending { days(it) }.thenBy { it.id })
        .map { job ->
            val from = maxOf(job.start, weekStart)
            val to = minOf(job.end, last)
            val a = ChronoUnit.DAYS.between(weekStart, from).toInt()
            val length = ChronoUnit.DAYS.between(from, to).toInt() + 1
            val cols = (a until a + length).toSet()
            var lane = occupied.indexOfFirst { used -> cols.none { it in used } }
            if (lane == -1) { occupied.add(mutableSetOf()); lane = occupied.lastIndex }
            occupied[lane].addAll(cols)
            Segment(job, a, length, lane, job.start < weekStart, job.end > last)
        }.toList()
}

fun plannedValue(jobs: List<Job>, start: LocalDate, end: LocalDate): Long =
    jobs.filter { it.status != "CANCELLED" && it.start >= start && it.start <= end }.sumOf { it.cents }

fun received(payments: List<Payment>, start: LocalDate, end: LocalDate): Long =
    payments.filter { it.paidAt >= start && it.paidAt <= end }.sumOf { it.cents }

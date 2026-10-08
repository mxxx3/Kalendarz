package pl.mojeroboty.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val dayLabels = listOf("Pon","Wt","Śr","Czw","Pt","Sob","Nd")
private val barPalette = listOf(
    Color(0xFFB8D8FF), Color(0xFFBEDDEE), Color(0xFFB0E7D5),
    Color(0xFFFFDFAB), Color(0xFFE1D3FC), Color(0xFFFFBFD0)
)

@Composable
fun CalendarScreen(
    jobs: List<Job>, selected: LocalDate, mode: String,
    onMode: (String) -> Unit,
    onDate: (LocalDate) -> Unit,
    onJob: (Job) -> Unit
) {
    val startMonth = selected.withDayOfMonth(1)
    val monthFirstMonday = monday(startMonth)
    val monthLastSunday = sunday(startMonth.withDayOfMonth(startMonth.lengthOfMonth()))
    val countWeeks = (ChronoUnit.DAYS.between(monthFirstMonday, monthLastSunday).toInt() + 1) / 7
    val dateTitle = when(mode) {
        "Tydzień" -> shownDate(monday(selected)) + " – " + shownDate(sunday(selected))
        "Dzień" -> shownDate(selected)
        else -> selected.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("pl-PL")))
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                onDate(when(mode) {
                    "Tydzień" -> selected.minusWeeks(1)
                    "Dzień" -> selected.minusDays(1)
                    else -> selected.minusMonths(1)
                })
            }) { Icon(Icons.Default.ChevronLeft, "Poprzedni okres") }
            Text(dateTitle, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold,
                fontSize = 17.sp, maxLines = 1)
            IconButton(onClick = {
                onDate(when(mode) {
                    "Tydzień" -> selected.plusWeeks(1)
                    "Dzień" -> selected.plusDays(1)
                    else -> selected.plusMonths(1)
                })
            }) { Icon(Icons.Default.ChevronRight, "Następny okres") }
            TextButton(onClick = { onDate(LocalDate.now()) }) { Text("Dziś") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Miesiąc","Tydzień","Dzień").forEach { value ->
                FilterChip(selected = value == mode, onClick = { onMode(value) },
                    label = { Text(value) })
            }
        }
        when(mode) {
            "Dzień" -> {
                Text("Roboty na " + shownDate(selected),
                    modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
                val dayJobs = jobs.filter { it.status != "CANCELLED" && intersects(it,selected,selected) }
                if(dayJobs.isEmpty()) Text("Brak robót na ten dzień",
                    modifier = Modifier.padding(20.dp))
                LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
                    items(dayJobs, key = { it.id }) { j ->
                        ElevatedCard(onClick = { onJob(j) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(j.title, fontWeight = FontWeight.Bold)
                                Text("Dzień " + (ChronoUnit.DAYS.between(j.start,selected)+1) +
                                    " z " + days(j) + " · " + (STATUS[j.status] ?: j.status))
                                Text("Cała robota: " + money(j.cents), color = Blue)
                            }
                        }
                    }
                }
            }
            "Tydzień" -> {
                Column(Modifier.verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)) {
                    PlannerWeek(monday(selected), jobs, selected, onDate, onJob, false)
                    Text("Wybierz pasek, aby zobaczyć szczegóły. Każda cena dotyczy całej roboty.",
                        color = Color.Gray, fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp))
                }
            }
            else -> {
                Column(Modifier.verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)) {
                    repeat(countWeeks) { n ->
                        PlannerWeek(monthFirstMonday.plusWeeks(n.toLong()), jobs,
                            selected, onDate, onJob, true)
                    }
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                    Text("Roboty wybranego dnia: " + shownDate(selected),
                        modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    val list = jobs.filter { it.status != "CANCELLED" && intersects(it,selected,selected) }
                    list.forEach { job ->
                        ElevatedCard(onClick = { onJob(job) },
                            modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(job.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                Text(money(job.cents), color = Blue)
                            }
                        }
                    }
                    if(list.isEmpty()) Text("Brak robót", Modifier.padding(12.dp), color = Color.Gray)
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun PlannerWeek(
    weekStart: LocalDate, jobs: List<Job>, selected: LocalDate,
    onDate: (LocalDate) -> Unit, onJob: (Job) -> Unit, compact: Boolean
) {
    val entries = remember(weekStart, jobs) { segments(jobs, weekStart) }
    val maxLanes = if(compact) 2 else Int.MAX_VALUE
    val visible = entries.filter { it.lane < maxLanes }
    val maxLane = (visible.maxOfOrNull { it.lane } ?: -1) + 1
    val cellHeight = if(compact) 23.dp else 38.dp
    val gridHeight = 34.dp + (maxLane.coerceAtLeast(1) * cellHeight.value).dp +
        if(compact) 24.dp else 16.dp
    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(gridHeight)
            .background(Color.White)) {
            val cellWidth = maxWidth / 7
            Row(Modifier.fillMaxWidth().height(gridHeight)) {
                repeat(7) { index ->
                    val date = weekStart.plusDays(index.toLong())
                    Column(
                        Modifier.width(cellWidth).fillMaxHeight()
                            .background(if(date == selected) Color(0xFFE6F0FF)
                                else if(index % 2 == 0) Color(0xFFF9FBFE) else Color.White)
                            .clickable { onDate(date) }
                            .padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dayLabels[index], fontSize = 10.sp, color = Color.DarkGray)
                        Text(date.dayOfMonth.toString(), fontSize = 12.sp,
                            fontWeight = if(date == selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            visible.forEach { seg ->
                val tint = barPalette[Math.floorMod(seg.job.id.hashCode(), barPalette.size)]
                Box(
                    Modifier.offset(x = cellWidth * seg.offset,
                        y = 35.dp + (seg.lane * cellHeight.value).dp)
                        .width(cellWidth * seg.span - 2.dp)
                        .height(cellHeight - 3.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(tint)
                        .clickable { onJob(seg.job) }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(if(compact) seg.job.title else seg.job.title + " · " + money(seg.job.cents),
                        fontSize = if(compact) 10.sp else 12.sp, lineHeight = 12.sp,
                        maxLines = if(compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF123054))
                }
            }
            if(compact) {
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(21.dp)) {
                    repeat(7) { index ->
                        val hidden = entries.count {
                            it.lane >= maxLanes && index >= it.offset && index < it.offset + it.span
                        }
                        Box(Modifier.width(cellWidth).fillMaxHeight().clickable {
                            onDate(weekStart.plusDays(index.toLong()))
                        }, contentAlignment = Alignment.Center) {
                            if(hidden > 0) Text("+$hidden więcej", color = Blue,
                                fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

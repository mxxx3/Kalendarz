package pl.mojeroboty.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ModelTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun job(id: String, from: String, to: String, cents: Long = 10000) =
        Job(id,id,d(from),d(to),cents)

    @Test fun multiDayJobHasOneTotalPrice() {
        val a = job("malowanie","2026-10-12","2026-10-16",150000)
        assertEquals(5L,days(a))
        assertEquals(150000L,plannedValue(listOf(a),d("2026-10-01"),d("2026-10-31")))
        assertTrue(intersects(a,d("2026-10-14"),d("2026-10-14")))
        assertFalse(intersects(a,d("2026-10-17"),d("2026-10-20")))
    }

    @Test fun overlappingJobsUseDifferentCalendarLanes() {
        val jobs=listOf(
            job("a","2026-10-12","2026-10-14"),
            job("b","2026-10-13","2026-10-16"),
            job("c","2026-10-17","2026-10-18"))
        val result=segments(jobs,d("2026-10-12"))
        assertEquals(3,result.size)
        assertNotEquals(result.first { it.job.id=="a" }.lane,
            result.first { it.job.id=="b" }.lane)
        assertEquals(1, result.first { it.job.id=="c" }.span+(-1))
    }

    @Test fun jobAcrossYearAppearsInBothWeeks() {
        val a=job("year","2026-12-30","2027-01-03")
        val first=segments(listOf(a),d("2026-12-28")).single()
        assertEquals(2,first.offset)
        assertEquals(5,first.span)
        assertTrue(intersects(a,d("2027-01-01"),d("2027-01-31")))
    }

    @Test fun crossingWeekBoundaryHasContinuation() {
        val a=job("long","2026-10-10","2026-10-25")
        val first=segments(listOf(a),d("2026-10-12")).single()
        assertTrue(first.startsBefore)
        assertTrue(first.endsAfter)
        assertEquals(7,first.span)
        val next=segments(listOf(a),d("2026-10-19")).single()
        assertTrue(next.startsBefore)
    }

    @Test fun boundaryDayCountsAsCollision() {
        val j=job("x","2026-10-12","2026-10-14")
        assertTrue(intersects(j,d("2026-10-14"),d("2026-10-16")))
        assertFalse(intersects(j,d("2026-10-15"),d("2026-10-16")))
    }

    @Test fun euroParsingNeverUsesFloatingPoint() {
        assertEquals(260000L,parseEuro("2 600,00"))
        assertEquals(1L,parseEuro("0,01"))
        assertEquals(0L,parseEuro("0"))
        assertEquals(99L,parseEuro("0.99"))
        assertThrows(IllegalArgumentException::class.java) { parseEuro("-1") }
        assertThrows(IllegalArgumentException::class.java) { parseEuro("0,001") }
        assertThrows(IllegalArgumentException::class.java) { parseEuro("abc") }
    }

    @Test fun paymentAndContractValuesAreSeparate() {
        val a=job("a","2026-10-10","2026-10-12",260000)
        val payments=listOf(Payment("p","a",50000,d("2026-11-01")))
        val snapshot=Snapshot(listOf(a),emptyList(),payments,true)
        assertEquals(210000L,snapshot.remaining(a))
        assertEquals(0L,received(payments,d("2026-10-01"),d("2026-10-31")))
        assertEquals(50000L,received(payments,d("2026-11-01"),d("2026-11-30")))
        assertEquals(260000L,plannedValue(listOf(a),d("2026-10-01"),d("2026-10-31")))
    }

    @Test fun cancelledJobsDoNotCountTowardPlan() {
        val a=job("a","2026-10-10","2026-10-12",100000)
        assertEquals(0L,plannedValue(listOf(a.copy(status="CANCELLED")),
            d("2026-10-01"),d("2026-10-31")))
    }

    @Test fun leapYearDateIsValid() {
        val a=job("leap","2028-02-28","2028-03-01")
        assertEquals(3L,days(a))
    }
}

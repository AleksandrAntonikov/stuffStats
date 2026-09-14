package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticDistanceTest {
    private val date = LocalDate.of(2026, 9, 13)
    private fun item(unit: String = "km") = Item(
        id = 7,
        name = "Walking shoes",
        category = Category.FOOTWEAR,
        priceMinor = 9000,
        currency = "USD",
        purchaseDate = LocalDate.of(2026, 8, 1),
        metric = UsageMetric(MetricType.DISTANCE, unit),
        notes = "",
    )

    @Test fun convertsMetersIntoSupportedDistanceUnitsExactly() {
        assertEquals(BigDecimal("1609.344"), DistanceImportCalculations.fromMeters(BigDecimal("1609.344"), "m"))
        assertEquals(BigDecimal("1.609344"), DistanceImportCalculations.fromMeters(BigDecimal("1609.344"), "KM"))
        assertEquals(BigDecimal.ONE, DistanceImportCalculations.fromMeters(BigDecimal("1609.344"), "mi"))
        assertTrue(DistanceImportCalculations.supports(" km "))
        assertFalse(DistanceImportCalculations.supports("yards"))
    }

    @Test fun createsAnAutomaticEventForTheSelectedItemAndDate() {
        val event = DistanceImportCalculations.eventFor(item(), date, BigDecimal("5700"), null, createdAt = 42)

        assertEquals(0L, event.id)
        assertEquals(7L, event.itemId)
        assertEquals(BigDecimal("5.7"), event.value)
        assertEquals(date, event.date)
        assertEquals(UsageSource.AUTOMATIC, event.source)
        assertEquals(42L, event.createdAt)
    }

    @Test fun repeatedImportUpdatesTheExistingAutomaticEvent() {
        val old = UsageEvent(
            id = 11,
            itemId = 7,
            value = BigDecimal("4"),
            date = date,
            notes = "kept note",
            source = UsageSource.AUTOMATIC,
            createdAt = 100,
        )

        val refreshed = DistanceImportCalculations.eventFor(item(), date, BigDecimal("5250"), old, createdAt = 999)

        assertEquals(11L, refreshed.id)
        assertEquals(BigDecimal("5.25"), refreshed.value)
        assertEquals("kept note", refreshed.notes)
        assertEquals(100L, refreshed.createdAt)
    }

    @Test fun refusesToOverwriteAnUnrelatedOrManualEvent() {
        val manual = UsageEvent(3, 7, BigDecimal.ONE, date, "", UsageSource.MANUAL)
        val otherDate = manual.copy(source = UsageSource.AUTOMATIC, date = date.minusDays(1))

        assertTrue(runCatching { DistanceImportCalculations.eventFor(item(), date, BigDecimal.TEN, manual) }.isFailure)
        assertTrue(runCatching { DistanceImportCalculations.eventFor(item(), date, BigDecimal.TEN, otherDate) }.isFailure)
    }
}

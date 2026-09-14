package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageCalculationsTest {
    private fun item(priceMinor: Long? = 10000, purchaseDate: LocalDate? = null) = Item(
        id = 1,
        name = "Shoes",
        category = Category.FOOTWEAR,
        priceMinor = priceMinor,
        currency = "USD",
        purchaseDate = purchaseDate,
        metric = UsageMetric(MetricType.DISTANCE, "km"),
        notes = "",
    )

    private fun event(value: String, itemId: Long = 1, date: LocalDate = LocalDate.of(2026, 9, 10)) = UsageEvent(
        itemId = itemId,
        value = BigDecimal(value),
        date = date,
        notes = "",
    )

    @Test fun decimalInputIsExactAndNormalized() {
        assertEquals("5.7", UsageValidation.value("5,700000").toPlainString())
        assertEquals("1", UsageValidation.value("1.000000").toPlainString())
    }

    @Test fun invalidValuesAndFutureDatesAreRejected() {
        listOf("", "0", "-1", "1.1234567", "NaN", "1e3", "1234567890123456789").forEach {
            assertTrue(it, runCatching { UsageValidation.value(it) }.isFailure)
        }
        assertTrue(
            runCatching {
                UsageValidation.validate(event("1").copy(date = LocalDate.now().plusDays(1)))
            }.isFailure,
        )
    }

    @Test fun totalAndCostPerUnitComeFromMatchingSourceEvents() {
        val stats = UsageCalculations.forItem(item(), listOf(event("5"), event("7.0"), event("100", itemId = 2)))

        assertEquals(BigDecimal("12"), stats.totalUsage)
        assertEquals(BigDecimal("8.33"), stats.costPerUnit)
        assertEquals(2, stats.eventCount)
    }

    @Test fun absentPriceOrUsageHasNoCostPerUnit() {
        assertEquals(null, UsageCalculations.forItem(item(), emptyList()).costPerUnit)
        assertEquals(null, UsageCalculations.forItem(item(priceMinor = null), listOf(event("5"))).costPerUnit)
    }

    @Test fun lifetimeDatesAndAveragesUseSourceDates() {
        val today = LocalDate.of(2026, 9, 16)
        val stats = UsageCalculations.forItem(
            item(purchaseDate = LocalDate.of(2026, 9, 1)),
            listOf(event("5", date = LocalDate.of(2026, 9, 10)), event("7", date = LocalDate.of(2026, 9, 12))),
            today,
        )

        assertEquals(16L, stats.daysOwned)
        assertEquals(LocalDate.of(2026, 9, 10), stats.firstUsageDate)
        assertEquals(LocalDate.of(2026, 9, 12), stats.lastUsageDate)
        assertEquals(BigDecimal("12"), stats.averagePerWeek)
        assertEquals(BigDecimal("52.18"), stats.averagePerMonth)
    }
}

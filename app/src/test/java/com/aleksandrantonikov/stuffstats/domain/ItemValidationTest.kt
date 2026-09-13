package com.aleksandrantonikov.stuffstats.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ItemValidationTest {
    @Test fun moneyIsExactAndAcceptsDecimalComma() {
        assertEquals(12345L, ItemValidation.priceMinor("123,45", "USD"))
        assertEquals(123L, ItemValidation.priceMinor("123", "JPY"))
        assertEquals(1234L, ItemValidation.priceMinor("1.234", "KWD"))
        assertNull(ItemValidation.priceMinor("", "USD"))
        assertEquals(0L, ItemValidation.priceMinor("0", "USD"))
    }
    @Test fun invalidMoneyIsRejected() {
        listOf("-1", "NaN", "Infinity", "1.001", "1e3", "9223372036854775808", "1,2.3").forEach {
            assertTrue(it, runCatching { ItemValidation.priceMinor(it, "USD") }.isFailure)
        }
        assertTrue(runCatching { ItemValidation.priceMinor("1.1", "JPY") }.isFailure)
        assertTrue(runCatching { ItemValidation.priceMinor("", "BAD") }.isFailure)
    }
    private fun item() = Item(name = "Shoes", category = Category.FOOTWEAR, priceMinor = 10000,
        currency = "USD", purchaseDate = LocalDate.of(2026, 1, 1), metric = UsageMetric(MetricType.DISTANCE, "km"), notes = "")
    @Test fun invalidFieldsAreRejected() {
        listOf(item().copy(name = " "), item().copy(priceMinor = -1),
            item().copy(purchaseDate = LocalDate.now().plusDays(1)),
            item().copy(metric = UsageMetric(MetricType.CUSTOM, "")),
            item().copy(notes = "x".repeat(4001))).forEach {
            assertTrue(runCatching { ItemValidation.validate(it) }.isFailure)
        }
    }
    @Test fun customMetricIsIndependentOfCategory() {
        ItemValidation.validate(item().copy(category = Category.ELECTRONICS, metric = UsageMetric(MetricType.CUSTOM, "cycles")))
        assertEquals("100.00", ItemValidation.priceText(item()))
    }
}

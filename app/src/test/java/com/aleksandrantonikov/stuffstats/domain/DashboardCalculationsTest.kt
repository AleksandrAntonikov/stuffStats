package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardCalculationsTest {
    private fun item(
        id: Long,
        name: String,
        priceMinor: Long,
        currency: String = "USD",
        type: MetricType = MetricType.DISTANCE,
        unit: String = "km",
        archived: Boolean = false,
    ) = Item(id, name, Category.OTHER, priceMinor, currency, null, UsageMetric(type, unit), "", archived)

    private fun event(itemId: Long, value: String, day: Int) = UsageEvent(
        itemId = itemId,
        value = BigDecimal(value),
        date = LocalDate.of(2026, 9, day),
        notes = "",
    )

    @Test
    fun dashboardUsesActiveItemsAndOnlyComparesEquivalentCosts() {
        val shoes = item(1, "Shoes", 10000)
        val boots = item(2, "Boots", 5000)
        val shirt = item(3, "Shirt", 2000, "EUR", MetricType.WEAR_COUNT, "wears")
        val archived = item(4, "Archived", 99900, archived = true)
        val events = listOf(
            event(1, "2", 1), event(1, "3", 2), event(1, "7", 3),
            event(2, "10", 1),
            event(3, "2", 1),
            event(4, "1000", 1),
        )
        val photos = listOf(
            ItemPhoto(itemId = 1, imagePath = "one.jpg", date = LocalDate.of(2026, 9, 1), usageValueAtPhoto = BigDecimal.ZERO, notes = ""),
            ItemPhoto(itemId = 4, imagePath = "archived.jpg", date = LocalDate.of(2026, 9, 1), usageValueAtPhoto = BigDecimal.ZERO, notes = ""),
        )

        val dashboard = DashboardCalculations.from(listOf(shoes, boots, shirt, archived), events, photos)

        assertEquals(3, dashboard.activeItemCount)
        assertEquals(1, dashboard.archivedItemCount)
        assertEquals(5, dashboard.eventCount)
        assertEquals(1, dashboard.photoCount)
        assertEquals(listOf(MoneyTotal("EUR", BigDecimal("20")), MoneyTotal("USD", BigDecimal("150"))), dashboard.purchaseTotals)
        assertEquals("Shoes", dashboard.mostTrackedItem?.name)
        assertEquals(3, dashboard.mostTrackedItem?.eventCount)
        assertEquals(listOf("Shirt", "Boots"), dashboard.costLeaders.map(CostLeader::name))
    }
}

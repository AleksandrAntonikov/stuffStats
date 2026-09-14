package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogFilteringTest {
    private fun item(id: Long, name: String, category: Category, notes: String = "", archived: Boolean = false) = Item(
        id = id,
        name = name,
        category = category,
        priceMinor = null,
        currency = "USD",
        purchaseDate = null,
        metric = UsageMetric(MetricType.DISTANCE, "km"),
        notes = notes,
        isArchived = archived,
        createdAt = id,
    )

    @Test
    fun combinesArchiveCategoryAndSearchFilters() {
        val items = listOf(
            item(1, "Trail shoes", Category.FOOTWEAR, "red"),
            item(2, "Office shoes", Category.FOOTWEAR, "black", archived = true),
            item(3, "Backpack", Category.EQUIPMENT, "red"),
        )

        assertEquals(
            listOf("Trail shoes"),
            CatalogFiltering.apply(items, emptyList(), false, Category.FOOTWEAR, "red", CatalogSort.NAME).map(Item::name),
        )
        assertEquals(
            listOf("Office shoes"),
            CatalogFiltering.apply(items, emptyList(), true, null, "shoes", CatalogSort.NAME).map(Item::name),
        )
    }

    @Test
    fun lastUsedSortKeepsItemsWithoutEventsLast() {
        val items = listOf(item(1, "Old", Category.OTHER), item(2, "New", Category.OTHER), item(3, "Unused", Category.OTHER))
        val events = listOf(
            UsageEvent(itemId = 1, value = BigDecimal.ONE, date = LocalDate.of(2026, 9, 1), notes = ""),
            UsageEvent(itemId = 2, value = BigDecimal.ONE, date = LocalDate.of(2026, 9, 2), notes = ""),
        )

        assertEquals(
            listOf("New", "Old", "Unused"),
            CatalogFiltering.apply(items, events, false, null, "", CatalogSort.LAST_USED).map(Item::name),
        )
    }
}

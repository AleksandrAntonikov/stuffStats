package com.aleksandrantonikov.stuffstats.domain

import java.time.LocalDate
import java.util.Locale

enum class CatalogSort { RECENTLY_ADDED, NAME, LAST_USED }

object CatalogFiltering {
    fun apply(
        items: List<Item>,
        events: List<UsageEvent>,
        archived: Boolean,
        category: Category?,
        query: String,
        sort: CatalogSort,
    ): List<Item> {
        val needle = query.trim().lowercase(Locale.ROOT)
        val filtered = items.filter { item ->
            item.isArchived == archived &&
                (category == null || item.category == category) &&
                (needle.isEmpty() || searchableText(item).contains(needle))
        }
        return when (sort) {
            CatalogSort.RECENTLY_ADDED -> filtered.sortedWith(compareByDescending<Item>(Item::createdAt).thenByDescending(Item::id))
            CatalogSort.NAME -> filtered.sortedWith(compareBy<Item> { it.name.lowercase(Locale.ROOT) }.thenBy(Item::id))
            CatalogSort.LAST_USED -> {
                val lastDates = events.groupBy(UsageEvent::itemId).mapValues { (_, values) -> values.maxOf(UsageEvent::date) }
                filtered.sortedWith(
                    compareByDescending<Item> { lastDates[it.id] ?: LocalDate.MIN }
                        .thenByDescending(Item::createdAt)
                        .thenByDescending(Item::id),
                )
            }
        }
    }

    private fun searchableText(item: Item) = listOf(
        item.name,
        item.notes,
        item.category.name,
        item.metric.type.name,
        item.metric.unit,
    ).joinToString(" ").lowercase(Locale.ROOT)
}

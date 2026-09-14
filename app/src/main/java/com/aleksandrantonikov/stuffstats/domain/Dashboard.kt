package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.util.Currency

data class MoneyTotal(val currency: String, val amount: BigDecimal)

data class TrackedItem(
    val itemId: Long,
    val name: String,
    val eventCount: Int,
    val totalUsage: BigDecimal,
    val unit: String,
)

data class CostLeader(
    val itemId: Long,
    val name: String,
    val currency: String,
    val metricType: MetricType,
    val unit: String,
    val costPerUnit: BigDecimal,
)

data class DashboardStats(
    val activeItemCount: Int,
    val archivedItemCount: Int,
    val eventCount: Int,
    val photoCount: Int,
    val purchaseTotals: List<MoneyTotal>,
    val mostTrackedItem: TrackedItem?,
    val costLeaders: List<CostLeader>,
)

object DashboardCalculations {
    fun from(items: List<Item>, events: List<UsageEvent>, photos: List<ItemPhoto>): DashboardStats {
        val active = items.filterNot(Item::isArchived)
        val activeIds = active.mapTo(mutableSetOf(), Item::id)
        val activeEvents = events.filter { it.itemId in activeIds }
        val activePhotos = photos.filter { it.itemId in activeIds }
        val stats = active.associateWith { UsageCalculations.forItem(it, activeEvents) }
        val purchaseTotals = active
            .filter { it.priceMinor != null }
            .groupBy(Item::currency)
            .map { (currency, pricedItems) ->
                val digits = Currency.getInstance(currency).defaultFractionDigits
                MoneyTotal(
                    currency = currency,
                    amount = pricedItems.fold(BigDecimal.ZERO) { total, item ->
                        total + BigDecimal.valueOf(checkNotNull(item.priceMinor), digits)
                    }.normalize(),
                )
            }
            .sortedBy(MoneyTotal::currency)
        val mostTracked = active
            .mapNotNull { item ->
                val itemStats = checkNotNull(stats[item]).takeIf { it.eventCount > 0 } ?: return@mapNotNull null
                TrackedItem(item.id, item.name, itemStats.eventCount, itemStats.totalUsage, item.metric.unit)
            }
            .sortedWith(compareByDescending<TrackedItem>(TrackedItem::eventCount).thenBy(TrackedItem::name))
            .firstOrNull()
        val costLeaders = active
            .mapNotNull { item ->
                val cost = checkNotNull(stats[item]).costPerUnit ?: return@mapNotNull null
                CostLeader(item.id, item.name, item.currency, item.metric.type, item.metric.unit, cost)
            }
            .groupBy { Triple(it.currency, it.metricType, it.unit.lowercase()) }
            .values
            .map { candidates -> candidates.minWith(compareBy<CostLeader>(CostLeader::costPerUnit).thenBy(CostLeader::name)) }
            .sortedWith(compareBy(CostLeader::currency, CostLeader::metricType, CostLeader::unit))
        return DashboardStats(
            activeItemCount = active.size,
            archivedItemCount = items.size - active.size,
            eventCount = activeEvents.size,
            photoCount = activePhotos.size,
            purchaseTotals = purchaseTotals,
            mostTrackedItem = mostTracked,
            costLeaders = costLeaders,
        )
    }
}

package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Currency
import kotlinx.coroutines.flow.Flow

enum class UsageSource { MANUAL, AUTOMATIC }

data class UsageEvent(
    val id: Long = 0,
    val itemId: Long,
    val value: BigDecimal,
    val date: LocalDate,
    val notes: String,
    val source: UsageSource = UsageSource.MANUAL,
    val createdAt: Long = System.currentTimeMillis(),
)

data class ItemUsageStats(
    val totalUsage: BigDecimal,
    val costPerUnit: BigDecimal?,
    val eventCount: Int,
    val daysOwned: Long?,
    val firstUsageDate: LocalDate?,
    val lastUsageDate: LocalDate?,
    val averagePerWeek: BigDecimal?,
    val averagePerMonth: BigDecimal?,
)

object UsageValidation {
    private val decimal = Regex("[0-9]+([.,][0-9]{1,6})?")

    fun value(text: String): BigDecimal {
        val normalized = text.trim().replace(',', '.')
        require(decimal.matches(normalized))
        return BigDecimal(normalized).normalize().also {
            require(it > BigDecimal.ZERO)
            require(it.precision() <= 18)
        }
    }

    fun validate(event: UsageEvent) {
        require(event.itemId > 0)
        require(event.value > BigDecimal.ZERO)
        require(event.value.normalize().scale() <= 6)
        require(event.value.normalize().precision() <= 18)
        require(!event.date.isAfter(LocalDate.now()))
        require(event.notes.length <= 4000)
    }
}

object UsageCalculations {
    fun forItem(item: Item, events: List<UsageEvent>, today: LocalDate = LocalDate.now()): ItemUsageStats {
        val matching = events.filter { it.itemId == item.id }
        val total = matching.fold(BigDecimal.ZERO) { sum, event -> sum + event.value }.normalize()
        val pricePerUnit = item.priceMinor?.takeIf { total > BigDecimal.ZERO }?.let { minor ->
            val currencyDigits = Currency.getInstance(item.currency).defaultFractionDigits
            BigDecimal.valueOf(minor, currencyDigits).divide(total, currencyDigits, RoundingMode.HALF_UP)
        }
        val firstUsage = matching.minOfOrNull(UsageEvent::date)
        val lastUsage = matching.maxOfOrNull(UsageEvent::date)
        val observedDays = firstUsage?.let { ChronoUnit.DAYS.between(it, today).coerceAtLeast(0) + 1 }
        fun average(days: BigDecimal) = observedDays?.let {
            total.multiply(days).divide(BigDecimal.valueOf(it), 2, RoundingMode.HALF_UP).normalize()
        }
        return ItemUsageStats(
            totalUsage = total,
            costPerUnit = pricePerUnit,
            eventCount = matching.size,
            daysOwned = item.purchaseDate?.let { ChronoUnit.DAYS.between(it, today).coerceAtLeast(0) + 1 },
            firstUsageDate = firstUsage,
            lastUsageDate = lastUsage,
            averagePerWeek = firstUsage?.let { average(BigDecimal.valueOf(7)) },
            averagePerMonth = firstUsage?.let { average(BigDecimal("30.4375")) },
        )
    }
}

fun BigDecimal.normalize(): BigDecimal = stripTrailingZeros().let {
    if (it.scale() < 0) it.setScale(0) else it
}

fun BigDecimal.asPlainValue(): String = normalize().toPlainString()

interface UsageEventRepository {
    fun observeAll(): Flow<List<UsageEvent>>
    suspend fun get(id: Long): UsageEvent?
    suspend fun save(event: UsageEvent): Long
    suspend fun delete(id: Long)
}

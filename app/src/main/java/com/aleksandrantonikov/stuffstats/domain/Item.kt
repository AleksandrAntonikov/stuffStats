package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency
import kotlinx.coroutines.flow.Flow

enum class Category { FOOTWEAR, CLOTHING, EQUIPMENT, ELECTRONICS, OTHER }
enum class MetricType(val unit: String) {
    DISTANCE("km"), USE_COUNT("uses"), WEAR_COUNT("wears"), WASH_COUNT("washes"), HOURS("hours"), CUSTOM("")
}
data class UsageMetric(val type: MetricType, val unit: String)
data class Item(
    val id: Long = 0,
    val name: String,
    val category: Category,
    val priceMinor: Long?,
    val currency: String,
    val purchaseDate: LocalDate?,
    val metric: UsageMetric,
    val notes: String,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

object ItemValidation {
    fun priceMinor(text: String, code: String): Long? {
        val digits = Currency.getInstance(code).defaultFractionDigits
        require(digits >= 0)
        if (text.isBlank()) return null
        require(Regex("[0-9]+([.,][0-9]+)?").matches(text.trim()))
        return BigDecimal(text.trim().replace(',', '.')).movePointRight(digits).longValueExact()
    }
    fun validate(item: Item) {
        require(item.name.isNotBlank() && item.name.length <= 120)
        require(item.metric.unit.isNotBlank() && item.metric.unit.length <= 24)
        require(item.notes.length <= 4000)
        require(item.priceMinor == null || item.priceMinor >= 0)
        require(Currency.getInstance(item.currency).defaultFractionDigits >= 0)
        require(item.purchaseDate == null || !item.purchaseDate.isAfter(LocalDate.now()))
    }
    fun priceText(item: Item): String = item.priceMinor?.let {
        BigDecimal.valueOf(it, Currency.getInstance(item.currency).defaultFractionDigits).toPlainString()
    }.orEmpty()
}

interface ItemRepository {
    fun observeItems(): Flow<List<Item>>
    suspend fun get(id: Long): Item?
    suspend fun save(item: Item): Long
    suspend fun archive(id: Long, archived: Boolean)
}

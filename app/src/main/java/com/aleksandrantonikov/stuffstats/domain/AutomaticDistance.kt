package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class AutomaticDistanceAvailability { AVAILABLE, PROVIDER_UPDATE_REQUIRED, UNAVAILABLE }

interface AutomaticDistanceSource {
    fun availability(): AutomaticDistanceAvailability
    suspend fun hasPermission(): Boolean
    suspend fun readMeters(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): BigDecimal
}

enum class DistanceImportStatus {
    IDLE,
    CHECKING,
    READY,
    PERMISSION_REQUIRED,
    PROVIDER_UPDATE_REQUIRED,
    UNAVAILABLE,
    IMPORTING,
    IMPORTED,
    NO_DATA,
    UNSUPPORTED_UNIT,
    INVALID_DATE,
    FAILED,
}

data class DistanceImportState(
    val itemId: Long? = null,
    val status: DistanceImportStatus = DistanceImportStatus.IDLE,
    val date: LocalDate? = null,
    val importedValue: BigDecimal? = null,
)

object DistanceImportCalculations {
    private val metersPerMile = BigDecimal("1609.344")

    fun supports(unit: String): Boolean = normalizedUnit(unit) in setOf("m", "km", "mi")

    fun fromMeters(meters: BigDecimal, unit: String): BigDecimal? {
        require(meters >= BigDecimal.ZERO)
        val converted = when (normalizedUnit(unit)) {
            "m" -> meters
            "km" -> meters.divide(BigDecimal("1000"), 6, RoundingMode.HALF_UP)
            "mi" -> meters.divide(metersPerMile, 6, RoundingMode.HALF_UP)
            else -> return null
        }
        return converted.setScale(6, RoundingMode.HALF_UP).normalize()
    }

    fun eventFor(
        item: Item,
        date: LocalDate,
        meters: BigDecimal,
        existing: UsageEvent?,
        createdAt: Long = System.currentTimeMillis(),
    ): UsageEvent {
        require(item.metric.type == MetricType.DISTANCE)
        require(existing == null || (existing.itemId == item.id && existing.date == date && existing.source == UsageSource.AUTOMATIC))
        val value = checkNotNull(fromMeters(meters, item.metric.unit)).also { require(it > BigDecimal.ZERO) }
        return UsageEvent(
            id = existing?.id ?: 0,
            itemId = item.id,
            value = value,
            date = date,
            notes = existing?.notes.orEmpty(),
            source = UsageSource.AUTOMATIC,
            createdAt = existing?.createdAt ?: createdAt,
        )
    }

    private fun normalizedUnit(unit: String) = unit.trim().lowercase(Locale.ROOT)
}

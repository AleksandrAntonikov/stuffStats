package com.aleksandrantonikov.stuffstats.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.aleksandrantonikov.stuffstats.domain.AutomaticDistanceAvailability
import com.aleksandrantonikov.stuffstats.domain.AutomaticDistanceSource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectDistanceSource(context: Context) : AutomaticDistanceSource {
    private val appContext = context.applicationContext

    override fun availability(): AutomaticDistanceAvailability = when (
        HealthConnectClient.getSdkStatus(appContext, PROVIDER_PACKAGE_NAME)
    ) {
        HealthConnectClient.SDK_AVAILABLE -> AutomaticDistanceAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> AutomaticDistanceAvailability.PROVIDER_UPDATE_REQUIRED
        else -> AutomaticDistanceAvailability.UNAVAILABLE
    }

    override suspend fun hasPermission(): Boolean {
        if (availability() != AutomaticDistanceAvailability.AVAILABLE) return false
        return READ_DISTANCE_PERMISSION in client().permissionController.getGrantedPermissions()
    }

    override suspend fun readMeters(date: LocalDate, zoneId: ZoneId): BigDecimal {
        check(availability() == AutomaticDistanceAvailability.AVAILABLE)
        val start = date.atStartOfDay(zoneId).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()
        val end = if (date == LocalDate.now(zoneId)) minOf(endOfDay, Instant.now()) else endOfDay
        if (end <= start) return BigDecimal.ZERO
        val result = client().aggregate(
            AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        return result[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.let(BigDecimal::valueOf) ?: BigDecimal.ZERO
    }

    private fun client() = HealthConnectClient.getOrCreate(appContext)

    companion object {
        const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
        val READ_DISTANCE_PERMISSION: String = HealthPermission.getReadPermission(DistanceRecord::class)
        val REQUIRED_PERMISSIONS: Set<String> = setOf(READ_DISTANCE_PERMISSION)

        fun providerUpdateIntent(context: Context): Intent = Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.android.vending")
            data = Uri.parse("market://details?id=$PROVIDER_PACKAGE_NAME&url=healthconnect%3A%2F%2Fonboarding")
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
        }
    }
}

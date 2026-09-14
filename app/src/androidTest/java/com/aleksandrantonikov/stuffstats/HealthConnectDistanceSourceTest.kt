package com.aleksandrantonikov.stuffstats

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aleksandrantonikov.stuffstats.data.HealthConnectDistanceSource
import com.aleksandrantonikov.stuffstats.domain.AutomaticDistanceAvailability
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthConnectDistanceSourceTest {
    @Test fun availabilityAndPermissionChecksAreSafeOnTheCurrentDevice() = runBlocking {
        val source = HealthConnectDistanceSource(ApplicationProvider.getApplicationContext())

        assertEquals(
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthConnectDistanceSource.READ_DISTANCE_PERMISSION,
        )
        when (source.availability()) {
            AutomaticDistanceAvailability.AVAILABLE -> {
                if (source.hasPermission()) {
                    assertTrue(source.readMeters(LocalDate.now()) >= BigDecimal.ZERO)
                }
            }
            AutomaticDistanceAvailability.PROVIDER_UPDATE_REQUIRED,
            AutomaticDistanceAvailability.UNAVAILABLE,
            -> assertTrue(!source.hasPermission())
        }
    }
}

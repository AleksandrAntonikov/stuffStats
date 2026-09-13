package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoValidationTest {
    private fun photo() = ItemPhoto(
        itemId = 1,
        imagePath = "photo.jpg",
        date = LocalDate.now(),
        usageValueAtPhoto = BigDecimal("12.5"),
        notes = "Sole wear",
    )

    @Test
    fun acceptsExactGenericUsageSnapshot() {
        PhotoValidation.validate(photo())
    }

    @Test
    fun rejectsUnsafePathFutureDateAndNegativeSnapshot() {
        assertTrue(runCatching { PhotoValidation.validate(photo().copy(imagePath = "../outside.jpg")) }.isFailure)
        assertTrue(runCatching { PhotoValidation.validate(photo().copy(date = LocalDate.now().plusDays(1))) }.isFailure)
        assertTrue(runCatching { PhotoValidation.validate(photo().copy(usageValueAtPhoto = BigDecimal("-1"))) }.isFailure)
    }
}

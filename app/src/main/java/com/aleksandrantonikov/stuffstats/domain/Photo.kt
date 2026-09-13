package com.aleksandrantonikov.stuffstats.domain

import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class ItemPhoto(
    val id: Long = 0,
    val itemId: Long,
    val imagePath: String,
    val date: LocalDate,
    val usageValueAtPhoto: BigDecimal,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)

object PhotoValidation {
    fun validate(photo: ItemPhoto) {
        require(photo.itemId > 0)
        require(photo.imagePath.isNotBlank() && photo.imagePath.length <= 255)
        require(!photo.imagePath.contains('/') && !photo.imagePath.contains('\\'))
        require(!photo.date.isAfter(LocalDate.now()))
        require(photo.usageValueAtPhoto >= BigDecimal.ZERO)
        require(photo.usageValueAtPhoto.normalize().scale() <= 6)
        require(photo.usageValueAtPhoto.normalize().precision() <= 18)
        require(photo.notes.length <= 4000)
    }
}

interface ItemPhotoRepository {
    fun observeAll(): Flow<List<ItemPhoto>>
    suspend fun save(photo: ItemPhoto): Long
    suspend fun delete(id: Long): ItemPhoto
}

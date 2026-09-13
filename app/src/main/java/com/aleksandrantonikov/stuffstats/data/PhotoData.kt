package com.aleksandrantonikov.stuffstats.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.aleksandrantonikov.stuffstats.domain.ItemPhoto
import com.aleksandrantonikov.stuffstats.domain.ItemPhotoRepository
import com.aleksandrantonikov.stuffstats.domain.PhotoValidation
import com.aleksandrantonikov.stuffstats.domain.asPlainValue
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(
    tableName = "item_photos",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId")],
)
data class ItemPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val imagePath: String,
    val dateEpochDay: Long,
    val usageValueAtPhoto: String,
    val notes: String,
    val createdAt: Long,
) {
    fun toDomain() = ItemPhoto(
        id = id,
        itemId = itemId,
        imagePath = imagePath,
        date = LocalDate.ofEpochDay(dateEpochDay),
        usageValueAtPhoto = BigDecimal(usageValueAtPhoto),
        notes = notes,
        createdAt = createdAt,
    )
}

@Dao
interface ItemPhotoDao {
    @Query("SELECT * FROM item_photos ORDER BY dateEpochDay DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<ItemPhotoEntity>>

    @Query("SELECT * FROM item_photos WHERE id = :id")
    suspend fun get(id: Long): ItemPhotoEntity?

    @Insert
    suspend fun insert(photo: ItemPhotoEntity): Long

    @Query("DELETE FROM item_photos WHERE id = :id")
    suspend fun delete(id: Long): Int
}

class RoomItemPhotoRepository(private val db: StuffStatsDatabase) : ItemPhotoRepository {
    override fun observeAll() = db.itemPhotos().observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(photo: ItemPhoto): Long {
        PhotoValidation.validate(photo)
        checkNotNull(db.items().get(photo.itemId))
        check(photo.id == 0L)
        return db.itemPhotos().insert(
            ItemPhotoEntity(
                itemId = photo.itemId,
                imagePath = photo.imagePath,
                dateEpochDay = photo.date.toEpochDay(),
                usageValueAtPhoto = photo.usageValueAtPhoto.asPlainValue(),
                notes = photo.notes.trim(),
                createdAt = photo.createdAt,
            ),
        )
    }

    override suspend fun delete(id: Long): ItemPhoto {
        val photo = checkNotNull(db.itemPhotos().get(id)).toDomain()
        check(db.itemPhotos().delete(id) == 1)
        return photo
    }
}

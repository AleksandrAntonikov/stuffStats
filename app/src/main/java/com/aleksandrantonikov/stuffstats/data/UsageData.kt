package com.aleksandrantonikov.stuffstats.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.aleksandrantonikov.stuffstats.domain.UsageEvent
import com.aleksandrantonikov.stuffstats.domain.UsageEventRepository
import com.aleksandrantonikov.stuffstats.domain.UsageSource
import com.aleksandrantonikov.stuffstats.domain.UsageValidation
import com.aleksandrantonikov.stuffstats.domain.asPlainValue
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(
    tableName = "usage_events",
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
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val value: String,
    val dateEpochDay: Long,
    val notes: String,
    val source: String,
    val createdAt: Long,
) {
    fun toDomain() = UsageEvent(
        id = id,
        itemId = itemId,
        value = BigDecimal(value),
        date = LocalDate.ofEpochDay(dateEpochDay),
        notes = notes,
        source = UsageSource.valueOf(source),
        createdAt = createdAt,
    )
}

@Dao
interface UsageEventDao {
    @Query("SELECT * FROM usage_events ORDER BY dateEpochDay DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<UsageEventEntity>>

    @Query("SELECT * FROM usage_events WHERE id = :id")
    suspend fun get(id: Long): UsageEventEntity?

    @Insert
    suspend fun insert(event: UsageEventEntity): Long

    @Update
    suspend fun update(event: UsageEventEntity): Int

    @Query("DELETE FROM usage_events WHERE id = :id")
    suspend fun delete(id: Long): Int
}

class RoomUsageEventRepository(private val db: StuffStatsDatabase) : UsageEventRepository {
    override fun observeAll() = db.usageEvents().observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: Long) = db.usageEvents().get(id)?.toDomain()

    override suspend fun save(event: UsageEvent): Long {
        UsageValidation.validate(event)
        checkNotNull(db.items().get(event.itemId))
        val old = if (event.id == 0L) null else checkNotNull(db.usageEvents().get(event.id))
        check(old == null || old.itemId == event.itemId)
        val entity = UsageEventEntity(
            id = event.id,
            itemId = event.itemId,
            value = event.value.asPlainValue(),
            dateEpochDay = event.date.toEpochDay(),
            notes = event.notes.trim(),
            source = event.source.name,
            createdAt = old?.createdAt ?: event.createdAt,
        )
        return if (old == null) {
            db.usageEvents().insert(entity)
        } else {
            check(db.usageEvents().update(entity) == 1)
            event.id
        }
    }

    override suspend fun delete(id: Long) {
        check(db.usageEvents().delete(id) == 1)
    }
}

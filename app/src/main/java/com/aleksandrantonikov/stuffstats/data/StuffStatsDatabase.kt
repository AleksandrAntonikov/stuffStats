package com.aleksandrantonikov.stuffstats.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aleksandrantonikov.stuffstats.domain.*
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val category: String, val priceMinor: Long?, val currency: String,
    val purchaseEpochDay: Long?, val notes: String, val isArchived: Boolean, val createdAt: Long,
)
@Entity(tableName = "item_metrics", foreignKeys = [ForeignKey(
    entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE,
)], indices = [Index("itemId")])
data class MetricEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val itemId: Long, val type: String, val unit: String)
data class ItemWithMetrics(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId") val metrics: List<MetricEntity>,
) {
    fun toDomain(): Item {
        val metric = metrics.single()
        return Item(item.id, item.name, Category.valueOf(item.category), item.priceMinor, item.currency,
            item.purchaseEpochDay?.let(LocalDate::ofEpochDay), UsageMetric(MetricType.valueOf(metric.type), metric.unit),
            item.notes, item.isArchived, item.createdAt)
    }
}
@Dao
interface ItemDao {
    @Transaction @Query("SELECT * FROM items ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<ItemWithMetrics>>
    @Transaction @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): ItemWithMetrics?
    @Insert suspend fun insert(item: ItemEntity): Long
    @Update suspend fun update(item: ItemEntity): Int
    @Insert suspend fun insertMetric(metric: MetricEntity)
    @Update suspend fun updateMetric(metric: MetricEntity)
    @Query("UPDATE items SET isArchived = :archived WHERE id = :id")
    suspend fun archive(id: Long, archived: Boolean): Int
}
@Database(
    entities = [ItemEntity::class, MetricEntity::class, UsageEventEntity::class, ItemPhotoEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class StuffStatsDatabase : RoomDatabase() {
    abstract fun items(): ItemDao
    abstract fun usageEvents(): UsageEventDao
    abstract fun itemPhotos(): ItemPhotoDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `usage_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemId` INTEGER NOT NULL, `value` TEXT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `notes` TEXT NOT NULL, `source` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_events_itemId` ON `usage_events` (`itemId`)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `item_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemId` INTEGER NOT NULL, `imagePath` TEXT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `usageValueAtPhoto` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)""",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_photos_itemId` ON `item_photos` (`itemId`)")
            }
        }
        @Volatile private var instance: StuffStatsDatabase? = null
        fun get(context: Context): StuffStatsDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, StuffStatsDatabase::class.java, "stuffstats.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}
class RoomItemRepository(private val db: StuffStatsDatabase) : ItemRepository {
    override fun observeItems() = db.items().observeAll().map { rows -> rows.map { it.toDomain() } }
    override suspend fun get(id: Long) = db.items().get(id)?.toDomain()
    override suspend fun save(item: Item): Long {
        ItemValidation.validate(item)
        return db.withTransaction {
            val old = if (item.id == 0L) null else checkNotNull(db.items().get(item.id))
            val entity = ItemEntity(item.id, item.name.trim(), item.category.name, item.priceMinor, item.currency,
                item.purchaseDate?.toEpochDay(), item.notes.trim(), old?.item?.isArchived ?: item.isArchived,
                old?.item?.createdAt ?: item.createdAt)
            val id = if (old == null) db.items().insert(entity) else {
                check(db.items().update(entity) == 1); item.id
            }
            val metric = MetricEntity(old?.metrics?.single()?.id ?: 0, id, item.metric.type.name, item.metric.unit.trim())
            if (old == null) db.items().insertMetric(metric) else db.items().updateMetric(metric)
            id
        }
    }
    override suspend fun archive(id: Long, archived: Boolean) { check(db.items().archive(id, archived) == 1) }
}

package com.aleksandrantonikov.stuffstats

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aleksandrantonikov.stuffstats.data.*
import com.aleksandrantonikov.stuffstats.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ItemDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), StuffStatsDatabase::class.java)
    private val name = "phase1-test.db"
    private fun open() = Room.databaseBuilder(context, StuffStatsDatabase::class.java, name)
        .addMigrations(StuffStatsDatabase.MIGRATION_1_2, StuffStatsDatabase.MIGRATION_2_3)
        .build()
    @Before fun prepare() { context.deleteDatabase(name) }
    @After fun cleanup() { context.deleteDatabase(name) }
    private fun sample() = Item(name = "Shoes", category = Category.FOOTWEAR, priceMinor = 10000,
        currency = "USD", purchaseDate = LocalDate.of(2026, 1, 1), metric = UsageMetric(MetricType.DISTANCE, "km"), notes = "Initial")
    @Test fun createEditArchiveRestoreSurviveReopen() = runBlocking {
        val first = open()
        val repository = RoomItemRepository(first)
        val id = repository.save(sample())
        val original = repository.get(id)!!
        repository.save(original.copy(name = "Updated", notes = "Edited", priceMinor = 12345))
        repository.archive(id, true)
        first.close()
        val second = open()
        try {
            val reopened = RoomItemRepository(second)
            val item = reopened.get(id)!!
            assertEquals("Updated", item.name)
            assertEquals(12345L, item.priceMinor)
            assertEquals(original.createdAt, item.createdAt)
            assertEquals(original.metric, item.metric)
            assertEquals(original.purchaseDate, item.purchaseDate)
            assertTrue(item.isArchived)
            reopened.archive(id, false)
            assertFalse(reopened.observeItems().first().single().isArchived)
            assertEquals(1, second.items().get(id)!!.metrics.size)
        } finally { second.close() }
    }
    @Test fun invalidSaveDoesNotLeavePartialRows() = runBlocking {
        val db = open()
        try {
            val repository = RoomItemRepository(db)
            assertTrue(runCatching { repository.save(sample().copy(name = "")) }.isFailure)
            assertTrue(repository.observeItems().first().isEmpty())
            assertTrue(runCatching { repository.save(sample().copy(id = 999)) }.isFailure)
            assertTrue(repository.observeItems().first().isEmpty())
        } finally { db.close() }
    }
    @Test fun versionOneDataSurvivesExplicitMigration() = runBlocking {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO items (id, name, category, priceMinor, currency, purchaseEpochDay, notes, isArchived, createdAt) VALUES (1, 'Legacy', 'OTHER', 2500, 'USD', NULL, '', 0, 1)")
            execSQL("INSERT INTO item_metrics (id, itemId, type, unit) VALUES (1, 1, 'USE_COUNT', 'uses')")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, StuffStatsDatabase.MIGRATION_1_2).close()
        val db = open()
        try {
            val repository = RoomItemRepository(db)
            assertEquals("Legacy", repository.get(1)!!.name)
            assertTrue(RoomUsageEventRepository(db).observeAll().first().isEmpty())
        } finally { db.close() }
    }

    @Test fun usageEventsCreateEditDeleteAndRemainExact() = runBlocking {
        val db = open()
        try {
            val itemId = RoomItemRepository(db).save(sample())
            val usage = RoomUsageEventRepository(db)
            val id = usage.save(UsageEvent(itemId = itemId, value = java.math.BigDecimal("5.700000"),
                date = LocalDate.of(2026, 9, 10), notes = "Walk"))
            val created = usage.get(id)!!
            assertEquals("5.7", created.value.toPlainString())
            usage.save(created.copy(value = java.math.BigDecimal("7.25"), notes = "Edited"))
            assertEquals("7.25", usage.get(id)!!.value.toPlainString())
            usage.delete(id)
            assertTrue(usage.observeAll().first().isEmpty())
        } finally { db.close() }
    }

    @Test fun versionTwoUsageDataSurvivesPhotoMigration() = runBlocking {
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO items (id, name, category, priceMinor, currency, purchaseEpochDay, notes, isArchived, createdAt) VALUES (1, 'Legacy', 'OTHER', 2500, 'USD', NULL, '', 0, 1)")
            execSQL("INSERT INTO item_metrics (id, itemId, type, unit) VALUES (1, 1, 'USE_COUNT', 'uses')")
            execSQL("INSERT INTO usage_events (id, itemId, value, dateEpochDay, notes, source, createdAt) VALUES (1, 1, '3.5', 1, '', 'MANUAL', 1)")
            close()
        }
        helper.runMigrationsAndValidate(name, 3, true, StuffStatsDatabase.MIGRATION_2_3).close()
        val db = open()
        try {
            assertEquals("Legacy", RoomItemRepository(db).get(1)!!.name)
            assertEquals("3.5", RoomUsageEventRepository(db).get(1)!!.value.toPlainString())
            assertTrue(RoomItemPhotoRepository(db).observeAll().first().isEmpty())
        } finally { db.close() }
    }

    @Test fun photosCreateDeleteAndKeepExactSnapshot() = runBlocking {
        val db = open()
        try {
            val itemId = RoomItemRepository(db).save(sample())
            val photos = RoomItemPhotoRepository(db)
            val id = photos.save(ItemPhoto(itemId = itemId, imagePath = "condition.jpg",
                date = LocalDate.of(2026, 9, 10), usageValueAtPhoto = java.math.BigDecimal("12.500000"), notes = "Wear"))
            val stored = photos.observeAll().first().single()
            assertEquals(id, stored.id)
            assertEquals("12.5", stored.usageValueAtPhoto.toPlainString())
            assertEquals("Wear", stored.notes)
            assertEquals(stored, photos.delete(id))
            assertTrue(photos.observeAll().first().isEmpty())
        } finally { db.close() }
    }
}

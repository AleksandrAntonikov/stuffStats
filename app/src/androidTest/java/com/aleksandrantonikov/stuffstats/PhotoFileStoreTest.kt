package com.aleksandrantonikov.stuffstats

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aleksandrantonikov.stuffstats.data.PhotoFileStore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoFileStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun cameraImageIsNormalizedIntoPrivateStorageAndDeleted() {
        runBlocking {
            val store = PhotoFileStore(context)
            val capture = store.newCaptureTarget()
            context.contentResolver.openOutputStream(capture.uri).use { output ->
                requireNotNull(output)
                val bitmap = Bitmap.createBitmap(2200, 1000, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(android.graphics.Color.BLUE)
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
                } finally {
                    bitmap.recycle()
                }
            }

            val imagePath = store.import(capture.uri)
            store.discardCapture(capture.token)
            val file = requireNotNull(store.fileFor(imagePath))
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            assertTrue(bounds.outWidth > 0 && bounds.outHeight > 0)
            assertTrue(maxOf(bounds.outWidth, bounds.outHeight) <= 2048)
            store.delete(imagePath)
            assertNull(store.fileFor(imagePath))
            File(context.cacheDir, "photo-captures/${capture.token}").delete()
        }
    }

    @Test
    fun staleCapturesArePrunedWithoutDeletingFreshCapture() {
        runBlocking {
            val store = PhotoFileStore(context)
            val stale = store.newCaptureTarget()
            val fresh = store.newCaptureTarget()
            val staleFile = File(context.cacheDir, "photo-captures/${stale.token}")
            val freshFile = File(context.cacheDir, "photo-captures/${fresh.token}")
            val now = System.currentTimeMillis()
            assertTrue(staleFile.setLastModified(now - 2_000))

            store.pruneStaleTemporaryFiles(nowMillis = now, maximumAgeMillis = 1_000)

            assertTrue(!staleFile.exists())
            assertNotNull(freshFile.takeIf(File::exists))
            store.discardCapture(fresh.token)
        }
    }
}

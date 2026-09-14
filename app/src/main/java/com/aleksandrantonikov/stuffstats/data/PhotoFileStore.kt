package com.aleksandrantonikov.stuffstats.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhotoCaptureTarget(val uri: Uri, val token: String)

class PhotoFileStore(private val context: Context) {
    private val photoDirectory = File(context.filesDir, "item_photos")
    private val captureDirectory = File(context.cacheDir, "photo-captures")

    fun newCaptureTarget(): PhotoCaptureTarget {
        captureDirectory.mkdirs()
        val token = "capture-${UUID.randomUUID()}.jpg"
        val file = File(captureDirectory, token)
        check(file.createNewFile())
        return PhotoCaptureTarget(
            uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
            token = token,
        )
    }

    fun discardCapture(token: String) {
        safeFile(captureDirectory, token)?.delete()
    }

    suspend fun import(source: Uri): String = withContext(Dispatchers.IO) {
        captureDirectory.mkdirs()
        photoDirectory.mkdirs()
        val sourceCopy = File.createTempFile("import-", ".image", captureDirectory)
        try {
            context.contentResolver.openInputStream(source).use { input ->
                requireNotNull(input) { "The selected image cannot be opened" }
                FileOutputStream(sourceCopy).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_SOURCE_BYTES) { "The selected image is too large" }
                        output.write(buffer, 0, read)
                    }
                }
            }
            val bitmap = decodeSampled(sourceCopy)
            val orientation = FileInputStream(sourceCopy).use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
            val oriented = orient(bitmap, orientation)
            val scaled = scaleDown(oriented, MAX_EDGE)
            val fileName = "photo-${UUID.randomUUID()}.jpg"
            val pending = File(photoDirectory, ".$fileName.pending")
            val destination = File(photoDirectory, fileName)
            try {
                FileOutputStream(pending).use { output ->
                    check(scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output))
                    output.fd.sync()
                }
                check(pending.renameTo(destination))
                fileName
            } finally {
                pending.delete()
                if (scaled !== oriented) scaled.recycle()
                if (oriented !== bitmap) oriented.recycle()
                bitmap.recycle()
            }
        } finally {
            sourceCopy.delete()
        }
    }

    suspend fun delete(imagePath: String) = withContext(Dispatchers.IO) {
        safeFile(photoDirectory, imagePath)?.delete()
    }

    suspend fun pruneStaleTemporaryFiles(
        nowMillis: Long = System.currentTimeMillis(),
        maximumAgeMillis: Long = MAX_TEMPORARY_FILE_AGE_MILLIS,
    ) = withContext(Dispatchers.IO) {
        require(maximumAgeMillis >= 0)
        captureDirectory.deleteFilesOlderThan(nowMillis, maximumAgeMillis)
        photoDirectory.deleteFilesOlderThan(nowMillis, maximumAgeMillis) { file ->
            file.name.startsWith(".") && file.name.endsWith(".pending")
        }
    }

    fun fileFor(imagePath: String): File? = safeFile(photoDirectory, imagePath)?.takeIf(File::isFile)

    private fun decodeSampled(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected file is not a supported image" }
        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) sample *= 2
        return requireNotNull(BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }))
    }

    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> { setRotate(180f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        return if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap, maximum: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maximum) return bitmap
        val factor = maximum.toFloat() / largest
        return bitmap.scale((bitmap.width * factor).toInt(), (bitmap.height * factor).toInt())
    }

    private fun safeFile(directory: File, name: String): File? {
        if (name.isBlank() || name.contains('/') || name.contains('\\')) return null
        val file = File(directory, name)
        return file.takeIf { it.canonicalFile.parentFile == directory.canonicalFile }
    }

    private fun File.deleteFilesOlderThan(
        nowMillis: Long,
        maximumAgeMillis: Long,
        include: (File) -> Boolean = { true },
    ) {
        listFiles()
            ?.asSequence()
            ?.filter(File::isFile)
            ?.filter(include)
            ?.filter { nowMillis - it.lastModified() >= maximumAgeMillis }
            ?.forEach(File::delete)
    }

    private companion object {
        const val MAX_SOURCE_BYTES = 40L * 1024 * 1024
        const val MAX_EDGE = 2048
        const val JPEG_QUALITY = 90
        const val MAX_TEMPORARY_FILE_AGE_MILLIS = 24L * 60 * 60 * 1000
    }
}

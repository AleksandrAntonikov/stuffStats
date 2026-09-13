@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aleksandrantonikov.stuffstats.ui.item

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.aleksandrantonikov.stuffstats.R
import com.aleksandrantonikov.stuffstats.data.PhotoCaptureTarget
import com.aleksandrantonikov.stuffstats.domain.Item
import com.aleksandrantonikov.stuffstats.domain.ItemPhoto
import com.aleksandrantonikov.stuffstats.domain.PhotoValidation
import com.aleksandrantonikov.stuffstats.domain.asPlainValue
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoEditor(
    item: Item,
    busy: Boolean,
    error: Boolean,
    back: () -> Unit,
    newCapture: () -> PhotoCaptureTarget?,
    discardCapture: (String?) -> Unit,
    save: (Uri, String?, LocalDate, String) -> Unit,
) {
    var selectedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var captureToken by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            discardCapture(captureToken)
            captureToken = null
            pendingCameraUri = null
            selectedUri = uri.toString()
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            selectedUri = pendingCameraUri
        } else {
            discardCapture(captureToken)
            captureToken = null
            pendingCameraUri = null
        }
    }
    val leave = {
        discardCapture(captureToken)
        back()
    }

    ItemFrame(stringResource(R.string.add_photo_title), leave, busy) {
        Text(item.name, style = MaterialTheme.typography.titleLarge)
        selectedUri?.let { PhotoImage(it.toUri(), Modifier.fillMaxWidth().height(260.dp)) }
        Button(
            onClick = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("choose_photo"),
        ) { Text(stringResource(R.string.choose_photo)) }
        OutlinedButton(
            onClick = {
                if (captureToken != null) selectedUri = null
                discardCapture(captureToken)
                newCapture()?.let { target ->
                    captureToken = target.token
                    pendingCameraUri = target.uri.toString()
                    camera.launch(target.uri)
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("take_photo"),
        ) { Text(stringResource(R.string.take_photo)) }
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text(stringResource(R.string.photo_date)) },
            placeholder = { Text("YYYY-MM-DD") },
            enabled = !busy,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        if (invalid) Text(stringResource(R.string.photo_validation_error), color = MaterialTheme.colorScheme.error)
        if (error) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
        Button(
            enabled = !busy && selectedUri != null,
            modifier = Modifier.fillMaxWidth().testTag("save_photo"),
            onClick = {
                val parsed = runCatching {
                    val parsedDate = LocalDate.parse(date.trim())
                    PhotoValidation.validate(
                        ItemPhoto(
                            itemId = item.id,
                            imagePath = "pending.jpg",
                            date = parsedDate,
                            usageValueAtPhoto = java.math.BigDecimal.ZERO,
                            notes = notes,
                        ),
                    )
                    parsedDate
                }
                invalid = parsed.isFailure
                parsed.getOrNull()?.let { parsedDate ->
                    selectedUri?.toUri()?.let { source -> save(source, captureToken, parsedDate, notes) }
                }
            },
        ) { Text(stringResource(if (busy) R.string.saving else R.string.save)) }
    }
}

@Composable
fun PhotoHistory(
    item: Item,
    photos: List<ItemPhoto>,
    busy: Boolean,
    error: Boolean,
    back: () -> Unit,
    add: () -> Unit,
    delete: (ItemPhoto) -> Unit,
    photoFile: (String) -> File?,
) {
    var deleteCandidate by rememberSaveable { mutableStateOf<Long?>(null) }
    ItemFrame(stringResource(R.string.photo_history_title), back, busy) {
        Text(item.name, style = MaterialTheme.typography.titleLarge)
        Button(onClick = add, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_photo))
        }
        if (error) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
        if (photos.isEmpty()) {
            Text(stringResource(R.string.no_photos))
        } else {
            photos.forEach { photo ->
                Card(Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StoredPhotoImage(photo.imagePath, photoFile, Modifier.fillMaxWidth().height(240.dp))
                        Text("${photo.usageValueAtPhoto.asPlainValue()} ${item.metric.unit}", style = MaterialTheme.typography.titleLarge)
                        Text(photo.date.toString())
                        if (photo.notes.isNotEmpty()) Text(photo.notes)
                        TextButton(onClick = { deleteCandidate = photo.id }, enabled = !busy) {
                            Text(stringResource(R.string.delete_photo))
                        }
                    }
                }
            }
        }
    }
    val candidate = photos.find { it.id == deleteCandidate }
    if (candidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.delete_photo)) },
            text = { Text(stringResource(R.string.delete_photo_explanation)) },
            confirmButton = {
                TextButton(onClick = { deleteCandidate = null; delete(candidate) }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun StoredPhotoImage(imagePath: String, photoFile: (String) -> File?, modifier: Modifier = Modifier) {
    val file = photoFile(imagePath)
    val bitmap by produceState<ImageBitmap?>(null, file?.path) {
        value = file?.let { withContext(Dispatchers.IO) { decodeSampled(it)?.asImageBitmap() } }
    }
    PhotoImageContent(bitmap, modifier)
}

@Composable
private fun PhotoImage(uri: Uri, modifier: Modifier = Modifier) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (bounds.outWidth / sample > 1000 || bounds.outHeight / sample > 1000) sample *= 2
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }
        }
    }
    PhotoImageContent(bitmap, modifier)
}

@Composable
private fun PhotoImageContent(bitmap: ImageBitmap?, modifier: Modifier) {
    if (bitmap == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    } else {
        Image(bitmap, contentDescription = stringResource(R.string.item_photo), modifier = modifier, contentScale = ContentScale.Crop)
    }
}

private fun decodeSampled(file: File): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 1000 || bounds.outHeight / sample > 1000) sample *= 2
    return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
}

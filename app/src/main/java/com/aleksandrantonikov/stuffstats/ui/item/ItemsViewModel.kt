package com.aleksandrantonikov.stuffstats.ui.item

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aleksandrantonikov.stuffstats.data.PhotoCaptureTarget
import com.aleksandrantonikov.stuffstats.data.PhotoFileStore
import com.aleksandrantonikov.stuffstats.domain.*
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ItemsState(
    val loading: Boolean = true,
    val items: List<Item> = emptyList(),
    val events: List<UsageEvent> = emptyList(),
    val photos: List<ItemPhoto> = emptyList(),
    val failed: Boolean = false,
) {
    fun eventsFor(itemId: Long) = events.filter { it.itemId == itemId }
    fun photosFor(itemId: Long) = photos.filter { it.itemId == itemId }
    fun statsFor(item: Item) = UsageCalculations.forItem(item, events)
}

class ItemsViewModel(
    private val repository: ItemRepository,
    private val usageRepository: UsageEventRepository,
    private val photoRepository: ItemPhotoRepository,
    private val photoFiles: PhotoFileStore,
) : ViewModel() {
    val state = combine(repository.observeItems(), usageRepository.observeAll(), photoRepository.observeAll()) { items, events, photos ->
        ItemsState(loading = false, items = items, events = events, photos = photos)
    }
        .catch { emit(ItemsState(loading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ItemsState())
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _error = MutableStateFlow(false)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = false }
    fun save(item: Item, done: () -> Unit) = mutate({ repository.save(item) }, done)
    fun archive(item: Item, done: () -> Unit) = mutate({ repository.archive(item.id, !item.isArchived) }, done)
    fun saveUsage(event: UsageEvent, done: () -> Unit) = mutate({ usageRepository.save(event) }, done)
    fun deleteUsage(event: UsageEvent, done: () -> Unit) = mutate({ usageRepository.delete(event.id) }, done)
    fun newPhotoCapture(): PhotoCaptureTarget? = runCatching { photoFiles.newCaptureTarget() }
        .onFailure { _error.value = true }
        .getOrNull()
    fun discardPhotoCapture(token: String?) {
        token?.let(photoFiles::discardCapture)
    }
    fun photoFile(imagePath: String): File? = photoFiles.fileFor(imagePath)
    fun savePhoto(item: Item, source: Uri, captureToken: String?, date: LocalDate, notes: String, done: () -> Unit) {
        val snapshot = state.value.statsFor(item).totalUsage
        mutate(
            action = {
                var imagePath: String? = null
                try {
                    imagePath = photoFiles.import(source)
                    photoRepository.save(
                        ItemPhoto(
                            itemId = item.id,
                            imagePath = imagePath,
                            date = date,
                            usageValueAtPhoto = snapshot,
                            notes = notes,
                        ),
                    )
                    discardPhotoCapture(captureToken)
                } catch (error: Exception) {
                    imagePath?.let { photoFiles.delete(it) }
                    throw error
                }
            },
            done = done,
        )
    }
    fun deletePhoto(photo: ItemPhoto, done: () -> Unit) = mutate(
        action = {
            val deleted = photoRepository.delete(photo.id)
            photoFiles.delete(deleted.imagePath)
        },
        done = done,
    )
    private fun mutate(action: suspend () -> Unit, done: () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        _error.value = false
        viewModelScope.launch {
            try { action(); done() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _error.value = true }
            finally { _saving.value = false }
        }
    }
}

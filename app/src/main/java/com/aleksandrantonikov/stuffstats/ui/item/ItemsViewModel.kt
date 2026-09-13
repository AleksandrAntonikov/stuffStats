package com.aleksandrantonikov.stuffstats.ui.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aleksandrantonikov.stuffstats.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ItemsState(
    val loading: Boolean = true,
    val items: List<Item> = emptyList(),
    val events: List<UsageEvent> = emptyList(),
    val failed: Boolean = false,
) {
    fun eventsFor(itemId: Long) = events.filter { it.itemId == itemId }
    fun statsFor(item: Item) = UsageCalculations.forItem(item, events)
}

class ItemsViewModel(
    private val repository: ItemRepository,
    private val usageRepository: UsageEventRepository,
) : ViewModel() {
    val state = combine(repository.observeItems(), usageRepository.observeAll()) { items, events ->
        ItemsState(loading = false, items = items, events = events)
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

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.aleksandrantonikov.stuffstats.ui.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aleksandrantonikov.stuffstats.R
import com.aleksandrantonikov.stuffstats.domain.*
import java.time.LocalDate
import java.util.Locale

@Composable
fun ItemList(state: ItemsState, add: () -> Unit, open: (Long) -> Unit) {
    var archived by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) },
        floatingActionButton = { FloatingActionButton(onClick = add) { Text(stringResource(R.string.add_item), Modifier.padding(16.dp)) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !archived, onClick = { archived = false }, label = { Text(stringResource(R.string.active_items)) })
                FilterChip(selected = archived, onClick = { archived = true }, label = { Text(stringResource(R.string.archived_items)) })
            }
            when {
                state.loading -> CircularProgressIndicator()
                state.failed -> Text(stringResource(R.string.storage_error))
                else -> {
                    val visible = state.items.filter { it.isArchived == archived }
                    if (visible.isEmpty()) Text(stringResource(R.string.home_empty_title), Modifier.padding(vertical = 24.dp))
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visible, key = { it.id }) { item ->
                            val stats = state.statsFor(item)
                            Card(onClick = { open(item.id) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.titleLarge)
                                    Text(categoryLabel(item.category))
                                    Text("${stats.totalUsage.asPlainValue()} ${item.metric.unit}")
                                    stats.costPerUnit?.let { Text("${it.toPlainString()} ${item.currency} / ${item.metric.unit}") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ItemDetails(
    item: Item,
    events: List<UsageEvent>,
    stats: ItemUsageStats,
    busy: Boolean,
    error: Boolean,
    back: () -> Unit,
    edit: () -> Unit,
    addUsage: () -> Unit,
    editUsage: (Long) -> Unit,
    archive: () -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    ItemFrame(stringResource(R.string.item_details_title), back, busy) {
        Text(item.name, style = MaterialTheme.typography.headlineMedium)
        Text(categoryLabel(item.category))
        Text("${stats.totalUsage.asPlainValue()} ${item.metric.unit}", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.testTag("total_usage"))
        Text(
            stats.costPerUnit?.let { "${it.toPlainString()} ${item.currency} / ${item.metric.unit}" }
                ?: stringResource(R.string.cost_per_unit_unavailable),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("cost_per_unit"),
        )
        Text(stringResource(R.string.recorded_events, stats.eventCount))
        Text(stringResource(R.string.purchase_price) + ": " + if (item.priceMinor == null) "—" else "${ItemValidation.priceText(item)} ${item.currency}")
        Text(stringResource(R.string.purchase_date) + ": " + (item.purchaseDate?.toString() ?: "—"))
        if (item.notes.isNotEmpty()) Text(item.notes)
        if (error) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
        Button(onClick = addUsage, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_usage)) }
        Text(stringResource(R.string.usage_history), style = MaterialTheme.typography.titleLarge)
        if (events.isEmpty()) {
            Text(stringResource(R.string.no_usage_events))
        } else {
            events.forEach { event ->
                Card(onClick = { editUsage(event.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("+${event.value.asPlainValue()} ${item.metric.unit}", style = MaterialTheme.typography.titleMedium)
                        Text(event.date.toString())
                        if (event.notes.isNotEmpty()) Text(event.notes)
                    }
                }
            }
        }
        Button(onClick = edit, enabled = !busy) { Text(stringResource(R.string.edit_item)) }
        OutlinedButton(onClick = { confirm = true }, enabled = !busy) { Text(stringResource(if (item.isArchived) R.string.restore_item else R.string.archive_item)) }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(if (item.isArchived) R.string.restore_item else R.string.archive_item)) },
        text = { Text(stringResource(R.string.archive_explanation)) },
        confirmButton = { TextButton(onClick = { confirm = false; archive() }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } })
}
@Composable
fun ItemEditor(item: Item?, busy: Boolean, error: Boolean, back: () -> Unit, save: (Item) -> Unit) {
    var name by rememberSaveable { mutableStateOf(item?.name.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(item?.category?.name ?: Category.OTHER.name) }
    var price by rememberSaveable { mutableStateOf(item?.let(ItemValidation::priceText).orEmpty()) }
    var currency by rememberSaveable { mutableStateOf(item?.currency ?: "USD") }
    var date by rememberSaveable { mutableStateOf(item?.purchaseDate?.toString().orEmpty()) }
    var metric by rememberSaveable { mutableStateOf(item?.metric?.type?.name ?: MetricType.DISTANCE.name) }
    var unit by rememberSaveable { mutableStateOf(item?.metric?.unit ?: "km") }
    var notes by rememberSaveable { mutableStateOf(item?.notes.orEmpty()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    ItemFrame(stringResource(if (item == null) R.string.add_item_title else R.string.edit_item_title), back, busy) {
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.item_name)) }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("name"), singleLine = true)
        Choice(stringResource(R.string.category), Category.entries.map { it.name to categoryLabel(it) }, category, !busy) { category = it }
        OutlinedTextField(price, { price = it }, label = { Text(stringResource(R.string.purchase_price)) }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("price"), singleLine = true)
        OutlinedTextField(currency, { currency = it.uppercase(Locale.ROOT) }, label = { Text(stringResource(R.string.currency_code)) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(date, { date = it }, label = { Text(stringResource(R.string.purchase_date)) }, placeholder = { Text("YYYY-MM-DD") }, enabled = !busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Choice(stringResource(R.string.usage_metric), MetricType.entries.map { it.name to metricLabel(it) }, metric, !busy) {
            metric = it; unit = MetricType.valueOf(it).unit
        }
        OutlinedTextField(unit, { unit = it }, label = { Text(stringResource(R.string.unit)) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.notes)) }, enabled = !busy, modifier = Modifier.fillMaxWidth())
        if (invalid) Text(stringResource(R.string.validation_error), color = MaterialTheme.colorScheme.error)
        if (error) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
        Button(enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("save"), onClick = {
            val result = runCatching {
                Item(id = item?.id ?: 0, name = name.trim(), category = Category.valueOf(category),
                    priceMinor = ItemValidation.priceMinor(price, currency.trim()), currency = currency.trim(),
                    purchaseDate = date.trim().takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
                    metric = UsageMetric(MetricType.valueOf(metric), unit.trim()), notes = notes,
                    isArchived = item?.isArchived ?: false, createdAt = item?.createdAt ?: System.currentTimeMillis())
                    .also(ItemValidation::validate)
            }
            invalid = result.isFailure
            result.getOrNull()?.let(save)
        }) { Text(stringResource(if (busy) R.string.saving else R.string.save)) }
    }
}
@Composable
fun ItemFrame(title: String, back: () -> Unit, busy: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { TextButton(onClick = back, enabled = !busy) { Text(stringResource(R.string.back)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
private fun Choice(label: String, options: List<Pair<String, String>>, selected: String, enabled: Boolean, choose: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text("$label: ${options.first { it.first == selected }.second}") }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { (value, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { choose(value); expanded = false }) }
        }
    }
}
@Composable
fun categoryLabel(value: Category): String = stringResource(when (value) {
    Category.FOOTWEAR -> R.string.category_footwear
    Category.CLOTHING -> R.string.category_clothing
    Category.EQUIPMENT -> R.string.category_equipment
    Category.ELECTRONICS -> R.string.category_electronics
    Category.OTHER -> R.string.category_other
})
@Composable
fun metricLabel(value: MetricType): String = stringResource(when (value) {
    MetricType.DISTANCE -> R.string.metric_distance
    MetricType.USE_COUNT -> R.string.metric_uses
    MetricType.WEAR_COUNT -> R.string.metric_wears
    MetricType.WASH_COUNT -> R.string.metric_washes
    MetricType.HOURS -> R.string.metric_hours
    MetricType.CUSTOM -> R.string.metric_custom
})

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aleksandrantonikov.stuffstats.ui.item

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.aleksandrantonikov.stuffstats.R
import com.aleksandrantonikov.stuffstats.domain.Item
import com.aleksandrantonikov.stuffstats.domain.MetricType
import com.aleksandrantonikov.stuffstats.domain.UsageEvent
import com.aleksandrantonikov.stuffstats.domain.UsageSource
import com.aleksandrantonikov.stuffstats.domain.UsageValidation
import com.aleksandrantonikov.stuffstats.domain.asPlainValue
import java.time.LocalDate

@Composable
fun UsageEditor(
    item: Item,
    event: UsageEvent?,
    busy: Boolean,
    error: Boolean,
    back: () -> Unit,
    save: (UsageEvent) -> Unit,
    delete: (() -> Unit)? = null,
) {
    var value by rememberSaveable { mutableStateOf(event?.value?.asPlainValue().orEmpty()) }
    var date by rememberSaveable { mutableStateOf(event?.date?.toString() ?: LocalDate.now().toString()) }
    var notes by rememberSaveable { mutableStateOf(event?.notes.orEmpty()) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val counterMetric = item.metric.type in setOf(MetricType.USE_COUNT, MetricType.WEAR_COUNT, MetricType.WASH_COUNT)

    ItemFrame(stringResource(if (event == null) R.string.add_usage_title else R.string.edit_usage_title), back, busy) {
        Text(item.name)
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(stringResource(R.string.usage_value, item.metric.unit)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("usage_value"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
        if (counterMetric) {
            OutlinedButton(onClick = { value = "1" }, enabled = !busy) { Text(stringResource(R.string.use_plus_one)) }
        }
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text(stringResource(R.string.usage_date)) },
            placeholder = { Text("YYYY-MM-DD") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("usage_date"),
            singleLine = true,
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(R.string.notes)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("usage_notes"),
        )
        if (invalid) Text(stringResource(R.string.usage_validation_error))
        if (error) Text(stringResource(R.string.storage_error))
        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag("save_usage"),
            onClick = {
                val result = runCatching {
                    UsageEvent(
                        id = event?.id ?: 0,
                        itemId = item.id,
                        value = UsageValidation.value(value),
                        date = LocalDate.parse(date.trim()),
                        notes = notes,
                        source = event?.source ?: UsageSource.MANUAL,
                        createdAt = event?.createdAt ?: System.currentTimeMillis(),
                    ).also(UsageValidation::validate)
                }
                invalid = result.isFailure
                result.getOrNull()?.let(save)
            },
        ) { Text(stringResource(if (busy) R.string.saving else R.string.save)) }
        if (delete != null) {
            OutlinedButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.delete_usage))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_usage)) },
            text = { Text(stringResource(R.string.delete_usage_explanation)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; delete?.invoke() }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

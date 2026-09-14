@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aleksandrantonikov.stuffstats.ui.item

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.PermissionController
import com.aleksandrantonikov.stuffstats.R
import com.aleksandrantonikov.stuffstats.data.HealthConnectDistanceSource
import com.aleksandrantonikov.stuffstats.domain.DistanceImportState
import com.aleksandrantonikov.stuffstats.domain.DistanceImportStatus
import com.aleksandrantonikov.stuffstats.domain.Item
import com.aleksandrantonikov.stuffstats.domain.asPlainValue
import java.time.LocalDate

@Composable
fun DistanceImportScreen(
    item: Item,
    state: DistanceImportState,
    back: () -> Unit,
    prepare: () -> Unit,
    permissionResult: (Boolean) -> Unit,
    import: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    val displayState = state.takeIf { it.itemId == item.id } ?: DistanceImportState()
    var dateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var invalidDate by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted -> permissionResult(HealthConnectDistanceSource.REQUIRED_PERMISSIONS.all(granted::contains)) }

    LaunchedEffect(item.id) { prepare() }

    ItemFrame(
        title = stringResource(R.string.import_distance_title),
        back = back,
        busy = displayState.status in setOf(DistanceImportStatus.CHECKING, DistanceImportStatus.IMPORTING),
    ) {
        Text(item.name)
        Text(stringResource(R.string.import_distance_explanation, item.metric.unit))
        Text(stringResource(R.string.import_distance_privacy))
        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it; invalidDate = false },
            label = { Text(stringResource(R.string.import_distance_date)) },
            placeholder = { Text("YYYY-MM-DD") },
            modifier = Modifier.fillMaxWidth().testTag("distance_import_date"),
            singleLine = true,
        )

        when (displayState.status) {
            DistanceImportStatus.IDLE, DistanceImportStatus.CHECKING -> CircularProgressIndicator()
            DistanceImportStatus.PERMISSION_REQUIRED -> {
                Text(stringResource(R.string.health_permission_required))
                Button(
                    onClick = { permissionLauncher.launch(HealthConnectDistanceSource.REQUIRED_PERMISSIONS) },
                    modifier = Modifier.fillMaxWidth().testTag("request_health_permission"),
                ) { Text(stringResource(R.string.allow_health_distance)) }
            }
            DistanceImportStatus.PROVIDER_UPDATE_REQUIRED -> {
                Text(stringResource(R.string.health_connect_update_required))
                Button(
                    onClick = { runCatching { context.startActivity(HealthConnectDistanceSource.providerUpdateIntent(context)) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.install_health_connect)) }
            }
            DistanceImportStatus.UNAVAILABLE -> Text(stringResource(R.string.health_connect_unavailable))
            DistanceImportStatus.UNSUPPORTED_UNIT -> Text(stringResource(R.string.health_unit_unsupported, item.metric.unit))
            DistanceImportStatus.INVALID_DATE -> Text(stringResource(R.string.import_distance_invalid_date))
            DistanceImportStatus.NO_DATA -> Text(stringResource(R.string.import_distance_no_data, displayState.date.toString()))
            DistanceImportStatus.IMPORTED -> Text(
                stringResource(
                    R.string.import_distance_success,
                    checkNotNull(displayState.importedValue).asPlainValue(),
                    item.metric.unit,
                    displayState.date.toString(),
                ),
                modifier = Modifier.testTag("distance_import_success"),
            )
            DistanceImportStatus.FAILED -> Text(stringResource(R.string.import_distance_failed))
            DistanceImportStatus.READY, DistanceImportStatus.IMPORTING -> Unit
        }

        if (displayState.status in setOf(
                DistanceImportStatus.READY,
                DistanceImportStatus.IMPORTED,
                DistanceImportStatus.NO_DATA,
                DistanceImportStatus.INVALID_DATE,
                DistanceImportStatus.FAILED,
            )
        ) {
            Button(
                modifier = Modifier.fillMaxWidth().testTag("run_distance_import"),
                onClick = {
                    val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                    invalidDate = date == null || date.isAfter(LocalDate.now())
                    date?.takeUnless(LocalDate.now()::isBefore)?.let(import)
                },
            ) { Text(stringResource(if (displayState.status == DistanceImportStatus.IMPORTED) R.string.refresh_distance else R.string.import_distance)) }
        }
        if (invalidDate) Text(stringResource(R.string.import_distance_invalid_date))
        if (displayState.status == DistanceImportStatus.IMPORTING) {
            CircularProgressIndicator()
            Text(stringResource(R.string.importing_distance))
        }
    }
}

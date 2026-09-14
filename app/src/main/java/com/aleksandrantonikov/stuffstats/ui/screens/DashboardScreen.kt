@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aleksandrantonikov.stuffstats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.aleksandrantonikov.stuffstats.R
import com.aleksandrantonikov.stuffstats.domain.CostLeader
import com.aleksandrantonikov.stuffstats.domain.DashboardCalculations
import com.aleksandrantonikov.stuffstats.domain.asPlainValue
import com.aleksandrantonikov.stuffstats.ui.item.ItemsState
import com.aleksandrantonikov.stuffstats.ui.item.metricLabel

@Composable
fun DashboardScreen(state: ItemsState, back: () -> Unit, openItem: (Long) -> Unit) {
    val dashboard = remember(state.items, state.events, state.photos) {
        DashboardCalculations.from(state.items, state.events, state.photos)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard)) },
                navigationIcon = { TextButton(onClick = back) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        when {
            state.loading -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { CircularProgressIndicator() }
            state.failed -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) { Text(stringResource(R.string.storage_error)) }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text(stringResource(R.string.dashboard_overview), style = MaterialTheme.typography.headlineSmall) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(dashboard.activeItemCount.toString(), stringResource(R.string.active_items), Modifier.weight(1f))
                        SummaryCard(dashboard.archivedItemCount.toString(), stringResource(R.string.archived_items), Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(dashboard.eventCount.toString(), stringResource(R.string.usage_records), Modifier.weight(1f))
                        SummaryCard(dashboard.photoCount.toString(), stringResource(R.string.condition_photos_count), Modifier.weight(1f))
                    }
                }
                item { DashboardSection(stringResource(R.string.purchase_totals)) }
                if (dashboard.purchaseTotals.isEmpty()) {
                    item { Text(stringResource(R.string.no_purchase_data), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    dashboard.purchaseTotals.forEach { total ->
                        item(total.currency) {
                            SummaryCard("${total.amount.asPlainValue()} ${total.currency}", stringResource(R.string.active_purchase_total))
                        }
                    }
                }
                item { DashboardSection(stringResource(R.string.most_tracked_item)) }
                dashboard.mostTrackedItem?.let { tracked ->
                    item {
                        ElevatedCard(onClick = { openItem(tracked.itemId) }, modifier = Modifier.fillMaxWidth().testTag("most_tracked_item")) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(tracked.name, style = MaterialTheme.typography.titleLarge)
                                Text("${tracked.totalUsage.asPlainValue()} ${tracked.unit}", style = MaterialTheme.typography.headlineSmall)
                                Text(pluralStringResource(R.plurals.recorded_entries, tracked.eventCount, tracked.eventCount))
                            }
                        }
                    }
                } ?: item { Text(stringResource(R.string.no_activity_data), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { DashboardSection(stringResource(R.string.cost_leaders)) }
                if (dashboard.costLeaders.isEmpty()) {
                    item { Text(stringResource(R.string.no_cost_data), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    dashboard.costLeaders.forEach { leader ->
                        item("${leader.currency}-${leader.metricType}-${leader.unit}") { CostLeaderCard(leader, openItem) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun CostLeaderCard(leader: CostLeader, openItem: (Long) -> Unit) {
    ElevatedCard(onClick = { openItem(leader.itemId) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(leader.name, style = MaterialTheme.typography.titleMedium)
            Text("${leader.costPerUnit.asPlainValue()} ${leader.currency} / ${leader.unit}", style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.cost_leader_group, metricLabel(leader.metricType), leader.unit))
        }
    }
}

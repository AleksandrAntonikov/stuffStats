@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aleksandrantonikov.stuffstats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aleksandrantonikov.stuffstats.R
import com.aleksandrantonikov.stuffstats.navigation.AppDestination

@Composable
fun PlaceholderScreen(
    destination: AppDestination,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(destination.titleRes)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.phase_zero_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ItemDetailsPlaceholder(
    onBack: () -> Unit,
    onAddUsage: () -> Unit,
    onViewPhotos: () -> Unit,
    onEditItem: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.item_details_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.phase_zero_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAddUsage,
            ) {
                Text(stringResource(R.string.add_usage))
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onViewPhotos,
            ) {
                Text(stringResource(R.string.view_photos))
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onEditItem,
            ) {
                Text(stringResource(R.string.edit_item))
            }
        }
    }
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aleksandrantonikov.stuffstats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aleksandrantonikov.stuffstats.R

@Composable
fun HomeScreen(
    onAddItem: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenDashboard: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.home_title)) })
        },
    ) { contentPadding ->
        EmptyHomeContent(
            contentPadding = contentPadding,
            onAddItem = onAddItem,
            onOpenPreview = onOpenPreview,
            onOpenDashboard = onOpenDashboard,
        )
    }
}

@Composable
private fun EmptyHomeContent(
    contentPadding: PaddingValues,
    onAddItem: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenDashboard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_empty_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onAddItem,
        ) {
            Text(stringResource(R.string.add_item))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenPreview,
        ) {
            Text(stringResource(R.string.view_demo_item))
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenDashboard,
        ) {
            Text(stringResource(R.string.dashboard))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.phase_zero_label),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

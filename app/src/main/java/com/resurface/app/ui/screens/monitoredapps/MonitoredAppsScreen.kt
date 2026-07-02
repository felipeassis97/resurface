package com.resurface.app.ui.screens.monitoredapps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resurface.app.data.monitored.MonitoredApp
import com.resurface.app.data.monitored.SurfaceType
import com.resurface.app.ui.theme.ResurfaceTheme

@Composable
fun MonitoredAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MonitoredAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MonitoredAppsContent(
        state = state,
        onBack = onBack,
        onToggle = viewModel::toggle,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonitoredAppsContent(
    state: MonitoredAppsUiState,
    onBack: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Monitored apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.rows.isEmpty() -> EmptyState()
                else -> {
                    Text(
                        text = if (state.locked) {
                            "Selection is locked for the study."
                        } else {
                            "${state.selectedCount} app(s) monitored. Keep at least one."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.rows, key = { it.app.packageName }) { row ->
                            AppRow(row = row, locked = state.locked, onToggle = onToggle)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    row: MonitoredAppRow,
    locked: Boolean,
    onToggle: (String, Boolean) -> Unit,
) {
    // Disabled when locked (study) or when this is the last selected app (enforce ≥1).
    val toggleEnabled = !locked && !row.isLastSelected
    ListItem(
        headlineContent = { Text(row.app.displayName) },
        supportingContent = {
            val surface = when (row.app.surface) {
                SurfaceType.FEED -> "Feed"
                SurfaceType.SHORT_VIDEO -> "Short video"
                SurfaceType.MIXED -> "Feed + short video"
            }
            val install = if (row.installed) "installed" else "not installed"
            Text("$surface · $install")
        },
        trailingContent = {
            Switch(
                checked = row.selected,
                onCheckedChange = { onToggle(row.app.packageName, it) },
                enabled = toggleEnabled,
            )
        },
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Select at least one app to monitor before leaving.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MonitoredAppsPreview() {
    ResurfaceTheme {
        MonitoredAppsContent(
            state = MonitoredAppsUiState(
                rows = listOf(
                    MonitoredAppRow(
                        MonitoredApp("com.instagram.android", "Instagram", SurfaceType.MIXED),
                        selected = true, installed = true, isLastSelected = false,
                    ),
                    MonitoredAppRow(
                        MonitoredApp("com.twitter.android", "X", SurfaceType.FEED),
                        selected = false, installed = false, isLastSelected = false,
                    ),
                ),
                selectedCount = 1,
                locked = false,
            ),
            onBack = {},
            onToggle = { _, _ -> },
        )
    }
}

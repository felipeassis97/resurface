package com.resurface.app.ui.screens.monitoredapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resurface.app.config.StudyMode
import com.resurface.app.data.monitored.AppInstallChecker
import com.resurface.app.data.monitored.CuratedApps
import com.resurface.app.data.monitored.MonitoredApp
import com.resurface.app.data.monitored.MonitoredAppsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A curated app plus its live selection/installed status for the picker. */
data class MonitoredAppRow(
    val app: MonitoredApp,
    val selected: Boolean,
    val installed: Boolean,
    /** True when this is the only selected app: its toggle is disabled to enforce ≥1. */
    val isLastSelected: Boolean,
)

data class MonitoredAppsUiState(
    val rows: List<MonitoredAppRow> = emptyList(),
    val selectedCount: Int = 0,
    /** Study lock: all toggles disabled and writes rejected by the repository. */
    val locked: Boolean = false,
)

@HiltViewModel
class MonitoredAppsViewModel @Inject constructor(
    private val repo: MonitoredAppsRepository,
    private val installChecker: AppInstallChecker,
    studyMode: StudyMode,
) : ViewModel() {

    val uiState: StateFlow<MonitoredAppsUiState> =
        combine(repo.selection, studyMode.isSelectionLocked) { selected, locked ->
            val rows = CuratedApps.all.map { app ->
                MonitoredAppRow(
                    app = app,
                    selected = app.packageName in selected,
                    installed = installChecker.isInstalled(app.packageName),
                    isLastSelected = selected.size == 1 && app.packageName in selected,
                )
            }
            MonitoredAppsUiState(rows = rows, selectedCount = selected.size, locked = locked)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitoredAppsUiState())

    /** Toggle an app. The repository is authoritative; the UI never flips optimistically. */
    fun toggle(packageName: String, selected: Boolean) {
        viewModelScope.launch {
            repo.setSelected(packageName, selected)
            // Failures (last app, locked, non-catalog) leave the persisted set unchanged;
            // uiState re-emits from repo.selection, so no optimistic revert is needed.
        }
    }
}

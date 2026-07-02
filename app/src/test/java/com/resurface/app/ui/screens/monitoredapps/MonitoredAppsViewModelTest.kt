package com.resurface.app.ui.screens.monitoredapps

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.resurface.app.MainDispatcherRule
import com.resurface.app.config.StudyMode
import com.resurface.app.data.monitored.AppInstallChecker
import com.resurface.app.data.monitored.MonitoredAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoredAppsViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private val allInstalled = object : AppInstallChecker {
        override fun isInstalled(packageName: String) = true
    }
    private val ig = "com.instagram.android"

    private fun store(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) {
            tmp.newFile("ds-${System.nanoTime()}.preferences_pb")
        }

    @Test fun toggleSelectsApp() = runTest {
        val ds = store(backgroundScope)
        val vm = MonitoredAppsViewModel(MonitoredAppsRepository(ds), allInstalled, StudyMode(ds))
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()

        vm.toggle(ig, selected = true)
        runCurrent()

        val state = vm.uiState.first { it.selectedCount == 1 }
        assertTrue(state.rows.first { it.app.packageName == ig }.selected)
    }

    @Test fun cannotDeselectLastApp() = runTest {
        val ds = store(backgroundScope)
        val repo = MonitoredAppsRepository(ds)
        val vm = MonitoredAppsViewModel(repo, allInstalled, StudyMode(ds))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.toggle(ig, selected = true)
        runCurrent()

        vm.toggle(ig, selected = false)
        runCurrent()

        // Repository rejected the empty write, so IG remains and is flagged last-selected.
        assertEquals(setOf(ig), repo.selection.first())
        val row = vm.uiState.value.rows.first { it.app.packageName == ig }
        assertTrue(row.isLastSelected)
    }

    @Test fun lockReflectedInUiState() = runTest {
        val ds = store(backgroundScope)
        val studyMode = StudyMode(ds)
        val vm = MonitoredAppsViewModel(MonitoredAppsRepository(ds), allInstalled, studyMode)
        backgroundScope.launch { vm.uiState.collect {} }
        runCurrent()
        assertFalse(vm.uiState.value.locked)

        studyMode.setStudyStarted(true)
        runCurrent()

        // Locked only in a study build; assert against the compile-time flag.
        assertEquals(com.resurface.app.config.BuildScope.STUDY, vm.uiState.value.locked)
    }
}

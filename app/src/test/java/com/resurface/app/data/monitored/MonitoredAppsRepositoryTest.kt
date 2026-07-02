package com.resurface.app.data.monitored

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.resurface.app.config.StudyMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class MonitoredAppsRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun store(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) {
            tmp.newFile("ds-${System.nanoTime()}.preferences_pb")
        }

    private val ig = "com.instagram.android"
    private val x = "com.twitter.android"

    @Test fun persistsAndRestores() = runTest {
        val ds = store(backgroundScope)
        val repo = MonitoredAppsRepository(ds)

        assertTrue(repo.setSelected(ig, selected = true).isSuccess)

        assertEquals(setOf(ig), repo.selection.first())
    }

    @Test fun rejectsNonCatalogPackage() = runTest {
        val repo = MonitoredAppsRepository(store(backgroundScope))

        val result = repo.setSelected("com.example.unknown", selected = true)

        assertTrue(result.isFailure)
        assertEquals(emptySet<String>(), repo.selection.first())
    }

    @Test fun rejectsEmptyingTheSet() = runTest {
        val repo = MonitoredAppsRepository(store(backgroundScope))
        repo.setSelected(ig, selected = true).getOrThrow()

        val result = repo.setSelected(ig, selected = false)

        assertTrue(result.isFailure)
        assertEquals(setOf(ig), repo.selection.first())
    }

    @Test fun lockRejectsWrite() = runTest {
        val ds = store(backgroundScope)
        val repo = MonitoredAppsRepository(ds)
        val studyMode = StudyMode(ds)
        repo.setSelected(ig, selected = true).getOrThrow()

        studyMode.setStudyStarted(true)
        val result = repo.setSelected(x, selected = true)

        assertTrue(result.isFailure)
        assertEquals(setOf(ig), repo.selection.first())
    }
}

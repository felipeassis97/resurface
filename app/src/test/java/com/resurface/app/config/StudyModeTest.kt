package com.resurface.app.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * `isSelectionLocked = BuildScope.STUDY && studyStarted`. `STUDY` is a compile-time
 * const, so the runtime-varying input is `studyStarted`; we assert the lock against the
 * actual `STUDY` value to cover both build configurations honestly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StudyModeTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun store(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) {
            tmp.newFile("ds-${System.nanoTime()}.preferences_pb")
        }

    @Test fun notLockedBeforeStudyStarts() = runTest {
        val studyMode = StudyMode(store(backgroundScope))

        assertEquals(false, studyMode.studyStarted.first())
        assertEquals(false, studyMode.isSelectionLocked.first())
    }

    @Test fun lockMirrorsStudyFlagOnceStarted() = runTest {
        val studyMode = StudyMode(store(backgroundScope))

        studyMode.setStudyStarted(true)

        assertEquals(true, studyMode.studyStarted.first())
        // In a study build (STUDY=true) this is true; in a product build it stays false.
        assertEquals(BuildScope.STUDY, studyMode.isSelectionLocked.first())
    }
}

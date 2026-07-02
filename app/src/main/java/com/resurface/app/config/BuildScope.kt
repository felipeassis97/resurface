package com.resurface.app.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compile-time build scope. `STUDY = true` produces the deconfounded research build
 * (NOTES §11): the monitored set is frozen once the study starts so the trigger stays
 * constant across arms.
 */
object BuildScope {
    const val STUDY: Boolean = true
}

/**
 * Shared DataStore key for the study-started flag. Lives at file scope so
 * [StudyMode] and the monitored-apps repository read the *same* preference — the
 * repository enforces the selection lock atomically inside its own `edit{}`.
 */
internal val STUDY_STARTED_KEY = booleanPreferencesKey("study_started")

/**
 * Persisted study-progress state. `studyStarted` has a reachable setter so the lock
 * actually engages in a study build — the flag never ships inert (viability-council §7).
 */
@Singleton
class StudyMode @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val studyStarted: Flow<Boolean> = dataStore.data.map { it[STUDY_STARTED_KEY] ?: false }

    /** True only in a study build after the study has started — then the set is frozen. */
    val isSelectionLocked: Flow<Boolean> =
        studyStarted.map { BuildScope.STUDY && it }

    /** Reachable setter (hidden gesture / adb / cohort setup) so the freeze is achievable. */
    suspend fun setStudyStarted(started: Boolean) {
        dataStore.edit { it[STUDY_STARTED_KEY] = started }
    }
}

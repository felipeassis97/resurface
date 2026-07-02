package com.resurface.app.data.monitored

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.resurface.app.config.BuildScope
import com.resurface.app.config.STUDY_STARTED_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the set of monitored package names on-device. Mirrors [com.resurface.app
 * .data.onboarding.OnboardingRepository] (DataStore + Flow + Hilt constructor injection).
 *
 * All validation — catalog membership, the ≥1 rule, and the study lock — happens
 * atomically inside a single `edit{}` (read-modify-write, no race), and each write
 * returns a [Result] so callers never optimistically assume success.
 */
@Singleton
class MonitoredAppsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val MONITORED = stringSetPreferencesKey("monitored_packages")
    }

    /** Selected packages. Empty (never written empty) → empty-state prompt. */
    val selection: Flow<Set<String>> = dataStore.data.map { it[Keys.MONITORED] ?: emptySet() }

    /**
     * Add or remove [packageName] from the monitored set.
     *
     * Fails (leaving the set unchanged) when: the package is not in the curated catalog,
     * the change would empty the set, or the selection is locked (study started).
     */
    suspend fun setSelected(packageName: String, selected: Boolean): Result<Unit> {
        if (packageName !in CuratedApps.validPackages) {
            return Result.failure(IllegalArgumentException("Not a curated app: $packageName"))
        }
        var failure: Throwable? = null
        dataStore.edit { prefs ->
            val locked = BuildScope.STUDY && (prefs[STUDY_STARTED_KEY] ?: false)
            if (locked) {
                failure = IllegalStateException("Selection is locked (study started)")
                return@edit
            }
            val current = prefs[Keys.MONITORED] ?: emptySet()
            val next = if (selected) current + packageName else current - packageName
            if (next.isEmpty()) {
                failure = IllegalStateException("At least one app must remain monitored")
                return@edit
            }
            prefs[Keys.MONITORED] = next
        }
        return failure?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}

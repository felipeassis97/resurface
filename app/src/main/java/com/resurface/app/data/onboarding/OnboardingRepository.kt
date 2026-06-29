package com.resurface.app.data.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Current disclosure version. Bump when the disclosure text materially changes. */
const val DISCLOSURE_VERSION: Int = 1

/** Persisted onboarding state. Permission grant status is NOT stored here — it is queried live. */
data class OnboardingState(
    val consentGiven: Boolean,
    val consentTimestamp: Long,
    val consentDisclosureVersion: Int,
    val onboardingCompleted: Boolean,
)

@Singleton
class OnboardingRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val CONSENT_GIVEN = booleanPreferencesKey("consent_given")
        val CONSENT_TIMESTAMP = longPreferencesKey("consent_timestamp")
        val CONSENT_DISCLOSURE_VERSION = intPreferencesKey("consent_disclosure_version")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val state: Flow<OnboardingState> = dataStore.data.map { prefs ->
        OnboardingState(
            consentGiven = prefs[Keys.CONSENT_GIVEN] ?: false,
            consentTimestamp = prefs[Keys.CONSENT_TIMESTAMP] ?: 0L,
            consentDisclosureVersion = prefs[Keys.CONSENT_DISCLOSURE_VERSION] ?: 0,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        )
    }

    /** Record explicit consent with the current disclosure version and timestamp. */
    suspend fun recordConsent(timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            prefs[Keys.CONSENT_GIVEN] = true
            prefs[Keys.CONSENT_TIMESTAMP] = timestamp
            prefs[Keys.CONSENT_DISCLOSURE_VERSION] = DISCLOSURE_VERSION
        }
    }

    /** Mark the onboarding flow as completed (consent given + permissions walked through). */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }
}

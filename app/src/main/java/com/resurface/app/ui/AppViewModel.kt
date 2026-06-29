package com.resurface.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resurface.app.data.onboarding.OnboardingRepository
import com.resurface.app.permission.AppPermission
import com.resurface.app.permission.PermissionChecker
import com.resurface.app.ui.onboarding.OnboardingStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the launch gate sends the user after resolving persisted + live state. */
sealed interface StartRoute {
    data object Loading : StartRoute
    data class Onboarding(val initialStep: OnboardingStep) : StartRoute
    data object Main : StartRoute
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val _startRoute = MutableStateFlow<StartRoute>(StartRoute.Loading)
    val startRoute: StateFlow<StartRoute> = _startRoute.asStateFlow()

    private val _permissionStatuses = MutableStateFlow(permissionChecker.statuses())
    val permissionStatuses: StateFlow<Map<AppPermission, Boolean>> = _permissionStatuses.asStateFlow()

    init {
        refresh()
    }

    /** Recompute the gate decision and live permission statuses (call on resume). */
    fun refresh() {
        _permissionStatuses.value = permissionChecker.statuses()
        viewModelScope.launch {
            val state = onboardingRepository.state.first()
            _startRoute.value = when {
                !state.consentGiven -> StartRoute.Onboarding(OnboardingStep.WELCOME)
                !permissionChecker.allRequiredGranted() ->
                    StartRoute.Onboarding(OnboardingStep.PERMISSIONS)
                else -> StartRoute.Main
            }
        }
    }

    /** Settings intent for a special permission; null for runtime permissions. */
    fun settingsIntentFor(permission: AppPermission) = permissionChecker.settingsIntent(permission)

    fun recordConsent() {
        viewModelScope.launch {
            onboardingRepository.recordConsent()
            refresh()
        }
    }

    /** Called when the user finishes the permissions step; routes to Main if all granted. */
    fun completeOnboarding() {
        viewModelScope.launch {
            if (permissionChecker.allRequiredGranted()) {
                onboardingRepository.setOnboardingCompleted(true)
            }
            refresh()
        }
    }
}

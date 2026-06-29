package com.resurface.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resurface.app.ui.onboarding.OnboardingFlow

/**
 * Launch gate: resolves persisted consent + live permission status, then routes to
 * the onboarding flow or the main app. Re-evaluates on every resume so a permission
 * toggled in system settings (no result callback) is reflected immediately.
 */
@Composable
fun ResurfaceApp() {
    val appViewModel: AppViewModel = hiltViewModel()
    val startRoute by appViewModel.startRoute.collectAsStateWithLifecycle()
    val permissionStatuses by appViewModel.permissionStatuses.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        appViewModel.refresh()
        onPauseOrDispose { }
    }

    when (val route = startRoute) {
        StartRoute.Loading -> Box(modifier = Modifier.fillMaxSize())
        is StartRoute.Onboarding -> OnboardingFlow(
            appViewModel = appViewModel,
            initialStep = route.initialStep,
            permissionStatuses = permissionStatuses,
        )
        StartRoute.Main -> MainShell()
    }
}

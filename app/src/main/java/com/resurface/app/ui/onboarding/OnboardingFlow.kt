package com.resurface.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.resurface.app.permission.AppPermission
import com.resurface.app.ui.AppViewModel

/**
 * The onboarding step flow: Welcome → Disclosure → Consent → Grant permissions.
 * UI is intentionally minimal (centered text). [initialStep] lets the launch gate
 * drop the user straight onto the permissions step when consent already exists but
 * a required permission was revoked.
 */
@Composable
fun OnboardingFlow(
    appViewModel: AppViewModel,
    initialStep: OnboardingStep,
    permissionStatuses: Map<AppPermission, Boolean>,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val start: Any = when (initialStep) {
        OnboardingStep.WELCOME -> WelcomeRoute
        OnboardingStep.PERMISSIONS -> PermissionsRoute
    }

    NavHost(navController = navController, startDestination = start, modifier = modifier) {
        composable<WelcomeRoute> {
            OnboardingStepScaffold(
                title = "Welcome to Resurface",
                body = "Resurface helps you notice mindless scrolling and gently resurface for air.",
                buttonLabel = "Get started",
                onButton = { navController.navigate(DisclosureRoute) },
            )
        }
        composable<DisclosureRoute> {
            OnboardingStepScaffold(
                title = "What Resurface observes",
                body = "Observed: which app is open, session length, and scroll/interaction " +
                    "rhythm.\n\nNever captured: screen content, messages, or images.\n\n" +
                    "All processing stays on your device — nothing is sent to a server.",
                buttonLabel = "Continue",
                onButton = { navController.navigate(ConsentRoute) },
            )
        }
        composable<ConsentRoute> {
            OnboardingStepScaffold(
                title = "Your consent",
                body = "You can revoke consent at any time in Settings. By continuing you " +
                    "agree to the on-device monitoring described above.",
                buttonLabel = "I consent",
                onButton = {
                    appViewModel.recordConsent()
                    navController.navigate(PermissionsRoute)
                },
            )
        }
        composable<PermissionsRoute> {
            PermissionsStep(
                appViewModel = appViewModel,
                permissionStatuses = permissionStatuses,
            )
        }
    }
}

@Composable
private fun OnboardingStepScaffold(
    title: String,
    body: String,
    buttonLabel: String,
    onButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(onClick = onButton, modifier = Modifier.padding(top = 32.dp)) {
            Text(buttonLabel)
        }
    }
}

@Composable
private fun PermissionsStep(
    appViewModel: AppViewModel,
    permissionStatuses: Map<AppPermission, Boolean>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { appViewModel.refresh() }

    val allGranted = AppPermission.required.all { permissionStatuses[it] == true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Grant permissions",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        AppPermission.required.forEach { permission ->
            val granted = permissionStatuses[permission] == true
            val label = permission.displayName() + if (granted) " ✓" else ""
            Button(
                onClick = {
                    when (permission.type) {
                        AppPermission.Type.SPECIAL ->
                            appViewModel.settingsIntentFor(permission)?.let(context::startActivity)
                        AppPermission.Type.RUNTIME ->
                            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                enabled = !granted,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(label)
            }
        }
        Button(
            onClick = { appViewModel.completeOnboarding() },
            enabled = allGranted,
            modifier = Modifier.padding(top = 32.dp),
        ) {
            Text("Finish")
        }
    }
}

private fun AppPermission.displayName(): String = when (this) {
    AppPermission.USAGE_ACCESS -> "Usage access"
    AppPermission.ACCESSIBILITY -> "Accessibility"
    AppPermission.NOTIFICATIONS -> "Notifications"
}

package com.resurface.app.ui.onboarding

import kotlinx.serialization.Serializable

/** Type-safe routes for the onboarding step graph, in order. */
@Serializable
object WelcomeRoute

@Serializable
object DisclosureRoute

@Serializable
object ConsentRoute

@Serializable
object PermissionsRoute

/** The step the launch gate wants onboarding to start on. */
enum class OnboardingStep { WELCOME, PERMISSIONS }

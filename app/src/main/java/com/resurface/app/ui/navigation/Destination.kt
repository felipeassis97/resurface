package com.resurface.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/** Type-safe routes for the three top-level destinations. */
@Serializable
object HomeRoute

@Serializable
object InsightsRoute

@Serializable
object SettingsRoute

/**
 * The top-level destinations shown in the navigation container.
 *
 * Single source of truth: the adaptive shell iterates [all] to build navigation
 * items, and the nav host iterates them to register routes. Add a destination here
 * (plus its `composable<…>` in the host) and both stay in sync.
 */
enum class Destination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    INSIGHTS(InsightsRoute, "Insights", Icons.Filled.Insights, Icons.Outlined.Insights),
    SETTINGS(SettingsRoute, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings);

    companion object {
        /** Ordered list backing both the navigation items and the host registration. */
        val all: List<Destination> = entries.toList()

        /** Start destination shown on launch. */
        val start: Destination = HOME
    }
}

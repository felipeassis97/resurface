package com.resurface.app.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.resurface.app.ui.navigation.Destination
import com.resurface.app.ui.navigation.ResurfaceNavHost

/** The main app: adaptive navigation across the three top-level destinations. */
@Composable
fun MainShell() {
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentNavDestination = currentDestination?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Destination.all.forEach { destination ->
                val selected = currentNavDestination?.hierarchy?.any {
                    it.hasRoute(destination.route::class)
                } == true
                item(
                    selected = selected,
                    onClick = {
                        navController.navigate(destination.route) {
                            // Single source on the back stack; restore prior state on return.
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                )
            }
        },
    ) {
        ResurfaceNavHost(navController = navController)
    }
}

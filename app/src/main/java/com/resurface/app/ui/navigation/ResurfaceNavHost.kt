package com.resurface.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.resurface.app.ui.screens.home.HomeScreen
import com.resurface.app.ui.screens.insights.InsightsScreen
import com.resurface.app.ui.screens.monitoredapps.MonitoredAppsScreen
import com.resurface.app.ui.screens.settings.SettingsScreen

@Composable
fun ResurfaceNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.start.route,
        modifier = modifier,
    ) {
        composable<HomeRoute> { HomeScreen() }
        composable<InsightsRoute> { InsightsScreen() }
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateToMonitoredApps = { navController.navigate(MonitoredAppsRoute) },
            )
        }
        composable<MonitoredAppsRoute> {
            MonitoredAppsScreen(onBack = { navController.popBackStack() })
        }
    }
}

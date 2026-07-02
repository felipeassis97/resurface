package com.resurface.app.data.monitored

/**
 * A candidate app the user can monitor. Display metadata lives in [CuratedApps] (code,
 * not user data) — the repository persists only [packageName]s and derives the rest.
 */
data class MonitoredApp(
    val packageName: String,
    val displayName: String,
    val surface: SurfaceType,
)

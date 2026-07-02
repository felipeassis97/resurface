package com.resurface.app.data.monitored

/**
 * The fixed, curated catalog of apps Resurface supports. Single source of truth for
 * display metadata and surface tags; the repository stores only package names.
 *
 * The study freezes app versions on the reference device, so a curated list (rather
 * than a `PackageManager` install scan) is sufficient and avoids `QUERY_ALL_PACKAGES`.
 * Known regional package variants are accepted so a participant on a variant is not
 * silently dropped (e.g. TikTok `com.ss.android.ugc.trill`).
 */
object CuratedApps {

    /** One row per app, shown in the picker (canonical package name). */
    val all: List<MonitoredApp> = listOf(
        MonitoredApp("com.instagram.android", "Instagram", SurfaceType.MIXED),
        MonitoredApp("com.zhiliaoapp.musically", "TikTok", SurfaceType.SHORT_VIDEO),
        MonitoredApp("com.google.android.youtube", "YouTube", SurfaceType.MIXED),
        MonitoredApp("com.twitter.android", "X", SurfaceType.FEED),
        MonitoredApp("com.reddit.frontpage", "Reddit", SurfaceType.FEED),
    )

    /** Known regional/alternate packages mapped to their canonical entry. */
    private val variants: Map<String, String> = mapOf(
        "com.ss.android.ugc.trill" to "com.zhiliaoapp.musically", // TikTok (non-CN markets vary)
    )

    /** Every package name that may be persisted: canonical + known variants. */
    val validPackages: Set<String> = all.map { it.packageName }.toSet() + variants.keys

    /** Surface tag for a package (resolving variants), or null if unknown. */
    fun surfaceFor(packageName: String): SurfaceType? {
        val canonical = variants[packageName] ?: packageName
        return all.firstOrNull { it.packageName == canonical }?.surface
    }
}

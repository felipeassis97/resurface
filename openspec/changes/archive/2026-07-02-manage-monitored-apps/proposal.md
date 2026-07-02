## Why

The AccessibilityService currently listens to *all* packages (stub config has no `packageNames` scope). Resurface only intervenes inside apps the user explicitly chose to monitor, and the research design (NOTES §11) requires a *constant, frozen* monitored set during data collection. There is no way to define or persist that set. F3 delivers the selection surface and scopes the live service to it — the prerequisite for F4 detection.

## What Changes

- New **monitored-apps** capability: a curated list of supported social apps (Instagram, TikTok, YouTube, X, Reddit), each tagged with a `SurfaceType` (FEED / SHORT_VIDEO / MIXED) that F4 detection will branch on.
- New **MonitoredAppsRepository** (DataStore-backed) persisting the selected `Set<packageName>`.
- New **monitored-apps screen** reached from Settings (a sub-screen, **not** a top-level destination); toggle apps on/off, empty-state prompt, selection count.
- **≥1 enforcement**: the last selected app cannot be deselected; an empty set is rejected.
- AccessibilityService gains `applyMonitoredScope()` — reconciles `packageNames` from the persisted set via `setServiceInfo()` on **every** `onServiceConnected` (guaranteed primary path); live updates on selection change are best-effort. Never passes an empty array (would widen to all packages). Event handling stays no-op (F4 owns it).
- New **`BuildScope.STUDY`** flag introduced here (first use in the codebase). In study mode, once `studyStarted` is set (via a reachable setter), the selection lock is enforced at the **repository write level** — greyed toggles are only cosmetic reinforcement. Keeps the trigger set constant (NOTES §11).
- Installed/not-installed status surfaced per app to prevent silent monitoring loss from regional package variants (e.g. TikTok `com.ss.android.ugc.trill`).
- Onboarding is **unchanged** — it stays scoped to consent + permissions.

## Capabilities

### New Capabilities
- `monitored-apps`: defining, persisting, and editing the set of apps Resurface observes, and scoping the AccessibilityService to that set at runtime.

### Modified Capabilities
<!-- No existing capability's requirements change. permissions/onboarding/app-navigation specs are untouched; F3 adds a Settings sub-screen without altering navigation requirements. -->

## Impact

- **Code (new):** `data/monitored/` (MonitoredApp, SurfaceType, CuratedApps, MonitoredAppsRepository), `config/BuildScope.kt`, `ui/screens/monitoredapps/` (screen + ViewModel), DI provider for the repository.
- **Code (modified):** `service/ResurfaceAccessibilityService.kt` (add `applyMonitoredScope()` + observe selection); `ui/screens/settings/SettingsScreen.kt` (entry point); navigation host (register sub-screen route).
- **Config:** `res/xml/accessibility_service_config.xml` — `packageNames` now driven at runtime; `canRetrieveWindowContent` stays `false` (F4 flips it).
- **Storage:** new DataStore keys for the monitored set; exportable as evaluation metadata (privacy: NOTES §14 — no record of apps the user did *not* pick beyond the curated candidates).
- **Out of scope:** detection/event handling (F4), overlay/intervention (F5), `PackageManager` full-install scan (product-future), per-app windows/quiet-hours/sensitivity (F10).

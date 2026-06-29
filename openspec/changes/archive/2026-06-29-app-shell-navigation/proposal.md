## Why

The app currently boots into a single "Hello Android" stub with no navigation. Before any feature work begins, Resurface needs a navigable app shell so screens have a home to live in. Establishing the navigation scaffold now — top-level destinations, routing, and adaptive container — sets the structural contract every later feature plugs into.

## What Changes

- Add a bottom navigation shell with three top-level destinations: **Home**, **Insights**, **Settings**.
- Introduce Navigation Compose with **type-safe routes** (`@Serializable` route objects) as the navigation engine.
- Use **`NavigationSuiteScaffold`** (Material 3 adaptive) as the container so the shell renders a bottom bar on compact (phone) and auto-morphs to a navigation rail / drawer on medium and expanded windows — adaptive-first per the I/O 2026 stance, no phone-only lock-in.
- Add three placeholder screen composables (text label only) wired into a `NavHost`.
- Replace the `Greeting` stub in `MainActivity` with the new `ResurfaceApp()` shell, applied inside `ResurfaceTheme`.
- Add dependencies: `androidx.navigation:navigation-compose`, `androidx.compose.material3:material3-adaptive-navigation-suite`, Kotlin serialization plugin + `kotlinx-serialization-json`.

## Capabilities

### New Capabilities
- `app-navigation`: Top-level app shell and navigation — the three primary destinations, type-safe routing between them, single-source selection state, reselection/back-stack behavior, and the adaptive navigation container that maps destinations to bottom bar / rail / drawer by window size.

### Modified Capabilities
<!-- None. The existing design-system capability is consumed (theme applied around the shell) but its requirements do not change. -->

## Impact

- **Code**: `MainActivity.kt` (stub removed); new `ui/navigation/` (destinations + nav host) and `ui/screens/` (Home, Insights, Settings placeholders) and `ui/ResurfaceApp.kt`.
- **Build**: `gradle/libs.versions.toml` and `app/build.gradle.kts` gain navigation, adaptive-navigation-suite, and serialization entries; root build adds the kotlin-serialization plugin.
- **Design system**: consumed unchanged — shell renders inside `ResurfaceTheme`, nav colors come from existing M3 color roles.
- **No user-facing features yet**: screens are label-only placeholders for verifying navigation.

## Context

Resurface is a Compose-first Android app (minSdk 34, target 36, Compose BOM 2026.02.01, Material3 stable). The theme layer (`com.resurface.app.ui.theme`) is complete and locked. `MainActivity` currently renders a `Greeting` stub inside `ResurfaceTheme` — there is no navigation. This change builds the app shell every later feature plugs into. No navigation dependency is present yet.

Decisions were settled in explore: three destinations (Home / Insights / Settings), Navigation Compose with type-safe routes, and `NavigationSuiteScaffold` as the adaptive container.

## Goals / Non-Goals

**Goals:**
- A working, adaptive bottom-navigation shell with three reachable destinations.
- Type-safe routes so later features add destinations without stringly-typed bugs.
- Correct bottom-nav semantics: single-top, save/restore state, sane back behavior.
- Placeholder screens (label only) to verify navigation end-to-end.
- A package structure later features extend without rework.

**Non-Goals:**
- Any real screen content, view models, or data — screens are text labels only.
- Nested/sub-navigation, deep links, or argument passing between screens.
- Animations/transitions beyond library defaults.
- List-detail / supporting-pane adaptive layouts (container adapts; screen content does not yet).

## Decisions

### D1: Navigation Compose with type-safe routes (over Nav3)
Use `androidx.navigation:navigation-compose` with `@Serializable` route objects and the reified `composable<T> { }` / `navigate(T)` APIs.
- **Why**: Stable, the documented Android default, type-safe args, mature back-stack handling.
- **Alternative — Navigation 3 (Nav3)**: Compose-native, app-owned back stack, but still alpha in early 2026. API churn risk is not worth it on a foundational scaffold. Rejected for now; revisit if Nav3 stabilizes.

### D2: `NavigationSuiteScaffold` as the container (over plain Scaffold + NavigationBar)
Use `androidx.compose.material3:material3-adaptive-navigation-suite`. It auto-selects bottom bar (compact) → rail (medium) → drawer (expanded) from window size.
- **Why**: Satisfies "bottom navigation on phone" today and future-proofs tablet/foldable with no rework — the I/O 2026 adaptive-first stance.
- **Alternative — plain `Scaffold` + `NavigationBar`**: simpler, one less dependency, but phone-locked; tablet support later means re-architecting. Rejected.

### D3: Destinations as a sealed type
Model destinations as a sealed interface/enum where each entry carries its `@Serializable` route object, label string, and selected/unselected icons. The container iterates this single list to build items; the host iterates it to register `composable<T>`.
- **Why**: One source of truth — adding a destination is a single edit. Keeps container and host in sync.

### D4: Selection + navigation wiring
Track current destination via `navController.currentBackStackEntryAsState()` and compare against each route. On item click `navigate(route)` with: `launchSingleTop = true`, `popUpTo(startDestination) { saveState = true }`, `restoreState = true` — the canonical bottom-nav pattern.
- **Why**: Gives the spec's single-top, state-restore, and back-to-start behavior.

### D5: Package structure
```
com.resurface.app
├── MainActivity.kt              → setContent { ResurfaceTheme { ResurfaceApp() } }
└── ui/
    ├── ResurfaceApp.kt          → NavigationSuiteScaffold + selection state + NavHost call
    ├── navigation/
    │   ├── Destination.kt       → sealed destinations (route + label + icons)
    │   └── ResurfaceNavHost.kt  → NavHost { composable<Home/Insights/Settings> }
    └── screens/
        ├── home/HomeScreen.kt
        ├── insights/InsightsScreen.kt
        └── settings/SettingsScreen.kt
```
- **Why**: Feature-per-package under `screens/` scales; navigation isolated from screens.

### D6: Icons
Use `material-icons-extended` for `Insights`/`BarChart`-style glyphs not in the core icon set; filled icon for selected, outlined for unselected per the M3 nav-bar guideline. If avoiding the extra dependency is preferred, fall back to core icons (`Home`, `Info`, `Settings`).

## Risks / Trade-offs

- **`NavigationSuiteScaffold` API maturity** → it is a stable Material3 adaptive API in this BOM; pin via the BOM and opt-in to experimental annotations only if the compiler requires. Verify against the resolved BOM at build.
- **Extra dependency surface** (adaptive-navigation-suite, serialization, icons-extended) → all first-party AndroidX/Kotlin; low risk. Icons-extended adds APK size — acceptable for a placeholder, revisit before release.
- **Window-size adaptation untested on phone-only dev** → verify with a tablet/foldable emulator or resizable emulator that rail/drawer appears at medium+ widths.
- **Serialization plugin/version drift** → align `kotlinx-serialization` plugin version with the Kotlin version (`2.2.10`) already in `libs.versions.toml`.

## Migration Plan

Additive change. The only removal is the `Greeting` stub + its preview in `MainActivity`. No data, no users, nothing to roll back beyond reverting the commit. Build-check (`assembleDebug`) gates completion; manual run on a phone emulator confirms bottom-bar navigation across the three screens.

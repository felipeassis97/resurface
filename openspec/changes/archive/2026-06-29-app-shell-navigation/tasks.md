## 1. Dependencies

- [x] 1.1 Add `kotlinx-serialization` plugin (version aligned to Kotlin 2.2.10) to `gradle/libs.versions.toml` plugins and root `build.gradle.kts`
- [x] 1.2 Add library entries to `libs.versions.toml`: `navigation-compose`, `material3-adaptive-navigation-suite`, `kotlinx-serialization-json`, and (optional) `material-icons-extended`
- [x] 1.3 Apply the serialization plugin and add the four dependencies in `app/build.gradle.kts`
- [x] 1.4 Gradle sync succeeds (`./gradlew help` or IDE sync, no resolution errors)

## 2. Navigation model

- [x] 2.1 Create `ui/navigation/Destination.kt`: sealed type with three entries, each carrying a `@Serializable` route object, label, and selected/unselected icons
- [x] 2.2 Define `@Serializable` route objects for Home, Insights, Settings; mark Home as the start destination
- [x] 2.3 Expose an ordered list of the three destinations for the container and host to iterate

## 3. Placeholder screens

- [x] 3.1 Create `ui/screens/home/HomeScreen.kt` — composable showing centered `Text("Home")`, plus `@Preview`
- [x] 3.2 Create `ui/screens/insights/InsightsScreen.kt` — `Text("Insights")` + `@Preview`
- [x] 3.3 Create `ui/screens/settings/SettingsScreen.kt` — `Text("Settings")` + `@Preview`

## 4. Navigation host

- [x] 4.1 Create `ui/navigation/ResurfaceNavHost.kt` with a `NavHost` (startDestination = Home) registering `composable<T>` for each route → its screen
- [x] 4.2 Accept `navController` and a `Modifier` (for scaffold inner padding) as parameters

## 5. Adaptive shell

- [x] 5.1 Create `ui/ResurfaceApp.kt` using `NavigationSuiteScaffold`; build items from the destination list with label + selected/unselected icon
- [x] 5.2 Track current destination via `navController.currentBackStackEntryAsState()`; mark the matching item selected
- [x] 5.3 On item click `navigate(route)` with `launchSingleTop = true`, `popUpTo(startDestination){ saveState = true }`, `restoreState = true`
- [x] 5.4 Render `ResurfaceNavHost` in the scaffold content; `NavigationSuiteScaffold` manages window insets for the nav container (no inner-padding param to forward)

## 6. Wire MainActivity

- [x] 6.1 Replace `Greeting` + its preview in `MainActivity.kt` with `ResurfaceApp()` inside `ResurfaceTheme`; keep `enableEdgeToEdge()`
- [x] 6.2 Remove now-unused imports/stub code

## 7. Verify

- [x] 7.1 `./gradlew assembleDebug` builds with no errors
- [ ] 7.2 Run on a phone emulator: bottom bar shows three items; tapping each switches screen and updates selection; system back from Settings returns to Home  _(pending — no emulator/device in this environment; run locally)_
- [ ] 7.3 Resize to a medium/expanded window (resizable or tablet emulator): container becomes a rail/drawer; navigation still works  _(pending — run locally)_

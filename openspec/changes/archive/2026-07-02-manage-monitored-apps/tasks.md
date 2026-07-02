## 1. Prerequisites

- [x] 1.1 Confirm `ResurfaceApplication` is annotated `@HiltAndroidApp`
- [x] 1.2 Add test dependencies (`kotlinx-coroutines-test`, `app.cash.turbine:turbine`, DataStore test) to `libs.versions.toml` + `app/build.gradle.kts`

## 2. Data layer (`data/monitored/`)

- [x] 2.1 Add `SurfaceType` enum (`FEED`, `SHORT_VIDEO`, `MIXED`)
- [x] 2.2 Add `MonitoredApp` data class (`packageName`, `displayName`, `surface`)
- [x] 2.3 Add `CuratedApps` (IG=MIXED, TikTok=SHORT_VIDEO incl. variant `com.ss.android.ugc.trill`, YouTube=MIXED, X=FEED, Reddit=FEED) with `all`, `validPackages`, `surfaceFor(pkg)`
- [x] 2.4 Add `MonitoredAppsRepository` (`@Singleton @Inject constructor(DataStore)`, `stringSetPreferencesKey`); `selection: Flow<Set<String>>` with `?: emptySet()` read; `setSelected(pkg, Boolean): Result<Unit>` validating catalog membership + ≥1 + lock **inside a single `edit{}`**
- [x] 2.5 Extract the pure scope decision (`Set<String>` → `Array<String>?`, empty → null-guarded) into a testable function usable by the service
- [x] 2.6 Unit test repo (DataStore in `TemporaryFolder`): persist/restore, reject non-catalog, reject empty write, lock rejects write

## 3. Build scope (`config/BuildScope.kt`)

- [x] 3.1 Add `const val STUDY`; persist `studyStarted` (DataStore `booleanPreferencesKey`) with callable `setStudyStarted(Boolean)` + `studyStarted: Flow<Boolean>`; expose `isSelectionLocked = STUDY && studyStarted`
- [x] 3.2 Unit test the 4-combination truth table for `isSelectionLocked`

## 4. DI

- [x] 4.1 Verify constructor injection suffices (no new `@Provides` module) and `@AndroidEntryPoint` on the service resolves against `SingletonComponent`

## 5. Service — reconcile-on-connect (`service/`)

- [x] 5.1 Annotate `ResurfaceAccessibilityService` `@AndroidEntryPoint`; `@Inject lateinit var repo`
- [x] 5.2 Add `serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`; in `onServiceConnected` cancel any prior collect job, then collect `repo.selection` with `.catch{}`; cancel scope in `onDestroy` (not `onUnbind`)
- [x] 5.3 Add `applyMonitoredScope(pkgs)`: early-return if empty; copy existing `serviceInfo` (return if null); set only `packageNames`; keep `canRetrieveWindowContent=false`; no detection
- [x] 5.4 On connect, log which monitored packages are installed (`PackageManager.getPackageInfo` in try/catch)
- [x] 5.5 Keep `accessibility_service_config.xml` without `packageNames` (runtime overrides)

## 6. UI (`ui/screens/monitoredapps/`)

- [x] 6.1 Add `MonitoredAppsViewModel` (`@HiltViewModel`): combine `CuratedApps.all` + `repo.selection` + `isSelectionLocked` + installed status into a `StateFlow<UiState>`; `toggle()` calls repo and handles the `Result` failure (no optimistic flip)
- [x] 6.2 Add `MonitoredAppsScreen`: `TopAppBar` with back, per-app `Switch`, selection count, empty-state prompt, last-app toggle disabled, all disabled when locked, installed/not-installed badge
- [x] 6.3 Nav: `@Serializable object MonitoredAppsRoute` (NOT in `Destination` enum); register `composable<MonitoredAppsRoute>` in host; change `SettingsScreen` to take `onNavigateToMonitoredApps: () -> Unit` and fix its `@Preview` with `{}`; add string resources
- [x] 6.4 Unit test ViewModel (`MainDispatcherRule` + temp/fake repo): toggle, ≥1 enforcement, rejected-write handling, lock disables toggles

## 7. Device verification (service must be enabled)

- [ ] 7.1 Select apps → kill/relaunch → confirm persistence
- [ ] 7.2 **Enable the AccessibilityService**, then confirm `packageNames` scope applied on connect via logcat; confirm ≥1 enforcement and empty rejection
- [ ] 7.3 If `STUDY`, set `studyStarted` via the test hook and confirm writes rejected + toggles disabled
- [ ] 7.4 Run the live re-scope check on ≥1 non-Pixel OEM device; treat live-update failure as acceptable provided reconcile-on-connect works

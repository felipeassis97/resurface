## 1. Build setup (Hilt, KSP, DataStore, lifecycle)

- [x] 1.1 Add KSP plugin (`com.google.devtools.ksp` `2.2.10-2.0.2`) and Hilt Gradle plugin (`2.60`) to `libs.versions.toml` + root `build.gradle.kts`
- [x] 1.2 Add library entries: `hilt-android`, `hilt-compiler`, `hilt-navigation-compose` `1.3.0`, `datastore-preferences` `1.2.1`, `lifecycle-viewmodel-compose`/`lifecycle-runtime-compose` `2.11.0`
- [x] 1.3 Apply `ksp` + `hilt` plugins and add dependencies (`ksp(hilt-compiler)`) in `app/build.gradle.kts`; set `android.disallowKotlinSourceSets=false` (AGP 9 built-in Kotlin vs KSP generated sources)
- [x] 1.4 `./gradlew :app:dependencies` resolves; no version conflicts

## 2. Application + DI bootstrap

- [x] 2.1 Create `ResurfaceApplication` annotated `@HiltAndroidApp`
- [x] 2.2 Register `android:name=".ResurfaceApplication"` in the manifest
- [x] 2.3 Annotate `MainActivity` with `@AndroidEntryPoint`
- [x] 2.4 Create `di/DataStoreModule` providing a singleton `DataStore<Preferences>`
- [x] 2.5 Build check: `./gradlew :app:assembleDebug` compiles with Hilt/KSP wired — PASS (lifecycle pinned to 2.9.4 for compileSdk 36)

## 3. Permission layer

- [x] 3.1 Define `AppPermission` (Usage Access, Accessibility, Notifications) with type (special/runtime) and required-set membership
- [x] 3.2 Create `PermissionChecker`: live status for Usage Access (AppOps `OPSTR_GET_USAGE_STATS`), Accessibility (enabled-accessibility-services), Notifications (`checkSelfPermission`, no-op below API 33)
- [x] 3.3 Provide intents for special permissions (`ACTION_USAGE_ACCESS_SETTINGS`, `ACTION_ACCESSIBILITY_SETTINGS`); runtime request wired via `RequestPermission` launcher in the permissions step
- [x] 3.4 Add `uses-permission` for `PACKAGE_USAGE_STATS` (with `tools:ignore="ProtectedPermissions"`) and `POST_NOTIFICATIONS`

## 4. Onboarding persistence

- [x] 4.1 Create `data/onboarding/OnboardingRepository` reading/writing consent (flag + timestamp + disclosure version) and onboarding-completed marker via DataStore
- [x] 4.2 Expose onboarding state as a flow for the launch gate to observe
- [x] 4.3 Define disclosure version constant (integer, start at 1)

## 5. Stub accessibility service

- [x] 5.1 Create `service/ResurfaceAccessibilityService` extending `AccessibilityService` with no-op `onAccessibilityEvent`/`onInterrupt`
- [x] 5.2 Add `res/xml/accessibility_service_config.xml` (broad event types for now)
- [x] 5.3 Declare the `<service>` in the manifest with `BIND_ACCESSIBILITY_SERVICE`, the accessibility intent-filter, and config meta-data

## 6. Onboarding UI + ViewModel

- [x] 6.1 Create `AppViewModel` (`@HiltViewModel`) holding gate decision, consent action, and live permission statuses; re-check statuses on resume
- [x] 6.2 Create onboarding screen composables: Welcome, Disclosure, Consent, Permissions — centered-text placeholders with advance actions
- [x] 6.3 Disclosure step states observed data vs never-captured data vs on-device processing
- [x] 6.4 Consent step requires explicit accept → persists consent record; advances to permissions on consent
- [x] 6.5 Permissions step lists the trio with per-permission grant action and live status; gate observes lifecycle to update on return from Settings

## 7. Launch gate + navigation

- [x] 7.1 Add onboarding routes (`@Serializable`) and a nested onboarding nav graph (`OnboardingFlow`)
- [x] 7.2 Create a root gate state holder (`AppViewModel`) exposing `StartRoute = Loading | Onboarding(step) | Main` from consent + live permission status
- [x] 7.3 Update `ResurfaceApp` to hold on `Loading`, then select onboarding flow or `MainShell` (F1 Home/Insights/Settings destinations preserved)
- [x] 7.4 Revoked required permission after consent routes to the Permissions step, not Welcome

## 8. Verify

- [x] 8.1 `./gradlew :app:assembleDebug` builds with no errors
- [ ] 8.2 Emulator: first launch shows onboarding; advance through steps; grant Usage Access + Accessibility via Settings and confirm status updates on resume; grant Notifications via dialog  _(pending — no emulator/device in this environment; run locally)_
- [ ] 8.3 Emulator: after completing onboarding, relaunch routes straight to the main shell; revoke a permission in Settings and confirm relaunch routes to the Permissions step  _(pending — run locally)_

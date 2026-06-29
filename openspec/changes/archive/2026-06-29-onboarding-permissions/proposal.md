## Why

Resurface cannot function until the user has been informed of what the app observes, has explicitly consented, and has granted the OS permissions the detection engine depends on. None of this exists yet — the app launches straight into the navigation shell with no permissions and no consent record. F2 builds the gated onboarding flow that every later feature (detection, intervention, logging) presupposes, and satisfies the disclosure/consent obligations in the README (§12 Accessibility & Policies, §13 Privacy & Ethics).

## What Changes

- Add a **first-run onboarding flow**: Welcome → Disclosure → Consent → Grant permissions. UI is intentionally minimal (centered text placeholders); functionality is the focus.
- Request the **core permission trio** needed by the detection engine: **Usage Access** (special), **Accessibility** (special), **Notifications** (runtime). Overlay (F5) and Bluetooth (F6) are deferred and requested just-in-time by their own features.
- Add a **stub `AccessibilityService`** (declared, no-op) so the user can actually enable accessibility during onboarding; the real detection logic lands in F4.
- **Record consent** (timestamp + disclosure version) and onboarding completion in **DataStore Preferences** — the first slice of the F7 data layer. Permission grant status is always queried live from the OS, never persisted.
- Add a **launch gate**: on start the app routes to the onboarding graph or the main shell based on consent + live permission status. A required permission revoked later bounces the user to the permissions step.
- Introduce **Hilt** dependency injection and a **ViewModel** state layer for onboarding; add **DataStore**, **lifecycle-viewmodel/runtime-compose**, **KSP**.

## Capabilities

### New Capabilities
- `onboarding`: First-run flow and launch gating — the Welcome → Disclosure → Consent → Permissions steps, consent recording, and the start-up routing decision between onboarding and the main app.
- `permissions`: Permission model and status checking — the set of permissions Resurface needs, how each is requested (special Settings-intent vs runtime dialog), and live grant-status checking with on-resume re-evaluation.

### Modified Capabilities
- `app-navigation`: The shell no longer hard-starts at Home; navigation now begins behind a launch gate that selects the onboarding graph or the main graph. The three top-level destinations are unchanged but become reachable only after onboarding completes.

## Impact

- **Code**: new `ResurfaceApplication` (`@HiltAndroidApp`); `MainActivity` becomes `@AndroidEntryPoint` and hosts the launch gate; new packages `di/`, `data/onboarding/`, `permission/`, `service/`, `ui/onboarding/`; `ui/navigation/` gains onboarding routes + gate.
- **Manifest**: `android:name=".ResurfaceApplication"`; `uses-permission` for `PACKAGE_USAGE_STATS` (with `tools:ignore="ProtectedPermissions"`) and `POST_NOTIFICATIONS`; `<service>` for the stub `AccessibilityService` with `BIND_ACCESSIBILITY_SERVICE` + config meta-data; new `res/xml/accessibility_service_config.xml`.
- **Build**: Hilt (`hilt-android`, `hilt-compiler` via KSP, `hilt-navigation-compose`, Hilt Gradle plugin), KSP plugin (version matched to Kotlin 2.2.10), `datastore-preferences`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`.
- **Risk**: Hilt + KSP on AGP 9 built-in Kotlin must be build-verified; special-permission correctness depends on on-resume polling.
- **No real detection yet**: the accessibility service is a no-op stub; screens are text placeholders.

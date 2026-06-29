## Context

After F1 the app launches straight into the navigation shell (`startDestination = Home`) with no permissions and no consent. F2 inserts a first-run onboarding gate and wires up the three OS permissions the detection engine (F4) depends on. The manifest is currently bare (no permissions, no Application class, no services). Distribution is sideloading for research, which softens Google Play's prominent-disclosure enforcement but does not remove the ethical disclosure/consent obligations in README §12–§13.

Decisions settled in explore: stub the accessibility service now, request only the core trio (Usage + Accessibility + Notifications), persist with DataStore Preferences, adopt Hilt + ViewModel.

## Goals / Non-Goals

**Goals:**
- A resumable Welcome → Disclosure → Consent → Grant-permissions flow.
- Correct request + live status checking for the three permission types.
- A launch gate that routes onboarding vs main on every start from persisted consent + live permission status.
- Consent recorded on-device (flag + timestamp + disclosure version), revocable later.
- DI and state-layer foundations (Hilt, ViewModel, DataStore) that F3–F10 build on.

**Non-Goals:**
- Real detection/intervention — the accessibility service is a no-op stub.
- Polished UI — steps are centered-text placeholders.
- Overlay and Bluetooth permissions — deferred to F5/F6, requested just-in-time.
- Full consent-revocation UI — that is F10; F2 only records consent so it can be revoked later.

## Decisions

### D1: Permission taxonomy drives the request code path
Model each permission as an `AppPermission` with its type. Special permissions (Usage Access, Accessibility) are requested by launching a Settings intent (`ACTION_USAGE_ACCESS_SETTINGS`, `ACTION_ACCESSIBILITY_SETTINGS`) and have **no result callback**; runtime permissions (Notifications) use the Activity Result API dialog.
- **Why**: The two classes behave fundamentally differently; conflating them is the classic bug source. A single `PermissionChecker` centralizes the five check APIs (AppOps `OPSTR_GET_USAGE_STATS`, enabled-accessibility-services string, `ContextCompat.checkSelfPermission`).
- **Alternative**: a third-party permissions library — rejected; special permissions aren't covered uniformly and we want minimal deps.

### D2: On-resume polling for special permissions
The onboarding permission screen observes lifecycle and re-evaluates all statuses on `ON_RESUME` (via `lifecycle-runtime-compose`), since returning from a Settings screen yields no callback.
- **Why**: Only reliable way to reflect a special-permission toggle.
- **Trade-off**: Polls on every resume, not event-driven — negligible cost for three checks.

### D3: Live status is never persisted
DataStore stores only consent (flag + timestamp + disclosure version) and an onboarding-completed marker. Permission grant status is always read from the OS.
- **Why**: Users revoke permissions in system settings outside the app; a stored "granted" flag would lie. The launch gate recomputes grant status each start.

### D4: Launch gate as a resolving root state
A root-level state holder (Hilt `@HiltViewModel` consumed in `MainActivity`/`ResurfaceApp`) exposes a `StartRoute` of `Loading | Onboarding(step) | Main`. While DataStore resolves, the gate shows nothing (or the splash); then the `NavHost` selects the onboarding graph or the main graph. A required permission missing after consent → `Onboarding(Permissions)` rather than `Onboarding(Welcome)`.
- **Why**: DataStore reads are async; routing must wait for a resolved value to avoid flashing the wrong graph.
- **Alternative**: block the main thread on a synchronous read — rejected (jank, discouraged).

### D5: Two nested nav graphs behind the gate
Extend the existing `ui/navigation` with an onboarding graph (Welcome/Disclosure/Consent/Permissions routes) and keep the F1 destinations as the main graph. The gate picks the start graph.
- **Why**: Reuses the type-safe Navigation Compose setup from F1; keeps onboarding and main cleanly separated.

### D6: Hilt + KSP for DI
Adopt Hilt (`@HiltAndroidApp ResurfaceApplication`, `@AndroidEntryPoint MainActivity`, `hiltViewModel()` via `hilt-navigation-compose`) with the compiler run through **KSP** (project has no annotation processor yet). KSP/Hilt versions pinned to Kotlin 2.2.10.
- **Why**: User chose full DI now; scales cleanly into F4–F10 (services, repositories). KSP over KAPT for speed and KSP2 alignment.
- **Risk**: Hilt + KSP on AGP 9 built-in Kotlin is the sharpest unknown — gated by a build check.

### D7: Stub accessibility service
Declare `ResurfaceAccessibilityService` (`BIND_ACCESSIBILITY_SERVICE`, intent-filter `android.accessibilityservice.AccessibilityService`, meta-data `res/xml/accessibility_service_config.xml`) with a no-op `onAccessibilityEvent`/`onInterrupt`.
- **Why**: The user can only enable accessibility if a real service is declared; detection logic is F4. The config can declare broad event types now and be narrowed in F4.

## Risks / Trade-offs

- **Hilt + KSP + AGP 9 built-in Kotlin compatibility** → verify with `assembleDebug` immediately after wiring DI; if KSP2 misaligns with Kotlin 2.2.10, pin a compatible KSP build or fall back to KAPT.
- **`PACKAGE_USAGE_STATS` lint error** (`ProtectedPermissions`) → add `tools:ignore="ProtectedPermissions"` on the `uses-permission`.
- **Special-permission status APIs are fiddly** (AppOps deprecations, accessibility string parsing across OEMs) → isolate in `PermissionChecker` with defensive parsing; verify on a real-ish emulator.
- **Accessibility stub + Play policy** → acceptable for sideload research; disclosure screen still states observed vs never-captured data so the flow is honest.
- **Launch flash / wrong-graph** → gate must hold on a `Loading` state until DataStore resolves; do not default to a concrete graph.

## Migration Plan

Additive plus one navigation change: the F1 main graph stops being the unconditional start destination and sits behind the gate. No persisted user data exists yet, so nothing to migrate. Rollback = revert the change; the app returns to launching straight into Home. Completion gated by `assembleDebug` (build) and a manual emulator pass of the grant flow (toggle each permission in Settings, confirm on-resume status update, confirm gate routes to Main).

## Open Questions

- Disclosure version scheme — simple integer vs semantic string? (Default: integer starting at 1.)
- Should the launch gate distinguish "consent given but never finished permissions" from "finished but permission later revoked"? Both currently route to the Permissions step; may want different copy later.
- Exact accessibility config event types for the stub — broad now, narrowed in F4.

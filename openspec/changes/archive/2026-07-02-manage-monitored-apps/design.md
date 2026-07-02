## Context

Resurface's AccessibilityService (F2 stub) listens to all packages and does no detection. F3 delivers the "manage monitored apps" surface: the user chooses which apps are observed, the choice persists, and the service is scoped to it. This is the direct prerequisite for F4 detection, which branches heuristics by app surface type (NOTES §8.3). The research design (NOTES §11) requires the monitored set to be *constant and frozen* during data collection.

This design was revised after a 5-reviewer viability council (`viability-council.md`, verdict: **buildable, no blockers**). The council confirmed every hard part has precedent in the codebase (`OnboardingRepository`, Hilt throughout, type-safe nav) and reshaped the runtime-scoping mechanism. Its corrections are folded into the decisions below.

Existing patterns to mirror: `OnboardingRepository` (DataStore + `Flow` + Hilt `@Singleton` constructor injection), `DataStoreModule`, type-safe nav routes in `Destination.kt` / `OnboardingRoutes.kt`, `@HiltViewModel` in `AppViewModel`.

## Goals / Non-Goals

**Goals:**
- Curated, surface-tagged app catalog as the single source of display metadata.
- DataStore-backed persistence of the selected `Set<packageName>`, observable as a `Flow`.
- Monitored-apps screen reached from Settings (sub-screen); toggles, empty-state, ≥1 enforcement, back affordance.
- Scope the AccessibilityService **reconcile-on-connect** (DataStore is source of truth; service is a projection).
- Introduce `BuildScope.STUDY` + a *reachable* study lock enforced at the repository write level.

**Non-Goals:**
- Detection / event handling / `canRetrieveWindowContent=true` (F4).
- Overlay / intervention (F5).
- Full `PackageManager` install scan for candidate discovery (product-future).
- Per-app windows, quiet hours, sensitivity (F10).
- A polished UI for setting `studyStarted` — a callable setter ships; the operator affordance (hidden gesture / adb / cohort constant) is minimal.

## Decisions

**D1 — Persist package names only; derive metadata from a static catalog.**
`MonitoredAppsRepository` stores `Set<String>`. `displayName`/`SurfaceType` come from `CuratedApps`. *Why:* display metadata is code, not user data; storing it duplicates a mutable copy and risks drift.

**D2 — Curated catalog now; no `PackageManager` scan.** Fixed candidates: IG `com.instagram.android`, TikTok `com.zhiliaoapp.musically` (+known variant `com.ss.android.ugc.trill`), YouTube `com.google.android.youtube`, X `com.twitter.android`, Reddit `com.reddit.frontpage`. *Why:* matches the study's frozen-version constraint, avoids `QUERY_ALL_PACKAGES` policy surface.

**D3 — F3 owns `packageNames` scoping; F4 owns event semantics + `canRetrieveWindowContent`.** *Why:* selection is meaningless unless something scopes the service, but expanding privilege belongs with the consumer that needs it (NOTES §14, least privilege).

**D4 — Reconcile-on-connect is primary; live update is best-effort *(revised per council)*.**
In **every** `onServiceConnected` the service reads the persisted set and applies scope — the known-good path across all OEMs. The service still collects `repo.selection`, which yields the connect-time reconciliation for free and covers live changes as *best-effort*. **Study integrity must not depend on live re-scope working on every OEM/Android 16.** *Why (council):* the set is frozen during the study, so live re-scope is unnecessary for correctness and is the least reliable path; the earlier "fallback" (re-scope on next bind) is now the guaranteed primary behavior.

**D5 — Never pass an empty `packageNames` array to `setServiceInfo` *(council, highest-priority correction)*.**
`packageNames = null` means "listen to ALL packages"; an empty array has ambiguous/inconsistent semantics across versions (historically treated as "all"). `applyMonitoredScope()` **early-returns on an empty set**; the repository rejects empty writes, so an empty array is never reachable. *Why:* under a frozen-set requirement, silently widening to "all" would corrupt the study.

**D6 — `applyMonitoredScope()` mutates the existing `serviceInfo`, never constructs a new one.**
Copy `serviceInfo` (null before connect → return), set only `packageNames`, keep `canRetrieveWindowContent=false`, reassign. *Why:* a fresh `AccessibilityServiceInfo()` wipes `eventTypes`/`feedbackType`/`flags`/`notificationTimeout` from the XML — a silent trap that only surfaces in the F4 PR. XML keeps **no** `packageNames`; runtime overrides it.

**D7 — Own coroutine scope; cancel in `onDestroy`, guard double-launch, `.catch{}` the collect.**
`AccessibilityService` is not a `LifecycleService` → no `lifecycleScope`. Use `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` (`setServiceInfo` touches connection state → main thread). `onServiceConnected` can fire more than once (rebind) → cancel any prior collect job before launching. Cancel the scope in `onDestroy`, **not** `onUnbind` (unbind→rebind would leak/break the collector). `.catch{}` on the collect is mandatory — DataStore emits `IOException` on corruption and a crashing a11y service silently self-disables.

**D8 — Repository validates atomically inside `edit{}` and enforces the study lock at the write level *(council)*.**
`setSelected(pkg, selected): Result<Unit>` does catalog-membership + ≥1 + lock checks inside a single `dataStore.edit{}` (read-modify-write, no race) and returns a `Result` so the ViewModel doesn't optimistically flip a rejected toggle. **The lock lives at the write level, not just greyed toggles** — a grey toggle is cosmetic; if the lock lived only in the UI, a future code path could mutate the set mid-collection → scientifically invalid dataset.

**D9 — `SurfaceType.MIXED` for IG and YouTube.** Both host a feed *and* short-video surface (Reels/Shorts). Tagging `MIXED` records the ambiguity F4 must branch on; cheap now, painful to backfill.

**D10 — `BuildScope.STUDY` const + persisted `studyStarted` *with a callable setter* `(council)`.**
`isSelectionLocked = STUDY && studyStarted`. `studyStarted` persists in DataStore with a callable `setStudyStarted(true)` (reachable via hidden gesture / adb / cohort constant). *Why:* a study build with `STUDY=true` and no way to set `studyStarted` means the lock never engages → the frozen-set requirement is unenforceable. The flag must not ship inert.

**D11 — DI is constructor injection, no new module.** `@Singleton @Inject constructor(dataStore: DataStore<Preferences>)` — Hilt already provides it (like `OnboardingRepository`). Service uses `@AndroidEntryPoint` + `@Inject lateinit var repo` (injected in `super.onCreate()`, safe to touch in `onServiceConnected`). A `@Provides` module is added only if an interface is introduced.

**D12 — Nav sub-screen, lambda-driven, back affordance.** `@Serializable object MonitoredAppsRoute` **outside** the `Destination` enum (the enum feeds the nav rail/bottom bar — it would render a wrong 4th tab). `SettingsScreen(onNavigateToMonitoredApps: () -> Unit)` (more testable than passing `NavController`); its `@Preview` gets `{}`. Screen has a `TopAppBar` with back.

**D13 — Package-drift visibility, not silent data loss *(council)*.** TikTok is `com.zhiliaoapp.musically` in most markets but `com.ss.android.ugc.trill` elsewhere; a participant on the variant gets no monitoring, no error. On connect, log which monitored packages are actually installed (`PackageManager.getPackageInfo` in try/catch); the screen shows an installed/not-installed badge. Record resolved package + versionCode at study start.

## Risks / Trade-offs

- **Live `setServiceInfo()` unreliable across OEMs/Android 16** → reconcile-on-connect is the guaranteed primary (D4); live update is best-effort and its failure is acceptable.
- **Hardcoded package drift / regional variants** → catalog carries known variants (D2/D13); connect-time install check + badge surfaces gaps early; study freezes versions on the reference device.
- **Lock enforced only in UI would allow mid-study mutation** → enforced at repository write level (D8).
- **Empty array silently widening to "all"** → repo rejects empty writes + service early-returns (D5).
- **Crashing a11y service self-disables silently** → `.catch{}` on the collect (D7).
- **Service is usually *disabled* during F3** (no functional reason to enable it pre-F4) → selection still persists; logcat verification (tasks) is explicitly gated on the service being enabled.
- **`AccessibilityService` not JVM-unit-testable** (framework-bound APIs) → extract the pure `Set<String>` → `Array<String>` decision into a testable function; keep the service a thin adapter.

## Migration Plan

Additive only — no existing behavior changes. New files under `data/monitored/`, `config/`, `ui/screens/monitoredapps/`; `ResurfaceAccessibilityService` gains injection + scoping; `SettingsScreen` gains a nav lambda; nav host registers one route. Test dependencies (`kotlinx-coroutines-test`, Turbine, DataStore test) added first. No data migration; new DataStore keys default to empty (→ empty-state prompt).

## Open Questions

- Exact operator mechanism to flip `studyStarted` for a cohort (adb command vs. hidden build-time constant) — resolved at study-setup time; a callable setter is sufficient for F3.
- Whether the installed/not-installed badge needs a "reinstall/switch variant" affordance or is purely informational in F3 (leaning informational; action deferred).

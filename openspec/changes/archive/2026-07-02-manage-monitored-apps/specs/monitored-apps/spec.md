## ADDED Requirements

### Requirement: Curated app catalog

The system SHALL provide a fixed, curated catalog of supported apps. Each catalog entry MUST carry a stable `packageName`, a human-readable `displayName`, and a `SurfaceType` of `FEED`, `SHORT_VIDEO`, or `MIXED`. The catalog is the single source of truth for display metadata; persisted selection stores only package names.

#### Scenario: Catalog exposes tagged entries
- **WHEN** the monitored-apps screen requests the candidate list
- **THEN** the system returns the curated entries (Instagram, TikTok, YouTube, X, Reddit) each with its `displayName` and `SurfaceType`

#### Scenario: Surface type available for a selected app
- **WHEN** another module resolves a selected `packageName` against the catalog
- **THEN** the system returns that app's `SurfaceType` from the catalog rather than from persisted state

### Requirement: Persist monitored selection

The system SHALL persist the set of monitored package names on-device via DataStore and expose it as an observable stream. Only package names present in the curated catalog MAY be persisted. Validation (catalog membership, ≥1, study lock) MUST occur atomically within a single read-modify-write transaction, and each write MUST return a success/failure result so callers do not optimistically assume success.

#### Scenario: Selection survives restart
- **WHEN** the user selects apps and later relaunches the app
- **THEN** the previously selected set is restored from DataStore

#### Scenario: Reject non-catalog package
- **WHEN** a write is attempted for a package name not in the curated catalog
- **THEN** the system rejects the write, returns a failure result, and the persisted set is unchanged

#### Scenario: Write returns explicit result
- **WHEN** any selection write is attempted
- **THEN** the repository returns a success or failure result rather than silently succeeding

### Requirement: Enforce at least one monitored app

The system SHALL reject an empty monitored set once a selection exists. The user MUST NOT be able to deselect the last remaining app.

#### Scenario: Cannot deselect the last app
- **WHEN** exactly one app is selected and the user attempts to deselect it
- **THEN** the toggle is prevented and the app remains selected

#### Scenario: Empty write rejected
- **WHEN** a write of an empty set is attempted on the repository
- **THEN** the write is rejected and the previous set is retained

#### Scenario: Initial empty state prompts selection
- **WHEN** no app has ever been selected and the user opens the monitored-apps screen
- **THEN** the screen shows an empty-state prompt asking the user to select at least one app before leaving

### Requirement: Scope AccessibilityService to monitored apps

The system SHALL scope the AccessibilityService to the monitored set by writing `packageNames` through `setServiceInfo()`. Reconciliation on `onServiceConnected` is the guaranteed primary mechanism: on every connect the service reads the persisted set and applies scope. Live re-scoping on selection change MAY be applied as best-effort, and correctness MUST NOT depend on it. When applying scope the service MUST mutate the existing `serviceInfo` (preserving `eventTypes`, `feedbackType`, `flags`, `notificationTimeout` from XML) rather than construct a new one. The service MUST NOT read window content or perform detection in this capability.

#### Scenario: Scope reconciled on service connect
- **WHEN** the AccessibilityService connects and a non-empty monitored set exists
- **THEN** the service reads the persisted set and sets `serviceInfo.packageNames` to exactly that set, preserving all other `serviceInfo` fields from XML

#### Scenario: Empty set never widens scope
- **WHEN** the resolved monitored set is empty
- **THEN** the service early-returns and never passes an empty or null `packageNames` array to `setServiceInfo` (which would widen monitoring to all packages)

#### Scenario: Live update is best-effort only
- **WHEN** the monitored set changes while the service is running and the live re-scope does not take effect on the device
- **THEN** the correct scope is still guaranteed on the next `onServiceConnected`

#### Scenario: No content retrieval added
- **WHEN** the service applies monitored scope
- **THEN** `canRetrieveWindowContent` remains disabled and `onAccessibilityEvent` performs no detection

#### Scenario: Service failure does not self-disable
- **WHEN** the persisted selection stream emits an error (e.g. DataStore corruption)
- **THEN** the collector catches it and the AccessibilityService keeps running rather than crashing and silently disabling itself

### Requirement: Study-mode selection lock

The system SHALL expose a build-scope flag (`BuildScope.STUDY`) and a persisted, callable `studyStarted` state (with a reachable setter). The lock is `STUDY && studyStarted`. When locked, the selection MUST NOT change, keeping the trigger set constant during data collection. The lock MUST be enforced at the repository write level — rejecting writes — not only by disabling UI toggles.

#### Scenario: Editable before study starts
- **WHEN** study mode is active but the study has not started
- **THEN** the user can freely add and remove monitored apps

#### Scenario: Write rejected when locked
- **WHEN** the lock is active and any code path attempts to change the selection
- **THEN** the repository rejects the write and the persisted set is unchanged

#### Scenario: Lock is reachable
- **WHEN** study mode is active
- **THEN** `studyStarted` can be set to true through a callable setter so the lock actually engages (the flag never ships inert)

#### Scenario: UI reflects the lock
- **WHEN** the lock is active and the monitored-apps screen is shown
- **THEN** the toggles are disabled as a reinforcement of the write-level lock

#### Scenario: Product mode always editable
- **WHEN** study mode is inactive
- **THEN** the selection is editable regardless of any study-started state

### Requirement: Surface installed status of monitored apps

The system SHALL detect whether each monitored package is actually installed and surface that status, so a missing or regional-variant package (e.g. TikTok `com.ss.android.ugc.trill`) does not cause silent, unlogged loss of monitoring. Installed status MUST be checked without crashing when a package is absent.

#### Scenario: Not-installed app is flagged
- **WHEN** the monitored-apps screen is shown and a selected app is not installed on the device
- **THEN** the screen displays a not-installed badge for that app

#### Scenario: Installed packages logged on connect
- **WHEN** the AccessibilityService connects
- **THEN** it logs which monitored packages are actually installed (resolving package presence in a way that tolerates absent packages)

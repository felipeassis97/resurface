## ADDED Requirements

### Requirement: Required permission set

The system SHALL define the permissions Resurface requires for core detection: **Usage Access** (`PACKAGE_USAGE_STATS`), **Accessibility** (the app's accessibility service enabled), and **Notifications** (`POST_NOTIFICATIONS`). Permissions for other features — overlay (`SYSTEM_ALERT_WINDOW`) and Bluetooth — SHALL NOT be requested during onboarding and SHALL be requested just-in-time by their own features.

#### Scenario: Onboarding requests only the core trio
- **WHEN** the Grant permissions step renders
- **THEN** it requests Usage Access, Accessibility, and Notifications
- **AND** it does not request overlay or Bluetooth permissions

### Requirement: Correct grant mechanism per permission type

The system SHALL request each permission using the mechanism its type requires: **special** permissions (Usage Access, Accessibility) SHALL be requested by launching the corresponding system Settings screen via intent; **runtime** permissions (Notifications, API 33+) SHALL be requested via the runtime permission dialog.

#### Scenario: Special permission opens settings
- **WHEN** the user chooses to grant Usage Access or Accessibility
- **THEN** the corresponding system Settings screen is opened via intent

#### Scenario: Runtime permission shows dialog
- **WHEN** the user chooses to grant Notifications on API 33+
- **THEN** the system runtime permission dialog is shown

#### Scenario: Notifications not requested below API 33
- **WHEN** the device API level is below 33
- **THEN** Notifications is treated as not-applicable and does not block completion

### Requirement: Live status checking with on-resume re-evaluation

The system SHALL determine each permission's grant status by querying the OS (AppOps for Usage Access, the enabled-accessibility-services list for Accessibility, the runtime check for Notifications), never from a stored flag. Because special permissions return no result callback, the system SHALL re-evaluate all permission statuses when the screen resumes after the user returns from a Settings screen.

#### Scenario: Status reflects current OS state
- **WHEN** permission status is requested
- **THEN** it is read live from the OS, not from persisted state

#### Scenario: Re-check on return from settings
- **WHEN** the user returns to the app after toggling a special permission in Settings
- **THEN** the permission statuses are re-evaluated on resume and the displayed state updates

### Requirement: Stub accessibility service declared

The app SHALL declare an accessibility service (guarded by `BIND_ACCESSIBILITY_SERVICE` with a service config) so the user can enable it during onboarding. In this change the service MAY be a no-op stub; it SHALL NOT yet perform detection.

#### Scenario: Service is enableable during onboarding
- **WHEN** the user opens accessibility settings from onboarding
- **THEN** the Resurface accessibility service appears and can be enabled

#### Scenario: Stub performs no detection
- **WHEN** the stub accessibility service is enabled and receives events
- **THEN** it performs no detection or intervention in this change

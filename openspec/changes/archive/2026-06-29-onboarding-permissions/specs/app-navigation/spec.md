## MODIFIED Requirements

### Requirement: Three top-level destinations

The app SHALL expose exactly three top-level navigation destinations — **Home**, **Insights**, and **Settings** — reachable from a persistent navigation container within the main app graph. **Home** SHALL be the start destination of the main graph. The main graph SHALL be reachable only after onboarding completes and the required permissions are granted; on launch the app SHALL NOT show Home unconditionally but SHALL first pass through the launch gate.

#### Scenario: Main app shows Home after onboarding
- **WHEN** the launch gate routes to the main app
- **THEN** the Home destination is displayed
- **AND** the Home navigation item is shown selected

#### Scenario: All three destinations present
- **WHEN** the navigation container renders in the main app
- **THEN** Home, Insights, and Settings items are shown, each with a label and an icon

#### Scenario: Destinations gated before onboarding
- **WHEN** the app launches and onboarding is not complete
- **THEN** the three top-level destinations are not reachable
- **AND** the onboarding flow is shown instead

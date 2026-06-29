## ADDED Requirements

### Requirement: Three top-level destinations

The app SHALL expose exactly three top-level navigation destinations — **Home**, **Insights**, and **Settings** — reachable from a persistent navigation container. **Home** SHALL be the start destination shown on launch.

#### Scenario: Launch shows Home
- **WHEN** the app starts
- **THEN** the Home destination is displayed
- **AND** the Home navigation item is shown selected

#### Scenario: All three destinations present
- **WHEN** the navigation container renders
- **THEN** Home, Insights, and Settings items are shown, each with a label and an icon

### Requirement: Type-safe routing

The system SHALL route between destinations using type-safe routes defined as serializable route objects (not raw strings). Each destination SHALL map to a distinct route type wired into a single navigation host.

#### Scenario: Navigate to a destination
- **WHEN** the user selects the Insights navigation item
- **THEN** the host displays the Insights screen
- **AND** the Insights navigation item becomes selected

#### Scenario: Selection reflects current destination
- **WHEN** the displayed destination changes
- **THEN** the selected navigation item updates to match the current route

### Requirement: Single-top reselection and back-stack preservation

When the user selects a top-level item, the system SHALL navigate single-top (no duplicate destination stacked) and SHALL preserve and restore each destination's state across switches. Re-selecting the already-current item SHALL NOT create a new entry.

#### Scenario: Re-selecting current item is a no-op stack-wise
- **WHEN** the user taps the Home item while Home is already shown
- **THEN** no additional Home entry is added to the back stack

#### Scenario: State restored on return
- **WHEN** the user leaves Insights, visits Settings, then returns to Insights
- **THEN** Insights restores its prior state rather than rebuilding from scratch

#### Scenario: System back from a non-start destination
- **WHEN** the user is on Settings and presses system back
- **THEN** the app returns to the Home start destination

### Requirement: Adaptive navigation container

The navigation container SHALL adapt to the current window size: a bottom navigation bar on compact width, and a navigation rail or drawer on medium and expanded widths. The same three destinations and selection state SHALL be used regardless of which form the container takes.

#### Scenario: Compact window uses bottom bar
- **WHEN** the app runs in a compact-width window (phone portrait)
- **THEN** the three destinations are presented in a bottom navigation bar

#### Scenario: Wider window uses rail or drawer
- **WHEN** the app runs in a medium or expanded-width window (tablet / foldable / landscape)
- **THEN** the three destinations are presented in a side navigation rail or drawer
- **AND** selecting an item behaves identically to the compact bottom bar

### Requirement: Theme and edge-to-edge consistency

The shell SHALL render inside the existing `ResurfaceTheme` and SHALL lay out edge-to-edge, applying window insets so content and the navigation container do not collide with system bars. Navigation colors SHALL come from the existing Material 3 color roles, not hard-coded values.

#### Scenario: Shell themed by ResurfaceTheme
- **WHEN** any destination renders
- **THEN** its colors derive from the active `ResurfaceTheme` color scheme

#### Scenario: Insets respected
- **WHEN** content renders edge-to-edge
- **THEN** it is inset so it is not obscured by the status bar, navigation bar, or the navigation container

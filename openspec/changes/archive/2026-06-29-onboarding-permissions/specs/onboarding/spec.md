## ADDED Requirements

### Requirement: First-run onboarding flow

On first run the app SHALL present an onboarding flow with four ordered steps — **Welcome**, **Disclosure**, **Consent**, **Grant permissions** — that the user advances through in order. The user SHALL NOT reach the main app until the flow is completed.

#### Scenario: First launch enters onboarding
- **WHEN** the app launches and onboarding has never been completed
- **THEN** the Welcome step is shown
- **AND** the main navigation shell is not reachable

#### Scenario: Steps advance in order
- **WHEN** the user advances from a step
- **THEN** the next step in the order Welcome → Disclosure → Consent → Grant permissions is shown

### Requirement: Prominent disclosure

The Disclosure step SHALL state, before consent is requested, what the app observes (app usage, scroll/interaction metrics) and what it never captures (screen content, messages, images), and SHALL state that all processing is on-device.

#### Scenario: Disclosure precedes consent
- **WHEN** the user reaches the Consent step
- **THEN** the Disclosure step has already been shown describing observed data and never-captured data

### Requirement: Explicit revocable consent recorded

The system SHALL require an explicit affirmative consent action and SHALL persist a consent record containing a consent flag, a timestamp, and the disclosure version. Consent SHALL be revocable later. The consent record SHALL be stored on-device only.

#### Scenario: Consent recorded on accept
- **WHEN** the user explicitly accepts at the Consent step
- **THEN** a consent record with flag, timestamp, and disclosure version is persisted on-device

#### Scenario: Consent required to proceed
- **WHEN** the user has not given consent
- **THEN** the flow does not advance past the Consent step

### Requirement: Launch gate

On every launch the app SHALL decide between the onboarding flow and the main app based on persisted onboarding/consent state and the live grant status of the required permissions. The decision SHALL NOT trust a persisted "granted" flag for permissions — grant status is queried from the OS each launch.

#### Scenario: Completed onboarding routes to main app
- **WHEN** the app launches, onboarding is completed, consent is recorded, and all required permissions are currently granted
- **THEN** the main navigation shell is shown

#### Scenario: Required permission revoked after completion
- **WHEN** the app launches with consent recorded but a required permission has been revoked in system settings
- **THEN** the user is routed to the Grant permissions step rather than the full Welcome step

#### Scenario: Decision resolves before showing a destination
- **WHEN** the launch gate is still reading persisted state
- **THEN** neither onboarding nor the main shell is shown until the decision resolves

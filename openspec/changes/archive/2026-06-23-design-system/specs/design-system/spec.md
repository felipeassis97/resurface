## ADDED Requirements

### Requirement: Fixed theme configuration

The system SHALL provide a fixed (non-dynamic) Material 3 theme with explicit light and dark color schemes. The theme SHALL NOT derive color from device wallpaper. Dark vs light selection SHALL follow the system setting by default.

#### Scenario: System in light mode
- **WHEN** the OS is in light mode and the app theme is applied
- **THEN** the light color scheme defined in this spec is used

#### Scenario: System in dark mode
- **WHEN** the OS is in dark mode and the app theme is applied
- **THEN** the dark color scheme defined in this spec is used

#### Scenario: Dynamic color unavailable
- **WHEN** the app runs on Android 12+ (API 31+) where dynamic color exists
- **THEN** the app still uses its fixed teal schemes and does NOT adopt wallpaper colors

### Requirement: Light color scheme tokens

The system SHALL define the light Material 3 color scheme with exactly these role values:

```
primary                   #006A65   on-primary                  #FFFFFF
primary-container         #A4F2EA   on-primary-container        #00201F
secondary                 #4A6360   on-secondary                #FFFFFF
secondary-container       #CCE8E3   on-secondary-container      #051F1D
tertiary                  #7C5800   on-tertiary                 #FFFFFF
tertiary-container        #FFDEA1   on-tertiary-container       #271900
error                     #BA1A1A   on-error                    #FFFFFF
error-container           #FFDAD6   on-error-container          #410002
surface                   #F5FAF7   on-surface                  #161D1C
on-surface-variant        #3F4946
surface-container-lowest  #FFFFFF   surface-container-low       #EFF5F2
surface-container         #EAEFEC   surface-container-high      #E4EAE7
surface-container-highest #DEE4E1
surface-dim               #D5DBD8   surface-bright              #F5FAF7
outline                   #6F7A77   outline-variant             #BFC9C5
inverse-surface           #2B3231   inverse-on-surface          #ECF2EF
inverse-primary           #5DDBD1
```

#### Scenario: Light primary applied
- **WHEN** a high-emphasis filled control renders in light mode
- **THEN** its container is `#006A65` and its label/icon is `#FFFFFF`

#### Scenario: Light surface background
- **WHEN** a screen background renders in light mode
- **THEN** it uses `surface #F5FAF7` (a cool off-white, never pure `#FFFFFF`) with `on-surface #161D1C` text

### Requirement: Dark color scheme tokens

The system SHALL define the dark Material 3 color scheme with exactly these role values:

```
primary                   #5DDBD1   on-primary                  #003735
primary-container         #00504C   on-primary-container        #A4F2EA
secondary                 #B1CCC7   on-secondary                #1C3531
secondary-container       #334B48   on-secondary-container      #CCE8E3
tertiary                  #F2BE48   on-tertiary                 #412D00
tertiary-container        #5E4200   on-tertiary-container       #FFDEA1
error                     #FFB4AB   on-error                    #690005
error-container           #93000A   on-error-container          #FFDAD6
surface                   #0E1413   on-surface                  #DEE4E1
on-surface-variant        #BFC9C5
surface-container-lowest  #090F0E   surface-container-low       #161D1C
surface-container         #1A2120   surface-container-high      #242B2A
surface-container-highest #2F3635
surface-dim               #0E1413   surface-bright              #343B39
outline                   #899491   outline-variant             #3F4946
inverse-surface           #DEE4E1   inverse-on-surface          #2B3231
inverse-primary           #006A65
```

#### Scenario: Dark surface is deep ink not black
- **WHEN** a screen background renders in dark mode
- **THEN** it uses `surface #0E1413` (deep water ink, not `#000000`)

#### Scenario: Dark primary contrast
- **WHEN** a filled control renders in dark mode
- **THEN** its container is `#5DDBD1` with `on-primary #003735` label/icon

### Requirement: Custom success color role

The system SHALL define a custom `success` color role beyond the standard M3 set, harmonized toward primary. Values: light `success #3B6939` / `on-success #FFFFFF` / `success-container #BCF0B4` / `on-success-container #00210A`; dark `success #A1D399` / `on-success #0A3910` / `success-container #235021` / `on-success-container #BCF0B4`.

#### Scenario: Positive reinforcement uses success not primary
- **WHEN** the UI confirms the user chose to stop scrolling / kept focus
- **THEN** it uses the `success` role, visually distinct from `primary` teal

### Requirement: Semantic state-to-color mapping

The system SHALL map app lifecycle states to color roles as follows, and SHALL NOT use the `error` role for routine scroll interventions.

| State | Role |
|-------|------|
| Armed (ambient) | secondary / surface |
| Active (monitoring) | primary |
| Intervene (nudge) | tertiary (amber) |
| Focus kept (positive) | success |
| True fault (BLE/permission) | error |

#### Scenario: Scroll nudge is warm, not alarming
- **WHEN** the app interrupts a mindless-scrolling session
- **THEN** the intervention surface uses `tertiary`/`tertiary-container` (amber)
- **AND** it does NOT use the `error` (red) role

#### Scenario: Error reserved for faults
- **WHEN** a BLE connection or required permission fails
- **THEN** the `error` role is used

### Requirement: Color pairing and contrast

The system SHALL only pair container roles with their matching `on-` role. Normal-size text SHALL meet ≥4.5:1 contrast; large text, icons, and interactive borders SHALL meet ≥3:1. Dividers SHALL use `outline-variant`; interactive boundaries needing 3:1 SHALL use `outline`.

#### Scenario: Divider uses outline-variant
- **WHEN** a divider or decorative card border renders
- **THEN** it uses `outline-variant`, not `outline`

#### Scenario: Text-field border uses outline
- **WHEN** an outlined text field renders its boundary
- **THEN** it uses `outline` (≥3:1 against surface)

### Requirement: Font resources

The system SHALL bundle two font families as Android font resources: **Fraunces** (weights Light 300, Regular 400, SemiBold 600, plus Italic 400) and **Plus Jakarta Sans** (weights Regular 400, Medium 500, SemiBold 600, Bold 700). Font files SHALL live in `res/font/` with flat, lowercase, underscore-only names so `R.font.*` is generated.

#### Scenario: Fonts resolvable as resources
- **WHEN** the theme references a bundled weight (e.g. `R.font.fraunces_light`)
- **THEN** the resource exists in `res/font/` and compiles

#### Scenario: No Medium Fraunces synthesized
- **WHEN** a type role would otherwise need Fraunces Medium 500
- **THEN** the system uses an available weight (Light 300 or Regular 400) instead of synthesizing 500

### Requirement: Typography scale

The system SHALL define the full M3 type scale mapped to the two families with these families, weights, sizes (sp), line heights (sp), and tracking:

```
Role          Family         Weight        Size/Line  Tracking
Display L     Fraunces       Light 300     57 / 64    -0.5
Display M     Fraunces       Light 300     45 / 52    -0.25
Display S     Fraunces       Light 300     36 / 44     0
Headline L    Fraunces       Regular 400   32 / 40     0
Headline M    Fraunces       Regular 400   28 / 36     0
Headline S    Fraunces       Regular 400   24 / 32     0
Title L       Plus Jakarta   SemiBold 600  22 / 28     0
Title M       Plus Jakarta   SemiBold 600  16 / 24    +0.15
Title S       Plus Jakarta   SemiBold 600  14 / 20    +0.1
Body L        Plus Jakarta   Regular 400   16 / 24    +0.5
Body M        Plus Jakarta   Regular 400   14 / 20    +0.25
Body S        Plus Jakarta   Regular 400   12 / 16    +0.4
Label L       Plus Jakarta   SemiBold 600  14 / 20    +0.1
Label M       Plus Jakarta   Medium 500    12 / 16    +0.5
Label S       Plus Jakarta   Medium 500    11 / 16    +0.5
```

Fraunces SHALL NOT be applied to any role below 24sp. Emphasized variants: Display/Headline emphasized use Fraunces SemiBold 600; Title/Label emphasized use Plus Jakarta Bold 700; reflective-accent words may use Fraunces Italic 400.

#### Scenario: Display uses airy Light serif
- **WHEN** a Display-scale text renders (e.g. the nudge question)
- **THEN** it is set in Fraunces Light 300 at the specified size

#### Scenario: Body uses sans
- **WHEN** any Body or Label text renders
- **THEN** it is set in Plus Jakarta Sans, never Fraunces

#### Scenario: Stat numbers are tabular
- **WHEN** usage numbers (e.g. minutes scrolled) render and may update
- **THEN** Plus Jakarta tabular figures are used so digit width does not jitter

### Requirement: Shape tokens

The system SHALL define M3 shape corners leaning to the rounded set: extra-small 4dp, small 8dp, medium 12dp, large 16dp, large-increased 20dp, extra-large 28dp, full (pill). Buttons and chips SHALL be `full`; cards SHALL be `large` (16dp); the intervention sheet/dialog SHALL be `extra-large` (28dp).

#### Scenario: Nudge sheet is soft
- **WHEN** the intervention surface renders
- **THEN** its corners are `extra-large` (28dp)

#### Scenario: Buttons are pills
- **WHEN** a button renders
- **THEN** its shape is `full`

### Requirement: Elevation tokens

The system SHALL communicate depth through tonal surface-container colors with near-zero drop shadows. Shadow elevation SHALL be reserved for elements needing separation from busy backgrounds (intervention sheet, FAB) at M3 level 3.

#### Scenario: Resting card has no shadow
- **WHEN** a card renders at rest
- **THEN** depth comes from a `surface-container*` tone, not a drop shadow

### Requirement: Spacing tokens

The system SHALL define spacing on an 8dp grid: 4, 8, 12, 16, 24, 32, 48 (dp). Screen margins SHALL be 16dp on compact and 24dp on medium+ window widths.

#### Scenario: Compact screen margin
- **WHEN** content renders on a compact-width window
- **THEN** the horizontal screen margin is 16dp

### Requirement: Motion tokens

The system SHALL define a calm "tidal" motion language: an armed-indicator breathing loop (≈4s inhale / 6s exhale); the intervention surface entering from the bottom with emphasized-decelerate easing `cubic-bezier(0.05,0.7,0.1,1)` over ~400ms and exiting with emphasized-accelerate over ~200ms; the usage counter rising via a low-stiffness spring. When the system reduce-motion setting is on, the breathing loop SHALL become a static glow and the intervention SHALL cross-fade instead of sliding.

#### Scenario: Nudge rises from bottom
- **WHEN** an intervention is triggered with motion enabled
- **THEN** the surface slides up from the bottom with emphasized-decelerate easing over ~400ms

#### Scenario: Reduced motion respected
- **WHEN** the system reduce-motion setting is enabled
- **THEN** the breathing loop is replaced by a static glow and the intervention cross-fades

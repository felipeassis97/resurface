## Why

Resurface is a digital-wellbeing app whose entire purpose is to interrupt mindless scrolling without coercion. Its UI must feel the opposite of the dopamine-loud feeds it counters: calm, spacious, autonomy-supportive. Today there is no defined visual language. We need a Material 3 design system — color, typography, shape, elevation, spacing, motion — that encodes the product's ethics (no dark patterns, gentle intervention) directly into its tokens, so every screen built later stays consistent and on-brand.

## What Changes

- Introduce a fixed (non-dynamic) Material 3 theme built around a **calm teal** brand identity ("surfacing for air").
- Define **light + dark color schemes** as complete M3 token sets (29+ roles each), hand-tuned from four key colors (teal / slate-teal / dawn-amber / cool-neutral) plus a custom `success` role.
- Establish a **semantic state→color mapping**: armed→secondary/surface, active→primary, intervene→**tertiary amber (never red)**, focus-kept→success, error reserved for true faults.
- Adopt **two typefaces** already added to the repo — **Fraunces** (Display/Headline) and **Plus Jakarta Sans** (Title/Body/Label/data) — and map the **full M3 type scale** to them, accounting for Fraunces' available weights only: **Light 300 / Regular 400 / SemiBold 600** (no Medium 500).
- Define **shape, elevation (tonal, near-zero shadow), 8dp spacing, and "tidal" motion** tokens including the breathing-indicator and rise-from-bottom nudge.
- Decision: **dynamic color OFF** for now (stable brand); future one-line toggle noted.
- Note two implementation gotchas to resolve: `res/fonts` → `res/font` (flat, lowercase) and thin-serif-on-dark legibility QA.

## Capabilities

### New Capabilities
- `design-system`: The complete Material 3 token foundation for Resurface — color schemes (light/dark + custom roles), typography scale mapped to the two bundled fonts, shape/elevation/spacing/motion tokens, and the theme configuration (fixed, non-dynamic).

### Modified Capabilities
<!-- none — greenfield -->

## Impact

- **New theme code (later, via /opsx:apply)**: `ui/theme/` — `Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt` (Jetpack Compose Material3).
- **Resources**: font files must move `app/src/main/res/fonts/**` → `app/src/main/res/font/` (flat, lowercase names) so `R.font.*` generates.
- **Dependencies**: `androidx.compose.material3` (+ adaptive), Compose BOM; no new third-party libs.
- **Downstream**: every future screen (onboarding, consent, permissions, pairing, insights dashboard, intervention overlay) consumes these tokens. No app behavior changes in this change — tokens + theme only.

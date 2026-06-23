## 1. Font resources

- [x] 1.1 Move font files from `app/src/main/res/fonts/**` to flat `app/src/main/res/font/`
- [x] 1.2 Rename to lowercase/underscore: `fraunces_light.ttf`, `fraunces_regular.ttf`, `fraunces_semibold.ttf`, `fraunces_italic.ttf`, `plus_jakarta_regular.ttf`, `plus_jakarta_medium.ttf`, `plus_jakarta_semibold.ttf`, `plus_jakarta_bold.ttf`
- [x] 1.3 Delete stray `.DS_Store` files from the resource dirs
- [x] 1.4 Confirm `R.font.*` IDs generate (build succeeds) — verified: all 8 symbols in R.txt, app-debug.apk built

## 2. Validate palette against M3

- [ ] 2.1 Enter the 4 key colors (teal/slate-teal/amber/neutral) into Material Theme Builder — _deferred: GUI tool, not runnable headless. Spec values are hand-tuned source of truth; run in MTB before final polish if exact HCT stops wanted._
- [ ] 2.2 Export and diff generated tonal stops against the spec's light/dark values; reconcile drift — _deferred with 2.1._
- [x] 2.3 Verify standard contrast for every `on-` pairing (≥4.5:1 text, ≥3:1 large/borders) — verified via WCAG script: 24/24 pairings pass (light + dark), 0 failures

## 3. Color theme code

- [x] 3.1 Create `ui/theme/Color.kt` with all light + dark role values from the spec
- [x] 3.2 Add custom `success`/`on-success`/`success-container`/`on-success-container` (light + dark); harmonize toward primary
- [x] 3.3 Build `lightColorScheme(...)` and `darkColorScheme(...)` (no dynamic color)
- [x] 3.4 Expose custom `success` roles via a theme extension (CompositionLocal) since M3 ColorScheme has no success slot

## 4. Typography code

- [x] 4.1 Define `FontFamily` for Fraunces (Light 300, Regular 400, SemiBold 600, Italic) and Plus Jakarta (400/500/600/700)
- [x] 4.2 Create `ui/theme/Type.kt` mapping all 15 M3 roles per the spec table (families, weights, sizes, line heights, tracking)
- [x] 4.3 Enforce Fraunces-never-below-24sp; map would-be Medium roles to Light/Regular (no synthesized 500)
- [x] 4.4 Enable tabular figures for numeric/stat text styles (`ResurfaceTextStyles.statDisplay`/`statBody`, `tnum`)

## 5. Shape, elevation, spacing, motion

- [x] 5.1 Create `ui/theme/Shape.kt` (extra-small 4 → extra-large 28; buttons full, cards 16, sheet 28)
- [x] 5.2 Define spacing constants on the 8dp grid (4/8/12/16/24/32/48) — `Spacing.kt`
- [x] 5.3 Define motion specs: breathing loop (4s/6s), nudge emphasized-decelerate enter 400ms / accelerate exit 200ms, counter spring — `Motion.kt`
- [ ] 5.4 Wire reduce-motion fallback (static glow + cross-fade) — _deferred: contract documented in `Motion.kt`; live wiring belongs to the intervention component (out of scope for this tokens-only change)._

## 6. Theme assembly

- [x] 6.1 Create `ui/theme/Theme.kt` `ResurfaceTheme` composable wiring colorScheme + typography + shapes (light/dark by system setting, dynamic OFF)
- [x] 6.2 Add the `success` theme extension into the theme provider (`LocalResurfaceColors` + `resurfaceColors`)

## 7. Verification

- [x] 7.1 Build a token preview screen (swatches + full type scale, light + dark) — `ThemePreview.kt` with two @Preview functions
- [ ] 7.2 QA thin-serif-on-dark: confirm Display S Light 300 legible on `surface #0E1413`; if weak, bump Display S to Regular 400 in dark scheme only — _deferred: needs visual render in Android Studio / on device. Preview built (7.1); open `Tokens · Dark` preview to judge._
- [x] 7.3 Run an MD3 compliance pass on the theme module — clean: no hardcoded colors outside Color.kt, no dynamic color, correct token + outline/outline-variant usage

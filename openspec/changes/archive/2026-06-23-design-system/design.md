## Context

Resurface (see README) detects mindless scrolling and intervenes gently, on-device, autonomy-first. The UI must read as the opposite of Instagram/TikTok: calm, spacious, low-arousal — "surfacing for air." Target platform is **Jetpack Compose + Material 3** (Compose-first per Google I/O 2026). Two fonts are already in the repo: **Fraunces** and **Plus Jakarta Sans**. This document records the design decisions behind the token system; the normative token values live in `specs/design-system/spec.md`.

Brand thesis: *coming up from underwater*. Submerged in the scroll → break the surface → breathe. Water, breath, dawn. This single image drives palette (teal water + amber dawn), motion (tidal/breathing), and type (airy Light serif).

## Goals / Non-Goals

**Goals:**
- A complete, fixed Material 3 theme (light + dark) usable by every future screen.
- Encode product ethics in tokens: gentle intervention = warm amber, never alarm-red.
- Map the full M3 type scale onto the two bundled fonts using only their real weights.
- Calm, "tidal" motion language and soft, rounded shapes.
- Accessibility: M3 contrast pairings, ≥4.5:1 body text, ≥3:1 large/borders, 48dp targets.

**Non-Goals:**
- No dynamic (wallpaper-based) color in this change.
- No screen layouts, components, or app logic — tokens + theme only.
- No new fonts beyond the two already added; no icon set decisions.
- No M3 Expressive shape-morph adoption yet (motion stays simple/calm).

## Decisions

### D1 — Brand palette from 4 key colors, not the M3 purple default
Teal primary `#006A65`, slate-teal secondary, **dawn-amber tertiary** `#7C5800`, cool neutral. Rejected the M3 baseline purple (generic) and the three AI-default looks (cream+terracotta / black+acid-green / broadsheet). Teal=calm water, amber=dawn/human warmth — chosen for this brief.
- *Alternative considered:* dynamic color from wallpaper → rejected (D5); brand stability wins for a calm identity.

### D2 — Semantic state→color mapping (the core ethical decision)
| App state | Role | Rationale |
|-----------|------|-----------|
| Armed (ambient) | secondary / surface | quiet presence, no nagging |
| Active (monitoring) | primary (teal) | calm attention |
| **Intervene (nudge)** | **tertiary (amber)** | warm tap on shoulder, **not** red alarm = no coercion |
| Focus kept (positive) | custom `success` (green) | gentle reward |
| True error | error (red) | reserved for BLE/permission faults only |
- Using red for the nudge would reproduce the punishment/dark-pattern the product explicitly forbids. Amber is the deliberate anti-alarm.

### D3 — Custom `success` color, harmonized
A green `success` role (light `#3B6939` / dark `#A1D399`) extends the M3 set for the "you chose to stop" moment. It SHALL be harmonized toward primary (`Blend.harmonize`) so it sits inside the teal family.

### D4 — Typography mapped to real Fraunces weights only
Fraunces ships **Light 300 / Regular 400 / SemiBold 600 / Italic** — **no Medium 500**. Instead of synthesizing 500, the scale uses Light for Display, Regular for Headline, SemiBold as the emphasized cap. Light-at-Display is *more* on-brand (airy = breath) than Medium would be. Fraunces is never used below 24sp (Headline S floor); all Title/Body/Label use Plus Jakarta Sans, keeping thin serif large-only for legibility.
- *Alternative considered:* let Compose synthesize Medium → rejected (fake interpolated weight, inconsistent rendering).

### D5 — Dynamic color OFF
Fixed schemes via `lightColorScheme`/`darkColorScheme`. Keeps a stable brand face. Re-enabling later is a one-line swap to `dynamicLightColorScheme`/`dynamicDarkColorScheme` gated on `Build.VERSION.SDK_INT >= S` — noted as a future toggle, not built now.

### D6 — Tonal elevation, near-zero shadows; soft shapes; 8dp spacing
Depth via surface-container tones, not drop shadows (calm = flat). Corners lean to the rounded/expressive-increased set (cards 16dp, nudge sheet 28dp, buttons full). Spacing on the 8dp grid (I/O 2026 token system).

### D7 — "Tidal" motion as the signature
Breathing armed-indicator (4s inhale / 6s exhale loop); nudge **rises from bottom** with emphasized-decelerate (400ms) like breaking the surface; minute counter rises like a tide. `prefers-reduced-motion`/system setting → breathing becomes static glow, nudge cross-fades.

## Risks / Trade-offs

- **Thin serif on dark surfaces** → Display Light 300 at 36sp may shimmer on `surface #0E1413`. *Mitigation:* Light only ≥36sp; Headlines already Regular 400; QA gate — if Display S reads weak on dark, bump that one role to Regular 400 in dark scheme only.
- **Font resource folder wrong** → files are in `res/fonts/` with subdirs; Android needs flat `res/font/` with lowercase names or `R.font.*` won't generate. *Mitigation:* move/rename as first task.
- **Hand-tuned tonal values may drift from exact HCT stops** → *Mitigation:* validate the 4 key colors through Material Theme Builder, export `Color.kt`, diff against the spec values; verify all three contrast levels.
- **Serif render cost** → Fraunces variable/static adds APK weight and layout cost vs all-sans. *Mitigation:* ship only the 4 needed weights; serif limited to large, infrequent text.
- **Custom `success` outside dynamic theming** → fine here since dynamic is off (D5); revisit if D5 is ever reversed.

## Open Questions

- Bundle Fraunces/Jakarta as **static per-weight ttf** or single **variable** files? (Affects `res/font` setup and APK size.)
- Big stat numbers: keep Plus Jakarta tabular figures, or introduce an instrument-style numeral face later? (Currently: Jakarta tabular, no third font.)
- Exact haptic-pattern ↔ visual-intensity correspondence for the escalation ladder (overlay → phone → wristband) — design-adjacent, may live in a later change.

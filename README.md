# 🎯 Resurface


## Technical Documentation — Android App for Mindless Scrolling Detection and Interruption + BLE Haptic Wristband


**System architecture and engineering document.**
This document consolidates hardware, Android APIs, detection methodology, AI layer, accessibility considerations, lifecycle management (foreground/background), component integration, BLE contract, and wristband firmware.

Companion document to the research proposal.

> **v2 — Simplified wristband.** The wristband was slimmed down for a wearable, validation-first build: a tiny, ultra-low-power **Seeed XIAO nRF52840** driving the existing ERM motor through a single **MOSFET**, powered by a small LiPo. This replaces the earlier ESP32-S3 Feather + DRV2605L + I²C multiplexer design. The app side is unchanged.

---

## Table of Contents

1. System Overview and Architecture
2. Hardware (BLE Wristband)
3. Software (Android App Modules)
4. Android APIs and Features
5. Lifecycle: Foreground vs. Background
6. Detection Methodology
7. AI Layer
8. Intervention
9. Component Integration (Data Flow)
10. BLE Contract
11. Wristband Firmware
12. Accessibility and Policies
13. Privacy, Security, and Ethics
14. Data Logging (for the Study)
15. Summary of Decisions and Open Questions

---

## 1. System Overview and Architecture

The system consists of two physical nodes:

* **Android Smartphone** (system brain)
* **BLE Wristband** (optional haptic actuator, built around a Seeed XIAO nRF52840)

The two devices communicate through **Bluetooth Low Energy (BLE)**.

The application remains **armed** in a lightweight state at all times and only becomes **active** when the user opens a monitored application (Instagram, TikTok, etc.).

Upon detecting a **mindless scrolling** state, the system performs a gentle, autonomy-supportive intervention through:

* On-screen overlay
* Smartphone vibration
* Wristband vibration

### General Architecture

```text
┌──────────────────────────────────────────────────────────┐
│                    ANDROID SMARTPHONE                    │
│                                                          │
│ [AccessibilityService] ── scroll/window events          │
│          │                                               │
│          ▼                                               │
│ [Detection Engine] ◄── UsageStatsManager                │
│          │            ◄── SensorManager (optional)      │
│          ▼                                               │
│ [JITAI Decision Engine] ◄── ML/Analytics                │
│          │                                               │
│          ├──► Overlay                                    │
│          ├──► Vibrator                                   │
│          ├──► Gemini Nano / ML Kit                       │
│          └──► BLE Client ────────────────┐               │
│                                          │               │
│ [On-Device Logger]                       │               │
└──────────────────────────────────────────┼───────────────┘
                                           │ BLE GATT
                                           ▼
┌──────────────────────────────────────────────────────────┐
│               XIAO nRF52840 WRISTBAND (BLE)              │
│                                                          │
│ [GATT Server] → [PWM pin] → [MOSFET + diode] → [ERM Motor]│
│ [Optional IMU — Sense variant] → Notify                  │
│ [LiPo Battery + onboard charger]                         │
└──────────────────────────────────────────────────────────┘
```

## Core Principle: "Armed vs. Active"

### Armed

Always running:

* AccessibilityService

### Active

Only activated when a monitored app is opened:

* Detection engine
* BLE
* Overlay
* Foreground Service

When the monitored application is closed, all components are shut down.

---

# 2. Hardware (BLE Wristband)

The wristband is a **lean, single-motor** design optimized for a wrist-worn device and for validating the core idea (BLE command → vibration). It reuses the ERM motor already owned.

**Why this replaced the earlier Feather + DRV2605L design:** the XIAO nRF52840 is far smaller (21 × 17.5 mm), draws dramatically less power (≈5 µA deep sleep vs. the ESP32's Wi-Fi-class consumption), has an onboard LiPo charger, and needs fewer parts. A single MOSFET replaces the I²C haptic driver. The DRV2605L can be reintroduced later if rich, pre-baked haptic effects become necessary.

## 2.1 Minimum Bill of Materials (Lean Single-Motor Build)

| Component | Part | Function |
|------------|------|----------|
| MCU + BLE | **Seeed XIAO nRF52840** (pre-soldered) | Nordic nRF52840, BLE 5.x, USB-C, onboard LiPo charger (BQ25101), ~5 µA deep sleep, 21 × 17.5 mm |
| Motor driver | **N-channel MOSFET 2N7000** (TO-92) | Low-side switch for the motor; PWM controls intensity |
| Flyback diode | **Schottky 1N5819** (DO-41) | Protects the MOSFET/MCU from inductive kickback |
| Gate resistor | **~220 Ω** | Limits gate current |
| Pull-down resistor | **~10 kΩ** | Keeps the MOSFET off when the pin is idle |
| Motor | **ERM coin motor** | Vibration (already owned) |
| Battery | **LiPo 3.7 V ~120 mAh** (Core Electronics CE04374) | Small, protected power cell |

## 2.2 Optional Components

| Component | Part | Purpose |
|------------|------|---------|
| IMU | **XIAO nRF52840 Sense** variant (built-in 6-axis IMU) or external breakout | Flick sensing (wristband as sensor + actuator) |
| Higher-quality haptics | LRA motor + dedicated driver | Better haptic feel (adds parts/complexity) |
| Rich effect library | Reintroduce DRV2605L (I²C) | Pre-baked haptic waveforms instead of PWM patterns |

## 2.3 Wiring (Low-Side MOSFET Switch)

```text
XIAO PWM GPIO ──[220 Ω]──► MOSFET Gate
MOSFET Gate ──[10 kΩ]──► GND        (pull-down)
MOSFET Source ─────────► GND
Motor:  V+ (3V3 / VBAT) ──► one lead
        MOSFET Drain     ──► other lead
Flyback diode across the motor:
        cathode (band) ► V+
        anode          ► Drain
Battery ──► XIAO BAT+ / BAT− solder pads
```

## 2.4 Hardware Notes

* The plain XIAO nRF52840 has **no onboard IMU** — use the **Sense** variant if sensing is required.
* The XIAO has **battery solder pads (BAT+ / BAT−)**, not a JST connector; the LiPo leads are soldered directly (its JST plug is removed or wired in).
* The ERM motor runs fine at **3.3–3.7 V** (slightly reduced amplitude vs. 5 V) — perfectly adequate for a "buzz."
* **Prototype on a breadboard powered over USB** before soldering the battery.
* The ESP32-S3 CAN (TWAI) bus and STEMMA QT / I²C chain from the previous design are **no longer used**.

---

# 3. Software (Android App Modules)

## Sensing Layer

* AccessibilityService
* UsageStatsManager
* SensorManager (optional)

## Detection Engine

Responsible for:

* Feature extraction
* Usage pattern detection
* Attention-state classification

## Decision Engine (JITAI)

Responsible for:

* Deciding whether to intervene
* Choosing the timing
* Selecting the intervention modality

## AI Layer

### Analytics / Machine Learning

* Risk prediction
* Pattern discovery

### Generative AI

* Gemini Nano
* Personalized nudges
* Reflective summaries

## Intervention Dispatcher

Triggers:

* Overlay
* Phone vibration
* Wristband BLE commands

## BLE Client

Responsible for:

* Discovery
* Connection
* Writing commands to the wristband

## Logger

Local logging of:

* Sessions
* Detections
* Interventions

## User Interface

* Onboarding
* Consent
* Permissions
* Pairing
* Insights dashboard

---

# 4. Android APIs and Features

| API / Feature | Purpose |
|---------------|---------|
| UsageStatsManager | App usage tracking |
| AccessibilityService | Scroll and window changes |
| SYSTEM_ALERT_WINDOW | Overlay |
| Foreground Service (connectedDevice) | Persistent BLE |
| Bluetooth GATT | Wristband communication |
| SensorManager | IMU access |
| Vibrator / VibratorManager | Local vibration |
| Notifications | Nudges and FGS |
| ML Kit GenAI | Gemini Nano |
| LiteRT | On-device inference |
| BOOT_COMPLETED | Restart handling |

## Foreground Service

Avoid:

* `dataSync`
* `shortService`

Use:

* `connectedDevice`

---

# 5. Lifecycle

## 5.1 Always Armed

The AccessibilityService remains active:

* No timeout
* No persistent notification
* No active BLE connection

## 5.2 Activation

When detecting:

```text
TYPE_WINDOW_STATE_CHANGED
```

for a monitored application:

1. Start session
2. Activate detection
3. Start Foreground Service
4. Connect BLE
5. Enable overlay

When exiting:

1. End session
2. Disconnect BLE
3. Stop Foreground Service
4. Remove overlay

## 5.3 Reboot

After device restart:

* Android restores the AccessibilityService
* Service resumes after the first device unlock

## 5.4 Why Not Start Everything at Boot?

Because:

* It is unnecessary
* Android restricts Foreground Services started through BOOT_COMPLETED

---

# 6. Detection Methodology

## 6.1 Signal Fusion

### UsageStatsManager

* Active application
* Session duration
* Time of day

### AccessibilityService

* Scrolls per minute
* Continuity
* Content changes

### IMU (Optional)

* Repetitive flicks
* Thumb movement

---

## 6.2 Feed vs. Short Video Detection

### Feed

Events:

```text
TYPE_VIEW_SCROLLED
```

Metrics:

* Scrolls/minute
* Scrolling speed

### Short Videos

Metrics:

* Videos/minute
* Content transitions

Requires empirical validation.

---

## 6.3 Operational Definition

Characteristics:

* Fast scrolling
* Long session
* Few pauses
* Passive consumption
* Time distortion

---

## 6.4 Classifier

### MVP

Heuristic model:

```text
speed
+ continuity
+ duration
+ passivity
```

### Future Evolution

Lightweight model:

* LiteRT
* Trained on-device

Important:

> LLMs will not be used for detection.

---

# 7. AI Layer

## 7.1 ML / Analytics

### Goals

* Identify patterns
* Discover risk contexts
* Personalize interventions

### Examples

* Usage immediately after waking up
* Late-night usage
* Weekend usage

---

## 7.2 Generative AI

Gemini Nano:

* Personalized nudges
* Reflective summaries
* Micro-education

Executed within:

* AICore
* Private Compute Core

No data is sent to external servers.

---

# 8. Intervention

## Overlay

Example:

> You have been scrolling for 22 minutes. Is this still what you want to be doing?

Buttons:

* Continue
* Take a break
* View statistics

## Micro-Friction

A small temporal interruption.

## Phone Vibration

Triggered through VibratorManager.

## Wristband Vibration

Triggered through BLE.

## Escalation Strategy

```text
Overlay
    ↓
Phone vibration
    ↓
Wristband vibration
```

Always preserving user autonomy.

---

# 9. Data Flow

```text
User opens Instagram
        │
        ▼
AccessibilityService detects
        │
        ▼
Session starts
        │
        ▼
BLE connects
        │
        ▼
Scroll events received
        │
        ▼
Detection engine
        │
        ▼
Classification
        │
        ▼
JITAI engine
        │
        ▼
Intervention
        │
 ┌──────┼─────────────┐
 │      │             │
 ▼      ▼             ▼
Overlay Phone Vib. BLE
        │
        ▼
User response
        │
        ▼
Logger
        │
        ▼
Session ends
```

---

## 10. BLE Contract

## Custom GATT Service

128-bit UUIDs.

> Note: because the wristband drives the motor directly via PWM (no DRV2605L), `effect_id` selects a **firmware-defined PWM pattern** rather than a chip effect-library entry. The command structure below is unchanged.

### Characteristic: Command

| Property | Payload |
|-----------|----------|
| Write / Write No Response | effect_id + intensity + duration |

Format:

```text
[effect_id (1B)]
[intensity (1B)]
[duration_ms (2B)]
```

---

### Characteristic: Status/Battery

| Property | Payload |
|-----------|----------|
| Read / Notify | battery + state |

Format:

```text
[battery_%]
[state]
```

---

### Characteristic: IMU

| Property | Payload |
|-----------|----------|
| Notify | acceleration |

Format:

```text
ax ay az ...
```

*(Only if the Sense variant / an IMU is fitted.)*

---

### Characteristic: Device Info

Standard service:

```text
0x180A
```

---

## Recommendations

* Use Write No Response
* Enable automatic reconnection
* Store vibration patterns in firmware

---

# 11. Wristband Firmware (XIAO nRF52840)

## Stack

* Arduino IDE + **Seeed nRF52 Boards** package
* **Bluefruit** (Adafruit nRF52) BLE library
* Built-in **PWM** output — no external driver library needed

---

## Initialization

1. Start BLE
2. Create GATT Server (custom service + characteristics)
3. Configure the MOSFET **gate pin as a PWM output** (idle LOW)
4. Start advertising
5. Initialize IMU (optional — Sense variant)

---

## Main Loop

When receiving:

```text
Command Write
```

Execute:

```text
effect_id
→ intensity  (PWM duty cycle)
→ duration   (pulse length)
→ vibration  (PWM on the gate pin)
```

Also publish:

* battery
* status
* IMU data (optional)

---

## Power

* ~120 mAh LiPo
* nRF52840 deep sleep (~5 µA)
* Onboard BQ25101 charger (USB-C)
* Wake on BLE

---

## Effects (firmware-generated PWM patterns)

| ID | Effect |
|----|---------|
| 0x01 | Gentle alert (short, low-intensity pulse) |
| 0x02 | Firm alert (strong / double pulse) |
| 0x03 | Escalation (ramping intensity) |

---

# 12. Accessibility and Policies

## 12.1 AccessibilityService

Required for:

* Scroll monitoring
* Window change detection

Google Play requirements:

* Formal declaration
* Explicit consent
* Prominent disclosure

---

## 12.2 Advanced Protection Mode

Future Android versions may restrict AccessibilityService.

Mitigation:

* Base detection primarily on UsageStatsManager.

---

## 12.3 App Accessibility

Requirements:

* Adequate contrast
* Screen reader compatibility
* Large touch targets
* Accessible overlays

---

# 13. Privacy, Security, and Ethics

## Data Minimization

Capture only:

* Metrics
* Behavioral patterns

Never capture:

* Screen content
* Messages
* Images

---

## Local Processing

Everything runs on-device:

* Detection
* Analytics
* Generative AI

---

## Consent

Users must be informed:

* What is observed
* How it is used
* How consent can be revoked

---

## BLE

Transmit only:

* Commands
* Battery information
* Device status

---

## Ethical Principle

Support autonomy.

Avoid:

* Punishment
* Coercion
* Dark patterns

---

# 14. Data Logging

Per session:

```text
timestamp
app
time
optional location
```

Metrics:

* Speed
* Continuity
* Videos/minute
* Duration

Classification:

* Detected state
* Confidence score

Intervention:

* Modality
* Message

Response:

* Interrupted?
* Continued?
* Time until exit?

Everything is stored locally.

---

# 15. Summary of Decisions and Open Questions

## Confirmed Decisions

* Native Android
* AccessibilityService as trigger
* UsageStatsManager as primary signal
* Optional IMU (via XIAO nRF52840 **Sense** variant)
* BLE through connectedDevice FGS
* Heuristics → LiteRT evolution
* Optional Gemini Nano
* **Lean wristband: XIAO nRF52840 + 2N7000 MOSFET + 1N5819 diode + ERM motor + ~120 mAh LiPo**
* **Motor driven directly by PWM through a MOSFET (no DRV2605L in the base build)**
* Fully local processing
* Research distribution via sideloading
* **Breadboard-first validation (USB-powered) before soldering the battery**

---

## Open Questions

### Detection

* Which events does TikTok actually emit?
* Which events does Instagram actually emit?
* What are the ideal thresholds?

### Ground Truth

* Self-report?
* EMA?
* Laboratory study?
* Retrospective labeling?

### Study Design

* Field or laboratory study?
* Number of participants?
* Counterbalancing strategy?

### Hardware

* Plain XIAO nRF52840 (actuator-only) or the **Sense** variant (adds IMU)?
* Motor supply from 3V3 or VBAT?
* Final enclosure / wrist strap?

### Intervention

* Final wording of nudges
* Definitive set of haptic (PWM) patterns

---

# Technical References

* Android AccessibilityService
* Android UsageStatsManager
* Android Foreground Service Types
* Android SYSTEM_ALERT_WINDOW
* Android BLE GATT
* Android ML Kit GenAI
* Gemini Nano / AICore
* LiteRT
* **Seeed XIAO nRF52840 (Nordic nRF52840, BLE) — Seeed nRF52 Boards / Bluefruit**
* **2N7000 N-channel MOSFET (TO-92)**
* **1N5819 Schottky diode (DO-41)**
* **LiPo 3.7 V ~120 mAh (Core Electronics CE04374)**
* ERM coin vibration motor
* Official Android, Seeed, and Adafruit documentation

## Tasks Priority

| ID  | Feature                               | Description                                                                 |
|-----|--------------------------------------|-----------------------------------------------------------------------------|
| F0  | Design system                        | ✅ DONE (archived)                                                          |
| F1  | App shell & navigation               | ✅ DONE (archived) — scaffold, adaptive shell, Home/Insights/Settings, type-safe routes |
| F2  | Onboarding & permissions             | ✅ DONE (archived) — welcome → disclosure → consent → grant permission trio |
| F3  | Manage monitored apps                | Pick/edit watched apps (Instagram, TikTok, etc.)                           |
| F4  | Detection engine + Home/Status       | AccessibilityService, armed/active state, foreground service               |
| F5  | Intervention                         | Overlay system, escalation logic, phone haptics                             |
| F6  | Wristband (BLE)                      | XIAO nRF52840 pairing, device management, PWM vibration test               |
| F7  | Logging / data layer                 | Sessions, detections, responses (local storage)                             |
| F8  | Insights & stats                     | Dashboard, per-app and time-based analytics                                 |
| F9  | Reflective summaries                 | Gemini Nano nudges and summaries                                            |
| F10 | Settings & privacy                   | Intensity control, revoke consent, export/delete data                       |
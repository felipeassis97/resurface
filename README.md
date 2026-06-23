# 🎯 Resurface


## Technical Documentation — Android App for Mindless Scrolling Detection and Interruption + ESP32 Haptic Wristband


**System architecture and engineering document.**
This document consolidates hardware, Android APIs, detection methodology, AI layer, accessibility considerations, lifecycle management (foreground/background), component integration, BLE contract, and ESP32 firmware.

Companion document to the research proposal.

---

## Table of Contents

1. System Overview and Architecture
2. Hardware (ESP32 Wristband)
3. Software (Android App Modules)
4. Android APIs and Features
5. Lifecycle: Foreground vs. Background
6. Detection Methodology
7. AI Layer
8. Intervention
9. Component Integration (Data Flow)
10. BLE Contract
11. ESP32 Firmware
12. Accessibility and Policies
13. Privacy, Security, and Ethics
14. Data Logging (for the Study)
15. Summary of Decisions and Open Questions

---

## 1. System Overview and Architecture

The system consists of two physical nodes:

* **Android Smartphone** (system brain)
* **ESP32 Wristband** (optional haptic actuator)

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
│                  ESP32-S3 WRISTBAND (BLE)                │
│                                                          │
│ [GATT Server] → [DRV2605L] → [Vibration Motor]           │
│ [Optional IMU] → Notify                                  │
│ [LiPo Battery + Charger]                                 │
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

# 2. Hardware (ESP32 Wristband)

The wristband reuses hardware already acquired.

For haptic interruption purposes, a single vibration motor is sufficient.

## 2.1 Minimum Bill of Materials (Single Motor)

| Component | Part | Function |
|------------|------|----------|
| MCU | ESP32-S3 Feather (Adafruit #5477) | BLE, USB-C, LiPo charger |
| Haptic Driver | DRV2605L (Adafruit #2305) | Motor control |
| Motor | ERM Coin Motor (Adafruit #1201) | Vibration |
| Battery | LiPo 400 mAh (Adafruit #3898) | Power supply |
| Cable | STEMMA QT JST-SH | Feather ↔ DRV2605L connection |

## 2.2 Optional Components

| Component | Part | Purpose |
|------------|------|---------|
| I2C Multiplexer | PCA9548 | Multiple motors |
| LRA Motor | Precision Microdrives C10-100 | Higher-quality haptics |
| IMU | LSM6DS3 (or similar) | Flick sensing |

## 2.3 Hardware Notes

* The Feather does not include an onboard IMU.
* The ERM motor operates in plug-and-play mode.
* Verify battery polarity before connection.
* The ESP32-S3 CAN (TWAI) bus will not be used.

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
* Writing commands to the ESP32

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

# 11. ESP32 Firmware

## Stack

* ESP-IDF or Arduino
* NimBLE
* Adafruit DRV2605

---

## Initialization

1. Start BLE
2. Create GATT Server
3. Initialize DRV2605L
4. Start advertising
5. Initialize IMU (optional)

---

## Main Loop

When receiving:

```text
Command Write
```

Execute:

```text
effect_id
→ intensity
→ duration
→ vibration
```

Also publish:

* battery
* status
* IMU data (optional)

---

## Power

* 400 mAh LiPo
* Light Sleep
* BLE wake-up

---

## Effects

| ID | Effect |
|----|---------|
| 0x01 | Gentle alert |
| 0x02 | Firm alert |
| 0x03 | Escalation |

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
* Optional IMU
* BLE through connectedDevice FGS
* Heuristics → LiteRT evolution
* Optional Gemini Nano
* Single-motor wristband
* Fully local processing
* Research distribution via sideloading

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

* Will the wristband include an IMU?
* Actuator-only design?

### Intervention

* Final wording of nudges
* Definitive set of haptic patterns

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
* ESP32-S3 Feather (#5477)
* DRV2605L (#2305)
* ERM Motor (#1201)
* LiPo 400 mAh (#3898)
* PCA9548 (#5626)
* Official Android and Adafruit Documentation

## Tasks Priority

| ID  | Feature                               | Description                                                                 |
|-----|--------------------------------------|-----------------------------------------------------------------------------|
| F0  | Design system                        | ✅ DONE (archived)                                                          |
| F1  | App shell & navigation               | Scaffold, navigation, theme setup, empty screens                           |
| F2  | Onboarding & permissions             | Welcome → disclosure → consent → grant permissions                         |
| F3  | Manage monitored apps                | Pick/edit watched apps (Instagram, TikTok, etc.)                           |
| F4  | Detection engine + Home/Status       | AccessibilityService, armed/active state, foreground service               |
| F5  | Intervention                         | Overlay system, escalation logic, phone haptics                             |
| F6  | Wristband (BLE)                      | Pairing, device management, vibration test                                 |
| F7  | Logging / data layer                 | Sessions, detections, responses (local storage)                             |
| F8  | Insights & stats                     | Dashboard, per-app and time-based analytics                                 |
| F9  | Reflective summaries                 | Gemini Nano nudges and summaries                                            |
| F10 | Settings & privacy                   | Intensity control, revoke consent, export/delete data                       |
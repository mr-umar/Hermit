<p align="center">
  <a href="https://hermit.umarmohammad.xyz">
    <img src="website/assets/hermit-icon.svg" width="112" height="112" alt="Hermit app icon" />
  </a>
</p>

<h1 align="center">Hermit</h1>

<p align="center">
  <b>A tranquil, physical sanctuary from digital distractions powered by standard NFC tags.</b><br>
  <i>100% local-first • Genuine Android application blocking • Free &amp; Open Source</i>
</p>

<p align="center">
  <a href="https://github.com/mr-umar/Hermit/releases/latest">
    <img src="https://img.shields.io/badge/Download_APK-Latest_Release-A5B092?style=for-the-badge&logo=android&logoColor=191A18" alt="Download APK" />
  </a>
  &nbsp;&nbsp;
  <a href="https://hermit.umarmohammad.xyz">
    <img src="https://img.shields.io/badge/Official_Website-hermit.umarmohammad.xyz-242521?style=for-the-badge&logo=googlechrome&logoColor=F0EDE5" alt="Official Website" />
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/mr-umar/Hermit/releases">
    <img src="https://img.shields.io/badge/All_Releases-GitHub-242521?style=for-the-badge&logo=github&logoColor=F0EDE5" alt="All Releases" />
  </a>
</p>

<p align="center">
  <a href="https://github.com/mr-umar/Hermit/releases"><img src="https://img.shields.io/badge/Release-v1.0.0-A5B092?style=flat-square" alt="Release v1.0.0"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B_(API_26%2B)-343432?style=flat-square" alt="Android 8.0+">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-AGPLv3-343432?style=flat-square" alt="License: AGPLv3"></a>
  <img src="https://img.shields.io/badge/Privacy-100%25_Local-343432?style=flat-square" alt="Privacy First">
  <img src="https://img.shields.io/badge/NFC-NTAG213_%7C_215_%7C_216-343432?style=flat-square" alt="NFC Tags">
</p>

<p align="center">
  <a href="README.md"><b>English</b></a> • <a href="README.es.md"><b>Español</b></a>
</p>

<p align="center">
  <img src="website/assets/mockup-app.svg" width="310" alt="Hermit Active Session Screen" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="website/assets/mockup-block.svg" width="310" alt="Hermit Mindful Block Screen" />
</p>

Hermit turns affordable, standard NFC tags into physical sanctuaries from digital distractions. By pairing physical gestures with genuine application blocking through Android's `AccessibilityService`, Hermit helps you cultivate deep, intentional focus without proprietary hardware, subscriptions, or cloud trackers.

---

## Table of Contents

1. [Overview & Philosophy](#overview--philosophy)
2. [Core Features](#core-features)
3. [Architecture & Technology Stack](#architecture--technology-stack)
4. [NFC Tag Compatibility & Capabilities](#nfc-tag-compatibility--capabilities)
5. [Tag Payload Specification](#tag-payload-specification)
6. [Application Blocking Implementation](#application-blocking-implementation)
7. [Hardware & Android Manufacturer Limitations](#hardware--android-manufacturer-limitations)
8. [Permissions & Privacy](#permissions--privacy)
9. [Build & Installation Guide](#build--installation-guide)
10. [Automated Testing & Verification](#automated-testing--verification)
11. [Product Website & Deployment](#product-website--deployment)
12. [License](#license)

---

## Overview & Philosophy

Modern smartphones are engineered for continuous engagement. Digital friction is often too weak to resist automatic muscle memory.

Hermit introduces a **physical barrier**:
1. You configure a focus profile (e.g., Deep Work, Study, Rest) with a selection of distracting apps to block.
2. You write an action or profile reference to an inexpensive, standard NFC tag.
3. Tapping the physical tag initiates your focus session and raises the blocking barrier.
4. Attempting to open any blocked app immediately presents a calm reflection screen requiring either a physical tag tap or a deliberate delay to exit.
5. All data, profiles, and history remain strictly local on your device.

---

## Core Features

- **NFC Tag Management:**
  - Create and register tags directly inside the application.
  - Custom tag labels and behavior assignment: `TOGGLE`, `ACTIVATE`, `DEACTIVATE`, `SWITCH`.
  - Step-by-step interactive NFC writer with live status and read-back verification.
  - Overwrite safety warning before erasing existing NDEF records.
  - Tag inspector that decodes UID, chip type, writable status, and NDEF capacity.
- **Genuine Application Blocking:**
  - `HermitAccessibilityService` monitors foreground window transitions in real time.
  - Safe whitelist preserving the system dialer, home launcher, settings, and emergency services.
  - Prevents tight loop triggers with intelligent debounce windows.
  - State persistence across process death and device reboots (`BootReceiver`).
- **Focus Profiles:**
  - Fully customizable profiles with names, descriptions, icons, and color accents.
  - Multi-select installed app picker with search and category filtering.
  - Optional session durations with countdown timers or open-ended focus.
  - Strict mode requiring a 60-second reflection pause before emergency exits.
- **Local Metrics & History:**
  - Track focus duration, completed sessions, and prevented distraction attempts.
  - Local Room database with zero remote network calls.
- **Bilingual Interface:**
  - Standard resource-based localization: English (`values/`) and Spanish (`values-es/`).
  - Automatic system locale detection with manual override in Settings.

---

## Architecture & Technology Stack

Hermit is built natively for Android following modern architectural best practices:

- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose with Material 3 components
- **Design Foundations:** Apple Human Interface Guidelines translated for Android:
  - Critically damped spring physics (`damping: 1.0`) on tactile button presses (`scale(0.96)`).
  - Translucent surfaces with blur on navigation headers.
  - Optical typographic hierarchy with negative letter tracking on display headings.
- **Persistence:**
  - **Room Database:** Structured storage for `FocusProfile`, `NfcTagRecord`, and `FocusSessionRecord`.
  - **Preferences DataStore:** Atomic active session state, locale, and theme preferences.
- **Concurrency:** Kotlin Coroutines & reactive `Flow` / `StateFlow`.
- **NFC Integration:** Native Android `NfcAdapter`, `Ndef`, `NdefFormatable`, and `enableReaderMode`.
- **System Monitoring:** Android `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`).

---

## NFC Tag Compatibility & Capabilities

Hermit prioritizes widely available, non-proprietary NFC tags conforming to NFC Forum Type 2 and ISO 14443-3A standards:

| Chip Family | Usable Memory | Compatibility Status | Notes |
| :--- | :--- | :--- | :--- |
| **NTAG213** | 144 bytes | **Fully Supported** | Compact payload (&lt;120B) fits with room to spare. Inexpensive. |
| **NTAG215** | 504 bytes | **Fully Supported** | Most common format, thoroughly verified with read-back checks. |
| **NTAG216** | 888 bytes | **Fully Supported** | Maximum Type 2 capacity. |
| **Generic Type 2 / Formatable** | Variable | **Supported** | Automatic NDEF formatting when supported by tag hardware. |
| **ISO 14443-4 / Type 4** | Variable | **Conditional** | Supported if formatted as standard NDEF by device NFC stack. |

> [!NOTE]
> Hermit checks tag writability, available capacity, and existing records at runtime. Locked or read-only tags are detected and prevented from write operations with meaningful feedback.

---

## Tag Payload Specification

Physical tags written by Hermit contain an application-specific MIME NDEF record followed by an Android Application Record (AAR):

### Record 1: Custom MIME Record
- **MIME Type:** `application/vnd.xyz.umarmohammad.hermit`
- **Payload Format:** Versioned UTF-8 JSON:
  ```json
  {
    "v": 1,
    "id": "e4b3c2a1-6789-4def-0123-456789abcdef",
    "act": "TOGGLE",
    "pid": "default_deep_work",
    "name": "Desk Tag"
  }
  ```
  - `v` (*Integer*): Payload format version (currently `1`).
  - `id` (*String*): Cryptographically random UUID identifying the registered tag record.
  - `act` (*String*): Focus action (`TOGGLE`, `ACTIVATE`, `DEACTIVATE`, `SWITCH`).
  - `pid` (*String, optional*): Target profile identifier.
  - `name` (*String, optional*): Human-readable tag label.

### Record 2: Android Application Record (AAR)
- **AAR Value:** `xyz.umarmohammad.hermit`
- Directs Android to automatically open or install Hermit when scanned from any screen or launcher.

---

## Application Blocking Implementation

Application blocking operates through `HermitAccessibilityService`:

1. **Event Filtering:** Listens exclusively for `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`. Window content parsing is disabled (`canRetrieveWindowContent="false"`) to protect privacy.
2. **Foreground Inspection:** Extracts the foreground application's package name and evaluates it against the active profile's blocked package list.
3. **Safety Whitelist:** The system launcher, dialer, emergency dialer, permission controller, and settings are strictly exempt from interception.
4. **Debounce Logic:** A 1200ms cooldown prevents repeated triggers and event loops when transitioning between activities.
5. **Blocking Screen:** Launches `BlockOverlayActivity` (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP`) presenting the mindful reflection screen, time remaining, and NFC unlock prompt.

---

## Hardware & Android Manufacturer Limitations

Different Android original equipment manufacturers (OEMs) implement varying background restrictions and NFC antenna placements:

- **Battery Optimization:** Aggressive task killers (e.g., Xiaomi MIUI/HyperOS, Huawei EMUI, Samsung OneUI) may terminate background accessibility services if left on "Optimized". Hermit provides a direct diagnostic shortcut in **Settings &gt; Battery Optimization** to grant unrestricted background execution.
- **NFC Antenna Alignment:** NFC coils vary by device model (e.g., center-back on Pixel, top-right near camera module on Samsung). Ensure steady contact during writing.
- **Accessibility Service Permissiveness:** On Android 13+, applications sideloaded outside an app store require enabling "Restricted Settings" under App Info before the Accessibility permission can be toggled.

---

## Permissions & Privacy

Hermit is committed to privacy and security:

| Permission | Purpose | Privacy Impact |
| :--- | :--- | :--- |
| `android.permission.NFC` | Read and write physical tags | Local hardware only |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Detect when blocked apps launch | Window content is disabled; package names only |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Restore active focus session after reboot | Local session state |
| `android.permission.VIBRATE` | Tactile haptic feedback on NFC scan | Sensory feedback |
| `android.permission.QUERY_ALL_PACKAGES` | Populate installed app picker | Stored locally in Room |

**No Network Access:** Hermit does not declare `android.permission.INTERNET`. It is physically incapable of transmitting data off your device.

---

## Build & Installation Guide

### Prerequisites
- JDK 17 or higher (tested up to OpenJDK 25).
- Android SDK with `compileSdk 35` and `build-tools 34.0.0+`.

### Build Debug APK
```bash
# Clone the repository
git clone https://github.com/mr-umar/Hermit.git
cd Hermit

# Set Android SDK path in local.properties
echo "sdk.dir=C\:/Users/Umar/AppData/Local/Android/Sdk" > local.properties

# Build debug APK
./gradlew assembleDebug
```
The installable APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Automated Testing & Verification

Hermit includes comprehensive unit tests validating core logic:

```bash
./gradlew testDebugUnitTest
```

### Verified Test Suites
- **`NfcPayloadTest`:** NDEF serialization, roundtrip integrity, version compatibility check, malformed ID rejection, corrupted byte rejection, and compact size limits.
- **`BlockingDecisionTest`:** Package whitelist checks, blocked app interception, debounce timing, and app-switching logic.
- **`SessionStateTransitionTest`:** Atomic focus session state transitions, toggle behavior, profile switching, disabled tag rejection, and reboot expiration calculations.

---

## Product Website & Deployment

The repository includes a dedicated static product website located in [`website/`](website/):

- **Production Domain:** [https://hermit.umarmohammad.xyz](https://hermit.umarmohammad.xyz)
- **Documentation:**
  - [Website Overview](website/README.md)
  - [DNS Configuration Guide](website/DNS.md)
  - [GitHub Pages Deployment Guide](website/DEPLOY.md)

---

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0-or-later)**. See the [LICENSE](LICENSE) file for complete terms and third-party notices.

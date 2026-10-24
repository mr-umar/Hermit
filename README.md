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
  <a href="https://github.com/mr-umar/Hermit/releases"><img src="https://img.shields.io/badge/Release-v1.1.1-A5B092?style=flat-square" alt="Release v1.1.1"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B_(API_26%2B)-343432?style=flat-square" alt="Android 8.0+">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-AGPLv3-343432?style=flat-square" alt="License: AGPLv3"></a>
  <img src="https://img.shields.io/badge/Privacy-100%25_Local-343432?style=flat-square" alt="Privacy First">
  <img src="https://img.shields.io/badge/NFC-NTAG213_%7C_215_%7C_216-343432?style=flat-square" alt="NFC Tags">
</p>

<p align="center">
  <a href="README.md"><b>English</b></a> • <a href="README.es.md"><b>Español</b></a>
</p>

<p align="center">
  <img src="website/assets/mockup-app.svg" width="240" alt="Hermit Active Session Screen" />
  &nbsp;&nbsp;
  <img src="website/assets/mockup-zen.svg" width="240" alt="Hermit Zen Launcher Screen" />
  &nbsp;&nbsp;
  <img src="website/assets/mockup-block.svg" width="240" alt="Hermit Mindful Block Screen" />
</p>

Hermit turns affordable, standard NFC tags into physical sanctuaries from digital distractions. By pairing physical gestures with genuine application blocking through Android's `AccessibilityService` and an integrated OLED **Zen Launcher**, Hermit helps you cultivate deep, intentional focus without proprietary hardware, subscriptions, or cloud surveillance.

---

## Table of Contents

1. [Overview & Philosophy](#overview--philosophy)
2. [Adaptive Onboarding & Setup](#adaptive-onboarding--setup)
3. [Zen Launcher Mode](#zen-launcher-mode)
4. [Core Features](#core-features)
5. [Architecture & Technology Stack](#architecture--technology-stack)
6. [NFC Tag Compatibility & Capabilities](#nfc-tag-compatibility--capabilities)
7. [Tag Payload Specification](#tag-payload-specification)
8. [Application Blocking Implementation](#application-blocking-implementation)
9. [Security & Anti-Tamper Protection](#security--anti-tamper-protection)
10. [Hardware & Android Manufacturer Limitations](#hardware--android-manufacturer-limitations)
11. [Permissions & Privacy](#permissions--privacy)
12. [Build & Installation Guide](#build--installation-guide)
13. [Automated Testing & Verification](#automated-testing--verification)
14. [Product Website & Deployment](#product-website--deployment)
15. [License](#license)

---

## Overview & Philosophy

Modern smartphones are engineered for continuous engagement. Digital friction alone is too fragile to resist automatic muscle memory.

Hermit introduces an unyielding **physical barrier**:
1. **Clean Slate Philosophy:** No pre-baked clutter or default profiles. You define your focus intentionally.
2. **Adaptive Configuration:** If you don't have an NFC tag yet, you can create timer-guarded profiles (up to 12h) with optional emergency reflection delays. If you pair an NFC tag, you unlock the universal physical tap switch and the full **Zen Launcher**.
3. **Physical Anchor:** Tapping your physical tag engages or disengages focus instantly.
4. **Mindful Reflection & Zen Seclusion:** Opening blocked apps triggers a calm reflection screen. In Zen Mode, your home screen is locked into an ultra-minimalist OLED launcher showing only essentials.
5. **Privacy First:** 100% of your focus habits, profiles, and statistics remain strictly local on your device.

---

## Adaptive Onboarding & Setup

Hermit greets first-time users with a streamlined two-step onboarding:

- **Step 1: Pair NFC Tag (Skippable):**
  - Hold any standard NTAG213, NTAG215, or NTAG216 tag to the back of your phone to pair.
  - If you do not have an NFC tag yet, tap **"Skip for now"** to proceed without friction.
- **Step 2: Adaptive Profile Creation:**
  - **Without NFC Tag (Timer Mode):**
    - Create focus profiles with a countdown timer (up to a strict **12-hour maximum**).
    - Emergency exit option: enabled by default with a 60-second mindful reflection delay, but can be completely disabled by the user to enforce an unbroken, irrevocable focus commitment until the timer expires.
    - Zen Launcher mode is disabled (requires physical NFC to exit safely).
  - **With NFC Tag (Physical Switch & Zen):**
    - The physical tag acts as a **Universal Physical Switch**: tapping toggles active focus on/off for whichever profile is currently selected.
    - Option to enable **Zen Mode (Launcher)** with strict physical NFC deactivation.
    - Protected by an internal 12-hour safety ceiling under all circumstances.

---

## Zen Launcher Mode

Hermit includes an integrated minimal home launcher (`ZenLauncherActivity`) designed for zero-distraction environments:

- **OLED Black Interface:** Pure `#000000` canvas displaying a clean clock, date, active session remaining badge, and an NFC tap-to-exit prompt.
- **Allowed Essentials:**
  - Phone (Dialer)
  - Camera
  - Spotify (if installed)
  - YouTube Music (if installed)
  - Optionally up to 5 user-chosen custom applications.
  - Hermit (for settings, stats, and adjustments).
- **Home Redirection via Accessibility:**
  - When Zen Mode is active, pressing the Android Home button or switching tasks automatically brings `ZenLauncherActivity` back to the front, eliminating accidental escape loops.
- **Core System Protection:**
  - Keyboards / Input Method Editors (e.g., Gboard), emergency calls, system UI dialogs, and permission controllers are hard-whitelisted and **never blocked**.
- **Strict Physical NFC Deactivation:**
  - Deactivating Zen Launcher early strictly requires scanning your paired physical NFC tag (or reaching the 12-hour internal safety timeout).

---

## Core Features

- **Universal NFC Tag Switch:**
  - Tap your tag to toggle whichever focus profile is currently active in the app.
  - Step-by-step interactive NFC writer with read-back verification and overwrite warnings.
  - Built-in tag inspector decoding UID, chip type, writable status, and NDEF capacity.
- **Genuine Application Blocking:**
  - `HermitAccessibilityService` monitors foreground window transitions in real time without inspecting personal messages or keystrokes.
  - Debounce cooldowns and state persistence across reboots (`BootReceiver`).
- **Customizable Focus Profiles:**
  - Multi-select installed app picker with search and category filters.
  - Countdown timers up to 12 hours with optional 60-second emergency reflection delays.
- **Anti-Tamper & Uninstall Protection:**
  - Optional Device Administrator privileges disable Android's uninstall button.
  - Deactivating protection requires completing a mindful cooldown reflection delay.
- **In-App GitHub Releases Updater:**
  - Automatic and manual update checking directly against GitHub Releases API with cryptographic SHA-256 verification and in-app APK installer.
- **Local Metrics & History:**
  - Track focus duration, completed sessions, and prevented distraction attempts in local encrypted Room storage.
- **Bilingual Interface:**
  - Full English (`values/`) and Spanish (`values-es/`) translations with automatic detection and manual override.

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
6. **Zen Launcher Home Interception:** When Zen Mode is engaged, navigation back to the system launcher automatically returns to `ZenLauncherActivity`. Keyboards (IMEs), dialer, camera, system UI, and Hermit itself remain 100% exempt.

---

## Security & Anti-Tamper Protection

To prevent impulsive self-sabotage during moments of weakness:

- **Device Administrator Protection:** Users can enable Device Administrator privileges (`HermitDeviceAdminReceiver`). Android disables the direct "Uninstall" button in system settings while active.
- **Mindful Cooldown Pause:** To deactivate Device Admin protection, the user must navigate a reflection countdown timer, preventing impulsive removal while preserving legitimate user agency.
- **12-Hour Safety Ceiling:** All active sessions enforce a strict hard-coded 12-hour limit (`SAFETY_12H_TIMEOUT`), ensuring that even in catastrophic hardware failure or lost tag scenarios, the device will never remain permanently locked.

---

## Hardware & Android Manufacturer Limitations

Different Android original equipment manufacturers (OEMs) implement varying background restrictions and NFC antenna placements:

- **Battery Optimization:** Aggressive task killers (e.g., Xiaomi MIUI/HyperOS, Huawei EMUI, Samsung OneUI) may terminate background accessibility services if left on "Optimized". Hermit provides a direct diagnostic shortcut in **Settings > Battery Optimization** to grant unrestricted background execution.
- **NFC Antenna Alignment:** NFC coils vary by device model (e.g., center-back on Pixel, top-right near camera module on Samsung). Ensure steady contact during writing.
- **Accessibility Service Permissiveness:** On Android 13+, applications sideloaded outside an app store require enabling "Restricted Settings" under App Info before the Accessibility permission can be toggled.

---

## Permissions & Privacy

Hermit is committed to privacy and security:

| Permission | Purpose | Privacy Impact |
| :--- | :--- | :--- |
| `android.permission.NFC` | Read and write physical tags | Local hardware only |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Detect foreground launches & route to Zen Launcher | Window content disabled; package names only |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Restore active focus session after reboot | Local session state |
| `android.permission.VIBRATE` | Tactile haptic feedback on NFC scan | Sensory feedback |
| `android.permission.QUERY_ALL_PACKAGES` | Populate installed app picker | Stored locally in Room |
| `android.permission.INTERNET` | Check GitHub Releases API & download APK updates | Verified releases only; zero telemetry |
| `android.permission.REQUEST_INSTALL_PACKAGES` | Install verified in-app APK updates | User-authorized package installation |

**Zero Surveillance:** Network access is strictly restricted to checking and downloading updates from GitHub Releases with SHA-256 verification. Hermit contains zero telemetry, zero analytics SDKs, and zero cloud accounts. 100% of focus data remains on your device.

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

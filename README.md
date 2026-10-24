# 🔔 Notification Saver V2.0.0

[![Android CI](https://github.com/EngAnasMuhammad/Notification-Saver/actions/workflows/ci.yml/badge.svg)](https://github.com/EngAnasMuhammad/Notification-Saver/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-blue.svg?logo=kotlin)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-37-orange.svg)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-purple.svg)
![Version](https://img.shields.io/badge/Release-v2.0.0-green.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**Notification Saver** is a modern, lightweight, privacy-first Android application that captures, organizes, and locally backs up incoming notifications across all installed apps. Built with 100% Jetpack Compose following **Clean Architecture** principles.

---

## ✨ Features in V2.0.0

- 🗂️ **Master-Detail Navigation & Chat Timeline**:
  - **Screen 1 (Home)**: 2-level expandable list (App Headers ➔ Conversations). Smooth spring expand/collapse animations.
  - **Screen 2 (Detail)**: Modern Material 3 Chat Bubble timeline with TopAppBar sender avatars and clean multiline formatting.
- 🌐 **Per-App Multilingual & RTL Support (Arabic & English)**:
  - Zero-flashing dynamic locale changes applied instantly via `AppCompatDelegate.setApplicationLocales`.
  - Full Arabic translation ([`values-ar/strings.xml`](app/src/main/res/values-ar/strings.xml)) with native RTL layout mirror.
- 🎨 **Dynamic Theme Engine**:
  - Toggle between **System Default**, **Light**, and **Dark** themes.
- 🛡️ **App Filter Settings & Blacklist Management**:
  - Exclude unwanted apps from notification saving using Jetpack DataStore preferences.
  - Features a live search bar and Material 3 `Switch`es.
  - **Blacklisted/turned-off apps are pinned directly at the top of the list** for instant toggling.
  - Instant `< 0.0001 ms` service-layer drop gate (`blockedPackagesCache`).
- 👆 **Swipe-to-Dismiss & Contextual Menus**:
  - Right-to-left swipe-to-dismiss on App Groups and Conversations with red action indicators and **Confirmation Dialogs**.
  - Long-press contextual dropdown menu on chat bubbles for **Copy to Clipboard** (with Toast feedback) and message deletion.
- 🎯 **Smart 5-Tier Sender Extraction**:
  - Parses `MessagingStyle` and `Person` bundles to extract actual sender handles, resolving receiver-sender handle mismatches in multi-account apps (like Instagram & WhatsApp).
- 🔄 **Active Status-Bar Sync on App Launch**:
  - Scans active status bar notifications on launch/resume (`ON_RESUME`) to catch any messages received while offline.
- ⚡ **Zero-Lag Memory Caching & Bounds**:
  - Thread-safe `AppIconCache` and 5-second bounded burst debouncer preventing memory leaks and frame drops.
- 🔒 **100% Offline-First & Private**:
  - All notifications remain strictly on your device inside an encrypted local Room SQLite database. Zero network calls or external tracking.

---

## 🛠️ Engineering Highlights

1. **5-Tier Smart Sender Handle & Avatar Alignment**:
   * *Problem*: In multi-account apps (e.g. Instagram), notifications format titles as `"ReceiverHandle: SenderHandle"`, causing receiver titles to be paired with sender avatars.
   * *Solution*: Implemented a 5-tier resolution pipeline inspecting `MessagingStyle.Message`, `EXTRA_MESSAGING_PERSON`, and colon prefixes, strictly aligning the saved sender name with the disk avatar hash.

2. **Zero-Flashing `AppCompatDelegate` Per-App Locale**:
   * *Problem*: Standard locale changes require manual Activity recreation (`recreate()`), causing jarring white flashes.
   * *Solution*: Extended `AppCompatActivity` and used `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))` inside `LaunchedEffect`, adjusting app resources smoothly on the fly.

3. **120 FPS Flattened `LazyColumn` & Spring Motion**:
   * *Problem*: Nested `LazyColumn`s cause scroll lag and crash the Compose layout engine.
   * *Solution*: Implemented a single flattened `LazyColumn` with stable keys (`"app_$packageName"`, `"conv_$conversationKey"`) and Material 3 spring physics for fluid inline expansion.

4. **Background Persistence & Auto-Rebind**:
   * *Problem*: Android OS terminates background services when swiped away from Recents.
   * *Solution*: Utilized PackageManager component-toggle (`forceRebind`) on `onTaskRemoved()` and `onListenerDisconnected()`, forcing the Android OS framework to restart and rebind the listener service.

---

## 🏗️ Architecture & Tech Stack

```
          ┌──────────────────────────────────────────────────────────┐
          │                    Presentation Layer                    │
          │ Compose UI │ NotificationViewModel │ SettingsViewModel    │
          └────────────────────────────┬─────────────────────────────┘
                                       │
                                       ▼
          ┌──────────────────────────────────────────────────────────┐
          │                       Domain Layer                       │
          │ Pure Kotlin Use Cases │ Domain Models │ Repository Interfaces │
          └────────────────────────────▲─────────────────────────────┘
                                       │
                                       ▼
          ┌──────────────────────────────────────────────────────────┐
          │                        Data Layer                        │
          │ Repositories │ Room Dao │ DataStore │ Listener Service  │
          └────────────────────────────┬─────────────────────────────┘
                                       │
                                       ▼
                          ┌────────────────────────┐
                          │ Room Database (SQLite) │
                          └────────────────────────┘
```

### Core Libraries & Tools
- **Language**: [Kotlin 2.4](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Navigation**: Compose Navigation with Material 3 Motion
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) v4 & [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **System Integration**: Android `NotificationListenerService`, `AppCompatDelegate`, `PowerManager`, & `PackageManager`

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Device running **Android 8.0 (API level 26)** or higher

### Building & Running
1. **Clone the repository**:
   ```bash
   git clone https://github.com/EngAnasMuhammad/Notification-Saver.git
   cd Notification-Saver
   ```

2. **Build Debug APK**:
   ```bash
   ./gradlew installDebug
   ```

3. **Grant Permissions**:
   - Open the app and grant **Notification Access**.
   - Tap **Fix** on the top banner to whitelist the app from **Battery Optimization**.

---

## 🔄 CI/CD Pipelines

- **Continuous Integration (`ci.yml`)**: Builds `./gradlew assembleDebug` on pushes/PRs to `main`/`master`.
- **Continuous Deployment (`cd.yml`)**: Triggered when a version tag (`v*`) is pushed, building a Release APK and creating a GitHub Release:
  ```bash
  git tag -a v2.0.0 -m "Release v2.0.0"
  git push origin master --tags
  ```

---

## 📄 License

```text
MIT License

Copyright (c) 2026 Anas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction rights, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

# 🔔 Notification Saver

[![Android CI](https://github.com/EngAnasMuhammad/Notification-Saver/actions/workflows/ci.yml/badge.svg)](https://github.com/EngAnasMuhammad/Notification-Saver/actions/workflows/ci.yml)
![Version](https://img.shields.io/badge/Version-2.1.0-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-blue.svg?logo=kotlin)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-37-orange.svg)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-purple.svg)
![GitHub Release](https://img.shields.io/github/v/release/EngAnasMuhammad/Notification-Saver?color=green&label=Release)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**Notification Saver** is a native, offline-first Android application that intercepts, deduplicates, and locally archives incoming status bar notifications. Built with 100% Jetpack Compose and Clean Architecture.

---

## ✨ Key Features

* **🗂️ Master-Detail Navigation & Chat Timeline**
    * **Home Screen:** 2-level expandable list (App Groups ➔ Conversations) with Material 3 spring physics.
    * **Detail Screen:** Chat bubble timeline with TopAppBar sender avatars, multiline formatting, and prefix cleaning.
* **🚀 Open Source App Launcher Button**
    * Launch the source application directly from `DetailScreen`'s TopAppBar with the **Open** (English) / **فتح** (Arabic) action button.
* **🔗 Interactive Blue Underlined Web Links**
    * Automatically detects embedded web URLs (`http://`, `https://`, `www.`), styles them in high-contrast blue with underlines, and opens them in the browser on click.
* **🛡️ App Blacklist & Real-Time Filtering**
    * Exclude specific apps from notification capture using Jetpack DataStore.
    * Live search bar with Material 3 switches and blacklisted apps pinned to the top.
    * Instant service-layer drop gate (`blockedPackagesCache`).
* **🌐 Dynamic Per-App Localization (English & Arabic)**
    * Zero-flashing runtime locale switching via `AppCompatDelegate.setApplicationLocales`.
    * Complete Arabic localization ([`values-ar/strings.xml`](app/src/main/res/values-ar/strings.xml)) with full RTL layout support.
* **🎨 Dynamic Material 3 Theming**
    * Supports **System Default**, **Light**, and **Dark** modes.
    * Custom dark splash screen (`#1D2328`) with transparent edge-to-edge system bar integration.
* **👆 Gestures & Contextual Actions**
    * Swipe-to-dismiss on App Groups and Conversations with confirmation dialogs.
    * Long-press context menu on chat bubbles to copy text or delete individual messages.
* **🔒 100% Offline & Private**
    * All data is stored locally in an indexed Room SQLite database with zero network permissions or external tracking.

---

## 🛠️ Engineering Highlights

| Challenge                                  | Engineering Solution                                                                                                                                                                                                          |
|:-------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Multi-Account Sender & Avatar Mismatch** | Built a package-aware extraction pipeline inspecting `MessagingStyle`, `LargeIcon`, and colon prefixes (`"Account: Sender"`), pairing the extracted sender name with a deduplicated `.webp` disk cache (`filesDir/avatars/`). |
| **Burst & Ghost Notification Duplicates**  | Combined a bounded 5-second `ConcurrentHashMap` in-memory debouncer with an exact-match SQLite check anchored to the original message timestamp (`notification.when`).                                                        |
| **120 FPS List Virtualization**            | Replaced nested lists with a single flattened `LazyColumn` using stable keys (`app_$packageName`, `conv_$conversationKey`), `contentType` definitions, and an in-memory `AppIconCache`.                                       |
| **OEM Background Process Termination**     | Elevated `NotificationListenerService` to a silent foreground service (`IMPORTANCE_MIN`) and implemented `PackageManager` component-state toggling (`forceRebind`) on `onTaskRemoved()` and `onListenerDisconnected()`.       |
| **Missed Offline Notifications**           | Added an active status bar sync (`activeNotifications`) triggered on `ON_RESUME` to capture notifications posted while the service was unbound.                                                                               |
| **Activity Flashing on Locale Change**     | Extended `AppCompatActivity` and bound `LocaleListCompat` updates to a reactive `StateFlow`, switching between LTR and RTL layouts without manual activity recreation.                                                        |

---

## 🏗️ Architecture & Tech Stack

```text
          ┌──────────────────────────────────────────────────────────┐
          │                    Presentation Layer                    │
          │   Compose UI  │  NotificationViewModel  │ SettingsViewModel  │
          └────────────────────────────┬─────────────────────────────┘
                                       │
                                       ▼
          ┌──────────────────────────────────────────────────────────┐
          │                       Domain Layer                       │
          │  Pure Kotlin Use Cases │ Domain Models │ Repo Contracts  │
          └────────────────────────────▲─────────────────────────────┘
                                       │
                                       ▼
          ┌──────────────────────────────────────────────────────────┐
          │                        Data Layer                        │
          │  Repositories │ Room DAO │ DataStore │ Listener Service  │
          └────────────────────────────┬─────────────────────────────┘
                                       │
                                       ▼
                     ┌───────────────────────────────────┐
                     │ Room SQLite (v4) & WebP Disk Cache│
                     └───────────────────────────────────┘
```

| Category                 | Technology                                                                                                                                                                                                 |
|:-------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Language**             | [Kotlin 2.4.21](https://kotlinlang.org/)                                                                                                                                                                   |
| **UI Framework**         | [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)                                                                                                                              |
| **Architecture**         | Clean Architecture + MVVM + Unidirectional Data Flow (UDF)                                                                                                                                                 |
| **Dependency Injection** | [Dagger Hilt](https://dagger.dev/hilt/)                                                                                                                                                                    |
| **Local Persistence**    | [Room Database](https://developer.android.com/training/data-storage/room) (v4 with non-destructive migrations) & [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) |
| **Concurrency**          | Kotlin Coroutines & `StateFlow`                                                                                                                                                                            |
| **Image Loading**        | [Coil Compose](https://coil-kt.github.io/coil/)                                                                                                                                                            |
| **System APIs**          | `NotificationListenerService`, `AppCompatDelegate`, `PowerManager`, `PackageManager`                                                                                                                       |

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug (2024.2.1) or newer
* JDK 17
* Android 8.0 (API 26) or higher

### Installation
1. **Clone the repository:**
   ```bash
   git clone [https://github.com/EngAnasMuhammad/Notification-Saver.git](https://github.com/EngAnasMuhammad/Notification-Saver.git)
   cd Notification-Saver
   ```
2. **Build and install the debug APK:**
   ```bash
   ./gradlew installDebug
   ```
3. **Configure permissions on launch:**
    * Grant **Notification Listener Access** when prompted.
    * Tap **Fix** on the warning banner to exempt the app from **Battery Optimization**.

---

## 🔄 CI/CD Workflow

* **Continuous Integration (`ci.yml`):** Runs `./gradlew assembleDebug` on every push and pull request to `main`/`master`.
* **Continuous Deployment (`cd.yml`):** Automatically builds a signed release APK (`./gradlew assembleRelease`) and publishes a GitHub Release when a version tag (`v*`) is pushed:
  ```bash
  git tag -a v2.1.0 -m "Release v2.1.0"
  git push origin master v2.1.0
  ```

---

## 📄 License

```text
MIT License

Copyright (c) 2026 Anas Muhammad

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

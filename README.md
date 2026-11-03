# 🔔 Notification Saver

[![Android CI](https://github.com/EngAnasMuhammad/Notification-Saver/actions/workflows/ci.yml/badge.svg)](https://github.com/EngAnasMuhammad/Notification-Saver/actions/workflows/ci.yml)
![Version](https://img.shields.io/badge/Version-2.3.2-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-blue.svg?logo=kotlin)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-37-orange.svg)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-purple.svg)
![GitHub Release](https://img.shields.io/github/v/release/EngAnasMuhammad/Notification-Saver?color=green&label=Release)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**Notification Saver** is a high-performance, native, offline-first Android application that intercepts, deduplicates, and locally archives incoming status bar notifications. Engineered with 100% Jetpack Compose (Material 3), Clean Architecture, and hardened background survival pipelines designed for 24/7 continuous operation.

---

## ✨ Key Features

* **🗂️ Master-Detail Navigation & Chat Timeline**
    * **Home Screen:** True virtualized 2-level expandable list (App Groups ➔ Conversations) with smooth spring physics and individual item recycling.
    * **Detail Screen:** Targeted chat bubble timeline with sticky/styled day headers, multiline formatting, and prefix cleaning.
* **📅 Intelligent Day Grouping & Professional Timestamps**
    * **Home Screen:** Relative time awareness showing `Today, 10:30 AM` / `اليوم، 10:30 ص` or `Yesterday` / `أمس`.
    * **Detail Screen:** Timeline messages automatically grouped by calendar day under Material 3 date chips with clean time labels inside individual chat bubbles.
* **⏰ Rolling-Window Auto-Delete Retention Policy**
    * Configurable rolling expiration window (`Off`, `24 Hours`, `10 Days`, `30 Days`) deleting notifications older than the exact millisecond cutoff timestamp, with automatic cascading disk avatar cleanup.
* **🛡️ First-Launch Privacy & Responsibility Disclaimer**
    * Localized first-launch dialog explaining 100% offline security (zero internet access), intended recovery purpose, and user data responsibility. Accessible anytime in Settings.
* **🚀 Open Source App Launcher Button**
    * Launch the source application directly from `DetailScreen`'s TopAppBar with the **Open** (English) / **فتح** (Arabic) action button.
* **🔗 Interactive Web Link Detection**
    * Automatically detects embedded web URLs (`http://`, `https://`, `www.`), styles them in high-contrast blue with underlines, and opens them in the browser on click.
* **🛡️ App Blacklist & Real-Time Filtering**
    * Exclude specific apps from notification capture using Jetpack DataStore with cached `PackageManager` queries for instant toggling.
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

## 🛠️ Engineering Highlights & Performance Optimizations

| Challenge                                    | Engineering Solution                                                                                                                                                                                                                |
|:---------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Day Grouping & Timeline Categorization**   | Implemented calendar day segmentation in Compose timelines paired with localized relative timestamps (`formatRelativeDateTime` & `formatDateHeader`) across English and Arabic locales.                                             |
| **24/7 Background Survival & Rebinding**     | Replaced aggressive component toggles with framework `requestRebind()` on listener disconnection, backed by a `BootCompletedReceiver` (`RECEIVE_BOOT_COMPLETED` & `MY_PACKAGE_REPLACED`) to survive phone reboots and app updates.  |
| **Low Memory Killer (LMK) Shielding**        | Added compound multi-column SQLite indexes and migrated single conversation history to targeted Room queries (`getConversationHistoryByPackageAndTitle`), eliminating full-table RAM dumps.                                         |
| **Zero-Leak Avatar Disk Engine**             | Migrated avatar storage from `filesDir` to managed `cacheDir/avatars_v2`, implemented fast `file.exists()` deduplication to eliminate redundant WebP encoding, and linked avatar file deletion to Room deletion/pruning use cases.  |
| **Buttery Accordion & Synchronized Physics** | Implemented `AnimatedVisibility` with non-bouncy spring `expandVertically` / `shrinkVertically` and synchronized `Modifier.animateItem()` across headers and conversations, eliminating visual popping and detached header sliding. |
| **Side-by-Side Debug & Release Isolation**   | Configured `applicationIdSuffix = ".debug"` and dedicated debug label resources to permit concurrent development and production testing on the same physical device with isolated databases and listener settings.                  |
| **Multi-Account Sender & Avatar Mismatch**   | Built a package-aware extraction pipeline inspecting `MessagingStyle`, `LargeIcon`, and colon prefixes (`"Account: Sender"`), pairing the extracted sender name with deduplicated `.webp` cache storage.                            |
| **Burst & Ghost Notification Duplicates**    | Combined a bounded 5-second `ConcurrentHashMap` in-memory debouncer with an exact-match SQLite check anchored to the original message timestamp (`notification.when`).                                                              |
| **Clean Architecture Decoupling**            | Fully decoupled `PackageManager` plumbing from Composable UI hierarchies, resolving system resources cleanly via `Context` at the edge.                                                                                             |
| **Seamless GitHub Release Upgrades**         | Hardened R8 ProGuard rules for Room SQLite models and configured automated Base64 keystore injection in GitHub Actions to prevent signature mismatch errors (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).                                 |

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
                     │ Room SQLite (v5) & WebP Disk Cache│
                     └───────────────────────────────────┘
```

| Category                 | Technology                                                                                                                                                                                                 |
|:-------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Language**             | [Kotlin 2.4.21](https://kotlinlang.org/)                                                                                                                                                                   |
| **UI Framework**         | [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)                                                                                                                              |
| **Architecture**         | Clean Architecture + MVVM + Unidirectional Data Flow (UDF)                                                                                                                                                 |
| **Dependency Injection** | [Dagger Hilt](https://dagger.dev/hilt/)                                                                                                                                                                    |
| **Local Persistence**    | [Room Database](https://developer.android.com/training/data-storage/room) (v5 with non-destructive migrations) & [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) |
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
   git clone https://github.com/EngAnasMuhammad/Notification-Saver.git
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
* **Continuous Deployment (`cd.yml`):** Automatically decodes release keystore credentials from repository secrets, builds an optimized release APK (`./gradlew assembleRelease`), and publishes a GitHub Release when a version tag (`v*`) is pushed:
  ```bash
  git tag -a v2.3.2 -m "Release v2.3.2"
  git push origin master v2.3.2
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

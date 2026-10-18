# 🔔 Notification Saver

[![Android CI](https://github.com/EngAnasMuhammad/Notification-Saver/actions/workflows/ci.yml/badge.svg)](https://github.com/EngAnasMuhammad/Notification-Saver/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-blue.svg?logo=kotlin)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-37-orange.svg)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-purple.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**Notification Saver** is a modern, lightweight, privacy-focused Android application that captures, organizes, and locally backs up incoming notifications across all installed apps. Built with 100% Jetpack Compose and modern Android development best practices following **Clean Architecture**.

---

## ✨ Features

- ⚡ **Automated Background Backup**: Automatically captures incoming status bar notifications silently in the background, even when the app is closed or swiped away.
- 🛡️ **Smart Spam & Ghost Filter**: Automatically discards ongoing media players, ongoing system sync events, WhatsApp/Telegram group summary containers, and blank ghost notifications.
- 🔋 **Battery Optimization Whitelist**: Integrated UI banner detects OEM task killers and routes users to whitelist the app for 100% background reliability.
- 🎨 **Modern Material 3 UI**: Clean list interface featuring single-line marquee text for long titles, content truncation, and smooth scrolling.
- ⚡ **Zero-Lag Dynamic Icon Caching**: Thread-safe in-memory caching for app icons preventing UI stutter during fast scrolling.
- 🗑️ **Management & Bulk Deletion**: Easily delete individual saved notifications or clear the entire database with a single tap.
- 🔒 **100% Privacy & Offline-First**: All data remains strictly on your device inside an encrypted local Room SQLite database. Zero network calls or external tracking.

---

## 🛠️ Engineering Highlights

How difficult technical challenges were solved in this project:

1. **5-Second SQL Deduplication Window**:
   * *Problem*: Chat apps and system sync services frequently fire duplicate notification events at the exact same millisecond or trigger redundant updates.
   * *Solution*: Implemented an optimized SQLite query using a 5-second sliding window (`timestamp >= :timestamp - 5000`) along with indexed columns (`timestamp`, `packageName`) to prevent duplicate database rows while maintaining `O(log N)` query performance.

2. **Zero-Lag `LazyColumn` Icon & Label Caching (`AppIconCache`)**:
   * *Problem*: Querying `PackageManager` via Binder IPC for app icons and labels on every frame during fast list scrolling causes severe UI jank and dropped frames.
   * *Solution*: Created a thread-safe `ConcurrentHashMap` memory cache (`AppIconCache`) that queries `PackageManager` at most once per app package lifetime, caching both the extracted `ImageBitmap` and label.

3. **Background Persistence, Auto-Rebind & `onTaskRemoved` Self-Heal**:
   * *Problem*: Android OS's `NotificationManagerService` can silently drop listener bindings or kill background processes when apps are swiped away from Recents.
   * *Solution*: 
     * **Component Toggle Trick (`forceRebind`)**: Utilized `PackageManager.setComponentEnabledSetting` inside `onTaskRemoved()` and `onListenerDisconnected()` to toggle the service state, forcing the Android OS framework to immediately restart and rebind the service.
     * **Silent Foreground Service (`IMPORTANCE_MIN`)**: Elevated process priority to `FOREGROUND` using `IMPORTANCE_MIN`, preventing Android's Low Memory Killer (LMK) from terminating the service without cluttering the status bar with notification icons.

4. **Battery Optimization Whitelisting (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**:
   * *Problem*: Aggressive OEM battery management (MIUI, ColorOS, OneUI) terminates background services when apps are closed.
   * *Solution*: Detects optimization state via `PowerManager.isIgnoringBatteryOptimizations()` and displays an actionable Material 3 warning card directing users to whitelist the app with one click.

5. **Robust Spam & Ghost Filtration**:
   * *Problem*: Apps flood the notification stream with ongoing media players, file download progress bars, and invisible parent group summary nodes.
   * *Solution*: Filtered out `sbn.isOngoing`, `NotificationCompat.isGroupSummary()`, self-notifications, and blank ghost events before hitting the persistence layer.

---

## 🏗️ Architecture & Tech Stack

The app strictly follows **Clean Architecture** (`com.anasdev.notificationsaver`) combined with **MVVM** and **Unidirectional Data Flow (UDF)**:

```
          ┌──────────────────────────────────────────────────────────┐
          │                    Presentation Layer                    │
          │         Jetpack Compose UI  ◄───  NotificationViewModel  │
          └────────────────────────────┬─────────────────────────────┘
                                       │
                                       ▼
          ┌──────────────────────────────────────────────────────────┐
          │                       Domain Layer                       │
          │    Use Cases  │  NotificationItem  │  Repository Interface │
          └────────────────────────────▲─────────────────────────────┘
                                       │
                                       ▼
          ┌──────────────────────────────────────────────────────────┐
          │                        Data Layer                        │
          │ Repository Impl │ NotificationDao │ Listener Service     │
          └────────────────────────────┬─────────────────────────────┘
                                       │
                                       ▼
                          ┌────────────────────────┐
                          │ Room Database (SQLite) │
                          └────────────────────────┘
```

### Core Libraries & Tools
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room)
- **Asynchronous Programming**: [Kotlin Coroutines & StateFlow](https://kotlinlang.org/docs/coroutines-overview.html)
- **System Integration**: Android `NotificationListenerService`, `PowerManager`, & `PackageManager` API

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android device or emulator running **Android 8.0 (API level 26)** or higher

### Building & Running
1. **Clone the repository**:
   ```bash
   git clone https://github.com/EngAnasMuhammad/Notification-Saver.git
   cd Notification-Saver
   ```

2. **Open in Android Studio** and sync Gradle dependencies.

3. **Build & Deploy** to your device:
   ```bash
   ./gradlew installDebug
   ```

4. **Grant Permissions**:
   - Open the app.
   - Tap **Open System Settings** to grant **Notification Access**.
   - If prompted by the top banner, tap **Fix** to whitelist the app from **Battery Optimization**.

---

## 🔄 CI/CD Pipelines

This repository is equipped with GitHub Actions for automated testing and release deployment:

- **Continuous Integration (`ci.yml`)**: Automatically triggers on pushes and PRs to `main`/`master` to verify Gradle build integrity.
- **Continuous Deployment (`cd.yml`)**: Triggered when a new git tag is pushed (e.g. `v1.1.0`), automatically building a Release APK and publishing a GitHub Release:
  ```bash
  git tag v1.1.0
  git push origin v1.1.0
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
OUT OF OR IN CONNECTION WITH THE SOFTWARE ORs THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

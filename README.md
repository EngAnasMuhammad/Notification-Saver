# 🔔 Notification Saver

[![Android CI](https://github.com/USER/NotificationSaver/actions/workflows/ci.yml/badge.svg)](https://github.com/USER/NotificationSaver/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg?logo=kotlin)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-37-orange.svg)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-purple.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**Notification Saver** is a modern, lightweight, privacy-focused Android application that captures, organizes, and locally backs up incoming notifications across all installed apps. Built with 100% Jetpack Compose and modern Android development best practices following **Clean Architecture**.

---

## ✨ Features

- ⚡ **Automated Local Backup**: Automatically captures incoming status bar notifications silently in the background.
- 🛡️ **Smart Spam & Ghost Filter**: Automatically discards ongoing media players, ongoing system sync events, WhatsApp/Telegram group summary containers, and blank ghost notifications.
- 🎨 **Modern Material 3 UI**: Clean list interface featuring single-line marquee text for long titles, content truncation, and smooth scrolling.
- ⚡ **Zero-Lag Dynamic Icon Caching**: Thread-safe in-memory caching for app icons preventing UI stutter during fast scrolling.
- 🗑️ **Management & Bulk Deletion**: Easily delete individual saved notifications or clear the entire database with a single tap.
- 🔒 **100% Privacy & Offline-First**: All data remains strictly on your device inside an encrypted local Room SQLite database. Zero network calls or external tracking.

---

## 🏗️ Architecture & Tech Stack

The app strictly follows **Clean Architecture** principles combined with **MVVM** and **Unidirectional Data Flow (UDF)**:

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
- **System Integration**: Android `NotificationListenerService` & `PackageManager` API

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android device or emulator running **Android 8.0 (API level 26)** or higher

### Building & Running
1. **Clone the repository**:
   ```bash
   git clone https://github.com/USER/NotificationSaver.git
   cd NotificationSaver
   ```

2. **Open in Android Studio** and sync Gradle dependencies.

3. **Build & Deploy** to your device:
   ```bash
   ./gradlew installDebug
   ```

4. **Grant Notification Access Permission**:
   - Open the app.
   - Tap **Open System Settings**.
   - Enable **Notification Access** for **Notification Saver**.

---

## 🔄 CI/CD Pipelines

This repository is equipped with GitHub Actions for automated testing and release deployment:

- **Continuous Integration (`ci.yml`)**: Automatically triggers on pushes and PRs to `main`/`master` to verify Gradle build integrity.
- **Continuous Deployment (`cd.yml`)**: Triggered when a new git tag is pushed (e.g. `v1.0.0`), automatically building a Release APK and publishing a GitHub Release:
  ```bash
  git tag v1.0.0
  git push origin v1.0.0
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

# ThumbPlan

A modern, gamified daily planner and habit tracker for Android. ThumbPlan transforms your daily routines into rewarding challenges with customizable alarms, streaks, coins, and level progression.

---

## ✨ Features

- **Gamified Planning**: Earn coins and maintain streaks by completing tasks on time. Level up and unlock achievements.
- **Smart Full-Screen Alarms**: Reliable full-screen alarm notifications that wake your device even in doze mode, complete with swipe-to-complete or snooze actions.
- **Rich Task Customization**: Organize tasks with custom colors, photos, categories, exact times, reminder intervals, and recurrence rules.
- **Robust Alarm Engine**: Automatic alarm recovery across device reboots and time zone changes via WorkManager and boot receivers.
- **Material 3 Design**: Clean UI with dynamic light and dark theme support, fluid animations, and custom navigation.

---

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room)
- **Background Work**: [AndroidX WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) & `AlarmManager`
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Koala / Ladybug or newer
- **JDK**: Java 11 or higher
- **Android SDK**: Compile SDK `35`, Minimum SDK `26` (Android 8.0+)

### Building from Source

1. Clone this repository:
   ```bash
   git clone https://github.com/kuroyama-web/thumbplan.git
   cd thumbplan
   ```

2. Open the project in Android Studio or build with Gradle:
   ```bash
   # Debug build
   ./gradlew assembleDebug

   # Run tests
   ./gradlew test
   ```

---

## 🔒 Security & Secrets

Sensitive signing keys (`keystore/`, `keystore.properties`) and environment files (`local.properties`) are ignored from version control. Builds automatically fall back to debug signing if release keystore properties are not present.

---

## 📄 License

This project is licensed under the terms of the [MIT License](LICENSE).

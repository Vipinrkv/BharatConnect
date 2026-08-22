# 📦 BharatConnect Android APK Build Guide

BharatConnect supports two high-performance Android compilation options:

---

## 🚀 Option 1: Standalone Hybrid Web App APK (Fast & Lightweight)

Uses the pre-configured Android Gradle build system in `android_app/`:

### 🛠️ Prerequisites
- **Java JDK**: JDK 17 or JDK 19 (`JAVA_HOME`)
- **Android SDK**: Android SDK platform tools (`ANDROID_HOME`)
- **Gradle**: Gradle 8.0+

### ⚡ 1-Click Build
Run the automated build script from the project root:
```cmd
build_apk.bat
```
*(or run `python build_apk.py` directly from PowerShell/CMD).*

### 📱 Output Binary
- **File**: `BharatConnect.apk` (generated at project root)
- **Features**: 100% offline self-contained webview app with full local database support.

---

## ⚡ Option 2: Native Android Kotlin + Jetpack Compose APK

Uses native Jetpack Compose with Room DB, Supabase Realtime, and WorkManager in `android_native/`:

### 🛠️ Prerequisites
- **Java JDK 19+**
- **Android SDK** (API Level 34)

### ⚡ 1-Click Build
Double-click:
```cmd
build_native_apk.bat
```
*(or run `gradlew.bat assembleDebug` inside `android_native/`).*

### 📱 Output Binary
- **File**: `android_native/app/build/outputs/apk/debug/app-debug.apk`
- **Root Artifact**: `BharatConnect-Native.apk`

---

## 📲 Deploying to Connected Device via ADB
To install directly to a connected Android phone or emulator:
```cmd
adb install -r BharatConnect.apk
```
*(or `adb install -r BharatConnect-Native.apk`)*



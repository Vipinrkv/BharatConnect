# 📦 Android Buildozer APK Build Guide

## Prerequisites

- **Python**: 3.11+
- **Buildozer**: 1.6.0
- **Java JDK**: OpenJDK 17 (`openjdk-17-jdk`)
- **Android SDK & NDK**: Automatically managed by Buildozer (API 34, Min API 21)
- **Build Dependencies**: `gcc`, `g++`, `cmake`, `ninja-build`, `git`, `zip`, `unzip`, `libffi-dev`, `libssl-dev`

---

## 🛠️ Build Steps (via WSL / Windows Launcher)

### Option A: 1-Click Windows Launcher
Double-click **`build_apk.bat`** (at project root) or **`android/build_apk.bat`**.
It delegates execution cleanly to the WSL Linux build environment.

### Option B: Trigger Build via Terminal / WSL
Run the compilation shell script inside WSL:
```bash
wsl bash -c "cd /mnt/c/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android && bash build_apk.sh"
```

---

## 📱 Locate Compiled APK

All compiled Android binaries are output directly into **`android/bin/`**:
- `android/bin/bharatconnect-1.0.0-arm64-v8a-debug.apk`


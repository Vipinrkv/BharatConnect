# 📦 Android Buildozer APK Build Guide

## Prerequisites

- **Python**: 3.11+
- **Buildozer**: 1.6.0
- **Java JDK**: OpenJDK 17 (`openjdk-17-jdk`)
- **Android SDK & NDK**: Automatically managed by Buildozer
- **Build Dependencies**: `gcc`, `g++`, `cmake`, `ninja-build`, `git`, `zip`, `unzip`, `libffi-dev`, `libssl-dev`

---

## 🛠️ Build Steps (via WSL / Linux)

1. **Configure buildozer.spec**
   Verify the `buildozer.spec` options:
   ```ini
   package.name = bharatconnect
   package.domain = com.vipin.bharatconnect
   android.api = 33
   android.permissions = INTERNET, READ_CONTACTS, WRITE_CONTACTS, READ_PHONE_STATE, SEND_SMS, RECEIVE_SMS, CAMERA, RECORD_AUDIO
   ```

2. **Trigger Build Command**
   Run Buildozer in WSL Ubuntu:
   ```bash
   wsl bash -c "cd /mnt/c/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect && yes | buildozer android debug"
   ```

3. **Locate Compiled APK**
   After build completion, your APK is stored in the `bin/` directory:
   `bin/bharatconnect-2.0.0-arm64-v8a-debug.apk`

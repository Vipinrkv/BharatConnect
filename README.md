# 🇮🇳 BharatConnect — Native Android (Jetpack Compose) Edition

**Modern • 60/120 FPS Fluid Animations • Offline-First Room SQLite • Supabase Realtime • Material 3**

BharatConnect is a production-grade native Android social & real-time messaging application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room SQLite Database**, **Supabase Realtime & Auth**, and **Cloudinary Media Engine**.

---

## 🚀 Key Features

* **🌟 Stories & Status**: Dynamic story rings on feed, rich status builder with 5 custom color gradients, and full-screen story viewer with auto-advancing progress timers.
* **📍 Nearby Discovery Radar**: Live animated pulse radar, radius filters (`1 km`, `5 km`, `10 km`), distance calculations, and instant 1-tap direct chat.
* **🛍️ Marketplace Hub**: 3 Sub-tabs (**Items** for buying & selling with ₹ pricing, **Jobs** with salary ranges and 1-tap apply, and **Quick Gigs** with instant payouts).
* **💬 Encrypted Chat Hub**: Sub-tabs for **Individual**, **Groups**, and **Communities**, WhatsApp-style attachment sheet (Camera, Gallery, Docs, GPS Location, Contacts), and built-in categorized emoji picker drawer.
* **🔔 Notifications Center**: Categorized activity feed (All, Messages, Likes, System) with mark-all-read.
* **👤 Complete Profile & Settings**: Post/Follower/Following stats, editable profile (avatar, bio, phone, DOB), Sentinel 7-Layer End-to-End Encryption status, and dark theme controls.

---

## ⚡ 1-Click Build & Compilation

To build the standalone debug APK on Windows:
```cmd
build_native_apk.bat
```
The script will compile the app via Gradle and output the final binary to:
- **`BharatConnect-Native.apk`** (in project root)

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9.23
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Local Database**: Room SQLite (`AppDatabase`, `PostDao`, `MessageDao`, `ConversationDao`, `UserDao`)
- **Backend & Auth**: Supabase Realtime + Ktor Client
- **Background Sync**: Android WorkManager (`SyncWorker`, `SyncManager`)
- **Media Engine**: Coil + Cloudinary on-device compression
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Architecture**: MVVM Clean Architecture (Domain, UseCases, Repositories, Presentation)

---

## 📁 Project Structure

```text
BharatConnect/
├── android_native/           # Complete Kotlin Jetpack Compose Android Project
│   ├── app/
│   │   ├── src/main/java/    # Kotlin source code
│   │   │   └── com/bharatconnect/app/
│   │   │       ├── core/     # Database, Network, Theme, Sync & Notifications
│   │   │       ├── data/     # Room entities, DAOs, DTOs & Repositories
│   │   │       ├── domain/   # Models, Repository Interfaces & Use Cases
│   │   │       └── presentation/ # ViewModels & Jetpack Compose Screens
│   │   ├── src/main/res/     # App icons, drawables & values
│   │   └── build.gradle.kts  # App-level dependencies & build configuration
│   └── gradlew.bat           # Gradle build tool
├── BharatConnect-Native.apk  # Standalone Compiled Native Android APK (~22 MB)
├── build_native_apk.bat      # 1-Click build launcher
└── README.md                 # Project documentation
```

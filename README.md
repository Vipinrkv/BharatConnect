# 🇮🇳 BharatConnect — Python Kivy + KivyMD Edition

**100% Free • Universal API & Decoupled Database • Offline-First • Cross-Platform**

BharatConnect is a modern, high-performance mobile social & real-time messaging application built with **Python 3.11**, **Kivy 2.3**, **KivyMD 2.0**, and **FastAPI**. It delivers sub-50ms offline message persistence, feed post interactions, marketplace listings, profile management, and a standalone universal backend API server supporting SQLite, PostgreSQL, and MySQL databases.

---

## ⚡ Quick Start (1-Click Launchers)

### 1. Launch Mobile/Desktop Client
Double-click **`start.bat`** (or **`run.bat`**) in the project folder to start the Kivy frontend app.

### 2. Launch Universal Backend API Server
Double-click **`start_backend.bat`** to start the standalone REST & WebSocket API server on `http://127.0.0.1:8000`.
- **Interactive OpenAPI Docs**: Explore API endpoints live at [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs).

---

## 🌐 Universal Backend & Decoupled Database

BharatConnect includes a standalone, server-agnostic backend (`backend/`) that can run on any server or cloud environment (AWS, GCP, Azure, DigitalOcean, Heroku, local VPS):

- **Framework**: **FastAPI** + **Uvicorn** for asynchronous, high-throughput REST and WebSocket execution.
- **Database ORM**: **SQLAlchemy 2.0** supporting:
  - SQLite (Local zero-config default): `sqlite:///./data/app.db`
  - PostgreSQL: `DATABASE_URL=postgresql://user:password@localhost:5432/bharatconnect`
  - MySQL / MariaDB: `DATABASE_URL=mysql+pymysql://user:password@localhost:3306/bharatconnect`
- **Security**: **JWT** (JSON Web Tokens) authentication with password hashing.
- **Real-Time Messaging**: WebSocket endpoint (`/ws/chat/{chat_id}`) for sub-50ms message broadcasting.

---

## 🔑 Demo Account Credentials

- **Username**: `alexmorgan` (or `alex.morgan@bharatconnect.com`)
- **Password**: `password123`

> 💡 **Quick Demo**: On the Splash Screen, click **Continue as Demo** to jump straight into the full interactive dashboard without logging in.

---

## 🎨 Design System & Color Palette

Designed according to premium modern mobile UI/UX standards:

| Token | Hex | RGBA | Description |
|---|---|---|---|
| `COLOR_6367FF` | `#6367FF` | `[0.388, 0.404, 1.000, 1.0]` | Primary Electric Indigo |
| `COLOR_8494FF` | `#8494FF` | `[0.518, 0.580, 1.000, 1.0]` | Soft Ice Blue Accent |
| `COLOR_C9BEFF` | `#C9BEFF` | `[0.788, 0.745, 1.000, 1.0]` | Light Frost Lavender |
| `COLOR_FFDBFD` | `#FFDBFD` | `[1.000, 0.859, 0.992, 1.0]` | Soft Pink Ice Highlight |
| `COLOR_2F2FE4` | `#2F2FE4` | `[0.184, 0.184, 0.894, 1.0]` | Royal Winter Indigo |
| `COLOR_162E93` | `#162E93` | `[0.086, 0.180, 0.576, 1.0]` | Midnight Winter Blue |
| `COLOR_1A1953` | `#1A1953` | `[0.102, 0.098, 0.325, 1.0]` | Cold Navy Surface Card |
| `COLOR_080616` | `#080616` | `[0.031, 0.024, 0.086, 1.0]` | Deep Frost Midnight |

---

## 📁 Repository Structure

```text
BharatConnect/
├── main.py                   # Main application entry point & ScreenManager setup
├── start.bat                 # One-click launcher for Kivy Mobile App
├── start_backend.bat         # One-click launcher for Universal Backend API Server
├── start_all.bat             # One-click launcher for full stack (Backend + App)
├── build_apk.py / .bat       # Standalone Hybrid WebView APK builder
├── build_native_apk.bat      # Native Android Jetpack Compose APK builder
├── requirements.txt          # Client dependencies
├── README.md                 # Project documentation & guide
├── backend/                  # Universal Server & Decoupled Database Layer
│   ├── server.py             # FastAPI REST & WebSocket API Server
│   ├── database.py           # SQLAlchemy ORM models & universal DB engine
│   ├── schemas.py            # Pydantic API request/response schemas
│   ├── auth.py               # JWT authentication & password security
│   ├── config.py             # Environment & server configurations
│   ├── fcm_push.py           # Firebase Cloud Messaging push notifications
│   ├── google_sheets.py      # Google Sheets cloud database sync connector
│   └── requirements.txt      # Standalone backend dependencies
├── app/                      # Mobile & Desktop Client Application
│   ├── api_client.py         # Universal REST API Client with offline fallback
│   ├── sync_engine.py        # Hybrid offline/online background sync engine
│   ├── session_manager.py    # Encrypted session persistence
│   ├── notifications.py      # Smart in-app notification engine
│   ├── theme.py              # Theme tokens & UI component facade
│   └── screens/              # Pure Python screen implementations
│       ├── splash.py         # Splash Screen
│       ├── login.py          # Login Screen
│       ├── register.py       # Register Screen
│       ├── forgot_password.py# Forgot Password Screen
│       ├── dashboard.py      # Bottom Navigation Container Screen
│       ├── home.py           # Home Feed View
│       ├── chat.py           # Real-Time Chat & Thread Views
│       ├── marketplace.py    # Marketplace View
│       ├── profile.py        # Profile View
│       ├── edit_profile.py   # Edit Profile View
│       ├── reels.py          # Immersive Short Videos View
│       ├── call.py           # E2EE Call Interface
│       └── settings.py       # Settings Screen
├── android_app/              # Standalone Hybrid WebView Android Project (Gradle)
├── android_native/           # Native Android Kotlin + Jetpack Compose Project (Gradle)
├── database/                 # SQLite Local Persistence Layer
│   ├── db.py                 # Engine helper wrappers
│   ├── database.py           # Local SQLite engine (SQLiteDatabaseEngine)
│   └── 01_supabase_master_schema.sql # Master PostgreSQL/Supabase schema
├── utils/                    # UI, Phone & Security Utilities
│   ├── helper.py             # Color tokens, GradientCard, canvas shaders, buttons
│   ├── security.py           # 9-Layer AES/E2EE encryption & token validation
│   ├── bharat_shield.py      # Quantum-Resistant Double-Ratchet security engine
│   ├── phone.py              # Canonical E.164 phone parser & normalizer
│   ├── local_storage.py      # JSON disk caching & offline store
│   ├── contact_sync.py       # Cross-platform device contact importer
│   ├── cloudinary_storage.py # Media storage & upload utility
│   └── smart_fallback.py     # 3-Tier failover handler
└── docs/                     # Comprehensive Documentation Hub
    ├── README.md             # Documentation index & navigation
    ├── architecture.md       # Client UI architecture & tokens
    ├── architecture_master.md# Full system architecture specification
    ├── database_schema.md    # Schema models & contact matching
    ├── user_guide.md         # User guide & feature walkthrough
    ├── apk_build_guide.md    # Android APK compilation guide
    ├── CLOUDINARY_SETUP.md   # Cloudinary media setup guide
    ├── FIREBASE_FCM_SETUP.md # Firebase Cloud Messaging setup
    └── google_sheets_guide.md# Google Sheets database sync guide
```

---

## 📱 Building Android APK

Refer to [`docs/apk_build_guide.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/docs/apk_build_guide.md) or run:
- **Option 1 (Hybrid App)**: Double-click **`build_apk.bat`** (or `python build_apk.py`).
- **Option 2 (Native Compose App)**: Double-click **`build_native_apk.bat`** (or `gradlew.bat assembleDebug` in `android_native/`).



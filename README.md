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
├── start.bat                 # One-click launcher for Kivy Frontend App
├── start_backend.bat         # One-click launcher for Universal Backend API Server
├── start_all.bat             # One-click launcher for full stack (Backend + App)
├── build_apk.bat             # Delegated launcher for Android APK compilation
├── requirements.txt          # Client dependencies
├── README.md                 # Project documentation & guide
├── android/                  # Isolated Android Module & Build System
│   ├── buildozer.spec        # Buildozer spec (API 34, Min API 21, Permissions)
│   ├── build_apk.bat         # 1-Click Windows WSL compilation script launcher
│   ├── build_apk.sh          # Linux/WSL compilation bash script
│   ├── README.md             # Android module guide
│   └── bin/                  # Output folder for compiled .apk binaries
├── backend/                  # Universal Server & Decoupled Database Layer
│   ├── server.py             # FastAPI REST & WebSocket API Server
│   ├── database.py           # SQLAlchemy ORM models & universal DB engine
│   ├── schemas.py            # Pydantic API request/response schemas
│   ├── auth.py               # JWT authentication & password security
│   ├── config.py             # Environment & server configurations
│   └── requirements.txt      # Standalone backend dependencies
├── app/
│   ├── api_client.py         # Universal REST API Client wrapper with offline fallback
│   ├── sync_engine.py        # Hybrid offline/online background sync engine
│   ├── session_manager.py    # Encrypted session persistence
│   ├── notifications.py      # In-app notifications engine
│   ├── screens/              # Pure Python screen implementations
│   │   ├── splash.py         # Splash Screen
│   │   ├── login.py          # Login Screen
│   │   ├── register.py       # Register Screen
│   │   ├── forgot_password.py# Forgot Password Screen
│   │   ├── dashboard.py      # Bottom Navigation Container Screen
│   │   ├── home.py           # Home Feed View
│   │   ├── chat.py           # Chats & Thread Views
│   │   ├── chat_individual.py# 1-on-1 Encrypted Chat
│   │   ├── chat_group.py     # Group Chat View
│   │   ├── chat_community.py # Public Community Channels
│   │   ├── marketplace.py    # Marketplace View
│   │   ├── profile.py        # Profile View
│   │   ├── reels.py          # Short Videos View
│   │   ├── call.py           # E2EE Call View
│   │   └── settings.py       # Settings Screen
│   └── theme.py              # Theme & Color Management
├── database/                 # SQLite Local Persistence Layer
│   ├── db.py                 # Engine helper wrappers
│   └── database.py           # Local SQLite connection & schema (SQLiteDatabaseEngine)
├── utils/                    # UI & Security Utilities
│   ├── helper.py             # Color tokens, GradientCard, RoundedRectangle canvas, buttons
│   ├── security.py           # Multi-layer AES/E2EE encryption
│   ├── local_storage.py      # Encrypted key-value offline storage
│   ├── contact_sync.py       # Cross-platform device contact importer
│   └── smart_fallback.py     # Offline database fallback handler
└── docs/                     # Technical Guides & Schema Docs
    ├── architecture.md
    ├── database_schema.md
    ├── user_guide.md
    └── apk_build_guide.md
```

---

## 📱 Building Android APK

Refer to [`docs/apk_build_guide.md`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/docs/apk_build_guide.md) or double-click **`build_apk.bat`** (or `android/build_apk.bat`) for detailed instructions on packaging BharatConnect into an Android `.apk` using **Buildozer** inside WSL.


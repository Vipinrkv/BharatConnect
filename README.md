# BharatConnect 🇮🇳 — WhatsApp-Style Python Messaging Platform

[![Python](https://img.shields.io/badge/Python-3.11%2B-blue.svg)](https://www.python.org/)
[![Kivy](https://img.shields.io/badge/Kivy-2.3.1-orange.svg)](https://kivy.org/)
[![KivyMD](https://img.shields.io/badge/KivyMD-2.0.1-purple.svg)](https://kivymd.readthedocs.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**BharatConnect** is a native Python messaging platform engineered with **Kivy & KivyMD**. It features a **cold winter vintage aesthetic** powered by an 8-color gradient palette, real-time message bubble interface, multi-identifier authentication, phone contact matching engine, WhatsApp status updates, and call history.

---

## 🎨 Cold Winter Vintage Color Scheme

The application UI is styled using exact winter gradient canvas tokens:

| Color Token | Hex Code | Purpose |
| :--- | :--- | :--- |
| **Electric Indigo** | `#6367FF` | Primary Action Buttons & Sender Chat Gradient Start |
| **Soft Ice Blue** | `#8494FF` | Secondary Container Outlines & Subtle Borders |
| **Frost Lavender** | `#C9BEFF` | Category Badges, Pills & Recipient Sender Titles |
| **Soft Pink Ice** | `#FFDBFD` | Reaction Pills, Highlight Alerts & Badges |
| **Royal Indigo** | `#2F2FE4` | Secondary Linear Gradient Color Blend |
| **Midnight Winter Blue** | `#162E93` | Top Navigation Header Navbar |
| **Cold Navy Surface** | `#1A1953` | Card Backgrounds & Recipient Message Bubbles |
| **Deep Frost Midnight** | `#080616` | Main Window Clear Color Backdrop |

---

## 🚀 Key Features

- **⚡ Sub-50ms Realtime Sync**: Fast O(1) hash map indexed Python storage engine.
- **💬 WhatsApp-Style Message Bubbles**: Sender bubbles in `#6367FF` -> `#2F2FE4` winter gradient with status ticks (`✓✓`), recipient bubbles in cold navy (`#1A1953`), typing indicators, and emoji quick-picker.
- **🔑 Flexible Authentication**:
  - **Sign In**: Login via Email, Mobile Number (`+91`), or Username + Password.
  - **Registration**: Full Name, Email, Mobile Number with Country Code (`+91`), Username, Date of Birth (`YYYY-MM-DD`), Password, and Confirm Password.
  - **Forgot Password**: Email verification with OTP code confirmation step.
- **📇 Address Book Contact Sync**: Matches device phone numbers (`+91 98123 45678`, etc.) against registered users with instant messaging actions.
- **⭕ Status Stories & Calls Log**: View contact status updates and voice/video call history.
- **📱 Android APK Ready**: Includes pre-configured `buildozer.spec` targeting Android API 33.

---

## 📁 Directory Structure

```text
BharatConnect/
├── main.py                  # Primary application entry point & ScreenManager
├── start.bat                # One-click launcher script (checks Python & requirements)
├── run.bat                  # Direct lightweight launcher shortcut
├── requirements.txt         # Python package dependencies
├── buildozer.spec           # Android APK build configuration
├── app/
│   ├── theme.py             # Theme engine, gradient canvas generator & GradientCard
│   └── screens/
│       ├── splash.py        # Cold winter splash screen with hero banner
│       ├── auth.py          # Sign In, Registration (with DOB & +91), Forgot Password
│       └── dashboard.py     # WhatsApp Dashboard (Chats, Status, Calls, Contacts, Settings)
├── database/
│   └── db.py                # O(1) database engine, auth, and phone contact sync
└── docs/
    ├── architecture.md      # System architecture & theme tokens documentation
    ├── database_schema.md   # Data models & phone matching engine docs
    ├── user_guide.md        # Complete user manual & auth flow docs
    └── apk_build_guide.md   # Buildozer Android APK compilation guide
```

---

## ⚡ Quick Start (One-Click Launch)

### On Windows (One-Click Launcher)
Double-click **`start.bat`** in File Explorer. It automatically checks Python 3.11+, installs missing dependencies, and opens the app!

### Via Command Line
```bash
# Install dependencies
pip install -r requirements.txt

# Run application
python main.py
```

---

## 📱 Building the Android APK

Buildozer 1.6.0 is configured for Android packaging. To compile the `.apk` file:

```bash
# Run buildozer in WSL / Linux
wsl bash -c "cd /mnt/c/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect && buildozer android debug"
```
The compiled APK will be saved in `bin/bharatconnect-2.0.0-arm64-v8a-debug.apk`.

---

## 📄 Documentation

Check out the full technical documentation in the [`docs/`](./docs/) directory:
- 🏗️ [System Architecture & Theme Tokens](./docs/architecture.md)
- 🗄️ [Database Schema & Phone Sync](./docs/database_schema.md)
- 📖 [User Guide & Auth Flow](./docs/user_guide.md)
- 📦 [Android Buildozer APK Guide](./docs/apk_build_guide.md)

---

## 📜 License
Licensed under the [MIT License](LICENSE). Built for India 🇮🇳.

# BharatConnect 🇮🇳 — Modern Production Text Messaging Platform (Python Edition)

![Python](https://img.shields.io/badge/Python-v3.11%2B-blue?logo=python)
![Kivy](https://img.shields.io/badge/Kivy-v2.3-green)
![KivyMD](https://img.shields.io/badge/KivyMD-v2.0-purple)
![FastAPI](https://img.shields.io/badge/FastAPI-v0.100%2B-teal?logo=fastapi)
![License](https://img.shields.io/badge/License-MIT-orange)

**BharatConnect** is a 100% Python-based, ultra-fast, secure, and reliable text messaging platform built from first principles for high-throughput, sub-50ms text communication across mobile and desktop devices using **Python**, **Kivy**, **KivyMD**, and **FastAPI**.

---

## ⚡ One-Click & Single-Command Launch

### 1-Click Launch (Windows Batch Script)
Double-click **`start.bat`** (or `run.bat`) in File Explorer, or run:

```cmd
start.bat
```

### Direct Terminal Launch
```bash
python main.py
```

### Optional: Run FastAPI Backend Server Independently
```bash
python -m api.server
```
Backend REST endpoints run on `http://localhost:5000/api/v1` and WebSocket gateway on `ws://localhost:5000/ws`.

---

## 🏗️ Architecture & Project Structure

The codebase is organized into modular Python packages with strict boundary separation:

```
BharatConnect/
├── main.py               # Primary GUI Launcher (Boots Kivy/KivyMD App & Services)
├── start.bat             # 1-Click Windows Batch Launcher
├── run.bat               # Launcher alias
├── requirements.txt      # Python Dependencies (kivy, kivymd, fastapi, uvicorn, websockets)
├── api/                  # Python FastAPI REST Routes & WebSocket Realtime Gateway
│   ├── server.py         # FastAPI Gateway & WebSocket Connection Manager
│   ├── routes.py         # REST Endpoints (Users, Chats, Messages, Communities, Marketplace)
│   └── ws.py             # WebSocket Realtime Event Dispatcher
├── database/             # Storage Layer: O(1) Indexed Python Database Engine (db.py)
├── docs/                 # Product Requirements & Architecture Specifications
└── app/                  # Kivy / KivyMD Native Desktop & Mobile GUI Application
    ├── screens/
    │   ├── splash.py     # Hero Splash Landing Screen
    │   ├── login.py      # Multi-User Identity Selector & Login Screen
    │   └── dashboard.py  # Primary Dashboard (Chats, Communities, Marketplace, Nearby, Settings)
```

---

## 🚀 Key Features

1. **Native Material Design GUI**:
   - Built with KivyMD 2.0+ featuring dark/light mode toggle, dynamic layout responsiveness, and modern glassmorphic feel.
2. **Sub-50ms Realtime Messaging**:
   - Speech bubbles with timestamps, read receipts (`✓✓`), typing simulator, quick emoji reactions (`🇮🇳`, `🚀`, `👍`), and message search filter.
3. **Multi-User Test Accounts**:
   - Instant identity switcher between `@vipin_k`, `@rahul_dev`, `@priya_design`, and `@ananya_pm`.
4. **Indian Tech Communities 🇮🇳**:
   - Discover and join developer & startup communities across India.
5. **Local Tech Marketplace & Nearby Discovery**:
   - List/buy hardware gear & services with instant seller contact via direct chat.
   - Proximity map/list of developers across Bengaluru, Mumbai, Gurgaon, and Hyderabad.

---

## 👥 Pre-Configured Test Accounts

| Handle | Display Name | Role | Status |
| :--- | :--- | :--- | :--- |
| `@vipin_k` | Vipin Kumar | Senior Architect & Core Developer | `ONLINE` |
| `@rahul_dev` | Rahul Sharma | Fullstack Engineer | `ONLINE` |
| `@priya_design` | Priya Patel | Lead Product Designer | `IDLE` |
| `@ananya_pm` | Ananya Verma | Product Manager | `OFFLINE` |

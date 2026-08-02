# System Architecture Document — BharatConnect (Python Edition)

This document describes the Python system architecture, component boundaries, Kivy/KivyMD UI layout specs, FastAPI REST & WebSocket server protocols, and $O(1)$ database storage engine.

---

## 1. System Topology & Performance Architecture

BharatConnect is built as a pure **Python modular client-server architecture**:

```
+-------------------------------------------------------------------------+
|                  Kivy / KivyMD GUI Application (app/)                   |
|  [screens/]                                   [components/]             |
|  - splash.py (Hero Landing)                   - Custom MDCard           |
|  - login.py (Account Switcher)                - Custom Speech Bubbles   |
|  - dashboard.py (Chats, Communities,          - Custom MDButton         |
|    Marketplace, Nearby, Settings)                                       |
+------------------------------------+------------------------------------+
                                     |
                          [FastAPI REST & WebSockets]
                          (/api/v1  &  /ws/{user_id})
                                     |
                                     v
+-------------------------------------------------------------------------+
|                Python FastAPI Server & WebSocket Gateway                 |
|                       (api/server.py & api/ws.py)                       |
+------------------------------------+------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                         In-Memory Storage Engine                        |
|                  (database/db.py - O(1) Hash Map Indexing)              |
+-------------------------------------------------------------------------+
```

---

## 2. Component Design & Directory Boundaries

### 2.1 Native GUI Layer (`app/`)
- **`main.py`**: App entry point configuring Window dimensions (1100x720), KivyMD Material Design Dark/Light themes, and screen routing via `ScreenManager`.
- **`app/screens/`**:
  - `splash.py`: Welcome landing screen with hero branding and action triggers.
  - `login.py`: Multi-identity account quick switcher grid (`@vipin_k`, `@rahul_dev`, `@priya_design`, `@ananya_pm`) plus custom credential login form.
  - `dashboard.py`: Core dashboard housing sidebar tabs:
    - 💬 **Chats View**: Direct & group chat list, unread badges, presence indicators, message timeline stream (bubbles, timestamps, `✓✓` read checkmarks), typing simulator, quick emoji dispatches (`🇮🇳`, `🚀`), and search filter.
    - 🌐 **Communities Hub**: Discover and join developer & startup communities across India with toggle actions.
    - 🛒 **Marketplace**: Hardware gear & technical service listings with direct seller chat initiation.
    - 📍 **Nearby Developers**: Proximity listing across Bengaluru, Mumbai, Gurgaon, and Hyderabad tech hubs.
    - ⚙️ **Settings & Profile**: Profile customization, status message editor, and dark/light mode toggle.

### 2.2 Backend & Storage Layer (`api/` & `database/`)
- **FastAPI Gateway (`api/server.py`)**: Mounts REST endpoints under `/api/v1` and WebSocket endpoint under `/ws/{user_id}`.
- **WebSocket Manager (`api/ws.py`)**: Handles active client socket connections, heartbeats, and real-time event broadcasting.
- **Database Engine (`database/db.py`)**: High-performance storage engine using $O(1)$ Hash Map lookups (`users`, `chats`, `messages`, `communities`, `marketplace`, `nearby`).

---

## 3. Communication Protocols & Events

### 3.1 REST API Endpoints (`api/routes.py`)

| Endpoint | Method | Description | Service Handler |
|:---|:---|:---|:---|
| `/api/v1/users` | `GET` | Fetch all user profiles | `db_engine.users` |
| `/api/v1/users/me` | `GET` | Get active user profile | `db_engine.get_current_user()` |
| `/api/v1/users/switch/{user_id}` | `POST` | Switch active identity | `db_engine.switch_user()` |
| `/api/v1/chats` | `GET` | Get user chat list | `db_engine.get_user_chats()` |
| `/api/v1/messages/{chat_id}` | `GET` | Get messages for chat | `db_engine.get_messages_for_chat()` |
| `/api/v1/messages` | `POST` | Send new message | `db_engine.send_message()` |
| `/api/v1/communities` | `GET` | List tech communities | `db_engine.communities` |
| `/api/v1/marketplace` | `GET` | List marketplace items | `db_engine.marketplace` |
| `/api/v1/nearby` | `GET` | List nearby developers | `db_engine.nearby` |

### 3.2 WebSocket Realtime Protocol (`/ws/{user_id}`)

- **`message.send`**: Computes `seq_id`, persists message, returns ACK, and broadcasts `message.received` to chat room participants.
- **`typing.start` / `typing.stop`**: Emits real-time typing indicators to room.
- **`presence.update`**: Broadcasts `ONLINE`, `IDLE`, or `OFFLINE` status changes.

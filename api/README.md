# BharatConnect API Router & Connection Hub

The **`api/`** directory serves as the **central connection hub** linking all components of the system (`frontend/`, `backend/`, and `database/`).

---

## 🔌 Connection Bridge Responsibilities

```
[ Frontend (Client) ] ── (HTTP / WebSocket) ──> [ api/ Router ]
                                                     │
                                   ┌─────────────────┴─────────────────┐
                                   ▼                                   ▼
                      [ backend/ Services ]                  [ database/ Engine ]
```

1. **`api/index.js`**: Assembly point that initializes Express, mounts REST routes, attaches WebSocket event handlers, and exports `createApiServer()`.
2. **`api/routes.js`**: Express REST router translating HTTP endpoints (`/api/v1/auth`, `/api/v1/users`, `/api/v1/chats`, `/api/v1/messages`) to calls on `backend/services/chatService.js`.
3. **`api/events.js`**: WebSocket event dispatcher translating real-time socket events (`auth`, `message.send`, `typing.start`, `message.read`) to backend service logic and `wsGateway` broadcasting.

---

## 📡 REST API Specifications

Base URL: `http://localhost:5000/api/v1`

| Category | Endpoint | Method | Connected Backend Service Method |
| :--- | :--- | :--- | :--- |
| **Auth** | `/auth/login` | `POST` | `chatService.authenticateUser()` |
| **Users** | `/users/me` | `GET` | `chatService.getCurrentUser()` |
| **Users** | `/users/search` | `GET` | `chatService.searchUsers()` |
| **Chats** | `/chats` | `GET` | `chatService.getUserChats()` |
| **Chats** | `/chats/direct` | `POST` | `chatService.createDirectChat()` |
| **Chats** | `/chats/group` | `POST` | `chatService.createGroupChat()` |
| **Messages**| `/chats/:id/messages` | `GET` | `chatService.getMessages()` |

---

## ⚡ WebSocket Gateway Event Specifications

Gateway URL: `ws://localhost:5000`

- `auth`: Client socket authentication & presence initialization.
- `message.send`: Client message submission -> assigns `seq_id`, saves to `database/db.js`, emits `message.ack` & broadcasts `message.receive`.
- `message.edit`: Edits message content & broadcasts `message.updated`.
- `message.delete`: Deletes message (for me/everyone) & broadcasts `message.updated`.
- `typing.start` / `typing.stop`: Broadcasts typing indicators.
- `message.read`: Marks messages read & broadcasts blue tick receipts.

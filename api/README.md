# BharatConnect API Connection Bridge

The **`api/`** directory acts as the central connection hub linking all layers of the application (`frontend/`, `backend/`, and `database/`).

---

## 🔌 Connection Bridge Architecture

```
[ Frontend Client ] ── (HTTP / WebSockets) ──> [ api/ Router ]
                                                     │
                                   ┌─────────────────┴─────────────────┐
                                   ▼                                   ▼
                      [ backend/ Services ]                  [ database/ Engine ]
```

1. **`api/index.js`**: Central API Server builder exporting `createApiServer(port)`.
2. **`api/routes.js`**: Express REST Router mapping HTTP endpoints to `backend/services/chatService.js`.
3. **`api/events.js`**: WebSocket Event Router mapping socket actions to `backend/services/chatService.js` and `backend/wsGateway.js` broadcasting.

---

## 📡 REST API Specifications

Base URL: `http://localhost:5000/api/v1`

| Category | Endpoint | Method | Connected Backend Method |
| :--- | :--- | :--- | :--- |
| **Auth** | `/auth/login` | `POST` | `chatService.authenticateUser()` |
| **Users** | `/users/me` | `GET` | `chatService.getCurrentUser()` |
| **Users** | `/users/search` | `GET` | `chatService.searchUsers()` |
| **Chats** | `/chats` | `GET` | `chatService.getUserChats()` |
| **Chats** | `/chats/direct` | `POST` | `chatService.createDirectChat()` |
| **Chats** | `/chats/group` | `POST` | `chatService.createGroupChat()` |
| **Messages**| `/chats/:id/messages` | `GET` | `chatService.getMessages()` |

---

## ⚡ Real-Time WebSocket Protocol

Gateway URL: `ws://localhost:5000`

- `auth`: Registers user socket and updates presence to `ONLINE`.
- `message.send`: Generates `seq_id`, persists to DB, sends ACK to sender, broadcasts `message.receive` to room.
- `message.edit`: Updates message text and broadcasts `message.updated`.
- `message.delete`: Soft-deletes message and broadcasts `message.updated`.
- `typing.start` / `typing.stop`: Broadcasts live typing state to room.
- `message.read`: Marks messages as read and broadcasts blue tick receipts.

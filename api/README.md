# BharatConnect API Connection Bridge & Performance Layer

The **`api/`** directory acts as the central connection hub linking all layers of the application (`frontend/`, `backend/`, and `database/`), optimized for high-throughput, low-latency performance.

---

## 🔌 Connection Bridge & Speedup Architecture

```
[ Frontend Client ] ── (HTTP / WebSockets) ──> [ apiManager.js ] ──> [ api/ Router & Speedup Layer ]
                                                                             │
                                                           ┌─────────────────┴─────────────────┐
                                                           ▼                                   ▼
                                              [ backend/ Services ]                  [ database/ Engine ]
                                                                                     (O(1) Map Indexes)
```

1. **`api/index.js`**: Central API Server builder exporting `createApiServer(port)`.
2. **`api/routes.js`**: Express REST Router mapping HTTP endpoints with `Cache-Control` speedup headers to `backend/services/chatService.js`.
3. **`api/events.js`**: WebSocket Event Router mapping socket actions to `backend/services/chatService.js` and `backend/wsGateway.js` broadcasting.

---

## 📡 REST API Specifications

Base URL: `http://localhost:5000/api/v1`

| Category | Endpoint | Method | Speedup Header | Connected Backend Method |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `/auth/login` | `POST` | N/A | `chatService.authenticateUser()` |
| **Users** | `/users/me` | `GET` | `Cache-Control: private, max-age=5` | `chatService.getCurrentUser()` |
| **Users** | `/users/search` | `GET` | `Cache-Control: private, max-age=3` | `chatService.searchUsers()` |
| **Chats** | `/chats` | `GET` | `Cache-Control: private, max-age=2` | `chatService.getUserChats()` |
| **Chats** | `/chats/direct` | `POST` | N/A | `chatService.createDirectChat()` |
| **Chats** | `/chats/group` | `POST` | N/A | `chatService.createGroupChat()` |
| **Messages**| `/chats/:id/messages` | `GET` | `Cache-Control: private, max-age=1` | `chatService.getMessages()` |

---

## ⚡ Real-Time WebSocket Protocol & API Manager Integration

Gateway URL: `ws://localhost:5000`

- **Handshake & Auth**: `auth` registers socket, sets presence `ONLINE`, dispatches connection events to `apiManager`.
- **Real-Time Messages**: `message.send` computes `seq_id`, saves to DB, returns ACK to sender, broadcasts `message.receive` to recipients.
- **Message Updates**: `message.edit` & `message.delete` broadcast `message.updated`.
- **Presence & Typing**: `typing.start`, `typing.stop`, and `user.presence` dispatch live room signals.
- **Read Receipts**: `message.read` marks messages as read and emits blue tick receipts.

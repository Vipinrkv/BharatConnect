# BharatConnect Backend Engine

The **`backend/`** directory contains the core business logic, real-time WebSocket Gateway, and permission validation services for BharatConnect.

---

## 🛠️ Modules & File Responsibilities

```
backend/
├── services/
│   └── chatService.js    # Business logic service (message sequencing, user search, permissions)
├── wsGateway.js          # Realtime WebSocket session gateway & broadcasting engine
├── server.js             # Standalone backend launcher (imports createApiServer from ../api)
└── package.json          # Backend Node dependencies (express, cors, ws)
```

---

## 🔌 Connection Flow

- **Receives Events From**: `api/routes.js` and `api/events.js`.
- **Interacts With**: `database/db.js` for data persistence.
- **Broadcasts To**: Connected WebSocket client sockets via `wsGateway.js`.

---

## 🚀 Independent Execution

While `npm start` in the root folder launches the full stack concurrently, you can also run the backend independently:

```bash
cd backend
npm start
```
- REST API Base: `http://localhost:5000/api/v1`
- WebSocket Gateway: `ws://localhost:5000`

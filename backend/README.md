# BharatConnect Backend Engine

The **`backend/`** directory contains the core business logic, permissions enforcement, and real-time WebSocket connection gateway for BharatConnect.

---

## 🏗️ Architecture & Modules

```
backend/
├── services/
│   └── chatService.js    # Business logic service (message sequence numbering, permissions, user search)
├── wsGateway.js          # WebSocket Realtime Gateway (connection session map & broadcasting engine)
├── server.js             # Backend server runner (imports unified API assembly from ../api)
└── package.json          # Backend dependencies
```

---

## 🔌 Connection Flow

- **Receives Calls From**: `api/routes.js` and `api/events.js`.
- **Sends Data To**: `database/db.js` for persistent storage and retrieval.
- **Emits Realtime Events To**: Frontend clients via `wsGateway.js`.

---

## 🚀 Running Backend Separately

While the entire system can be initiated with `node index.js` from root, the backend server can also be launched directly:

```bash
cd backend
npm start
```
- **REST Base**: `http://localhost:5000/api/v1`
- **WebSocket Gateway**: `ws://localhost:5000`

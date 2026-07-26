# BharatConnect 🇮🇳 — Modern Production Text Messaging Platform

BharatConnect is an ultra-fast, secure, reliable, and scalable text messaging platform built from first principles for high-throughput, low-latency text communication across mobile and desktop devices.

---

## ⚡ Single-Command System Initiation

You can launch the **entire platform** (Database Engine, Backend REST Server, WebSocket Gateway, and React Frontend App) with **one single command** from the project root:

```bash
npm start
```

When you run `npm start`:
1. 🟦 **Backend & API Engine** starts on `http://localhost:5000` (`/api/v1` REST + `ws://localhost:5000` WebSockets).
2. 🟪 **Frontend Web App** starts concurrently on `http://localhost:5173`.
3. Terminal displays color-coded logs for both processes simultaneously!

---

## 🏗️ Architecture & Folder Separation

```
BharatConnect/
├── index.js          # Root System Master Initiator (Boots DB Engine, Backend, & API Router)
├── package.json      # Root NPM single-command launcher (`npm start`)
├── api/              # Central Connection Hub: REST Routes (routes.js) & WebSocket Events Router (events.js)
├── backend/          # Core Business Services (chatService.js) & Realtime Gateway (wsGateway.js)
├── database/         # Database Layer: DDL Schemas (schema.sql) & Storage Model Engine (db.js)
├── docs/             # Product Requirements, System Architecture, & Schemas Specs
└── frontend/         # React (Vite) Web Application with Glassmorphism UI
```

---

## 🔌 Inter-Folder Connection Architecture

```
[ frontend/ (React UI) ] ── (HTTP / WebSocket) ──> [ api/ Router & Connection Bridge ]
                                                         │
                                       ┌─────────────────┴─────────────────┐
                                       ▼                                   ▼
                          [ backend/ Services ]                  [ database/ Engine ]
```

- **`frontend/`**: Sends HTTP REST requests (`/api/v1`) and opens WebSocket connections (`ws://localhost:5000`).
- **`api/` (Central Connection Bridge)**:
  - `api/routes.js`: Translates HTTP REST calls into `backend/services/chatService.js` invocations.
  - `api/events.js`: Dispatches real-time WebSocket frames (`message.send`, `typing.start`, `message.read`) to backend services and `backend/wsGateway.js`.
  - `api/index.js`: Assembles Express, mounts REST routes, attaches WebSocket handlers, and exports `createApiServer()`.
- **`backend/`**: Contains core business logic (`backend/services/chatService.js`) and real-time socket gateway (`backend/wsGateway.js`).
- **`database/`**: Contains database schemas (`database/schema.sql`) and data storage model engine (`database/db.js`).

---

## 📚 Documentation Index

- 📘 [docs/product_requirements.md](docs/product_requirements.md)
- 🏗️ [docs/system_architecture.md](docs/system_architecture.md)
- 🗄️ [docs/database_schema.md](docs/database_schema.md)
- 📖 [docs/bharatsphere_comprehensive_guide.md](docs/bharatsphere_comprehensive_guide.md)

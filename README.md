# BharatConnect 🇮🇳 — Modern Production Text Messaging Platform

![Node.js](https://img.shields.io/badge/Node.js-v18%2B-green?logo=node.js)
![React](https://img.shields.io/badge/React-v18.3-blue?logo=react)
![Vite](https://img.shields.io/badge/Vite-v8.1-646CFF?logo=vite)
![WebSocket](https://img.shields.io/badge/WebSocket-Gateway-purple)
![License](https://img.shields.io/badge/License-MIT-orange)

**BharatConnect** is an ultra-fast, secure, reliable, and scalable text messaging platform built from first principles for high-throughput, sub-50ms text communication across mobile and desktop devices.

---

## ⚡ Single-Command System Initiation

You can launch the **entire platform** (Database Storage Engine, Backend Server, API Gateway, Realtime WebSocket Gateway, and React Web App) with a **single command** from the root folder:

```bash
npm start
```

When you run `npm start`:
- 🟦 **Backend & API Engine** starts on `http://localhost:5000` (`/api/v1` REST + `ws://localhost:5000` WebSockets).
- 🟪 **React Frontend App** starts concurrently on `http://localhost:5173`.
- Terminal displays color-coded logs for both processes simultaneously!

---

## 🏗️ Architecture & Folder Separation

The system follows a strict modular structure where responsibilities are separated across dedicated folders, with **`api/`** acting as the central connection bridge:

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

## 👥 Instant Multi-User Testing Accounts

The system comes pre-configured with four test identities for live multi-user WebSocket testing across browser tabs:

| Handle | Display Name | Role | Presence |
| :--- | :--- | :--- | :--- |
| `@vipin_k` | Vipin Kumar | Senior Architect & Developer | `ONLINE` |
| `@rahul_dev` | Rahul Sharma | Fullstack Engineer | `ONLINE` |
| `@priya_design` | Priya Patel | Lead Product Designer | `IDLE` |
| `@ananya_pm` | Ananya Verma | Product Manager | `OFFLINE` |

*Use the **Account Switcher Dropdown** in the top-left sidebar of the web app to switch identities on the fly!*

---

## 📚 Master Documentation Index

- 📘 **[docs/product_requirements.md](docs/product_requirements.md)** — PRD, vision, zero-media text scope.
- 🏗️ **[docs/system_architecture.md](docs/system_architecture.md)** — Component topology, WebSocket gateway load balancing, presence state machine.
- 🗄️ **[docs/database_schema.md](docs/database_schema.md)** — PostgreSQL DDL tables, indexes, ScyllaDB migration plan.
- 📖 **[docs/bharatsphere_comprehensive_guide.md](docs/bharatsphere_comprehensive_guide.md)** — Master Blueprint & Engineering Guide.

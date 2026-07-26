# BharatConnect Architecture & System Specification Documentation

The **`docs/`** directory contains all product requirement documents, system architecture diagrams, database schemas, and master engineering blueprints.

---

## 📑 Specification Documents Index

- 📘 **[product_requirements.md](product_requirements.md)** — PRD, vision, zero-media text focus, non-functional requirements.
- 🏗️ **[system_architecture.md](system_architecture.md)** — High-level System Architecture, component topology, WebSocket load balancing, presence state machine.
- 🗄️ **[database_schema.md](database_schema.md)** — Relational PostgreSQL DDL tables, indexes, and ScyllaDB migration roadmap.
- 📖 **[bharatsphere_comprehensive_guide.md](bharatsphere_comprehensive_guide.md)** — Master Blueprint & System Engineering Guide.

---

## 🔌 System Topology Overview

```
[ frontend/ (React App) ]
          │
          ▼ HTTP / WebSockets
  [ api/ (Router) ]
          │
          ▼ Service Calls
[ backend/ (ChatService & wsGateway) ]
          │
          ▼ Queries & Persistence
[ database/ (SQL Schema & db.js) ]
```

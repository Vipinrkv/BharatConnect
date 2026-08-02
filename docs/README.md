# BharatConnect Architecture & System Specification Documentation (Python Edition)

The **`docs/`** directory contains all product requirement documents, system architecture specs, database models, and engineering blueprints for **BharatConnect (Python Edition)**.

---

## 📑 Specification Documents Index

- 📘 **[product_requirements.md](product_requirements.md)** — PRD, vision, sub-50ms latency text focus, and non-functional specifications.
- 🏗️ **[system_architecture.md](system_architecture.md)** — Python system topology, Kivy/KivyMD component hierarchy, FastAPI REST & WebSocket event dispatching.
- 🗄️ **[database_schema.md](database_schema.md)** — Python $O(1)$ Hash-Indexed Data Engine, relational schema definitions, and persistent storage engine.
- 📖 **[bharatsphere_comprehensive_guide.md](bharatsphere_comprehensive_guide.md)** — Comprehensive System Engineering Blueprint & Feature Specifications.

---

## 🔌 System Topology Overview

```
[ Native Kivy / KivyMD Desktop & Mobile App (app/screens/) ]
                           │
                           ▼ HTTP / WebSockets
       [ FastAPI Gateway & API Router (api/server.py) ]
                           │
                           ▼ Realtime Event Dispatch
   [ Python Storage & O(1) Data Engine (database/db.py) ]
```

# BharatConnect Architecture & System Specification Documentation

The **`docs/`** directory contains all authoritative product requirement documents, system architecture diagrams, database specifications, and master technical design guides for BharatConnect.

---

## 📑 Specification Documents Index

- 📘 **[product_requirements.md](product_requirements.md)**: Product Requirements Document (PRD), core vision, non-functional requirements, scope boundaries (Phase 1 zero-media focus).
- 🏗️ **[system_architecture.md](system_architecture.md)**: High-level System Architecture, component topology, WebSocket load balancing, message lifecycle state machines, edge caching, and scalability plan (1k to 10M+ users).
- 🗄️ **[database_schema.md](database_schema.md)**: Database relational model, SQL DDL statements, indexing strategies, and ScyllaDB migration path.
- 📖 **[bharatsphere_comprehensive_guide.md](bharatsphere_comprehensive_guide.md)**: Master Blueprint & System Engineering Guide.

---

## 🔌 System Architecture Summary

```
[ frontend/ (React UI) ]
          │
          ▼ HTTP & WebSocket
  [ api/ (Router) ]
          │
          ▼ Service Calls
[ backend/ (ChatService & Gateway) ]
          │
          ▼ Queries & Persistence
[ database/ (SQL & Storage Model) ]
```

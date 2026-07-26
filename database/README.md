# BharatConnect Database & Storage Layer

The **`database/`** directory contains the database schema SQL definitions, indexing structures, and data abstraction engine for BharatConnect.

---

## 📁 Files in database/

```
database/
├── schema.sql      # DDL SQL Schema statements (PostgreSQL tables, indexes, constraints)
├── db.js           # Abstract Data Engine (User queries, chat storage, message operations)
└── README.md       # Directory documentation
```

---

## 🗄️ Core Entities & Schema Model

1. **`users`**: User identities, Handles (`@username`), status messages, presence flags.
2. **`chats`**: Direct & Group chats, titles, descriptions, owner ID, participant arrays.
3. **`messages`**: Sequence ordered messages per room (`chat_id`, `seq_id`), client UUIDs (`client_message_id`), replies, edit/delete flags.
4. **`contacts` & `user_blocks`**: Contact lists and block matrices.

---

## 🔌 Connection Flow

- **Imported By**: `backend/services/chatService.js` and root `index.js`.
- **Purpose**: Provides asynchronous data access methods (`getUserById`, `getChatsForUser`, `saveMessage`, `updateUserPresence`) so backend services remain completely decoupled from underlying storage implementation (PostgreSQL / ScyllaDB).

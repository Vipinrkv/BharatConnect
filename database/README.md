# BharatConnect Database & Persistence Layer

The **`database/`** directory manages database DDL schema definitions, SQL table constraints, indexing rules, and data access engines.

---

## 📁 Files & Structure

```
database/
├── schema.sql      # PostgreSQL DDL table definitions & composite sequence indexes
├── db.js           # Abstract Data Engine exporting user, chat, and message operations
└── README.md       # Directory documentation
```

---

## 🗄️ Database Table Entities

1. **`users`**: Handles (`@username`), Display names, bios, status messages, verification flags.
2. **`chats`**: Direct and Group chats, titles, descriptions, owner ID, participant arrays.
3. **`messages`**: Sequence ordered messages per room (`chat_id`, `seq_id`), client UUIDs (`client_message_id`), replies, edit/deletion markers.
4. **`contacts` & `user_blocks`**: User contacts and block matrix.

---

## 🔌 Connection Flow

- **Imported By**: `backend/services/chatService.js` and root `index.js`.
- **Decoupled Architecture**: All storage operations are abstracted in `db.js` so switching from SQLite/PostgreSQL to ScyllaDB at 10M+ scale requires zero changes to API routes or WebSocket handlers.

-- BharatConnect Core PostgreSQL Database DDL Schema

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(30) UNIQUE NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    status_message VARCHAR(160) DEFAULT 'Hey there! I am using BharatConnect.',
    bio VARCHAR(160),
    presence VARCHAR(20) DEFAULT 'OFFLINE',
    last_seen TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chats (
    chat_id VARCHAR(36) PRIMARY KEY,
    chat_type VARCHAR(10) NOT NULL, -- DIRECT, GROUP
    title VARCHAR(100),
    description TEXT,
    owner_id VARCHAR(36) REFERENCES users(user_id),
    participants JSONB NOT NULL, -- Array of user_ids
    roles JSONB DEFAULT '{}'::jsonb,
    pinned_by JSONB DEFAULT '[]'::jsonb,
    archived_by JSONB DEFAULT '[]'::jsonb,
    muted_by JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS messages (
    message_id VARCHAR(36) PRIMARY KEY,
    chat_id VARCHAR(36) REFERENCES chats(chat_id) ON DELETE CASCADE,
    sender_id VARCHAR(36) REFERENCES users(user_id),
    seq_id BIGINT NOT NULL,
    client_message_id VARCHAR(64) UNIQUE NOT NULL,
    parent_message_id VARCHAR(36),
    content TEXT NOT NULL,
    is_edited BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_forwarded BOOLEAN DEFAULT FALSE,
    is_pinned BOOLEAN DEFAULT FALSE,
    status VARCHAR(15) DEFAULT 'DELIVERED', -- SENT, DELIVERED, READ
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_seq ON messages(chat_id, seq_id DESC);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

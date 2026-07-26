# Database Schema Guide - BharatSphere

BharatSphere uses SQLite for portable local development. This document details all tables, fields, constraints, relationships, and queries.

---

## 1. Entity Relationship Overview

```
                      +-------------------+
                      |       users       | <------+
                      +-------------------+        |
                       |       |       ^           |
                       |       |       |           |
                       v       v       |           |
        +-------------------+ +------------------+ |
        |      friends      | |  group_members   | |
        +-------------------+ +------------------+ |
                                       ^           |
                                       |           |
                      +-------------------+        |
                      |      groups       | <------+
                      +-------------------+        |
                               |                   |
                               v                   |
                      +-------------------+        |
                      |     messages      | -------+
                      +-------------------+
                               |
                               v
                      +-------------------+
                      |   media_assets    |
                      +-------------------+
```

---

## 2. Table Definitions

### 2.1 Users Table
Stores information for authenticated user accounts.
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uid_10 TEXT UNIQUE NOT NULL,            -- 10-char Alphanumeric unique ID
    username TEXT UNIQUE NOT NULL,          -- User's chosen handle
    email TEXT UNIQUE NOT NULL,             -- Contact email
    phone TEXT UNIQUE NOT NULL,             -- Phone number (with country code)
    password_hash TEXT NOT NULL,            -- Hashed password (bcrypt)
    latitude REAL,                          -- Current/Simulated latitude
    longitude REAL,                         -- Current/Simulated longitude
    avatar_url TEXT,                        -- Avatar image path
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_uid ON users(uid_10);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_phone ON users(phone);
```

### 2.2 Friends Table
Tracks mutual friendships. Users can only message if status is `ACCEPTED`.
```sql
CREATE TABLE friends (
    user_id TEXT NOT NULL,                 -- Initiating User uid_10
    friend_id TEXT NOT NULL,               -- Targeted User uid_10
    status TEXT NOT NULL,                   -- 'PENDING', 'ACCEPTED', 'BLOCKED'
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(uid_10) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(uid_10) ON DELETE CASCADE
);

CREATE INDEX idx_friends_lookup ON friends(user_id, status);
```

### 2.3 Groups & Communities Table
Contains details for group chats and communities.
```sql
CREATE TABLE groups (
    id TEXT PRIMARY KEY,                    -- Generated UUID
    name TEXT NOT NULL,                     -- Title
    description TEXT,                       -- Purpose description
    type TEXT NOT NULL,                     -- 'PUBLIC', 'PRIVATE'
    is_community BOOLEAN DEFAULT FALSE,     -- True if it's a Community shell
    parent_community_id TEXT,               -- Link if group belongs to a community
    created_by TEXT NOT NULL,               -- Owner uid_10
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(uid_10) ON DELETE CASCADE,
    FOREIGN KEY (parent_community_id) REFERENCES groups(id) ON DELETE SET NULL
);

CREATE INDEX idx_groups_community ON groups(parent_community_id);
```

### 2.4 Group Members Table
Tracks membership in groups/communities and privileges.
```sql
CREATE TABLE group_members (
    group_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    role TEXT NOT NULL,                     -- 'OWNER', 'ADMIN', 'MEMBER'
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(uid_10) ON DELETE CASCADE
);
```

### 2.5 Messages Table
Stores individual chat and group chat messages.
```sql
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_type TEXT NOT NULL,                -- 'DIRECT', 'GROUP'
    sender_id TEXT NOT NULL,                -- Sender uid_10
    receiver_id TEXT,                       -- Receiver uid_10 (NULL for group messages)
    group_id TEXT,                          -- Group ID (NULL for direct messages)
    content TEXT,                           -- Text content
    media_url TEXT,                         -- URL/path to shared file
    media_hash TEXT,                        -- Deduplication identifier
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(uid_10),
    FOREIGN KEY (receiver_id) REFERENCES users(uid_10),
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_direct ON messages(sender_id, receiver_id);
CREATE INDEX idx_messages_group ON messages(group_id);
```

### 2.6 Posts Table
Holds user posts displayed on the global/friend timeline.
```sql
CREATE TABLE posts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,                  -- Creator uid_10
    content TEXT NOT NULL,                  -- Post body
    media_url TEXT,                         -- Accompanying photo/video
    media_hash TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(uid_10) ON DELETE CASCADE
);
```

### 2.7 Marketplace Listings Table
Stores products, gigs, jobs, and emergencies.
```sql
CREATE TABLE marketplace (
    id TEXT PRIMARY KEY,                    -- Generated UUID
    user_id TEXT NOT NULL,                  -- Seller/Poster uid_10
    type TEXT NOT NULL,                     -- 'GIG', 'JOB', 'PRODUCT', 'EMERGENCY'
    title TEXT NOT NULL,                    -- Title of listing
    description TEXT NOT NULL,              -- Detailed explanation
    price REAL DEFAULT 0.0,                 -- Price/Compensation (0 for emergency)
    latitude REAL NOT NULL,                 -- Location latitude
    longitude REAL NOT NULL,                -- Location longitude
    media_url TEXT,                         -- Display image
    media_hash TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(uid_10) ON DELETE CASCADE
);

CREATE INDEX idx_marketplace_location ON marketplace(latitude, longitude);
CREATE INDEX idx_marketplace_type ON marketplace(type);
```

### 2.8 Media Assets (Deduplication) Table
Used to resolve deduplication of physical media files.
```sql
CREATE TABLE media_assets (
    file_hash TEXT PRIMARY KEY,             -- SHA-256 string
    file_path TEXT NOT NULL,                -- Physical local path
    file_size INTEGER NOT NULL,             -- Length in bytes
    mime_type TEXT,                         -- File type
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 3. Important Queries

### 3.1 Mutual Friendship Check
Check if User A and User B are mutual friends to allow messaging:
```sql
SELECT 1 FROM friends
WHERE ((user_id = :userA AND friend_id = :userB) OR (user_id = :userB AND friend_id = :userA))
  AND status = 'ACCEPTED';
```

### 3.2 Nearby Location SQL Filtering (Bounding Box)
Query public listings and emergency requests within a bounded region around `lat` and `lon`:
```sql
-- Delta degrees computed by node backend application
SELECT *, 
       ( (latitude - :lat) * (latitude - :lat) + 
         (longitude - :lon) * (longitude - :lon) ) AS distance_squared
FROM marketplace
WHERE latitude BETWEEN :minLat AND :maxLat
  AND longitude BETWEEN :minLon AND :maxLon
ORDER BY distance_squared ASC
LIMIT 100;
```

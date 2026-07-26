# System Architecture Document - BharatSphere

This document describes the system architecture, component boundaries, communication protocols, and technology selections for BharatSphere.

---

## 1. System Topology

BharatSphere is structured as a client-server architecture. The frontend communicates with the backend via two protocols:
1. **REST APIs**: For non-realtime operations (authentication, feed fetching, posting marketplace items, updating locations, user searches).
2. **WebSockets (Socket.io)**: For bidirectional, low-latency communication (direct messages, group typing indicators, live messaging state).

```
                      +-----------------------------+
                      |     React + Vite Client     |
                      |   (Mobile Sandbox Shell)    |
                      +--------------+--------------+
                                     |
                       HTTP REST     |   WebSockets
                       (JSON APIs)   |   (Socket.io)
                                     v
                      +--------------+--------------+
                      |   Node.js + Express Backend  |
                      |   (Application Server)      |
                      +--------------+--------------+
                                     |
                       +-------------+-------------+
                       |                           |
                       v                           v
              +--------+--------+         +--------+--------+
              |     SQLite      |         |  Local Storage  |
              |  Database File  |         |  Media Assets   |
              +-----------------+         +-----------------+
```

---

## 2. Component Design

### 2.1 Backend Core (Express + Socket.io)
The server runs on Node.js using Express. It is designed to be fully self-contained for local running.
- **REST Endpoints**: Divided into controllers:
  - `/api/auth`: Handles login, registration (10-digit UID generation, password checks).
  - `/api/users`: Contact syncing, global search, profile management.
  - `/api/friends`: Friendship requests, acceptance, and blocking.
  - `/api/posts`: Uploading posts, home feed generation.
  - `/api/marketplace`: Listing products/services, searching listings.
  - `/api/nearby`: Hyper-local query aggregator.
- **WebSockets Server**: Leverages `socket.io` over the same HTTP port. Binds active socket connections to authenticated User UIDs.
- **SQLite Engine**: Uses `sqlite3` to store state. Provides ease of local deployment with zero external dependencies.

### 2.2 Frontend Shell (React + CSS)
Since the app features WhatsApp, Instagram, Telegram, and location tools, we require a layout that organizes these distinct applications nicely.
- **Mobile Sandbox Shell**: The app renders a clean mobile-style phone container on desktop viewports, with dynamic dimensions, glassmorphic styling, and native-feeling slide animations. On mobile devices, it scales to full screen.
- **Top Utility Bar**: Displays the system date, day, and time using a React state ticking every second.
- **Application Sandbox Grid**: A bottom-nav-driven layout switching between tabs:
  - **Chats**: Lists DM threads and group channels.
  - **Feed**: Scrollable timeline of posts (text, photos, video previews) with comments/likes.
  - **Marketplace**: Classified ads filtered by category (Job/Gig/Product/Emergency) with post button.
  - **Nearby Explorer**: Proximity radar showing active users, emergency posts, and products.
  - **Profile / Contacts**: Configuration settings, location overrides, mock phone contacts sync list.

---

## 3. Communication Protocols & Events

### 3.1 REST API Routes Reference

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---|
| `POST` | `/api/auth/register` | Create account (validates password, assigns unique 10-char ID) | No |
| `POST` | `/api/auth/login` | Login user, issues JWT token | No |
| `GET` | `/api/users/search` | Search user profiles by username | Yes |
| `POST` | `/api/users/sync-contacts` | Sync mock phone contacts list and match users | Yes |
| `GET` | `/api/friends/status` | Get relationship status between logged-in user and another user | Yes |
| `POST` | `/api/friends/request` | Send friendship request | Yes |
| `POST` | `/api/friends/accept` | Accept friendship request (mutually unlocks chat) | Yes |
| `GET` | `/api/posts` | Fetch home posts feed | Yes |
| `POST` | `/api/posts/create` | Create a post (text and optional media reference) | Yes |
| `GET` | `/api/marketplace` | Fetch classified listings (optional category filters) | Yes |
| `POST` | `/api/marketplace/create` | Create job/gig/product/emergency listing | Yes |
| `GET` | `/api/nearby` | Query items (users, marketplace, emergencies) within radius | Yes |
| `POST` | `/api/users/location` | Update current coordinates (lat, long) | Yes |

### 3.2 WebSocket Events

- **Authentication Event**:
  - `client -> server`: `register_socket` with payload `{ token: "JWT_TOKEN" }`. Links connection ID to user ID.
- **Direct Messaging**:
  - `client -> server`: `send_message` with payload `{ receiverId: "UID_10", content: "text", mediaUrl: "optional" }`.
  - `server -> client`: `receive_message` delivering incoming message to active socket.
  - `server -> client`: `message_delivered` confirmation.
- **Typing Indicators**:
  - `client -> server`: `typing_start` / `typing_stop` with `{ receiverId: "UID_10" }`.
  - `server -> client`: `user_typing` toggling UI indicator.
- **Group Messaging**:
  - `client -> server`: `join_room` / `leave_room` with `{ groupId: "ID" }`.
  - `client -> server`: `send_group_message` with `{ groupId: "ID", content: "text" }`.

---

## 4. Smart Storage Implementation

```
[Uploaded File] ---> [Calculate SHA-256 Hash] ---> [Query DB for Hash]
                                                           |
                                      +--------------------+--------------------+
                                      | Yes                                     | No
                                      v                                         v
                            [Use Existing File Path]                  [Save to Disk]
                            [Delete Uploaded Temp]                    [Insert File Path to DB]
                            [Link Record to Hash]                     [Link Record to Hash]
```

### Hash Check Algorithm (Middleware)
1. Media uploads are written to a temporary directory (`uploads/temp/`).
2. Multer middleware intercepts request and hashes the file.
3. Server executes:
   ```sql
   SELECT file_path FROM media_assets WHERE file_hash = ?;
   ```
4. If a match is found:
   - The file at `uploads/temp/filename` is immediately deleted to save space.
   - The application links the new message or post record to the existing `file_path`.
5. If no match is found:
   - The file is renamed and moved to `uploads/media/`.
   - A new row is inserted into `media_assets` with the hash and physical path.

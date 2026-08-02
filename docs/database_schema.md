# Database Schema & Storage Specifications — BharatConnect (Python Edition)

This document specifies the database storage models, dictionary indexing schemas, and data access methods for **BharatConnect (Python Edition)**.

---

## 1. Storage Architecture Overview

BharatConnect utilizes a high-performance **Python In-Memory Database Engine (`database/db.py`)** with $O(1)$ Hash Map lookups.

```
                  +--------------------------------+
                  |         DatabaseEngine         |
                  +--------------------------------+
                                  │
         ┌────────────────────────┼────────────────────────┐
         ▼                        ▼                        ▼
  self.users (dict)        self.chats (dict)      self.messages (list)
  Key: user_id             Key: chat_id           Indexed by chat_id
```

---

## 2. Model Schemas

### 2.1 Users Model (`self.users`)
```python
{
    "user_id": "u-101",          # String Primary Key
    "username": "vipin_k",       # Unique String Handle
    "display_name": "Vipin Kumar",
    "email": "vipin@bharatconnect.com",
    "phone": "+91 98765 43210",
    "country": "India 🇮🇳",
    "status_message": "Building BharatConnect 🚀",
    "bio": "Senior Architect & Core Developer",
    "presence": "ONLINE",        # ONLINE, IDLE, OFFLINE
    "last_seen": "Just now",
    "avatar_color": "#673AB7"
}
```

### 2.2 Chats Model (`self.chats`)
```python
{
    "chat_id": "c-group-1",      # String Primary Key
    "chat_type": "GROUP",        # DIRECT, GROUP
    "title": "BharatConnect Core Team 🇮🇳",
    "description": "System Announcement Channel",
    "owner_id": "u-101",
    "participants": ["u-101", "u-102", "u-103", "u-104"],
    "roles": {"u-101": "OWNER", "u-102": "ADMIN"},
    "pinned_by": ["u-101"],
    "unread_count": 0,
    "icon": "account-group"
}
```

### 2.3 Messages Model (`self.messages`)
```python
{
    "message_id": "m-1",         # String Primary Key
    "chat_id": "c-group-1",      # Foreign Key -> chats.chat_id
    "sender_id": "u-101",        # Foreign Key -> users.user_id
    "content": "Welcome to BharatConnect!",
    "is_pinned": True,
    "status": "READ",            # SENT, DELIVERED, READ
    "created_at": "10:30 AM",
    "reactions": ["🇮🇳", "🚀"]
}
```

### 2.4 Communities Model (`self.communities`)
```python
{
    "community_id": "comm-101",
    "name": "Tech Innovators India 🇮🇳",
    "slug": "tech-innovators-india",
    "description": "Official Hub for Developers across India.",
    "category": "TECHNOLOGY",
    "owner_id": "u-101",
    "members_count": 1420,
    "is_joined": True,
    "icon": "laptop"
}
```

### 2.5 Marketplace Model (`self.marketplace`)
```python
{
    "item_id": "item-1",
    "title": "Mechanical Keyboard RGB",
    "price": "₹4,500",
    "seller_id": "u-102",
    "seller_name": "Rahul Sharma",
    "category": "HARDWARE",
    "description": "Wireless mechanical keyboard.",
    "location": "Bengaluru, Karnataka",
    "tags": ["Hardware", "Tech"]
}
```

### 2.6 Nearby Developers Model (`self.nearby`)
```python
{
    "user_id": "u-102",
    "display_name": "Rahul Sharma",
    "username": "rahul_dev",
    "role": "Fullstack Engineer",
    "distance_km": "1.2 km",
    "city": "Bengaluru (Indiranagar)",
    "skills": ["Python", "Kivy", "React"],
    "presence": "ONLINE"
}
```

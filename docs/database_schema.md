# 🗄️ Database Schema & Phone Contact Sync Engine

## Data Models

The `DatabaseEngine` class (`database/db.py`) uses O(1) Hash Map structures for sub-50ms performance.

### 1. User Account Object
```python
{
    "user_id": "u-101",
    "username": "vipin_k",
    "display_name": "Vipin Kumar",
    "email": "vipin@bharatconnect.com",
    "phone": "+91 98765 43210",
    "country": "India 🇮🇳",
    "dob": "1998-05-15",
    "status_message": "Building BharatConnect 🚀",
    "bio": "Senior Architect & Core Developer",
    "presence": "ONLINE",
    "last_seen": "Just now",
    "avatar_color": "#6367FF"
}
```

### 2. Message Object
```python
{
    "message_id": "m-1",
    "chat_id": "c-group-1",
    "sender_id": "u-101",
    "content": "Welcome to BharatConnect!",
    "is_pinned": True,
    "status": "READ",
    "created_at": "10:30 AM",
    "reactions": ["🇮🇳", "🚀"]
}
```

---

## 📇 Phone Contact Number Matching Engine

The phone contact matching engine sanitizes and compares phone numbers from the device address book against registered user phone numbers in `db_engine.users`.

```python
def match_device_contacts(self):
    matched = []
    registered_phones = {u["phone"].replace(" ", ""): u for u in self.users.values()}
    for c in self.device_address_book:
        clean_p = c["phone"].replace(" ", "")
        reg_user = registered_phones.get(clean_p)
        if reg_user:
            matched.append({
                "name": c["name"],
                "phone": c["phone"],
                "is_registered": True,
                "matched_user_id": reg_user["user_id"],
                "username": reg_user["username"],
                "presence": reg_user["presence"]
            })
        else:
            matched.append({
                "name": c["name"],
                "phone": c["phone"],
                "is_registered": False,
                "matched_user_id": None,
                "username": None,
                "presence": "OFFLINE"
            })
    return matched
```

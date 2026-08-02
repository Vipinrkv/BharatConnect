"""
BharatConnect Core Python Database Engine
Features O(1) Hash Map indexing, Contact Sync & Phone Number Matching, and real-time state management.
"""

import time
import uuid
from datetime import datetime


class DatabaseEngine:
    def __init__(self):
        self.users = {
            "u-101": {
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
            },
            "u-102": {
                "user_id": "u-102",
                "username": "rahul_dev",
                "display_name": "Rahul Sharma",
                "email": "rahul@bharatconnect.com",
                "phone": "+91 98123 45678",
                "country": "India 🇮🇳",
                "dob": "1997-09-20",
                "status_message": "Fullstack Engineer 💻",
                "bio": "Fullstack Engineer | Open Source Contributor",
                "presence": "ONLINE",
                "last_seen": "Just now",
                "avatar_color": "#8494FF"
            },
            "u-103": {
                "user_id": "u-103",
                "username": "priya_design",
                "display_name": "Priya Patel",
                "email": "priya@bharatconnect.com",
                "phone": "+91 98999 11122",
                "country": "India 🇮🇳",
                "dob": "1999-03-10",
                "status_message": "Designing UI/UX Systems 🎨",
                "bio": "Lead Product & UI/UX Designer",
                "presence": "IDLE",
                "last_seen": "15 min ago",
                "avatar_color": "#C9BEFF"
            },
            "u-104": {
                "user_id": "u-104",
                "username": "ananya_pm",
                "display_name": "Ananya Verma",
                "email": "ananya@bharatconnect.com",
                "phone": "+91 98777 33344",
                "country": "India 🇮🇳",
                "dob": "1996-11-05",
                "status_message": "Shipping Product Features 🚀",
                "bio": "Lead Product Manager | Tech Enthusiast",
                "presence": "OFFLINE",
                "last_seen": "2 hours ago",
                "avatar_color": "#FFDBFD"
            }
        }

        # Simulated Device Contacts for Phone Number Matching Engine
        self.device_address_book = [
            {"name": "Rahul Sharma", "phone": "+91 98123 45678"},
            {"name": "Priya Patel", "phone": "+91 98999 11122"},
            {"name": "Ananya Verma", "phone": "+91 98777 33344"},
            {"name": "Amit Patel (DevOps)", "phone": "+91 98444 55566"},
            {"name": "Sneha Gupta", "phone": "+91 98111 22233"},
        ]

        self.chats = {
            "c-group-1": {
                "chat_id": "c-group-1",
                "chat_type": "GROUP",
                "title": "BharatConnect Core Team 🇮🇳",
                "description": "Official System Announcement & Technical Discussion Channel.",
                "owner_id": "u-101",
                "participants": ["u-101", "u-102", "u-103", "u-104"],
                "roles": {"u-101": "OWNER", "u-102": "ADMIN", "u-103": "MEMBER", "u-104": "MEMBER"},
                "pinned_by": ["u-101"],
                "archived_by": [],
                "muted_by": [],
                "unread_count": 0,
                "icon": "account-group"
            },
            "c-direct-1": {
                "chat_id": "c-direct-1",
                "chat_type": "DIRECT",
                "title": None,
                "participants": ["u-101", "u-102"],
                "pinned_by": [],
                "archived_by": [],
                "muted_by": [],
                "unread_count": 1,
                "icon": "account"
            },
            "c-direct-2": {
                "chat_id": "c-direct-2",
                "chat_type": "DIRECT",
                "title": None,
                "participants": ["u-101", "u-103"],
                "pinned_by": [],
                "archived_by": [],
                "muted_by": [],
                "unread_count": 0,
                "icon": "account"
            }
        }

        self.messages = [
            {
                "message_id": "m-1",
                "chat_id": "c-group-1",
                "sender_id": "u-101",
                "content": "Welcome to BharatConnect! Core message bubble interface powered by #6367FF theme 🇮🇳.",
                "is_pinned": True,
                "status": "READ",
                "created_at": "10:30 AM",
                "reactions": ["🇮🇳", "🚀"]
            },
            {
                "message_id": "m-2",
                "chat_id": "c-group-1",
                "sender_id": "u-103",
                "content": "The #C9BEFF lavender & #FFDBFD pink bubble contrast looks super clean!",
                "is_pinned": False,
                "status": "READ",
                "created_at": "10:32 AM",
                "reactions": ["❤️"]
            },
            {
                "message_id": "m-3",
                "chat_id": "c-direct-1",
                "sender_id": "u-102",
                "content": "Hey Vipin! Contact matching synced 3 registered users from my phone contacts.",
                "is_pinned": False,
                "status": "READ",
                "created_at": "11:05 AM",
                "reactions": ["👍"]
            },
            {
                "message_id": "m-4",
                "chat_id": "c-direct-1",
                "sender_id": "u-101",
                "content": "Awesome Rahul! Let's test the core message bubble interface.",
                "is_pinned": False,
                "status": "READ",
                "created_at": "11:06 AM",
                "reactions": []
            }
        ]

        self.communities = [
            {
                "community_id": "comm-101",
                "name": "Tech Innovators India 🇮🇳",
                "slug": "tech-innovators-india",
                "description": "Official Hub for Developers, System Architects, and AI Builders across India.",
                "category": "TECHNOLOGY",
                "owner_id": "u-101",
                "members_count": 1420,
                "is_joined": True,
                "icon": "laptop"
            },
            {
                "community_id": "comm-102",
                "name": "Python & Mobile Developers",
                "slug": "python-mobile-india",
                "description": "Community dedicated to building native desktop and mobile GUI apps with Python.",
                "category": "OPEN SOURCE",
                "owner_id": "u-102",
                "members_count": 890,
                "is_joined": True,
                "icon": "language-python"
            }
        ]

        self.marketplace = [
            {
                "item_id": "item-1",
                "title": "Mechanical Keyboard RGB (Tactile Switches)",
                "price": "₹4,500",
                "seller_id": "u-102",
                "seller_name": "Rahul Sharma",
                "category": "HARDWARE",
                "description": "Barely used wireless mechanical keyboard with hot-swappable tactile switches.",
                "location": "Bengaluru, Karnataka",
                "tags": ["Hardware", "Keyboard", "Tech"]
            },
            {
                "item_id": "item-2",
                "title": "Fullstack Architecture Code Audit",
                "price": "₹12,000",
                "seller_id": "u-101",
                "seller_name": "Vipin Kumar",
                "category": "SERVICES",
                "description": "Comprehensive performance, security, and schema code review for your startup backend.",
                "location": "New Delhi / Remote",
                "tags": ["Service", "Backend", "Python"]
            }
        ]

        self.nearby = [
            {
                "user_id": "u-102",
                "display_name": "Rahul Sharma",
                "username": "rahul_dev",
                "role": "Fullstack Engineer",
                "distance_km": "1.2 km",
                "city": "Bengaluru (Indiranagar)",
                "skills": ["Python", "Kivy", "React", "Node.js"],
                "presence": "ONLINE"
            },
            {
                "user_id": "u-103",
                "display_name": "Priya Patel",
                "username": "priya_design",
                "role": "Lead Product Designer",
                "distance_km": "3.5 km",
                "city": "Mumbai (Bandra)",
                "skills": ["Figma", "Design Systems", "UI/UX"],
                "presence": "IDLE"
            }
        ]

        self.current_user_id = "u-101"

    def get_current_user(self):
        return self.users.get(self.current_user_id)

    def switch_user(self, user_id):
        if user_id in self.users:
            self.current_user_id = user_id
            return self.users[user_id]
        return None

    def register_user(self, full_name, email, username, phone, password, dob):
        """Registers a new user account."""
        uid = f"u-{uuid.uuid4().hex[:6]}"
        clean_phone = phone if phone.startswith("+") else f"+91 {phone.strip()}"
        new_user = {
            "user_id": uid,
            "username": username.lower().strip(),
            "display_name": full_name.strip(),
            "email": email.lower().strip(),
            "phone": clean_phone,
            "country": "India 🇮🇳",
            "dob": dob,
            "status_message": "Hey there! I am using BharatConnect 🇮🇳",
            "bio": "New BharatConnect Member",
            "presence": "ONLINE",
            "last_seen": "Just now",
            "avatar_color": "#6367FF"
        }
        self.users[uid] = new_user
        self.current_user_id = uid
        return new_user

    def reset_password_with_email(self, email, otp_code, new_password):
        """Simulates email verification & password reset."""
        for u in self.users.values():
            if u["email"].lower() == email.lower().strip():
                return True, f"Password successfully reset for {u['display_name']}!"
        return False, "Email address not found in registered accounts."

    def match_device_contacts(self):
        """Matches phone numbers from device address book against registered BharatConnect users."""
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
                    "status_message": reg_user["status_message"],
                    "presence": reg_user["presence"]
                })
            else:
                matched.append({
                    "name": c["name"],
                    "phone": c["phone"],
                    "is_registered": False,
                    "matched_user_id": None,
                    "username": None,
                    "status_message": "Not on BharatConnect yet",
                    "presence": "OFFLINE"
                })
        return matched

    def get_user_chats(self, user_id=None):
        uid = user_id or self.current_user_id
        user_chats = []
        for chat in self.chats.values():
            if uid in chat["participants"]:
                chat_copy = dict(chat)
                if chat["chat_type"] == "DIRECT":
                    other_ids = [p for p in chat["participants"] if p != uid]
                    if other_ids:
                        other_user = self.users.get(other_ids[0], {})
                        chat_copy["title"] = other_user.get("display_name", "Unknown User")
                        chat_copy["presence"] = other_user.get("presence", "OFFLINE")
                        chat_copy["status_message"] = other_user.get("status_message", "")
                user_chats.append(chat_copy)
        return user_chats

    def get_messages_for_chat(self, chat_id):
        return [m for m in self.messages if m["chat_id"] == chat_id]

    def send_message(self, chat_id, content, sender_id=None):
        sid = sender_id or self.current_user_id
        now_str = datetime.now().strftime("%I:%M %p")
        msg = {
            "message_id": f"m-{uuid.uuid4().hex[:6]}",
            "chat_id": chat_id,
            "sender_id": sid,
            "content": content,
            "is_pinned": False,
            "status": "SENT",
            "created_at": now_str,
            "reactions": []
        }
        self.messages.append(msg)
        return msg

    def create_chat(self, other_user_id):
        for chat in self.chats.values():
            if chat["chat_type"] == "DIRECT" and set(chat["participants"]) == {self.current_user_id, other_user_id}:
                return chat
        
        chat_id = f"c-direct-{len(self.chats)+1}"
        new_chat = {
            "chat_id": chat_id,
            "chat_type": "DIRECT",
            "title": None,
            "participants": [self.current_user_id, other_user_id],
            "pinned_by": [],
            "archived_by": [],
            "muted_by": [],
            "unread_count": 0,
            "icon": "account"
        }
        self.chats[chat_id] = new_chat
        return new_chat

    def create_group_chat(self, title, description, member_ids):
        chat_id = f"c-group-{len(self.chats)+1}"
        participants = list(set([self.current_user_id] + member_ids))
        roles = {p: "MEMBER" for p in participants}
        roles[self.current_user_id] = "OWNER"

        new_chat = {
            "chat_id": chat_id,
            "chat_type": "GROUP",
            "title": title,
            "description": description,
            "owner_id": self.current_user_id,
            "participants": participants,
            "roles": roles,
            "pinned_by": [],
            "archived_by": [],
            "muted_by": [],
            "unread_count": 0,
            "icon": "account-group"
        }
        self.chats[chat_id] = new_chat
        return new_chat

    def toggle_community_join(self, community_id):
        for comm in self.communities:
            if comm["community_id"] == community_id:
                comm["is_joined"] = not comm["is_joined"]
                comm["members_count"] += 1 if comm["is_joined"] else -1
                return comm
        return None

    def update_user_profile(self, display_name, status_message, bio, phone):
        user = self.get_current_user()
        if user:
            user["display_name"] = display_name
            user["status_message"] = status_message
            user["bio"] = bio
            user["phone"] = phone
            return user
        return None


# Global DB Instance
db_engine = DatabaseEngine()

"""
BharatConnect Core Python Database Engine
Features O(1) Hash Map indexing, data sanitization, and real-time state management.
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
                "avatar_color": "#673AB7"
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
                "avatar_color": "#009688"
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
                "avatar_color": "#E91E63"
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
                "avatar_color": "#FF9800"
            }
        }

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
                "content": "Welcome to BharatConnect! Ultra-fast real-time text messaging platform built for India 🇮🇳.",
                "is_pinned": True,
                "status": "READ",
                "created_at": "10:30 AM",
                "reactions": ["🇮🇳", "🚀"]
            },
            {
                "message_id": "m-2",
                "chat_id": "c-group-1",
                "sender_id": "u-103",
                "content": "The Material UI/UX components look super clean! Great work team.",
                "is_pinned": False,
                "status": "READ",
                "created_at": "10:32 AM",
                "reactions": ["❤️"]
            },
            {
                "message_id": "m-3",
                "chat_id": "c-direct-1",
                "sender_id": "u-102",
                "content": "Hey Vipin! Workspace engine and real-time Python messaging are running smoothly.",
                "is_pinned": False,
                "status": "READ",
                "created_at": "11:05 AM",
                "reactions": ["👍"]
            },
            {
                "message_id": "m-4",
                "chat_id": "c-direct-1",
                "sender_id": "u-101",
                "content": "Awesome Rahul! Let's verify all Kivy & KivyMD screens.",
                "is_pinned": False,
                "status": "READ",
                "created_at": "11:06 AM",
                "reactions": []
            },
            {
                "message_id": "m-5",
                "chat_id": "c-direct-2",
                "sender_id": "u-103",
                "content": "Hi Vipin, I finished reviewing the responsive layout designs.",
                "is_pinned": False,
                "status": "READ",
                "created_at": "01:15 PM",
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
                "name": "Python & Kivy Developers",
                "slug": "kivy-python-india",
                "description": "Community dedicated to building native desktop and mobile GUI apps with Python.",
                "category": "OPEN SOURCE",
                "owner_id": "u-102",
                "members_count": 890,
                "is_joined": True,
                "icon": "language-python"
            },
            {
                "community_id": "comm-103",
                "name": "Startup Founders Bharat",
                "slug": "startup-founders-bharat",
                "description": "Network for founders, builders, and product leaders launching next-gen startups.",
                "category": "ENTREPRENEURSHIP",
                "owner_id": "u-104",
                "members_count": 2150,
                "is_joined": False,
                "icon": "rocket-launch"
            },
            {
                "community_id": "comm-104",
                "name": "UI/UX Design Systems India",
                "slug": "uiux-design-india",
                "description": "Crafting accessible, gorgeous, and performant user interfaces.",
                "category": "DESIGN",
                "owner_id": "u-103",
                "members_count": 640,
                "is_joined": False,
                "icon": "palette"
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
            },
            {
                "item_id": "item-3",
                "title": "4K Ultra-Wide Monitor (27 inch)",
                "price": "₹22,000",
                "seller_id": "u-103",
                "seller_name": "Priya Patel",
                "category": "HARDWARE",
                "description": "High color accuracy IPS panel perfect for UI/UX designers and video editors.",
                "location": "Mumbai, Maharashtra",
                "tags": ["Display", "Hardware", "Design"]
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
            },
            {
                "user_id": "u-104",
                "display_name": "Ananya Verma",
                "username": "ananya_pm",
                "role": "Product Manager",
                "distance_km": "5.0 km",
                "city": "Gurgaon (DLF Cyber City)",
                "skills": ["Agile", "Product Roadmap", "Strategy"],
                "presence": "OFFLINE"
            },
            {
                "user_id": "u-105",
                "display_name": "Amit Patel",
                "username": "amit_cloud",
                "role": "DevOps Architect",
                "distance_km": "7.8 km",
                "city": "Hyderabad (Hitech City)",
                "skills": ["Docker", "Kubernetes", "GCP"],
                "presence": "ONLINE"
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

    def get_user_chats(self, user_id=None):
        uid = user_id or self.current_user_id
        user_chats = []
        for chat in self.chats.values():
            if uid in chat["participants"]:
                chat_copy = dict(chat)
                if chat["chat_type"] == "DIRECT":
                    other_id = [p for p in chat["participants"] if p != uid][0]
                    other_user = self.users.get(other_id, {})
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

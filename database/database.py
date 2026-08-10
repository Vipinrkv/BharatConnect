"""
BharatConnect SQLite data layer.

The app is designed to work offline, so this module keeps the complete local
state in a SQLite database under data/app.db.
"""

import hashlib
import json
import os
import random
import sqlite3
import uuid
from datetime import datetime


DB_FILE_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "data",
    "app.db",
)


class SQLiteDatabaseEngine:
    def __init__(self, db_path=DB_FILE_PATH):
        self.db_path = db_path
        os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
        self.init_database()

    def get_connection(self):
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    def _hash_password(self, password):
        return hashlib.sha256(f"bharatconnect:{password}".encode("utf-8")).hexdigest()

    def _column_exists(self, cursor, table_name, column_name):
        cursor.execute(f"PRAGMA table_info({table_name})")
        return any(row["name"] == column_name for row in cursor.fetchall())

    def _ensure_column(self, cursor, table_name, column_name, definition):
        if not self._column_exists(cursor, table_name, column_name):
            cursor.execute(f"ALTER TABLE {table_name} ADD COLUMN {column_name} {definition}")

    def _initials(self, name):
        return "".join(part[0].upper() for part in name.split()[:2]) or "BC"

    def init_database(self):
        with self.get_connection() as conn:
            cursor = conn.cursor()

            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    display_name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    phone TEXT,
                    country TEXT DEFAULT 'India',
                    dob TEXT,
                    status_message TEXT,
                    bio TEXT,
                    presence TEXT DEFAULT 'ONLINE',
                    last_seen TEXT DEFAULT 'Just now',
                    avatar_initials TEXT,
                    avatar_color TEXT,
                    posts_count INTEGER DEFAULT 0,
                    followers_count TEXT DEFAULT '0',
                    following_count INTEGER DEFAULT 0,
                    password_hash TEXT,
                    reset_code TEXT
                )
                """
            )

            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS stories (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    is_user BOOLEAN DEFAULT 0,
                    avatar TEXT NOT NULL,
                    color TEXT NOT NULL,
                    has_unseen BOOLEAN DEFAULT 1
                )
                """
            )

            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS posts (
                    id TEXT PRIMARY KEY,
                    author_id TEXT NOT NULL,
                    author_name TEXT NOT NULL,
                    time_ago TEXT NOT NULL,
                    content TEXT NOT NULL,
                    image_title TEXT,
                    likes_count INTEGER DEFAULT 0,
                    comments_count INTEGER DEFAULT 0,
                    is_liked BOOLEAN DEFAULT 0,
                    user_avatar TEXT,
                    avatar_color TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (author_id) REFERENCES users (id)
                )
                """
            )

            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS comments (
                    id TEXT PRIMARY KEY,
                    post_id TEXT NOT NULL,
                    author_name TEXT NOT NULL,
                    text TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (post_id) REFERENCES posts (id)
                )
                """
            )

            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS chats (
                    id TEXT PRIMARY KEY,
                    chat_type TEXT NOT NULL,
                    title TEXT NOT NULL,
                    subtitle TEXT,
                    pinned_message TEXT,
                    unread_count INTEGER DEFAULT 0,
                    icon TEXT DEFAULT 'account',
                    avatar_initials TEXT,
                    avatar_color TEXT DEFAULT '#6367FF',
                    last_message TEXT,
                    last_message_time TEXT,
                    is_pinned BOOLEAN DEFAULT 0,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
            )

            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT PRIMARY KEY,
                    chat_id TEXT NOT NULL,
                    sender_id TEXT NOT NULL,
                    sender_name TEXT NOT NULL,
                    text TEXT NOT NULL,
                    time TEXT NOT NULL,
                    is_me BOOLEAN DEFAULT 0,
                    avatar_color TEXT,
                    reactions_json TEXT,
                    link_url TEXT,
                    link_title TEXT,
                    link_desc TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (chat_id) REFERENCES chats (id)
                )
                """
            )

            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS marketplace (
                    id TEXT PRIMARY KEY,
                    category TEXT NOT NULL,
                    title TEXT NOT NULL,
                    price_payout TEXT NOT NULL,
                    type_tag TEXT,
                    icon TEXT,
                    color1 TEXT,
                    color2 TEXT
                )
                """
            )

            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """
            )

            self.migrate_database(cursor)
            conn.commit()

        self.seed_initial_data()
        self.ensure_runtime_defaults()

    def migrate_database(self, cursor):
        self._ensure_column(cursor, "users", "password_hash", "TEXT")
        self._ensure_column(cursor, "users", "reset_code", "TEXT")
        self._ensure_column(cursor, "chats", "avatar_initials", "TEXT")
        self._ensure_column(cursor, "chats", "avatar_color", "TEXT DEFAULT '#6367FF'")
        self._ensure_column(cursor, "chats", "last_message", "TEXT")
        self._ensure_column(cursor, "chats", "last_message_time", "TEXT")
        self._ensure_column(cursor, "chats", "is_pinned", "BOOLEAN DEFAULT 0")
        self._ensure_column(cursor, "chats", "updated_at", "TIMESTAMP")
        self._ensure_column(cursor, "chats", "target_user_id", "TEXT")

        # Performance Indexes for Scalability
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_messages_chat_created ON messages (chat_id, created_at);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);")


    def seed_initial_data(self):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM users")
            if cursor.fetchone()[0] > 0:
                return

            demo_hash = self._hash_password("password123")
            users_data = [
                (
                    "u-alex",
                    "alexmorgan",
                    "Alex Morgan",
                    "alex.morgan@bharatconnect.com",
                    "+91 98765 43210",
                    "India",
                    "1998-05-15",
                    "Passionate about technology, coffee, and making a difference.",
                    "Passionate about technology, coffee, and making a difference.",
                    "ONLINE",
                    "Just now",
                    "AM",
                    "#6367FF",
                    128,
                    "1.2K",
                    320,
                    demo_hash,
                ),
                (
                    "u-emma",
                    "emma_watson",
                    "Emma Watson",
                    "emma@bharatconnect.com",
                    "+91 98123 45678",
                    "India",
                    "1999-04-15",
                    "Exploring design and code.",
                    "Exploring design and code.",
                    "ONLINE",
                    "Just now",
                    "EW",
                    "#8494FF",
                    45,
                    "850",
                    190,
                    demo_hash,
                ),
                (
                    "u-alice",
                    "alice_j",
                    "Alice Johnson",
                    "alice@bharatconnect.com",
                    "+91 98999 11122",
                    "India",
                    "1997-08-20",
                    "Nature lover and photographer.",
                    "Nature lover and photographer.",
                    "ONLINE",
                    "5 min ago",
                    "AJ",
                    "#C9BEFF",
                    82,
                    "2.1K",
                    410,
                    demo_hash,
                ),
                (
                    "u-john",
                    "john_doe",
                    "John Doe",
                    "john@bharatconnect.com",
                    "+91 98777 33344",
                    "India",
                    "1996-03-10",
                    "Fullstack engineer.",
                    "Fullstack engineer.",
                    "OFFLINE",
                    "1 hour ago",
                    "JD",
                    "#FFDBFD",
                    34,
                    "540",
                    120,
                    demo_hash,
                ),
                (
                    "u-michael",
                    "michael_b",
                    "Michael Brown",
                    "michael@bharatconnect.com",
                    "+91 98444 55566",
                    "India",
                    "1995-11-05",
                    "Coffee plus code equals life.",
                    "Coffee plus code equals life.",
                    "ONLINE",
                    "Just now",
                    "MB",
                    "#2F2FE4",
                    95,
                    "1.5K",
                    300,
                    demo_hash,
                ),
            ]
            cursor.executemany(
                """
                INSERT INTO users (
                    id, username, display_name, email, phone, country, dob,
                    status_message, bio, presence, last_seen, avatar_initials,
                    avatar_color, posts_count, followers_count, following_count,
                    password_hash
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                users_data,
            )

            stories_data = [
                ("story-me", "Your Story", 1, "AM", "#6367FF", 0),
                ("story-1", "Alice", 0, "AJ", "#8494FF", 1),
                ("story-2", "John", 0, "JD", "#C9BEFF", 1),
                ("story-3", "Emma", 0, "EW", "#FFDBFD", 0),
                ("story-4", "More", 0, "+", "#162E93", 0),
            ]
            cursor.executemany("INSERT INTO stories VALUES (?, ?, ?, ?, ?, ?)", stories_data)

            posts_data = [
                (
                    "post-1",
                    "u-alice",
                    "Alice Johnson",
                    "2 hours ago",
                    "Witnessed a beautiful sunset today. Nature never fails to amaze.",
                    "Sunset View Over Horizon",
                    124,
                    12,
                    0,
                    "AJ",
                    "#8494FF",
                ),
                (
                    "post-2",
                    "u-michael",
                    "Michael Brown",
                    "4 hours ago",
                    "Coffee plus code made a perfect morning. Building new features for BharatConnect.",
                    "Developer Setup",
                    89,
                    8,
                    1,
                    "MB",
                    "#2F2FE4",
                ),
            ]
            cursor.executemany(
                """
                INSERT INTO posts (
                    id, author_id, author_name, time_ago, content, image_title,
                    likes_count, comments_count, is_liked, user_avatar, avatar_color
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                posts_data,
            )

            comments_data = [
                ("c-1", "post-1", "Emma Watson", "Stunning view!"),
                ("c-2", "post-1", "John Doe", "Where was this taken?"),
                ("c-3", "post-2", "Alex Morgan", "Keep shipping."),
            ]
            cursor.executemany(
                "INSERT INTO comments (id, post_id, author_name, text) VALUES (?, ?, ?, ?)",
                comments_data,
            )

            chats_data = [
                (
                    "c-individual",
                    "INDIVIDUAL",
                    "Emma Watson",
                    "Online",
                    "",
                    0,
                    "account",
                    "EW",
                    "#8494FF",
                    1,
                ),
                (
                    "c-group",
                    "GROUP",
                    "Project Team",
                    "8 members, 3 online",
                    "",
                    2,
                    "account-group",
                    "PT",
                    "#6367FF",
                    0,
                ),
                (
                    "c-community",
                    "COMMUNITY",
                    "Tech Community",
                    "1.2K members, 120 online",
                    "Welcome to Tech Community. Share useful knowledge and resources.",
                    5,
                    "earth",
                    "TC",
                    "#2F2FE4",
                    0,
                ),
            ]
            cursor.executemany(
                """
                INSERT INTO chats (
                    id, chat_type, title, subtitle, pinned_message, unread_count,
                    icon, avatar_initials, avatar_color, is_pinned
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                chats_data,
            )

            reactions = json.dumps([["Like", 9], ["Love", 25]])
            messages_data = [
                ("m1", "c-individual", "u-emma", "Emma Watson", "Hey! How are you?", "10:30 AM", 0, "#8494FF", None, None, None, None),
                ("m2", "c-individual", "u-alex", "Alex Morgan", "I'm good, thanks. What about you?", "10:30 AM", 1, "#6367FF", None, None, None, None),
                ("m3", "c-individual", "u-emma", "Emma Watson", "I'm great. Working on something exciting.", "10:32 AM", 0, "#8494FF", None, None, None, None),
                ("m4", "c-individual", "u-alex", "Alex Morgan", "That's awesome. Can't wait to see it.", "10:33 AM", 1, "#6367FF", None, None, None, None),
                ("m5", "c-individual", "u-emma", "Emma Watson", "Sure, I'll show you soon!", "10:34 AM", 0, "#8494FF", None, None, None, None),
                ("mg1", "c-group", "u-john", "John Doe", "Hey team! How's the project going?", "10:30 AM", 0, "#C9BEFF", None, None, None, None),
                ("mg2", "c-group", "u-sarah", "Sarah Lee", "We're on track. Just finishing the design.", "10:31 AM", 0, "#8494FF", None, None, None, None),
                ("mg3", "c-group", "u-mike", "Mike Ross", "Great. Let's sync tomorrow.", "10:32 AM", 0, "#6367FF", None, None, None, None),
                ("mg4", "c-group", "u-lisa", "Lisa Ray", "Looks good to me.", "10:32 AM", 0, "#FFDBFD", None, None, None, None),
                ("mc1", "c-community", "u-admin", "Admin", "Welcome everyone. Feel free to share your knowledge and resources.", "10:30 AM", 0, "#FFDBFD", reactions, None, None, None),
                ("mc2", "c-community", "u-devmaster", "DevMaster", "Check out this new AI tool I found. It's amazing.", "10:32 AM", 0, "#8494FF", None, "https://aitool.com", "AI Tool", "Best AI tools for developers."),
            ]
            cursor.executemany(
                """
                INSERT INTO messages (
                    id, chat_id, sender_id, sender_name, text, time, is_me,
                    avatar_color, reactions_json, link_url, link_title, link_desc
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                messages_data,
            )

            marketplace_data = [
                ("item-1", "popular_items", "iPhone 14 Pro", "$899", "Featured", "cellphone", "#6367FF", "#2F2FE4"),
                ("item-2", "popular_items", "Gaming Laptop", "$1,299", "High End", "laptop", "#162E93", "#1A1953"),
                ("job-1", "jobs", "UI/UX Designer", "Remote", "Full Time", "palette", "#8494FF", "#2F2FE4"),
                ("job-2", "jobs", "Digital Marketer", "Full-time", "Urgent", "bullhorn", "#1A1953", "#6367FF"),
                ("qj-1", "quick_jobs", "Need a logo designer", "$50 - $150", "1-2 days", "brush", "#162E93", "#8494FF"),
                ("qj-2", "quick_jobs", "Fix my website issue", "$30 - $80", "Today", "code-tags", "#2F2FE4", "#C9BEFF"),
            ]
            cursor.executemany("INSERT INTO marketplace VALUES (?, ?, ?, ?, ?, ?, ?, ?)", marketplace_data)

            settings_data = [
                ("theme_dark_mode", "1"),
                ("language", "English"),
                ("current_user_id", "u-alex"),
            ]
            cursor.executemany("INSERT INTO settings VALUES (?, ?)", settings_data)

            self._refresh_chat_summaries(cursor)
            conn.commit()

    def ensure_runtime_defaults(self):
        demo_hash = self._hash_password("password123")
        user_updates = {
            "u-alex": ("India", "Passionate about technology, coffee, and making a difference.", "Passionate about technology, coffee, and making a difference."),
            "u-emma": ("India", "Exploring design and code.", "Exploring design and code."),
            "u-alice": ("India", "Nature lover and photographer.", "Nature lover and photographer."),
            "u-john": ("India", "Fullstack engineer.", "Fullstack engineer."),
            "u-michael": ("India", "Coffee plus code equals life.", "Coffee plus code equals life."),
        }
        chat_defaults = {
            "c-individual": ("EW", "#8494FF", 1),
            "c-group": ("PT", "#6367FF", 0),
            "c-community": ("TC", "#2F2FE4", 0),
        }
        post_updates = {
            "post-1": ("Witnessed a beautiful sunset today. Nature never fails to amaze.", "Sunset View Over Horizon"),
            "post-2": ("Coffee plus code made a perfect morning. Building new features for BharatConnect.", "Developer Setup"),
        }
        message_updates = {
            "m2": "I'm good, thanks. What about you?",
            "m3": "I'm great. Working on something exciting.",
            "m4": "That's awesome. Can't wait to see it.",
            "mg2": "We're on track. Just finishing the design.",
            "mg3": "Great. Let's sync tomorrow.",
            "mg4": "Looks good to me.",
            "mc1": "Welcome everyone. Feel free to share your knowledge and resources.",
            "mc2": "Check out this new AI tool I found. It's amazing.",
        }

        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "UPDATE users SET password_hash=? WHERE password_hash IS NULL OR password_hash=''",
                (demo_hash,),
            )
            for user_id, (country, status, bio) in user_updates.items():
                cursor.execute(
                    "UPDATE users SET country=?, status_message=?, bio=? WHERE id=?",
                    (country, status, bio, user_id),
                )
            for post_id, (content, title) in post_updates.items():
                cursor.execute("UPDATE posts SET content=?, image_title=? WHERE id=?", (content, title, post_id))
            for msg_id, text in message_updates.items():
                cursor.execute("UPDATE messages SET text=? WHERE id=?", (text, msg_id))
            cursor.execute(
                """
                UPDATE chats
                SET pinned_message='Welcome to Tech Community. Share useful knowledge and resources.'
                WHERE id='c-community'
                """
            )
            cursor.execute("SELECT id, title, avatar_initials FROM chats")
            for chat in cursor.fetchall():
                initials, color, pinned = chat_defaults.get(
                    chat["id"],
                    (self._initials(chat["title"]), "#6367FF", 0),
                )
                if not chat["avatar_initials"]:
                    cursor.execute(
                        "UPDATE chats SET avatar_initials=?, avatar_color=?, is_pinned=? WHERE id=?",
                        (initials, color, pinned, chat["id"]),
                    )
            self._refresh_chat_summaries(cursor)
            conn.commit()

    def _refresh_chat_summaries(self, cursor):
        cursor.execute("SELECT id FROM chats")
        for chat in cursor.fetchall():
            cursor.execute(
                """
                SELECT text, time, created_at
                FROM messages
                WHERE chat_id=?
                ORDER BY datetime(created_at) DESC, id DESC
                LIMIT 1
                """,
                (chat["id"],),
            )
            msg = cursor.fetchone()
            if msg:
                cursor.execute(
                    """
                    UPDATE chats
                    SET last_message=?, last_message_time=?, updated_at=?
                    WHERE id=?
                    """,
                    (msg["text"], msg["time"], msg["created_at"], chat["id"]),
                )

    def get_current_user(self):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT value FROM settings WHERE key='current_user_id'")
            row = cursor.fetchone()
            current_id = row["value"] if row else "u-alex"
            cursor.execute("SELECT * FROM users WHERE id=?", (current_id,))
            user = cursor.fetchone()
            if user:
                return dict(user)
            return {
                "id": "u-alex",
                "username": "alexmorgan",
                "display_name": "Alex Morgan",
                "email": "alex.morgan@bharatconnect.com",
                "phone": "+91 98765 43210",
                "bio": "Passionate about technology, coffee, and making a difference.",
                "avatar_initials": "AM",
                "avatar_color": "#6367FF",
                "posts_count": 128,
                "followers_count": "1.2K",
                "following_count": 320,
            }

    def update_user_profile(self, user_id, display_name=None, username=None, bio=None, phone=None, status_message=None, country=None, avatar_initials=None, avatar_color=None):
        fields = []
        values = []
        updates = {
            "display_name": display_name,
            "username": username,
            "bio": bio,
            "phone": phone,
            "status_message": status_message,
            "country": country,
            "avatar_initials": avatar_initials,
            "avatar_color": avatar_color,
        }
        for key, val in updates.items():
            if val is not None:
                fields.append(f"{key}=?")
                values.append(val)
        if not fields:
            return True, "No changes provided."

        values.append(user_id)
        sql = f"UPDATE users SET {', '.join(fields)} WHERE id=?"

        with self.get_connection() as conn:
            cursor = conn.cursor()
            try:
                cursor.execute(sql, values)
                conn.commit()
                return True, "Profile updated successfully."
            except sqlite3.IntegrityError:
                return False, "Username is already taken by another account."


    def authenticate_user(self, login_identifier, password):
        clean_id = login_identifier.strip().lower()
        phone_id = clean_id.replace(" ", "")
        clean_password = (password or "").strip()

        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                SELECT * FROM users
                WHERE LOWER(username)=?
                   OR LOWER(email)=?
                   OR REPLACE(LOWER(phone), ' ', '')=?
                """,
                (clean_id, clean_id, phone_id),
            )
            user = cursor.fetchone()
            if not user:
                return False, "No account found for that email, phone, or username."
            if not clean_password:
                return False, "Enter your password to continue."

            user_dict = dict(user)
            if user_dict.get("password_hash") != self._hash_password(clean_password):
                return False, "Incorrect password. Demo accounts use password123."

            cursor.execute("INSERT OR REPLACE INTO settings (key, value) VALUES ('current_user_id', ?)", (user_dict["id"],))
            conn.commit()
            return True, user_dict

    def switch_user(self, user_id):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM users WHERE id=?", (user_id,))
            row = cursor.fetchone()
            if not row:
                return False, "User not found."
            cursor.execute("INSERT OR REPLACE INTO settings (key, value) VALUES ('current_user_id', ?)", (user_id,))
            conn.commit()
            return True, dict(row)

    def get_users(self):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM users ORDER BY display_name ASC")
            return [dict(row) for row in cursor.fetchall()]

    def register_user(self, full_name, email, phone, username, dob, password):
        clean_name = full_name.strip() or "New Member"
        clean_email = email.strip().lower()
        clean_username = (username.strip().lower() or clean_email.split("@")[0]).replace(" ", "_")
        clean_phone = phone.strip() or "+91 98765 00000"
        clean_password = (password or "").strip()

        if not clean_email or "@" not in clean_email:
            raise ValueError("Please enter a valid email address.")
        if len(clean_password) < 6:
            raise ValueError("Password must be at least 6 characters.")

        user_id = f"u-{uuid.uuid4().hex[:8]}"
        initials = self._initials(clean_name)

        with self.get_connection() as conn:
            cursor = conn.cursor()
            try:
                cursor.execute(
                    """
                    INSERT INTO users (
                        id, username, display_name, email, phone, country, dob,
                        status_message, bio, presence, last_seen, avatar_initials,
                        avatar_color, posts_count, followers_count, following_count,
                        password_hash
                    )
                    VALUES (?, ?, ?, ?, ?, 'India', ?, 'Hey there! I am using BharatConnect.',
                            'New BharatConnect member.', 'ONLINE', 'Just now', ?, '#6367FF',
                            0, '0', 0, ?)
                    """,
                    (
                        user_id,
                        clean_username,
                        clean_name,
                        clean_email,
                        clean_phone,
                        dob,
                        initials,
                        self._hash_password(clean_password),
                    ),
                )
            except sqlite3.IntegrityError as exc:
                raise ValueError("That email or username is already registered.") from exc

            cursor.execute("INSERT OR REPLACE INTO settings (key, value) VALUES ('current_user_id', ?)", (user_id,))
            conn.commit()
        return self.get_current_user()

    def reset_password_with_email(self, email, otp=None, new_password=None):
        clean_email = email.strip().lower()
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM users WHERE LOWER(email)=?", (clean_email,))
            row = cursor.fetchone()
            if not row:
                return False, "No account exists with that email address."

            if otp is None and new_password is None:
                code = str(random.randint(100000, 999999))
                cursor.execute("UPDATE users SET reset_code=? WHERE id=?", (code, row["id"]))
                conn.commit()
                return True, f"Reset code generated: {code}"

            if (otp or "").strip() != (row["reset_code"] or ""):
                return False, "Reset code does not match."

            clean_password = (new_password or "").strip()
            if len(clean_password) < 6:
                return False, "New password must be at least 6 characters."

            cursor.execute(
                "UPDATE users SET password_hash=?, reset_code=NULL WHERE id=?",
                (self._hash_password(clean_password), row["id"]),
            )
            conn.commit()
            return True, "Password updated. You can log in now."

    def get_stories(self):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM stories")
            return [dict(row) for row in cursor.fetchall()]

    def get_posts(self):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM posts ORDER BY datetime(created_at) DESC, id DESC")
            posts = [dict(row) for row in cursor.fetchall()]
            for post in posts:
                cursor.execute("SELECT author_name, text FROM comments WHERE post_id=?", (post["id"],))
                post["comments"] = [dict(comment) for comment in cursor.fetchall()]
            return posts

    def toggle_like_post(self, post_id):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT is_liked, likes_count FROM posts WHERE id=?", (post_id,))
            row = cursor.fetchone()
            if row:
                new_is_liked = 0 if row["is_liked"] else 1
                new_count = max(0, row["likes_count"] + (1 if new_is_liked else -1))
                cursor.execute(
                    "UPDATE posts SET is_liked=?, likes_count=? WHERE id=?",
                    (new_is_liked, new_count, post_id),
                )
                conn.commit()

    def add_post(self, text, image_title="Community Photo"):
        user = self.get_current_user()
        post_id = f"post-{uuid.uuid4().hex[:8]}"
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO posts (
                    id, author_id, author_name, time_ago, content, image_title,
                    likes_count, comments_count, is_liked, user_avatar, avatar_color
                )
                VALUES (?, ?, ?, 'Just now', ?, ?, 1, 0, 1, ?, ?)
                """,
                (
                    post_id,
                    user["id"],
                    user["display_name"],
                    text,
                    image_title,
                    user["avatar_initials"],
                    user["avatar_color"],
                ),
            )
            cursor.execute("UPDATE users SET posts_count = posts_count + 1 WHERE id=?", (user["id"],))
            conn.commit()

    def _resolve_individual_chat_partner(self, chat, current_user):
        """
        Resolves individual chat title and avatar by looking up target partner's
        phone number in device contact list or server profile.
        """
        if chat.get("chat_type") != "INDIVIDUAL":
            return chat

        target_user_id = chat.get("target_user_id")
        if not target_user_id:
            if chat["id"] == "c-individual":
                target_user_id = "u-emma" if current_user["id"] == "u-alex" else "u-alex"
            elif chat["id"].startswith("c-"):
                parts = chat["id"].split("-")[1:]
                for p in parts:
                    if f"u-{p}" != current_user["id"]:
                        target_user_id = f"u-{p}"
                        break

        if not target_user_id:
            return chat

        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM users WHERE id=?", (target_user_id,))
            target_user = cursor.fetchone()

        if target_user:
            t_dict = dict(target_user)
            t_phone = t_dict.get("phone", "")
            t_name = t_dict.get("display_name") or t_dict.get("username")

            from utils.contact_sync import PhoneContactSyncEngine
            local_name = PhoneContactSyncEngine.get_contact_name_for_phone(t_phone, fallback=t_name)

            chat["title"] = local_name
            chat["avatar_initials"] = t_dict.get("avatar_initials") or self._initials(local_name)
            chat["avatar_color"] = t_dict.get("avatar_color") or "#6367FF"
            chat["subtitle"] = t_dict.get("presence") or "Online"
            chat["target_user_id"] = target_user_id

        return chat

    def get_chats(self):
        current_user = self.get_current_user()
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM chats ORDER BY is_pinned DESC, id ASC")
            chats = [dict(row) for row in cursor.fetchall()]
            return [self._resolve_individual_chat_partner(c, current_user) for c in chats]

    def get_or_create_individual_chat(self, target_user_id):
        current_user = self.get_current_user()
        if target_user_id == current_user["id"]:
            chat_id = "c-individual"
        else:
            p1 = current_user["id"].replace("u-", "")
            p2 = str(target_user_id).replace("u-", "")
            sorted_parts = sorted([p1, p2])
            chat_id = f"c-{sorted_parts[0]}-{sorted_parts[1]}"

        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM chats WHERE id=?", (chat_id,))
            row = cursor.fetchone()
            if not row:
                cursor.execute("SELECT * FROM users WHERE id=?", (target_user_id,))
                target_user = cursor.fetchone()
                target_name = target_user["display_name"] if target_user else "User"
                target_initials = target_user["avatar_initials"] if target_user else "BC"
                target_color = target_user["avatar_color"] if target_user else "#6367FF"

                cursor.execute(
                    """
                    INSERT INTO chats (
                        id, chat_type, title, subtitle, unread_count, icon,
                        avatar_initials, avatar_color, is_pinned, target_user_id
                    ) VALUES (?, 'INDIVIDUAL', ?, 'Online', 0, 'account', ?, ?, 0, ?)
                    """,
                    (chat_id, target_name, target_initials, target_color, target_user_id),
                )
                conn.commit()
        return chat_id

    def get_chat_summaries(self, search_text=""):
        needle = (search_text or "").strip().lower()
        chats = self.get_chats()

        if not needle:
            return chats
        return [
            chat
            for chat in chats
            if needle in chat["title"].lower()
            or needle in (chat.get("subtitle") or "").lower()
            or needle in (chat.get("last_message") or "").lower()
        ]

    def mark_chat_read(self, chat_id):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("UPDATE chats SET unread_count=0 WHERE id=?", (chat_id,))
            conn.commit()

    def match_registered_phone_contacts(self, phone_list):
        if not phone_list:
            return []
        
        target_digits = set()
        for p in phone_list:
            if p:
                digits = "".join(filter(str.isdigit, str(p)))
                if len(digits) >= 10:
                    target_digits.add(digits[-10:])

        if not target_digits:
            return []

        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM users WHERE phone IS NOT NULL AND phone != ''")
            all_users = [dict(row) for row in cursor.fetchall()]

        matched = []
        for u in all_users:
            u_digits = "".join(filter(str.isdigit, str(u.get("phone", ""))))
            if len(u_digits) >= 10 and u_digits[-10:] in target_digits:
                matched.append(u)
        return matched

    def get_group_messages(self, group_id="g-team"):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            try:
                cursor.execute("SELECT * FROM group_messages WHERE group_id=? ORDER BY id ASC", (group_id,))
                rows = cursor.fetchall()
                if rows:
                    return [dict(row) for row in rows]
            except Exception:
                pass
            return [
                {"id": "gm-1", "group_id": group_id, "sender_name": "John Doe", "message": "Hey team! How's the project going?", "timestamp": "10:30 AM"},
                {"id": "gm-2", "group_id": group_id, "sender_name": "Sarah Lee", "message": "We're on track! Just finishing the design.", "timestamp": "10:31 AM"},
                {"id": "gm-3", "group_id": group_id, "sender_name": "Mike Ross", "message": "Great! Let's sync tomorrow.", "timestamp": "10:32 AM"},
                {"id": "gm-4", "group_id": group_id, "sender_name": "Lisa Ray", "message": "Looks good to me.", "timestamp": "10:32 AM"},
            ]

    def get_chat_messages(self, chat_id, limit=None, offset=0):
        current_user = self.get_current_user()
        with self.get_connection() as conn:
            cursor = conn.cursor()
            query = "SELECT * FROM messages WHERE chat_id=? ORDER BY datetime(created_at) ASC, id ASC"
            params = [chat_id]
            if limit is not None and isinstance(limit, int) and limit > 0:
                query += " LIMIT ? OFFSET ?"
                params.extend([limit, offset])

            cursor.execute(query, tuple(params))
            messages = []
            for row in cursor.fetchall():
                msg = dict(row)
                msg["is_me"] = (msg.get("sender_id") == current_user["id"])
                
                if not msg["is_me"] and msg.get("sender_id"):
                    cursor.execute("SELECT phone, display_name FROM users WHERE id=?", (msg["sender_id"],))
                    s_user = cursor.fetchone()
                    if s_user:
                        from utils.contact_sync import PhoneContactSyncEngine
                        msg["sender_name"] = PhoneContactSyncEngine.get_contact_name_for_phone(
                            s_user["phone"], fallback=s_user["display_name"] or msg.get("sender_name")
                        )

                try:
                    msg["reactions"] = json.loads(msg["reactions_json"]) if msg.get("reactions_json") else None
                except json.JSONDecodeError:
                    msg["reactions"] = None

                msg["link_preview"] = None
                if msg.get("link_url"):
                    msg["link_preview"] = {
                        "url": msg["link_url"],
                        "title": msg["link_title"] or msg["link_url"],
                        "desc": msg["link_desc"] or "",
                    }
                messages.append(msg)
            return messages


    def send_chat_message(self, chat_id, text):
        user = self.get_current_user()
        msg_id = f"m-{uuid.uuid4().hex[:8]}"
        now = datetime.now()
        now_str = now.strftime("%I:%M %p")
        now_iso = now.strftime("%Y-%m-%d %H:%M:%S")

        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO messages (
                    id, chat_id, sender_id, sender_name, text, time, is_me,
                    avatar_color, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)
                """,
                (
                    msg_id,
                    chat_id,
                    user["id"],
                    user["display_name"],
                    text,
                    now_str,
                    user["avatar_color"],
                    now_iso,
                ),
            )
            cursor.execute(
                """
                UPDATE chats
                SET last_message=?, last_message_time=?, updated_at=?, unread_count=0
                WHERE id=?
                """,
                (text, now_str, now_iso, chat_id),
            )
            conn.commit()


    def get_marketplace_data(self):
        with self.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM marketplace")
            rows = [dict(row) for row in cursor.fetchall()]
            data = {"popular_items": [], "jobs": [], "quick_jobs": []}
            for row in rows:
                category = row["category"]
                item = {
                    "id": row["id"],
                    "title": row["title"],
                    "price": row["price_payout"],
                    "payout": row["price_payout"],
                    "type": row["price_payout"],
                    "tag": row["type_tag"],
                    "icon": row["icon"],
                    "gradient": [row["color1"], row["color2"]],
                }
                if category in data:
                    data[category].append(item)
            return data


db_engine = SQLiteDatabaseEngine()

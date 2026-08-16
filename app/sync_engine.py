"""
BharatConnect Hybrid Offline & Online Sync Engine (app/sync_engine.py)

Delivers seamless sub-50ms offline database operation with automatic
background synchronization when connected to the backend API server.
"""

import json
import threading
import time
import urllib.request
import urllib.parse
from database.database import SQLiteDatabaseEngine
from app.api_client import BharatConnectAPIClient
from utils.local_storage import local_storage


class HybridSyncEngine:
    def __init__(self, base_url=None):
        import os
        if not base_url:
            base_url = os.environ.get("API_BASE_URL") or "https://bharatconnect-api.onrender.com/api/v1"
        self.local_db = SQLiteDatabaseEngine()
        self.api_client = BharatConnectAPIClient(base_url=base_url)

        self._is_online = False
        self._sync_thread = None
        self._ws_thread = None
        self._running = True
        self._chat_listeners = {}

        # Initialize pending_sync column in local DB tables if missing
        self._ensure_sync_tables()
        self.start_background_sync()
        self.start_websocket_listener()

    def register_chat_listener(self, chat_id, callback):
        if chat_id not in self._chat_listeners:
            self._chat_listeners[chat_id] = []
        if callback not in self._chat_listeners[chat_id]:
            self._chat_listeners[chat_id].append(callback)

    def unregister_chat_listener(self, chat_id, callback):
        if chat_id in self._chat_listeners and callback in self._chat_listeners[chat_id]:
            self._chat_listeners[chat_id].remove(callback)

    def notify_chat_listeners(self, chat_id, data=None):
        listeners = list(self._chat_listeners.get(chat_id, []))
        for cb in listeners:
            try:
                cb(data)
            except Exception:
                pass

    def start_websocket_listener(self):
        def _ws_runner():
            import asyncio
            try:
                import websockets
            except ImportError:
                return

            async def _listen():
                base_ws = self.api_client.base_url.replace("http://", "ws://").replace("https://", "wss://").replace("/api/v1", "")
                ws_url = f"{base_ws}/ws/stream"
                while self._running:
                    try:
                        async with websockets.connect(ws_url, ping_interval=20, ping_timeout=10) as ws:
                            while self._running:
                                msg_raw = await ws.recv()
                                try:
                                    payload = json.loads(msg_raw)
                                    event = payload.get("event")
                                    if event == "message.new" and isinstance(payload.get("data"), dict):
                                        mdata = payload["data"]
                                        chat_id = mdata.get("chat_id")
                                        if chat_id:
                                            cur_user = self.get_current_user()
                                            cur_user_id = cur_user.get("id") if isinstance(cur_user, dict) else None
                                            sender_id = mdata.get("sender_id") or "u-remote"
                                            sender_name = mdata.get("sender_name") or "User"
                                            text = mdata.get("text") or ""
                                            time_str = mdata.get("time") or ""
                                            is_me = 1 if (cur_user_id and sender_id == cur_user_id) else (1 if mdata.get("is_me") else 0)
                                            msg_id = mdata.get("id") or f"m-{time.time()}"
                                            with self.local_db.get_connection() as conn:
                                                cursor = conn.cursor()
                                                cursor.execute(
                                                    """
                                                    INSERT OR REPLACE INTO messages (
                                                        id, chat_id, sender_id, sender_name, text, time, is_me, avatar_color
                                                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                                    """,
                                                    (msg_id, chat_id, sender_id, sender_name, text, time_str, is_me, "#8494FF")
                                                )
                                                conn.commit()
                                            self.notify_chat_listeners(chat_id, mdata)
                                except Exception:
                                    pass
                    except Exception:
                        await asyncio.sleep(3)

            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            loop.run_until_complete(_listen())

        self._ws_thread = threading.Thread(target=_ws_runner, daemon=True)
        self._ws_thread.start()


    def _ensure_sync_tables(self):
        with self.local_db.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS pending_sync (
                    id TEXT PRIMARY KEY,
                    action_type TEXT NOT NULL,
                    payload_json TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """
            )
            conn.commit()

    def check_connection(self) -> bool:
        """Non-blocking network health check."""
        self._is_online = self.api_client.check_server_online()
        return self._is_online

    @property
    def is_online(self) -> bool:
        return self._is_online

    def get_status_text(self) -> str:
        return "🌐 ONLINE • CLOUD SYNCED" if self._is_online else "📱 OFFLINE • LOCAL MODE"

    def start_background_sync(self):
        def _loop():
            while self._running:
                was_online = self._is_online
                is_now_online = self.check_connection()
                if is_now_online and not was_online:
                    self.flush_pending_sync_queue()
                time.sleep(10)

        self._sync_thread = threading.Thread(target=_loop, daemon=True)
        self._sync_thread.start()

    def flush_pending_sync_queue(self):
        """Uploads queued offline actions to backend API server when back online."""
        if not self._is_online:
            return

        with self.local_db.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM pending_sync ORDER BY created_at ASC")
            rows = [dict(row) for row in cursor.fetchall()]

            for row in rows:
                action = row["action_type"]
                payload = json.loads(row["payload_json"])
                try:
                    if action == "ADD_POST":
                        self.api_client.create_post(payload["content"], payload.get("image_title"))
                    elif action == "LIKE_POST":
                        self.api_client.toggle_like(payload["post_id"])
                    elif action == "SEND_MESSAGE":
                        self.api_client.send_message(
                            payload["chat_id"],
                            payload["text"],
                            sender_id=payload.get("sender_id"),
                            sender_name=payload.get("sender_name")
                        )

                    
                    cursor.execute("DELETE FROM pending_sync WHERE id=?", (row["id"],))
                    conn.commit()
                except Exception:
                    break

    # --- Unified Data API (Hybrid Offline & Online) ---

    def get_current_user(self):
        return self.local_db.get_current_user()

    def update_user_profile(self, user_id, **kwargs):
        return self.local_db.update_user_profile(user_id, **kwargs)


    def authenticate_user(self, identifier, password):
        if self.check_connection():
            try:
                success, res = self.api_client.login(identifier, password)
                if success and isinstance(res, dict) and "id" in res:
                    with self.local_db.get_connection() as conn:
                        cursor = conn.cursor()
                        cursor.execute("INSERT OR REPLACE INTO settings (key, value) VALUES ('current_user_id', ?)", (res["id"],))
                        conn.commit()
                    return success, res
            except Exception:
                pass
        return self.local_db.authenticate_user(identifier, password)

    def register_user(self, full_name, email, username, password, phone="", dob=""):
        if self.check_connection():
            try:
                success, res = self.api_client.register(full_name, username, email, password, phone=phone, dob=dob)
                if success:
                    user_data = res if isinstance(res, dict) else (res.get("user") if isinstance(res, dict) else res)
                    if user_data and "id" in user_data:
                        with self.local_db.get_connection() as conn:
                            cursor = conn.cursor()
                            cursor.execute("INSERT OR REPLACE INTO settings (key, value) VALUES ('current_user_id', ?)", (user_data["id"],))
                            conn.commit()
                    return user_data
            except Exception:
                pass
        return self.local_db.register_user(full_name, email, phone=phone, username=username, dob=dob, password=password)

    def reset_password(self, email):
        return self.local_db.reset_password(email)

    def get_stories(self):
        if self.check_connection():
            try:
                server_stories = self.api_client.get_stories()
                if server_stories:
                    return server_stories
            except Exception:
                pass
        return self.local_db.get_stories()

    def get_posts(self):
        # 1. Fetch from local SQLite DB & cache into local_storage
        posts = self.local_db.get_posts()
        if posts:
            local_storage.cache_posts(posts)
        else:
            posts = local_storage.get_cached_posts()

        # 2. Refresh from online server if reachable
        if self.check_connection():
            try:
                server_posts = self.api_client.get_posts()
                if server_posts:
                    local_storage.cache_posts(server_posts)
                    return server_posts
            except Exception:
                pass
        return posts

    def add_post(self, content, image="Community Photo"):
        # 1. Instant local write (Sub-5ms feedback!)
        self.local_db.add_post(content, image)
        posts = self.local_db.get_posts()
        local_storage.cache_posts(posts)

        # 2. Sync to online server or queue for later
        if self.check_connection():
            try:
                self.api_client.create_post(content, image)
            except Exception:
                self._queue_offline_action("ADD_POST", {"content": content, "image_title": image})
        else:
            self._queue_offline_action("ADD_POST", {"content": content, "image_title": image})

    def toggle_like_post(self, post_id):
        # 1. Instant local write
        self.local_db.toggle_like_post(post_id)

        # 2. Sync online or queue
        if self.check_connection():
            try:
                self.api_client.toggle_like(post_id)
            except Exception:
                self._queue_offline_action("LIKE_POST", {"post_id": post_id})
        else:
            self._queue_offline_action("LIKE_POST", {"post_id": post_id})

    def get_chats(self):
        return self.local_db.get_chats()

    def get_or_create_individual_chat(self, target_user_id):
        return self.local_db.get_or_create_individual_chat(target_user_id)

    def get_chat_messages(self, chat_id):
        if self.check_connection():
            try:
                server_msgs = self.api_client.get_chat_messages(chat_id)
                if server_msgs and isinstance(server_msgs, list):
                    current_user = self.get_current_user()
                    cur_user_id = current_user.get("id") if isinstance(current_user, dict) else None
                    with self.local_db.get_connection() as conn:
                        cursor = conn.cursor()
                        for sm in server_msgs:
                            if not isinstance(sm, dict):
                                continue
                            sm_id = sm.get("id")
                            if not sm_id:
                                continue
                            sender_id = sm.get("sender_id") or "u-remote"
                            sender_name = sm.get("sender_name") or "User"
                            text = sm.get("text") or ""
                            time_str = sm.get("time") or ""
                            is_me = 1 if (cur_user_id and sender_id == cur_user_id) else (1 if sm.get("is_me") else 0)
                            cursor.execute(
                                """
                                INSERT OR REPLACE INTO messages (
                                    id, chat_id, sender_id, sender_name, text, time, is_me, avatar_color
                                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                """,
                                (sm_id, chat_id, sender_id, sender_name, text, time_str, is_me, "#8494FF")
                            )
                        conn.commit()
            except Exception:
                pass
        return self.local_db.get_chat_messages(chat_id)

    def send_chat_message(self, chat_id, text):
        res = self.local_db.send_chat_message(chat_id, text)
        user = self.get_current_user()
        sender_id = user.get("id") if isinstance(user, dict) else "u-user"
        sender_name = user.get("display_name") if isinstance(user, dict) else "Member"
        if self.check_connection():
            try:
                self.api_client.send_message(chat_id, text, sender_id=sender_id, sender_name=sender_name)
            except Exception:
                self._queue_offline_action("SEND_MESSAGE", {"chat_id": chat_id, "text": text, "sender_id": sender_id, "sender_name": sender_name})
        else:
            self._queue_offline_action("SEND_MESSAGE", {"chat_id": chat_id, "text": text, "sender_id": sender_id, "sender_name": sender_name})
        return res


    def match_registered_phone_contacts(self, phone_list):
        return self.local_db.match_registered_phone_contacts(phone_list)


    def get_group_messages(self, group_id="g-team"):
        return self.local_db.get_group_messages(group_id)

    def get_chat_summaries(self, search_text=""):
        return self.local_db.get_chat_summaries(search_text)

    def get_marketplace_data(self):
        if self.check_connection():
            try:
                data = self.api_client.get_marketplace("ALL")
                if data and any(data.values()):
                    return data
            except Exception:
                pass
        return self.local_db.get_marketplace_data()

    def _queue_offline_action(self, action_type, payload):
        import uuid
        with self.local_db.get_connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO pending_sync (id, action_type, payload_json) VALUES (?, ?, ?)",
                (f"sync-{uuid.uuid4().hex[:8]}", action_type, json.dumps(payload)),
            )
            conn.commit()


# Global Facade Instance
sync_engine = HybridSyncEngine()

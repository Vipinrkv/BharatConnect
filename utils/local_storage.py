"""
BharatConnect Persistent Local Storage & Offline Cache System (utils/local_storage.py)

Provides instant key-value and structured JSON storage on local disk (data/local_storage.json)
for caching feed posts, chat logs, user drafts, settings, and session state.
"""

import os
import json
import threading
from typing import Any, Optional, Dict, List

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")
STORAGE_FILE = os.path.join(DATA_DIR, "local_storage.json")


class LocalStorageManager:
    def __init__(self, filepath=STORAGE_FILE):
        self.filepath = filepath
        os.makedirs(os.path.dirname(self.filepath), exist_ok=True)
        self._lock = threading.Lock()
        self._data: Dict[str, Any] = {}
        self._load()

    def _load(self):
        """Loads local storage data from disk."""
        with self._lock:
            if os.path.exists(self.filepath):
                try:
                    with open(self.filepath, "r", encoding="utf-8") as f:
                        self._data = json.load(f)
                except Exception:
                    self._data = {}
            else:
                self._data = {
                    "posts_cache": [],
                    "chats_cache": {},
                    "drafts": {},
                    "user_preferences": {},
                }
                self._save_unlocked()

    def _save_unlocked(self):
        """Saves current state to JSON file on disk."""
        try:
            temp_path = f"{self.filepath}.tmp"
            with open(temp_path, "w", encoding="utf-8") as f:
                json.dump(self._data, f, indent=2, ensure_ascii=False)
            os.replace(temp_path, self.filepath)
        except Exception as e:
            print(f"[LocalStorage] Save error: {e}")

    def save(self):
        """Saves data safely with thread lock."""
        with self._lock:
            self._save_unlocked()

    # --- Generic Key-Value API ---

    def set(self, key: str, value: Any):
        """Stores a key-value pair locally."""
        with self._lock:
            self._data[key] = value
            self._save_unlocked()

    def get(self, key: str, default: Any = None) -> Any:
        """Retrieves a key from local storage."""
        with self._lock:
            return self._data.get(key, default)

    def delete(self, key: str):
        """Deletes a key from local storage."""
        with self._lock:
            if key in self._data:
                del self._data[key]
                self._save_unlocked()

    def clear(self):
        """Clears all local storage data."""
        with self._lock:
            self._data = {}
            self._save_unlocked()

    # --- Structured Domain Caching API ---

    def cache_posts(self, posts: List[dict]):
        """Caches feed posts locally."""
        self.set("posts_cache", posts)

    def get_cached_posts(self) -> List[dict]:
        """Retrieves locally cached feed posts."""
        return self.get("posts_cache", [])

    def cache_messages(self, chat_id: str, messages: List[dict]):
        """Caches chat message stream for a specific chat thread locally."""
        with self._lock:
            chats_cache = self._data.get("chats_cache", {})
            chats_cache[chat_id] = messages
            self._data["chats_cache"] = chats_cache
            self._save_unlocked()

    def get_cached_messages(self, chat_id: str) -> List[dict]:
        """Retrieves locally cached chat messages."""
        with self._lock:
            chats_cache = self._data.get("chats_cache", {})
            return chats_cache.get(chat_id, [])

    def save_draft(self, draft_type: str, content: str):
        """Saves unsaved text post/message draft locally."""
        with self._lock:
            drafts = self._data.get("drafts", {})
            drafts[draft_type] = content
            self._data["drafts"] = drafts
            self._save_unlocked()

    def get_draft(self, draft_type: str) -> str:
        """Retrieves unsaved text draft locally."""
        with self._lock:
            drafts = self._data.get("drafts", {})
            return drafts.get(draft_type, "")

    def clear_draft(self, draft_type: str):
        """Clears a text draft after publishing."""
        with self._lock:
            drafts = self._data.get("drafts", {})
            if draft_type in drafts:
                del drafts[draft_type]
                self._data["drafts"] = drafts
                self._save_unlocked()


# Global Local Storage Instance Singleton
local_storage = LocalStorageManager()

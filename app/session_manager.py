"""
BharatConnect Persistent Encrypted Session Manager (app/session_manager.py)

Securely stores, restores, and validates logged-in user sessions across application restarts
using AES-256 encrypted local session files (data/session.dat).
"""

import os
import json
import time
from typing import Optional, Dict, Any

from utils.security import security_engine
from utils.local_storage import local_storage

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")
SESSION_FILE = os.path.join(DATA_DIR, "session.dat")


class SessionManager:
    def __init__(self, session_path=SESSION_FILE):
        self.session_path = session_path
        os.makedirs(os.path.dirname(self.session_path), exist_ok=True)

    def save_session(self, user_dict: dict, token: Optional[str] = None) -> bool:
        """Saves active user login session securely encrypted."""
        try:
            session_payload = {
                "user": user_dict,
                "token": token or f"tok-{user_dict.get('id', 'u-alex')}",
                "timestamp": time.time(),
            }
            json_text = json.dumps(session_payload)
            encrypted = security_engine.encrypt_payload(json_text)

            with open(self.session_path, "w", encoding="utf-8") as f:
                f.write(encrypted)

            # Store user_id in local storage
            local_storage.set("current_user_id", user_dict.get("id"))
            return True
        except Exception as e:
            print(f"[SessionManager] Save error: {e}")
            return False

    def get_session(self) -> Optional[Dict[str, Any]]:
        """Restores and validates active user session from disk."""
        if not os.path.exists(self.session_path):
            return None

        try:
            with open(self.session_path, "r", encoding="utf-8") as f:
                encrypted = f.read().strip()

            if not encrypted:
                return None

            json_text = security_engine.decrypt_payload(encrypted)
            if not json_text:
                return None

            session_data = json.loads(json_text)
            created_ts = session_data.get("timestamp", 0)

            # Validate Layer 7 session expiration (7 days TTL)
            if security_engine.is_session_expired(created_ts, ttl_seconds=604800):
                self.logout()
                return None

            return session_data
        except Exception as e:
            print(f"[SessionManager] Restore error: {e}")
            return None

    def is_logged_in(self) -> bool:
        """Checks if a valid session exists."""
        return self.get_session() is not None

    def logout(self) -> bool:
        """Clears active user session."""
        try:
            if os.path.exists(self.session_path):
                os.remove(self.session_path)
            local_storage.delete("current_user_id")
            return True
        except Exception:
            return False


# Global Session Manager Singleton
session_manager = SessionManager()

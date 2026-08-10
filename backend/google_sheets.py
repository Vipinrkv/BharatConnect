"""
BharatConnect Google Sheets Database Connector (backend/google_sheets.py)

Enables storing and retrieving application data (Users, Posts, Messages, Marketplace)
directly using Google Sheets as a free, accessible cloud database.
"""

import json
import requests
from typing import Dict, List, Optional

from backend.config import GOOGLE_SHEETS_WEB_APP_URL


class GoogleSheetsConnector:
    def __init__(self, web_app_url: Optional[str] = None):
        """
        :param web_app_url: Google Apps Script Web App Endpoint URL (e.g. https://script.google.com/macros/s/.../exec)
        """
        self.web_app_url = web_app_url or GOOGLE_SHEETS_WEB_APP_URL

    def is_configured(self) -> bool:
        return bool(self.web_app_url and self.web_app_url.startswith("https://script.google.com/"))

    def post_action(self, action: str, payload: dict) -> dict:
        """Sends data payload to Google Apps Script Web App."""
        if not self.is_configured():
            return {"status": "error", "message": "Google Sheets Web App URL not configured."}

        try:
            resp = requests.post(
                self.web_app_url,
                json={"action": action, "payload": payload},
                timeout=15.0,
                headers={"Content-Type": "application/json"},
                verify=False,
            )
            return resp.json()
        except Exception as e:
            return {"status": "error", "message": str(e)}

    def fetch_sheet(self, sheet_name: str) -> List[dict]:
        """Fetches rows from a specific Google Sheet tab."""
        if not self.is_configured():
            return []

        try:
            resp = requests.get(
                self.web_app_url,
                params={"action": "read", "sheet": sheet_name},
                timeout=15.0,
                verify=False,
            )
            data = resp.json()
            return data.get("rows", [])
        except Exception:
            return []

    # --- Domain Methods ---

    def save_user(self, user_dict: dict) -> dict:
        return self.post_action("save_user", user_dict)

    def save_post(self, post_dict: dict) -> dict:
        return self.post_action("save_post", post_dict)

    def save_message(self, message_dict: dict) -> dict:
        return self.post_action("save_message", message_dict)

    def fetch_posts(self) -> List[dict]:
        return self.fetch_sheet("posts")

    def fetch_messages(self, chat_id: str) -> List[dict]:
        rows = self.fetch_sheet("messages")
        return [r for r in rows if r.get("chat_id") == chat_id]

"""
BharatConnect Universal API Client (app/api_client.py)
Provides asynchronous/synchronous HTTP communication with the BharatConnect Universal Backend Server,
with seamless fallback to local database engine when offline.
"""

import json
import urllib.request
import urllib.parse
from database.database import db_engine as local_db_engine


class BharatConnectAPIClient:
    def __init__(self, base_url="http://127.0.0.1:8000/api/v1"):
        self.base_url = base_url.rstrip('/')
        self.auth_token = None
        self.current_user = None

    def check_server_online(self) -> bool:
        """Checks if the universal backend server is reachable."""
        try:
            req = urllib.request.Request(f"{self.base_url}/health", method="GET")
            with urllib.request.urlopen(req, timeout=1.5) as resp:
                data = json.loads(resp.read().decode())
                return data.get("status") == "online"
        except Exception:
            return False

    def login(self, identifier, password):
        """Authenticates user against backend API."""
        payload = json.dumps({"identifier": identifier, "password": password}).encode("utf-8")
        headers = {"Content-Type": "application/json"}
        try:
            req = urllib.request.Request(f"{self.base_url}/auth/login", data=payload, headers=headers, method="POST")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                data = json.loads(resp.read().decode())
                self.auth_token = data.get("access_token")
                self.current_user = data.get("user")
                return True, self.current_user
        except urllib.error.HTTPError as e:
            try:
                err_data = json.loads(e.read().decode())
                detail = err_data.get("detail", "Invalid username/email or password.")
            except Exception:
                detail = "Invalid username/email or password."
            return False, detail
        except Exception:
            return False, "Internet connection required to log in. Unable to connect to server."

    def register(self, full_name, username, email, password, phone="", dob=""):
        """Registers a new user on backend API server."""
        payload = json.dumps({
            "full_name": full_name,
            "display_name": full_name or username,
            "username": username,
            "email": email,
            "phone": phone,
            "password": password
        }).encode("utf-8")
        headers = {"Content-Type": "application/json"}
        try:
            req = urllib.request.Request(f"{self.base_url}/auth/register", data=payload, headers=headers, method="POST")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                data = json.loads(resp.read().decode())
                self.auth_token = data.get("access_token")
                self.current_user = data.get("user")
                return True, self.current_user
        except urllib.error.HTTPError as e:
            try:
                err_data = json.loads(e.read().decode())
                detail = err_data.get("detail", "Registration failed.")
            except Exception:
                detail = "Registration failed."
            return False, detail
        except Exception as e:
            return False, str(e)

    def get_stories(self):
        """Fetches stories list from backend API server."""
        try:
            req = urllib.request.Request(f"{self.base_url}/stories", method="GET")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                return json.loads(resp.read().decode())
        except Exception:
            return local_db_engine.get_stories()

    def get_posts(self):
        """Fetches feed posts from backend API or local DB fallback."""
        headers = {}
        if self.auth_token:
            headers["Authorization"] = f"Bearer {self.auth_token}"
        try:
            req = urllib.request.Request(f"{self.base_url}/posts", headers=headers, method="GET")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                return json.loads(resp.read().decode())
        except Exception:
            return local_db_engine.get_posts()

    def create_post(self, content, image_title=None):
        """Publishes a new post to backend API server."""
        payload = json.dumps({"content": content, "image_title": image_title}).encode("utf-8")
        headers = {"Content-Type": "application/json"}
        if self.auth_token:
            headers["Authorization"] = f"Bearer {self.auth_token}"
        try:
            req = urllib.request.Request(f"{self.base_url}/posts", data=payload, headers=headers, method="POST")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                return json.loads(resp.read().decode())
        except Exception as e:
            print(f"[APIClient] create_post error: {e}")
            return None

    def toggle_like(self, post_id):
        """Toggles like status for a post on backend API server."""
        headers = {}
        if self.auth_token:
            headers["Authorization"] = f"Bearer {self.auth_token}"
        try:
            req = urllib.request.Request(f"{self.base_url}/posts/{post_id}/like", headers=headers, method="POST")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                return json.loads(resp.read().decode())
        except Exception as e:
            print(f"[APIClient] toggle_like error: {e}")
            return None

    def get_chats(self):
        """Fetches conversations list from backend API or local DB fallback."""
        try:
            req = urllib.request.Request(f"{self.base_url}/chats", method="GET")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                return json.loads(resp.read().decode())
        except Exception:
            return local_db_engine.get_chats()

    def get_chat_messages(self, chat_id):
        """Fetches message thread for a chat from backend API server."""
        try:
            req = urllib.request.Request(f"{self.base_url}/chats/{urllib.parse.quote(chat_id)}/messages", method="GET")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                return json.loads(resp.read().decode())
        except Exception:
            return local_db_engine.get_chat_messages(chat_id)

    def send_message(self, chat_id, text, sender_id=None, sender_name=None, client_message_id=None):
        """Sends a message in a chat thread to backend API server."""
        payload_dict = {
            "text": text,
            "client_message_id": client_message_id,
        }
        if sender_id:
            payload_dict["sender_id"] = sender_id
        if sender_name:
            payload_dict["sender_name"] = sender_name

        payload = json.dumps(payload_dict).encode("utf-8")
        headers = {"Content-Type": "application/json"}
        if self.auth_token:
            headers["Authorization"] = f"Bearer {self.auth_token}"
        try:
            url = f"{self.base_url}/chats/{urllib.parse.quote(chat_id)}/messages"
            req = urllib.request.Request(url, data=payload, headers=headers, method="POST")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                return json.loads(resp.read().decode())
        except Exception as e:
            print(f"[APIClient] send_message error: {e}")
            return None


    def get_marketplace(self, category="ALL"):
        """Fetches marketplace listings from backend API or local DB fallback."""
        try:
            url = f"{self.base_url}/marketplace"
            if category and category != "ALL":
                url += f"?category={urllib.parse.quote(category)}"
            req = urllib.request.Request(url, method="GET")
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                items = json.loads(resp.read().decode())
                res = {"popular_items": [], "jobs": [], "quick_jobs": []}
                for item in items:
                    cat = item.get("category", "popular_items")
                    if cat in res:
                        res[cat].append(item)
                return res
        except Exception:
            return local_db_engine.get_marketplace_data()


# Global API Client Singleton
api_client = BharatConnectAPIClient()


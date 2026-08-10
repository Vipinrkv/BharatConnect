"""
BharatConnect Firebase Push Notification Engine (backend/fcm_push.py)
Handles dispatching FCM push notifications using Firebase Admin SDK.
"""

import json
import os
from pathlib import Path
from typing import Dict, Any, Optional

try:
    import firebase_admin
    from firebase_admin import credentials, messaging
    FIREBASE_ADMIN_AVAILABLE = True
except ImportError:
    FIREBASE_ADMIN_AVAILABLE = False


def init_firebase_admin() -> bool:
    """Initializes Firebase Admin SDK using service account JSON (from ENV string or file)."""
    if not FIREBASE_ADMIN_AVAILABLE:
        return False

    if firebase_admin._apps:
        return True

    # 1. Check environment variable containing full JSON string
    env_json_str = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON", "").strip()
    if env_json_str:
        try:
            cert_dict = json.loads(env_json_str)
            cred = credentials.Certificate(cert_dict)
            firebase_admin.initialize_app(cred)
            return True
        except Exception as e:
            print(f"Error initializing Firebase Admin from env JSON string: {e}")

    # 2. Check local file paths
    base_dir = Path(__file__).resolve().parent
    key_paths = [
        base_dir / "firebase_service_account.json",
        base_dir.parent / "firebase_service_account.json",
        os.environ.get("FIREBASE_SERVICE_ACCOUNT_PATH", ""),
    ]

    for key_path in key_paths:
        if key_path and os.path.exists(key_path):
            try:
                cred = credentials.Certificate(str(key_path))
                firebase_admin.initialize_app(cred)
                return True
            except Exception as e:
                print(f"Error initializing Firebase Admin with {key_path}: {e}")
                continue

    return False


def send_push_notification(
    token: str,
    title: str,
    body: str,
    data: Optional[Dict[str, str]] = None
) -> Dict[str, Any]:
    """
    Sends a FCM push notification to a target device token.
    
    :param token: Target device FCM registration token.
    :param title: Notification title.
    :param body: Notification body text.
    :param data: Optional custom data payload.
    :return: Response status dictionary.
    """
    if not init_firebase_admin():
        return {
            "success": False,
            "error": "Firebase Admin SDK not initialized or service account key missing."
        }

    try:
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data=data or {},
            token=token,
        )
        response = messaging.send(message)
        return {"success": True, "message_id": response}
    except Exception as e:
        return {"success": False, "error": str(e)}


if __name__ == "__main__":
    initialized = init_firebase_admin()
    print(f"Firebase Admin SDK initialized: {initialized}")

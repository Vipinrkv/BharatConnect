"""
BharatConnect Universal Backend Config (backend/config.py)
"""

import os
from pathlib import Path

try:
    from dotenv import load_dotenv
    BASE_DIR = Path(__file__).resolve().parent.parent
    load_dotenv(BASE_DIR / ".env")
    load_dotenv(Path(__file__).resolve().parent / ".env")
except ImportError:
    BASE_DIR = Path(__file__).resolve().parent.parent

# Server Host & Port
HOST = os.environ.get("HOST", "0.0.0.0")
PORT = int(os.environ.get("PORT", 8000))

# Supabase Credentials (loaded from .env or environment variables)
SUPABASE_PROJECT_ID = os.environ.get("SUPABASE_PROJECT_ID", "ykbfynoofjvibnyfkifi")
SUPABASE_URL = os.environ.get("SUPABASE_URL", "https://ykbfynoofjvibnyfkifi.supabase.co")
SUPABASE_PUBLISHABLE_KEY = os.environ.get("SUPABASE_PUBLISHABLE_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlrYmZ5bm9vZmp2aWJueWZraWZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYzNzAxNjQsImV4cCI6MjEwMTk0NjE2NH0.XDeixsULEe8Z03OsxTOeACHXGkQU30MbuOvXWQrO9xw")
SUPABASE_SECRET_KEY = os.environ.get("SUPABASE_SECRET_KEY", "")

# Firebase Cloud Messaging Credentials (loaded from .env or environment variables)
FCM_PROJECT_ID = os.environ.get("FCM_PROJECT_ID", "bharatconnect-fcm")
FCM_SENDER_ID = os.environ.get("FCM_SENDER_ID", "247753000307")

# Cloudinary Storage Credentials (loaded from .env or environment variables)
CLOUDINARY_CLOUD_NAME = os.environ.get("CLOUDINARY_CLOUD_NAME", "twiesyqj")
CLOUDINARY_API_KEY = os.environ.get("CLOUDINARY_API_KEY", "446197212112895")
CLOUDINARY_API_SECRET = os.environ.get("CLOUDINARY_API_SECRET", "AZhHnq586KtBkyhKFEdwYRwbiiA")
CLOUDINARY_URL = os.environ.get("CLOUDINARY_URL", "")

# Universal Database Connection String
DATA_DIR = os.path.join(str(BASE_DIR), "data")
os.makedirs(DATA_DIR, exist_ok=True)
DEFAULT_SQLITE_PATH = os.path.join(DATA_DIR, "app.db").replace("\\", "/")
DATABASE_URL = os.environ.get("DATABASE_URL", f"sqlite:///{DEFAULT_SQLITE_PATH}")

# JWT Authentication Config
JWT_SECRET_KEY = os.environ.get("JWT_SECRET_KEY", "bharatconnect-super-secret-jwt-key-2026")
JWT_ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24 * 7  # 7 Days

# Google Sheets Database Sync Config
GOOGLE_SHEETS_WEB_APP_URL = os.environ.get(
    "GOOGLE_SHEETS_WEB_APP_URL",
    "https://script.google.com/macros/s/AKfycbzGLTXgK7hXN3pOCGlrG_yQo3ozUWQKD-wLXTTIeo6VkmhGPPXfII0PirHh5lr8coD2/exec"
)

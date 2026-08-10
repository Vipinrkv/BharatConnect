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
SUPABASE_PROJECT_ID = os.environ.get("SUPABASE_PROJECT_ID", "")
SUPABASE_URL = os.environ.get("SUPABASE_URL", "")
SUPABASE_PUBLISHABLE_KEY = os.environ.get("SUPABASE_PUBLISHABLE_KEY", "")
SUPABASE_SECRET_KEY = os.environ.get("SUPABASE_SECRET_KEY", "")

# Universal Database Connection String
DATA_DIR = os.path.join(str(BASE_DIR), "data")
os.makedirs(DATA_DIR, exist_ok=True)
DEFAULT_SQLITE_PATH = os.path.join(DATA_DIR, "app.db").replace("\\", "/")

# Reads DATABASE_URL from .env or environment; defaults to local SQLite if unset
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

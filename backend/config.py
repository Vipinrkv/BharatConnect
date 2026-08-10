"""
BharatConnect Universal Backend Config (backend/config.py)
"""

import os

# Server Host & Port
HOST = os.environ.get("HOST", "127.0.0.1")
PORT = int(os.environ.get("PORT", 8000))

# Universal Database Connection String
# Defaults to local SQLite, but can be set via env var to PostgreSQL, MySQL, etc.
# Example PostgreSQL: postgresql://user:password@localhost:5432/bharatconnect
# Example MySQL: mysql+pymysql://user:password@localhost:3306/bharatconnect
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")
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

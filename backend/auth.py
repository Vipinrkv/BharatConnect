"""
BharatConnect JWT & Password Authentication Helper (backend/auth.py)
"""

import hashlib
from datetime import datetime, timedelta
from typing import Optional

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt
from sqlalchemy.orm import Session

from backend.config import JWT_SECRET_KEY, JWT_ALGORITHM, ACCESS_TOKEN_EXPIRE_MINUTES
from backend.database import get_db, UserModel

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login", auto_error=False)


import hmac

SALT = (JWT_SECRET_KEY.encode("utf-8")[:16]) if len(JWT_SECRET_KEY) >= 16 else b"bharatconnect_sec"

def hash_password(password: str) -> str:
    """Computes PBKDF2 HMAC SHA-256 hash (100,000 iterations) for security."""
    key = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), SALT, 100000)
    return key.hex()


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verifies plain text password using constant-time comparison against PBKDF2 and SHA-256 fallback."""
    computed_pbkdf2 = hash_password(plain_password)
    if hmac.compare_digest(computed_pbkdf2, hashed_password):
        return True
    computed_sha256 = hashlib.sha256(plain_password.encode("utf-8")).hexdigest()
    return hmac.compare_digest(computed_sha256, hashed_password)


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """Generates signed JWT Access Token."""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, JWT_SECRET_KEY, algorithm=JWT_ALGORITHM)
    return encoded_jwt


def get_current_user(token: Optional[str] = Depends(oauth2_scheme), db: Session = Depends(get_db)) -> Optional[UserModel]:
    """Retrieves authenticated user from JWT bearer token."""
    if not token:
        return None
    try:
        payload = jwt.decode(token, JWT_SECRET_KEY, algorithms=[JWT_ALGORITHM])
        user_id: str = payload.get("sub")
        if user_id is None:
            return None
    except JWTError:
        return None

    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    return user

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


import base64
import hmac
import os
import secrets

try:
    from passlib.hash import argon2
    HAS_ARGON2 = True
except ImportError:
    HAS_ARGON2 = False

SALT = (JWT_SECRET_KEY.encode("utf-8")[:16]) if len(JWT_SECRET_KEY) >= 16 else b"bharatconnect_sec"


def hash_password(password: str) -> str:
    """
    Hashes new passwords using Argon2id (or scrypt/PBKDF2 salted fallback).
    """
    if HAS_ARGON2:
        try:
            return argon2.using(type="id").hash(password)
        except Exception:
            pass
    
    # High-security scrypt salted hash fallback format: $scrypt$salt$hash
    salt = secrets.token_hex(16)
    key = hashlib.scrypt(password.encode("utf-8"), salt=salt.encode("utf-8"), n=16384, r=8, p=1)
    return f"$scrypt${salt}${key.hex()}"



def verify_password(plain_password: str, hashed_password: str) -> bool:
    """
    Verifies plain text password against Argon2id, scrypt, PBKDF2, and legacy SHA-256 hashes.
    """
    if not hashed_password or not plain_password:
        return False

    # 1. Argon2id / Passlib Hash Format Verification
    if HAS_ARGON2 and (hashed_password.startswith("$argon2id$") or hashed_password.startswith("$argon2i$")):
        try:
            return argon2.verify(plain_password, hashed_password)
        except Exception:
            return False

    # 2. Scrypt Salted Hash Format Verification
    if hashed_password.startswith("$scrypt$"):
        try:
            parts = hashed_password.split("$")
            if len(parts) == 4:
                _, _, salt_hex, key_hex = parts
                computed_key = hashlib.scrypt(plain_password.encode("utf-8"), salt=salt_hex.encode("utf-8"), n=16384, r=8, p=1)
                return hmac.compare_digest(computed_key.hex(), key_hex)
        except Exception:
            pass

    # 3. Legacy PBKDF2 HMAC SHA-256 Verification
    computed_pbkdf2 = hashlib.pbkdf2_hmac("sha256", plain_password.encode("utf-8"), SALT, 100000).hex()
    if hmac.compare_digest(computed_pbkdf2, hashed_password):
        return True

    # 4. Legacy SHA-256 Verification Fallback
    computed_sha256 = hashlib.sha256(plain_password.encode("utf-8")).hexdigest()
    if hmac.compare_digest(computed_sha256, hashed_password):
        return True

    # 5. Local DB Engine Salted SHA-256 Verification Fallback
    computed_bc_sha256 = hashlib.sha256(f"bharatconnect:{plain_password}".encode("utf-8")).hexdigest()
    return hmac.compare_digest(computed_bc_sha256, hashed_password)



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
    """Retrieves authenticated user from JWT bearer token if present."""
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


def get_required_user(user: Optional[UserModel] = Depends(get_current_user)) -> UserModel:
    """Enforces valid authentication, raising HTTP 401 if unauthenticated."""
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication credentials were not provided or invalid",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return user


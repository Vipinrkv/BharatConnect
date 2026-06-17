import time
import jwt
from typing import Dict, Optional
from fastapi import HTTPException, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from app.config import settings
from app.core.redis import redis_manager

security_agent = HTTPBearer()

# Token validation configuration
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30
REFRESH_TOKEN_EXPIRE_DAYS = 30

def create_access_token(user_id: str, metadata: Optional[Dict] = None) -> str:
    payload = {
        "sub": user_id,
        "exp": time.time() + (ACCESS_TOKEN_EXPIRE_MINUTES * 60),
        "iat": time.time(),
        "type": "access",
        "metadata": metadata or {}
    }
    return jwt.encode(payload, settings.SUPABASE_JWT_SECRET, algorithm=ALGORITHM)

def create_refresh_token(user_id: str, session_id: str) -> str:
    payload = {
        "sub": user_id,
        "sid": session_id,
        "exp": time.time() + (REFRESH_TOKEN_EXPIRE_DAYS * 24 * 3600),
        "iat": time.time(),
        "type": "refresh"
    }
    return jwt.encode(payload, settings.SUPABASE_JWT_SECRET, algorithm=ALGORITHM)

def verify_token(token: str) -> Dict:
    try:
        payload = jwt.decode(token, settings.SUPABASE_JWT_SECRET, algorithms=[ALGORITHM])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has expired"
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authorization token"
        )

async def get_current_user_id(credentials: HTTPAuthorizationCredentials = Security(security_agent)) -> str:
    """
    Dependency injection helper to validate JWTs in route requests
    """
    payload = verify_token(credentials.credentials)
    if payload.get("type") != "access":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token type"
        )
    return payload["sub"]

async def check_rate_limit(key: str, limit: int, window_seconds: int):
    """
    Redis-backed sliding-window rate limiter using sorted sets (ZSET).
    Thread-safe and resistant to race conditions under heavy concurrent loads.
    """
    client = await redis_manager.get_client()
    redis_key = f"rate_limit:{key}"
    now = time.time()
    clear_before = now - window_seconds
    member_id = f"{now}:{uuid_generator()}" # unique member identifier to prevent collisions

    pipe = client.pipeline()
    # Remove logs older than sliding window threshold
    pipe.zremrangebyscore(redis_key, 0, clear_before)
    # Count remaining requests in current sliding window
    pipe.zcard(redis_key)
    # Add current request timestamp
    pipe.zadd(redis_key, {member_id: now})
    # Set expiration on the set to free memory automatically
    pipe.expire(redis_key, window_seconds)

    results = await pipe.execute()
    current_requests_count = results[1]

    if current_requests_count >= limit:
        # Revert addition to avoid penalizing lock periods on retry spikes
        await client.zrem(redis_key, member_id)
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Rate limit exceeded. Please try again later."
        )

def uuid_generator() -> str:
    import uuid
    return uuid.uuid4().hex[:6]

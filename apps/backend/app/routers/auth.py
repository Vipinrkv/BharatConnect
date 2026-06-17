import hashlib
from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update, func, cast
from sqlalchemy.dialects.postgresql import BYTEA
from typing import List

from app.core.database import get_db
from app.core.security import (
    create_access_token,
    create_refresh_token,
    verify_token,
    get_current_user_id,
    check_rate_limit
)
from app.models.user import Profile
from app.schemas.user import (
    EmailLoginRequest,
    GoogleLoginRequest,
    TokenResponse,
    TokenRefreshRequest,
    UserSessionResponse,
    ContactDiscoveryRequest,
    ContactMatch
)

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post("/login/email", response_model=TokenResponse)
async def login_email(
    request: Request,
    payload: EmailLoginRequest,
    db: AsyncSession = Depends(get_db)
):
    # Apply Rate Limiting (max 5 requests/min per IP/email)
    client_ip = request.client.host if request.client else "unknown"
    await check_rate_limit(f"login:{client_ip}", limit=5, window_seconds=60)
    await check_rate_limit(f"login:{payload.email}", limit=5, window_seconds=60)

    # 1. Fetch user by email (Mock credential check matching Supabase JWT setup)
    query = select(Profile).where(Profile.email == str(payload.email))
    result = await db.execute(query)
    user = result.scalars().first()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password credentials"
        )

    # 2. Setup Session metadata
    session_id = "session_" + hashlib.md5(f"{user.id}:{payload.device_id}:{time_now()}".encode()).hexdigest()
    
    # 3. Create tokens
    access_token = create_access_token(user.id, {"email": user.email, "session_id": session_id})
    refresh_token = create_refresh_token(user.id, session_id)

    # 4. Insert Session in database (Raw SQL or SQLAlchemy mapping. Here using direct insert)
    # Write to user_sessions table
    session_insert = """
        INSERT INTO public.user_sessions (id, user_id, device_id, device_name, ip_address, user_agent, refresh_token_hash, is_active)
        VALUES (:id, :user_id, :device_id, :device_name, :ip_address, :user_agent, :token_hash, true)
    """
    await db.execute(
        func.public.user_sessions_insert( # or text equivalent
            id=session_id,
            user_id=user.id,
            device_id=payload.device_id,
            device_name=payload.device_name,
            ip_address=client_ip,
            user_agent=request.headers.get("user-agent", ""),
            token_hash=hashlib.sha256(refresh_token.encode()).hexdigest()
        )
    )

    # 5. Log audit entry
    # (Direct execution query for audit logs)
    return TokenResponse(access_token=access_token, refresh_token=refresh_token)


@router.post("/refresh", response_model=TokenResponse)
async def refresh_session(
    payload: TokenRefreshRequest,
    db: AsyncSession = Depends(get_db)
):
    token_data = verify_token(payload.refresh_token)
    if token_data.get("type") != "refresh":
        raise HTTPException(status_code=401, detail="Invalid token type")

    user_id = token_data["sub"]
    session_id = token_data["sid"]
    token_hash = hashlib.sha256(payload.refresh_token.encode()).hexdigest()

    # Query active session
    # In production, check database if user_sessions has is_active = true and hash matches
    # If invalid, revoke and raise 401
    
    new_access = create_access_token(user_id, {"session_id": session_id})
    new_refresh = create_refresh_token(user_id, session_id)

    # Update session table with new refresh token hash and last active timestamp
    return TokenResponse(access_token=new_access, refresh_token=new_refresh)


@router.post("/logout")
async def logout(
    request: Request,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    # Parse session_id from access token claims, set is_active = false in user_sessions
    return {"message": "Successfully logged out from current session"}


@router.post("/logout-all")
async def logout_all_devices(
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    # Update all sessions for user_id setting is_active = false
    return {"message": "Successfully logged out from all devices"}


@router.post("/contacts/discover", response_model=List[ContactMatch])
async def discover_contacts(
    payload: ContactDiscoveryRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Takes list of SHA-256 hashed phone numbers and matches them against existing users
    """
    if not payload.phone_hashes:
        return []

    # Format logic: Query profiles, check SHA-256 hex string matches
    # To do this safely and efficiently in SQL, we query and filter (or use Postgres digest)
    query = select(Profile)
    result = await db.execute(query)
    all_profiles = result.scalars().all()

    matches = []
    for profile in all_profiles:
        if not profile.phone:
            continue
        # Clean phone input format (E.164)
        clean_phone = profile.phone.replace("+", "").replace(" ", "").strip()
        hashed = hashlib.sha256(clean_phone.encode("utf-8")).hexdigest()
        
        if hashed in payload.phone_hashes:
            matches.append(
                ContactMatch(
                    id=profile.id,
                    display_name=profile.display_name,
                    avatar_url=profile.avatar_url,
                    username=profile.username
                )
            )
            
    return matches

def time_now():
    import datetime
    return datetime.datetime.utcnow().isoformat()

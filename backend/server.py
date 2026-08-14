"""
BharatConnect Universal Standalone REST & WebSocket API Server (backend/server.py)
Can run on any cloud provider or server (AWS, GCP, Azure, DigitalOcean, VPS, Local).
"""

import os
import re
import sys
import uuid
from typing import List, Dict
from datetime import datetime

from fastapi import FastAPI, Depends, HTTPException, status, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
import sqlalchemy
from sqlalchemy.orm import Session

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from backend.config import HOST, PORT
from backend.database import (
    init_db,
    get_db,
    SessionLocal,
    UserModel,
    PostModel,
    ChatModel,
    MessageModel,
    StoryModel,
    MarketplaceModel,
)
from backend.schemas import (
    LoginRequest,
    RegisterRequest,
    ProfileUpdateRequest,
    TokenResponse,
    UserResponse,
    PostCreateRequest,
    PostResponse,
    ChatResponse,
    MessageCreateRequest,
    MessageResponse,
    StoryResponse,
    MarketplaceItemResponse,
    ContactMatchRequest,
)
from backend.auth import hash_password, verify_password, create_access_token, get_current_user
from utils.cloudinary_storage import upload_media


from contextlib import asynccontextmanager


@asynccontextmanager
async def lifespan(app: FastAPI):
    seed_initial_data()
    yield


# Initialize FastAPI Application
app = FastAPI(
    title="BharatConnect Universal API Server",
    description="Sub-50ms Realtime Messaging & Social REST/WebSocket API Server",
    version="2.0.0",
    lifespan=lifespan,
)

# Enable CORS for universal client access (Kivy, Web, Flutter, React)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# WebSocket Connection Manager for Real-Time Broadcasting
class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[str, List[WebSocket]] = {}
        self.global_connections: List[WebSocket] = []

    async def connect_global(self, websocket: WebSocket):
        await websocket.accept()
        self.global_connections.append(websocket)

    def disconnect_global(self, websocket: WebSocket):
        if websocket in self.global_connections:
            self.global_connections.remove(websocket)

    async def broadcast_global(self, event_data: dict):
        for connection in self.global_connections:
            try:
                await connection.send_json(event_data)
            except Exception:
                pass

    async def connect(self, chat_id: str, websocket: WebSocket):
        await websocket.accept()
        if chat_id not in self.active_connections:
            self.active_connections[chat_id] = []
        self.active_connections[chat_id].append(websocket)

    def disconnect(self, chat_id: str, websocket: WebSocket):
        if chat_id in self.active_connections:
            if websocket in self.active_connections[chat_id]:
                self.active_connections[chat_id].remove(websocket)

    async def broadcast(self, chat_id: str, message_data: dict):
        if chat_id in self.active_connections:
            for connection in self.active_connections[chat_id]:
                try:
                    await connection.send_json(message_data)
                except Exception:
                    pass


ws_manager = ConnectionManager()


# Initialize Database Tables (No Demo Data)
def seed_initial_data():
    init_db()


# API Health Endpoint
@app.get("/")
@app.get("/api/v1/health")
def health_check(db: Session = Depends(get_db)):
    db_status = "healthy"
    try:
        db.execute(sqlalchemy.text("SELECT 1"))
    except Exception:
        db_status = "degraded"

    return {
        "status": "online",
        "service": "BharatConnect Universal API Server",
        "timestamp": datetime.utcnow().isoformat(),
        "database": db_status,
        "environment": "production" if os.environ.get("RENDER") else "development",
    }


def normalize_phone_number(phone: str) -> str:
    """
    Normalizes phone numbers to standard digits format (e.g. 10 digits).
    Strips non-digits, country code '+91' / '91' (if 12 digits), and leading zeros ('0').
    Example: '08261867326', '+91 8261867326', '8261 867 326' -> '8261867326'
    """
    if not phone:
        return ""
    digits = "".join([ch for ch in str(phone) if ch.isdigit()])
    if len(digits) == 12 and digits.startswith("91"):
        digits = digits[2:]
    return digits.lstrip("0")


# Authentication Endpoints
@app.post("/api/v1/auth/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    id_clean = payload.identifier.strip().lower()
    norm_phone = normalize_phone_number(payload.identifier)

    login_filters = [
        sqlalchemy.func.lower(UserModel.username) == id_clean,
        sqlalchemy.func.lower(UserModel.email) == id_clean,
        UserModel.phone == id_clean,
    ]
    if norm_phone:
        login_filters.extend([
            UserModel.phone == norm_phone,
            UserModel.phone == '0' + norm_phone,
            UserModel.phone == '+91' + norm_phone,
            UserModel.phone == '91' + norm_phone,
        ])

    user = db.query(UserModel).filter(sqlalchemy.or_(*login_filters)).first()
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid username, email, or password")

    token = create_access_token({"sub": user.id, "username": user.username})
    user_dict = {
        "id": user.id,
        "username": user.username,
        "display_name": user.display_name or user.username,
        "email": user.email,
        "phone": user.phone or "",
        "bio": user.bio or "Hey there! I am using BharatConnect 🚀",
        "user_avatar": getattr(user, 'user_avatar', None) or 'logo.png',
        "avatar_initials": user.avatar_initials or "BC",
        "avatar_color": user.avatar_color or "#6367FF",
    }
    return {"access_token": token, "token_type": "bearer", "user": user_dict}


@app.post("/api/v1/auth/register", response_model=TokenResponse)
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    uname_clean = payload.username.strip().lower()
    email_clean = payload.email.strip().lower()
    raw_phone = payload.phone.strip() if payload.phone and payload.phone.strip() else ""
    norm_phone = normalize_phone_number(raw_phone)

    filters = [
        sqlalchemy.func.lower(UserModel.username) == uname_clean,
        sqlalchemy.func.lower(UserModel.email) == email_clean,
    ]
    if norm_phone:
        filters.extend([
            UserModel.phone == norm_phone,
            UserModel.phone == raw_phone,
            UserModel.phone == '0' + norm_phone,
            UserModel.phone == '+91' + norm_phone,
            UserModel.phone == '91' + norm_phone,
        ])

    existing = (
        db.query(UserModel)
        .filter(sqlalchemy.or_(*filters))
        .first()
    )
    if existing:
        if existing.username and existing.username.lower() == uname_clean:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Username '@{payload.username}' is already registered! Please choose a different username.")
        elif existing.email and existing.email.lower() == email_clean:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Email '{payload.email}' is already registered! Please log in instead.")
        elif norm_phone and existing.phone and (normalize_phone_number(existing.phone) == norm_phone):
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=f"Phone number '{payload.phone}' is already registered! Please log in instead.")
        else:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="An account with these details is already registered! Please log in instead.")

    display_name = payload.display_name or payload.full_name or payload.username
    new_id = f"u-{uuid.uuid4().hex[:8]}"
    initials = "".join([part[0].upper() for part in display_name.split()[:2]]) or "BC"
    
    avatar_val = payload.user_avatar or 'logo.png'
    if avatar_val and avatar_val.startswith('data:image/'):
        c_res = upload_media(avatar_val, folder="bharatconnect_avatars")
        if c_res.get('success') and c_res.get('url'):
            avatar_val = c_res.get('url')

    user = UserModel(
        id=new_id,
        username=payload.username.strip(),
        display_name=display_name.strip(),
        email=email_clean,
        phone=norm_phone or raw_phone or None,
        password_hash=hash_password(payload.password),
        user_avatar=avatar_val,
        avatar_initials=initials,
        avatar_color="#6367FF",
        bio="Hey there! I am using BharatConnect 🚀",
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    token = create_access_token({"sub": user.id, "username": user.username})
    user_dict = {
        "id": user.id,
        "username": user.username,
        "display_name": user.display_name,
        "email": user.email,
        "phone": user.phone or "",
        "bio": user.bio,
        "user_avatar": user.user_avatar or 'logo.png',
        "avatar_initials": user.avatar_initials,
        "avatar_color": user.avatar_color,
    }
    return {"access_token": token, "token_type": "bearer", "user": user_dict}


@app.put("/api/v1/auth/profile/{user_id}")
def update_profile(user_id: str, payload: ProfileUpdateRequest, db: Session = Depends(get_db)):
    user = db.query(UserModel).filter(UserModel.id == user_id).first()
    if not user:
        # Fallback query by username
        user = db.query(UserModel).filter(UserModel.username == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    if payload.display_name:
        user.display_name = payload.display_name.strip()
    if payload.bio is not None:
        user.bio = payload.bio.strip()
    if payload.email:
        user.email = payload.email.strip().lower()
    if payload.phone is not None:
        user.phone = payload.phone.strip()
    if payload.user_avatar is not None:
        new_avatar = payload.user_avatar
        if new_avatar and new_avatar.startswith('data:image/'):
            c_res = upload_media(new_avatar, folder="bharatconnect_avatars")
            if c_res.get('success') and c_res.get('url'):
                new_avatar = c_res.get('url')
        user.user_avatar = new_avatar

    db.commit()
    db.refresh(user)
    return {
        "id": user.id,
        "username": user.username,
        "display_name": user.display_name,
        "email": user.email,
        "phone": user.phone or "",
        "bio": user.bio,
        "user_avatar": user.user_avatar or 'logo.png',
        "avatar_initials": user.avatar_initials,
        "avatar_color": user.avatar_color,
    }


@app.get("/api/v1/auth/users")
def list_users(db: Session = Depends(get_db)):
    users = db.query(UserModel).all()
    return [
        {
            "id": u.id,
            "username": u.username,
            "display_name": u.display_name or u.username,
            "email": u.email,
            "phone": u.phone or "",
            "bio": u.bio or "",
            "user_avatar": getattr(u, 'user_avatar', None) or 'logo.png',
            "avatar_initials": u.avatar_initials or "BC",
            "avatar_color": u.avatar_color or "#6367FF",
        }
        for u in users
    ]


# Feed Posts Endpoints
@app.get("/api/v1/posts", response_model=List[PostResponse])
def get_posts(db: Session = Depends(get_db)):
    posts = db.query(PostModel).order_by(PostModel.created_at.desc()).all()
    result = []
    for p in posts:
        result.append(
            {
                "id": p.id,
                "author_id": p.author_id,
                "author_name": p.author_name,
                "user_avatar": p.user_avatar,
                "avatar_color": p.avatar_color,
                "content": p.content,
                "image_title": p.image_title,
                "likes_count": p.likes_count,
                "comments_count": p.comments_count,
                "time_ago": p.time_ago or "Just now",
                "is_liked": bool(p.is_liked),
            }
        )
    return result


@app.post("/api/v1/posts", response_model=PostResponse)
def create_post(payload: PostCreateRequest, db: Session = Depends(get_db)):
    new_id = f"post-{uuid.uuid4().hex[:8]}"
    img_val = payload.image_data or payload.image_title or "Community Update"
    if img_val and img_val.startswith('data:image/'):
        c_res = upload_media(img_val, folder="bharatconnect_posts")
        if c_res.get('success') and c_res.get('url'):
            img_val = c_res.get('url')

    post = PostModel(
        id=new_id,
        author_id="u-user",
        author_name="Member",
        user_avatar="logo.png",
        avatar_color="#6367FF",
        content=payload.content,
        image_title=img_val,
        likes_count=0,
        comments_count=0,
        time_ago="Just now",
    )
    db.add(post)
    db.commit()
    db.refresh(post)
    return {
        "id": post.id,
        "author_id": post.author_id,
        "author_name": post.author_name,
        "user_avatar": post.user_avatar,
        "avatar_color": post.avatar_color,
        "content": post.content,
        "image_title": post.image_title,
        "likes_count": post.likes_count,
        "comments_count": post.comments_count,
        "time_ago": "Just now",
        "is_liked": False,
    }


@app.post("/api/v1/posts/{post_id}/like")
def toggle_like(post_id: str, db: Session = Depends(get_db)):
    post = db.query(PostModel).filter(PostModel.id == post_id).first()
    if not post:
        raise HTTPException(status_code=404, detail="Post not found")
    post.likes_count += 1
    post.is_liked = True
    db.commit()
    return {"status": "success", "post_id": post_id, "likes_count": post.likes_count}


# Stories Endpoints
@app.get("/api/v1/stories", response_model=List[StoryResponse])
def get_stories(db: Session = Depends(get_db)):
    return db.query(StoryModel).all()


# Chats & Messages Endpoints
@app.get("/api/v1/chats", response_model=List[ChatResponse])
def get_chats(db: Session = Depends(get_db)):
    return db.query(ChatModel).all()


@app.get("/api/v1/chats/{chat_id}/messages", response_model=List[MessageResponse])
def get_chat_messages(chat_id: str, db: Session = Depends(get_db)):
    filters = [MessageModel.chat_id == chat_id]
    if chat_id.startswith("chat_"):
        parts = chat_id.replace("chat_", "").split("_")
        if len(parts) == 2:
            p1, p2 = parts[0], parts[1]
            if p1 and p2:
                filters.append(
                    sqlalchemy.and_(
                        MessageModel.sender_id.like(f"%{p1}%"),
                        MessageModel.recipient_id.like(f"%{p2}%"),
                    )
                )
                filters.append(
                    sqlalchemy.and_(
                        MessageModel.sender_id.like(f"%{p2}%"),
                        MessageModel.recipient_id.like(f"%{p1}%"),
                    )
                )

    digits = re.sub(r"\D", "", chat_id)
    if len(digits) >= 7:
        filters.append(MessageModel.sender_id.like(f"%{digits}%"))
        filters.append(MessageModel.recipient_id.like(f"%{digits}%"))

    messages = db.query(MessageModel).filter(sqlalchemy.or_(*filters)).order_by(MessageModel.created_at.asc()).all()
    return messages


@app.post("/api/v1/chats/{chat_id}/messages", response_model=MessageResponse)
def send_message(chat_id: str, payload: MessageCreateRequest, db: Session = Depends(get_db)):
    msg_id = f"m-{uuid.uuid4().hex[:8]}"
    now_str = payload.time or datetime.now().strftime("%I:%M %p").lstrip("0")

    img_url = payload.image_url
    if img_url and img_url.startswith("data:image/"):
        c_res = upload_media(img_url, folder="bharatconnect_chat_media")
        if c_res.get("success") and c_res.get("url"):
            img_url = c_res.get("url")

    message = MessageModel(
        id=msg_id,
        chat_id=chat_id,
        sender_id=payload.sender_id or "u-user",
        sender_name=payload.sender_name or "Member",
        recipient_id=payload.recipient_id,
        text=payload.text,
        image_url=img_url,
        is_me=False,
        time=now_str,
    )
    db.add(message)
    db.commit()
    db.refresh(message)
    return message


# Marketplace Endpoints
@app.get("/api/v1/marketplace", response_model=List[MarketplaceItemResponse])
def get_marketplace_items(category: str = None, db: Session = Depends(get_db)):
    query = db.query(MarketplaceModel)
    if category and category != "ALL":
        query = query.filter(MarketplaceModel.category == category)
    return query.all()


# Privacy-Preserving Contact Discovery Endpoint
@app.post("/api/v1/contacts/match")
def match_contacts(payload: ContactMatchRequest, db: Session = Depends(get_db)):
    clean_targets = set()
    for p in payload.phone_numbers:
        digits = "".join(filter(str.isdigit, str(p)))
        if len(digits) >= 10:
            clean_targets.add(digits[-10:])

    if not clean_targets:
        return {"status": "success", "matched_users": []}

    users = db.query(UserModel).filter(UserModel.phone.isnot(None)).all()
    matched = []
    for u in users:
        u_digits = "".join(filter(str.isdigit, str(u.phone or "")))
        if len(u_digits) >= 10 and u_digits[-10:] in clean_targets:
            matched.append(
                {
                    "id": u.id,
                    "username": u.username,
                    "display_name": u.display_name,
                    "phone": u.phone,
                    "avatar_initials": u.avatar_initials,
                    "avatar_color": u.avatar_color,
                    "presence": u.presence or "ONLINE",
                }
            )
    return {"status": "success", "matched_users": matched}



# WebSocket Real-Time Chat Broadcast Endpoint
@app.websocket("/ws/chat/{chat_id}")
async def websocket_chat(websocket: WebSocket, chat_id: str):
    await ws_manager.connect(chat_id, websocket)
    try:
        while True:
            data = await websocket.receive_json()
            # Broadcast message to all connected clients in the chat thread
            await ws_manager.broadcast(chat_id, data)
    except WebSocketDisconnect:
        ws_manager.disconnect(chat_id, websocket)


# WebSocket Global Real-Time Live Data Stream Endpoint
@app.websocket("/ws/stream")
async def websocket_stream(websocket: WebSocket):
    await ws_manager.connect_global(websocket)
    try:
        while True:
            data = await websocket.receive_json()
            # Broadcast live feed events (posts, likes, user updates)
            await ws_manager.broadcast_global(data)
    except WebSocketDisconnect:
        ws_manager.disconnect_global(websocket)


if __name__ == "__main__":
    import uvicorn

    print(f"🚀 Starting BharatConnect Universal API Server at http://{HOST}:{PORT}")
    uvicorn.run("backend.server:app", host=HOST, port=PORT, reload=True)

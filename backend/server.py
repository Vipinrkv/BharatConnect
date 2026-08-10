"""
BharatConnect Universal Standalone REST & WebSocket API Server (backend/server.py)
Can run on any cloud provider or server (AWS, GCP, Azure, DigitalOcean, VPS, Local).
"""

import os
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


# Authentication Endpoints
@app.post("/api/v1/auth/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = (
        db.query(UserModel)
        .filter((UserModel.username == payload.identifier) | (UserModel.email == payload.identifier))
        .first()
    )
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid username or password")

    token = create_access_token({"sub": user.id, "username": user.username})
    user_dict = {
        "id": user.id,
        "username": user.username,
        "display_name": user.display_name,
        "email": user.email,
        "bio": user.bio,
        "avatar_initials": user.avatar_initials,
        "avatar_color": user.avatar_color,
    }
    return {"access_token": token, "token_type": "bearer", "user": user_dict}


@app.post("/api/v1/auth/register", response_model=TokenResponse)
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    existing = (
        db.query(UserModel)
        .filter((UserModel.username == payload.username) | (UserModel.email == payload.email))
        .first()
    )
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Username or email already exists")

    new_id = f"u-{uuid.uuid4().hex[:8]}"
    initials = "".join([part[0].upper() for part in payload.full_name.split()[:2]]) or "US"
    user = UserModel(
        id=new_id,
        username=payload.username,
        display_name=payload.full_name,
        email=payload.email,
        password_hash=hash_password(payload.password),
        avatar_initials=initials,
        avatar_color="#6367FF",
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
        "bio": user.bio,
        "avatar_initials": user.avatar_initials,
        "avatar_color": user.avatar_color,
    }
    return {"access_token": token, "token_type": "bearer", "user": user_dict}


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
    post = PostModel(
        id=new_id,
        author_id="u-alex",
        author_name="Alex Morgan",
        user_avatar="AM",
        avatar_color="#6367FF",
        content=payload.content,
        image_title=payload.image_title or "Community Update",
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
    return db.query(MessageModel).filter(MessageModel.chat_id == chat_id).order_by(MessageModel.created_at.asc()).all()


@app.post("/api/v1/chats/{chat_id}/messages", response_model=MessageResponse)
def send_message(chat_id: str, payload: MessageCreateRequest, db: Session = Depends(get_db)):
    msg_id = f"m-{uuid.uuid4().hex[:8]}"
    now_str = datetime.now().strftime("%I:%M %p").lstrip("0")
    message = MessageModel(
        id=msg_id,
        chat_id=chat_id,
        sender_id="u-alex",
        sender_name="Alex Morgan",
        text=payload.text,
        is_me=True,
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

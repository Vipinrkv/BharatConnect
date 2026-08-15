"""
BharatConnect Universal Database Abstraction Layer (backend/database.py)
Supports SQLite, PostgreSQL, MySQL, MariaDB, and any SQL database.
"""

import os
from datetime import datetime
from sqlalchemy import create_engine, Column, String, Integer, Boolean, DateTime, ForeignKey, Text
from sqlalchemy.exc import OperationalError
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship

from backend.config import DATABASE_URL, DEFAULT_SQLITE_PATH


def create_db_engine(db_url: str):
    connect_args = {"check_same_thread": False} if "sqlite" in db_url else {}
    return create_engine(db_url, connect_args=connect_args, echo=False)


# Primary Engine
active_db_url = DATABASE_URL
engine = create_db_engine(active_db_url)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    """Dependency helper for FastAPI endpoints."""
    global SessionLocal
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


class UserModel(Base):
    __tablename__ = "users"

    id = Column(String(50), primary_key=True, index=True)
    username = Column(String(50), unique=True, index=True, nullable=False)
    display_name = Column(String(100), nullable=False)
    email = Column(String(100), unique=True, index=True, nullable=False)
    phone = Column(String(30), nullable=True)
    country = Column(String(50), default="India")
    status_message = Column(Text, default="Building BharatConnect 🚀")
    bio = Column(Text, default="Passionate about technology and code.")
    presence = Column(String(20), default="ONLINE")
    last_seen = Column(String(50), default="Just now")
    avatar_initials = Column(String(10), default="AM")
    avatar_color = Column(String(20), default="#6367FF")
    user_avatar = Column(Text, nullable=True)
    posts_count = Column(Integer, default=0)
    followers_count = Column(String(20), default="1.2K")
    following_count = Column(Integer, default=320)
    password_hash = Column(String(255), nullable=True)
    fcm_token = Column(String(255), nullable=True)


class PostModel(Base):
    __tablename__ = "posts"

    id = Column(String(50), primary_key=True, index=True)
    author_id = Column(String(50), nullable=False)
    author_name = Column(String(100), nullable=False)
    time_ago = Column(String(50), default="Just now")
    content = Column(Text, nullable=False)
    image_title = Column(String(255), nullable=True)
    likes_count = Column(Integer, default=0)
    comments_count = Column(Integer, default=0)
    is_liked = Column(Boolean, default=False)
    user_avatar = Column(String(10), default="AM")
    avatar_color = Column(String(20), default="#6367FF")
    created_at = Column(DateTime, default=datetime.utcnow)


class ChatModel(Base):
    __tablename__ = "chats"

    id = Column(String(50), primary_key=True, index=True)
    chat_type = Column(String(20), default="INDIVIDUAL")
    title = Column(String(100), nullable=False)
    subtitle = Column(String(100), default="Online")
    pinned_message = Column(Text, nullable=True)
    unread_count = Column(Integer, default=0)
    icon = Column(String(50), default="account")
    avatar_initials = Column(String(10), default="BC")
    avatar_color = Column(String(20), default="#6367FF")
    last_message = Column(Text, nullable=True)
    last_message_time = Column(String(20), nullable=True)
    is_pinned = Column(Boolean, default=False)


class ConversationModel(Base):
    __tablename__ = "conversations"

    id = Column(String(50), primary_key=True, index=True)  # UUID
    chat_type = Column(String(20), default="INDIVIDUAL")  # INDIVIDUAL, GROUP, COMMUNITY
    title = Column(String(100), nullable=True)
    avatar_url = Column(Text, nullable=True)
    legacy_pairwise_id = Column(String(100), index=True, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class ConversationMemberModel(Base):
    __tablename__ = "conversation_members"

    id = Column(String(50), primary_key=True, index=True)
    conversation_id = Column(String(50), ForeignKey("conversations.id"), nullable=False, index=True)
    user_id = Column(String(50), ForeignKey("users.id"), nullable=False, index=True)
    role = Column(String(20), default="MEMBER")
    joined_at = Column(DateTime, default=datetime.utcnow)


class MessageModel(Base):
    __tablename__ = "messages"

    id = Column(String(50), primary_key=True, index=True)
    chat_id = Column(String(50), nullable=False, index=True)
    client_message_id = Column(String(100), unique=True, index=True, nullable=True)
    sequence = Column(Integer, default=0, index=True)
    sender_id = Column(String(50), nullable=False, index=True)
    sender_name = Column(String(100), nullable=False)
    recipient_id = Column(String(50), nullable=True)
    text = Column(Text, nullable=False)
    image_url = Column(Text, nullable=True)
    status = Column(String(20), default="SENT")  # SENT, DELIVERED, READ
    time = Column(String(20), default="10:30 AM")
    is_me = Column(Boolean, default=False)
    avatar_color = Column(String(20), default="#6367FF")
    created_at = Column(DateTime, default=datetime.utcnow)


class StoryModel(Base):
    __tablename__ = "stories"

    id = Column(String(50), primary_key=True, index=True)
    name = Column(String(50), nullable=False)
    is_user = Column(Boolean, default=False)
    avatar = Column(String(10), default="US")
    color = Column(String(20), default="#6367FF")
    has_unseen = Column(Boolean, default=True)


class MarketplaceModel(Base):
    __tablename__ = "marketplace"

    id = Column(String(50), primary_key=True, index=True)
    category = Column(String(30), nullable=False)
    title = Column(String(100), nullable=False)
    price_payout = Column(String(50), nullable=False)
    type_tag = Column(String(50), nullable=True)
    icon = Column(String(50), default="📱")
    color1 = Column(String(20), default="#6367FF")
    color2 = Column(String(20), default="#2F2FE4")


def init_db():
    """Initializes database tables with automatic fallback to SQLite on connection errors."""
    global engine, SessionLocal
    try:
        Base.metadata.create_all(bind=engine)
    except (OperationalError, Exception) as e:
        print(f"Warning: Primary database connection failed ({e}). Falling back to local SQLite database.")
        sqlite_url = f"sqlite:///{DEFAULT_SQLITE_PATH}"
        engine = create_db_engine(sqlite_url)
        SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
        Base.metadata.create_all(bind=engine)

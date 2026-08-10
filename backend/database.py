"""
BharatConnect Universal Database Abstraction Layer (backend/database.py)
Supports SQLite, PostgreSQL, MySQL, MariaDB, and any SQL database.
"""

from datetime import datetime
from sqlalchemy import create_engine, Column, String, Integer, Boolean, DateTime, ForeignKey, Text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship

from backend.config import DATABASE_URL

# Setup SQLAlchemy Engine with SQLite thread handling if SQLite
connect_args = {"check_same_thread": False} if "sqlite" in DATABASE_URL else {}
engine = create_engine(DATABASE_URL, connect_args=connect_args, echo=False)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    """Dependency helper for FastAPI endpoints."""
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
    posts_count = Column(Integer, default=0)
    followers_count = Column(String(20), default="1.2K")
    following_count = Column(Integer, default=320)
    password_hash = Column(String(255), nullable=True)


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


class MessageModel(Base):
    __tablename__ = "messages"

    id = Column(String(50), primary_key=True, index=True)
    chat_id = Column(String(50), nullable=False)
    sender_id = Column(String(50), nullable=False)
    sender_name = Column(String(100), nullable=False)
    text = Column(Text, nullable=False)
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
    """Initializes database tables."""
    Base.metadata.create_all(bind=engine)

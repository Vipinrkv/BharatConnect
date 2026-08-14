"""
BharatConnect Pydantic API Schemas (backend/schemas.py)
"""

from typing import Optional, List
from pydantic import BaseModel, EmailStr


class LoginRequest(BaseModel):
    identifier: str
    password: str


class RegisterRequest(BaseModel):
    full_name: Optional[str] = None
    display_name: Optional[str] = None
    username: str
    email: str
    phone: Optional[str] = None
    password: str
    user_avatar: Optional[str] = None


class ProfileUpdateRequest(BaseModel):
    display_name: Optional[str] = None
    username: Optional[str] = None
    bio: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    user_avatar: Optional[str] = None


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict


class UserResponse(BaseModel):
    id: str
    username: str
    display_name: str
    email: str
    bio: Optional[str] = ""
    user_avatar: Optional[str] = None
    avatar_initials: Optional[str] = "AM"
    avatar_color: Optional[str] = "#6367FF"
    followers_count: Optional[str] = "1.2K"
    following_count: Optional[int] = 320


class PostCreateRequest(BaseModel):
    content: str
    image_title: Optional[str] = None
    image_data: Optional[str] = None


class PostResponse(BaseModel):
    id: str
    author_id: str
    author_name: str
    user_avatar: str
    avatar_color: str
    content: str
    image_title: Optional[str] = None
    likes_count: int
    comments_count: int
    time_ago: str
    is_liked: bool = False


class ChatResponse(BaseModel):
    id: str
    title: str
    chat_type: str
    subtitle: Optional[str] = None
    pinned_message: Optional[str] = None
    avatar_initials: str
    avatar_color: str
    unread_count: int


class MessageCreateRequest(BaseModel):
    sender_id: Optional[str] = "u-user"
    sender_name: Optional[str] = "Member"
    recipient_id: Optional[str] = None
    text: str
    image_url: Optional[str] = None
    time: Optional[str] = None


class MessageResponse(BaseModel):
    id: str
    chat_id: str
    sender_id: str
    sender_name: str
    recipient_id: Optional[str] = None
    text: str
    image_url: Optional[str] = None
    is_me: bool = False
    time: str


class StoryResponse(BaseModel):
    id: str
    name: str
    avatar: str
    color: str
    is_user: bool


class MarketplaceItemResponse(BaseModel):
    id: str
    category: str
    title: str
    price_payout: str
    type_tag: Optional[str] = ""
    icon: Optional[str] = "📱"


class ContactMatchRequest(BaseModel):
    phone_numbers: List[str]


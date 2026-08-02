"""
BharatConnect Python REST API Routes
Provides endpoints for Users, Chats, Messages, Communities, Marketplace, & Nearby discovery.
"""

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import Optional, List
from database.db import db_engine

router = APIRouter(prefix="/api/v1")


class MessageCreate(BaseModel):
    chat_id: str
    content: str
    sender_id: Optional[str] = None


class ProfileUpdate(BaseModel):
    display_name: str
    status_message: str
    bio: str
    phone: str


@router.get("/users")
def get_users():
    return list(db_engine.users.values())


@router.get("/users/me")
def get_current_user():
    return db_engine.get_current_user()


@router.post("/users/switch/{user_id}")
def switch_user(user_id: str):
    user = db_engine.switch_user(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return {"status": "success", "user": user}


@router.get("/chats")
def get_user_chats(user_id: Optional[str] = None):
    return db_engine.get_user_chats(user_id)


@router.get("/messages/{chat_id}")
def get_chat_messages(chat_id: str):
    return db_engine.get_messages_for_chat(chat_id)


@router.post("/messages")
def send_message(payload: MessageCreate):
    msg = db_engine.send_message(payload.chat_id, payload.content, payload.sender_id)
    return {"status": "success", "message": msg}


@router.get("/communities")
def get_communities():
    return db_engine.communities


@router.post("/communities/{community_id}/toggle-join")
def toggle_community_join(community_id: str):
    comm = db_engine.toggle_community_join(community_id)
    if not comm:
        raise HTTPException(status_code=404, detail="Community not found")
    return {"status": "success", "community": comm}


@router.get("/marketplace")
def get_marketplace():
    return db_engine.marketplace


@router.get("/nearby")
def get_nearby_users():
    return db_engine.nearby


@router.put("/users/profile")
def update_profile(payload: ProfileUpdate):
    user = db_engine.update_user_profile(
        payload.display_name,
        payload.status_message,
        payload.bio,
        payload.phone
    )
    return {"status": "success", "user": user}

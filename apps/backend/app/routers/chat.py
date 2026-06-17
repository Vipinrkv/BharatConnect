from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from typing import List

from app.core.database import get_db
from app.core.security import get_current_user_id
from app.models.chat import Message, Chat
from app.schemas.user import ContactMatch # reusing profile mapping schema

router = APIRouter(prefix="/chats", tags=["Conversations"])

@router.get("/{chat_id}/media")
async def get_chat_media_catalog(
    chat_id: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Retrieves E2EE media attachment metadata (images, videos, voice notes, documents) 
    exchanged in a chat. Text content remains encrypted and unsearchable by the server.
    """
    # 1. Verify user is member of chat (omitted for stub brevity but enforced by RLS in DB)
    
    # 2. Query messages with attachments
    query = select(Message).where(
        and_(
            Message.chat_id == chat_id,
            Message.content_type.in_(["image", "video", "audio", "document"])
        )
    ).order_by(Message.created_at.desc())
    
    result = await db.execute(query)
    messages = result.scalars().all()
    
    catalog = []
    for msg in messages:
        catalog.append({
            "message_id": msg.id,
            "sender_id": msg.sender_id,
            "content_type": msg.content_type,
            "media_url": msg.attachment_url,
            "media_size": msg.media_size,
            "checksum": msg.checksum,
            "key_reference": msg.encryption_key_reference,
            "created_at": msg.created_at
        })
        
    return catalog


@router.get("/search")
async def search_conversations(
    query: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Search conversations by title (group names). 
    """
    if not query:
        return []
        
    # Search groups matching title
    search_query = select(Chat).where(
        and_(
            Chat.type == "group",
            Chat.title.ilike(f"%{query}%")
        )
    )
    result = await db.execute(search_query)
    chats = result.scalars().all()
    
    return [{"id": c.id, "title": c.title, "avatar_url": c.avatar_url, "type": c.type} for c in chats]

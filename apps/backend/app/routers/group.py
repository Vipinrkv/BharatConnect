import uuid
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, delete, update
from typing import List

from app.core.database import get_db
from app.core.security import get_current_user_id
from app.models.chat import Chat
from app.schemas.group import (
    GroupCreateRequest,
    AddMemberRequest,
    UpdateMemberRoleRequest,
    PinMessageRequest,
    PollCreateRequest,
    VoteRequest
)

router = APIRouter(prefix="/groups", tags=["Group Messaging"])

async def check_user_role(chat_id: str, user_id: str, db: AsyncSession) -> str:
    """
    Utility checking user role in a group chat. Returns 'owner', 'admin', 'moderator', 'member' or raises 403.
    """
    # Raw SQL execution mapping for membership checks
    query = """
        SELECT role FROM public.chat_members 
        WHERE chat_id = :chat_id AND profile_id = :user_id
    """
    result = await db.execute(query, {"chat_id": chat_id, "user_id": user_id})
    row = result.first()
    if not row:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not a member of this group"
        )
    return row[0]

@router.post("")
async def create_group(
    payload: GroupCreateRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Create a new group conversation, auto-registering the creator as 'owner'
    """
    chat_id = str(uuid.uuid4())
    invite_token = f"inv_{uuid.uuid4().hex[:12]}"
    
    # 1. Create chat record
    chat_query = """
        INSERT INTO public.chats (id, type, title, avatar_url, description, banner_url, invite_token)
        VALUES (:id, 'group', :title, :avatar_url, :description, :banner_url, :invite_token)
    """
    await db.execute(
        chat_query,
        {
            "id": chat_id,
            "title": payload.title,
            "avatar_url": payload.avatar_url,
            "description": payload.description,
            "banner_url": payload.banner_url,
            "invite_token": invite_token
        }
    )

    # 2. Register owner
    member_query = """
        INSERT INTO public.chat_members (chat_id, profile_id, role)
        VALUES (:chat_id, :user_id, 'owner')
    """
    await db.execute(member_query, {"chat_id": chat_id, "user_id": user_id})
    await db.commit()

    return {
        "chat_id": chat_id,
        "title": payload.title,
        "invite_token": invite_token,
        "role": "owner"
    }

@router.post("/{chat_id}/members")
async def add_member(
    chat_id: str,
    payload: AddMemberRequest,
    requester_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Add a new member to the group (Requires owner, admin, or moderator roles)
    """
    role = await check_user_role(chat_id, requester_id, db)
    if role not in ["owner", "admin", "moderator"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Insufficient permissions to add members"
        )

    # Add member
    member_query = """
        INSERT INTO public.chat_members (chat_id, profile_id, role)
        VALUES (:chat_id, :user_id, :role)
        ON CONFLICT (chat_id, profile_id) DO NOTHING
    """
    await db.execute(member_query, {"chat_id": chat_id, "user_id": payload.user_id, "role": payload.role})
    await db.commit()

    return {"message": f"Successfully added user {payload.user_id} to group"}

@router.delete("/{chat_id}/members/{user_id}")
async def remove_member(
    chat_id: str,
    user_id: str,
    requester_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Remove member from group (Requires owner, admin, or moderator roles depending on target)
    """
    req_role = await check_user_role(chat_id, requester_id, db)
    target_role = await check_user_role(chat_id, user_id, db)

    # Permission check chain
    if req_role == "moderator" and target_role in ["owner", "admin", "moderator"]:
        raise HTTPException(status_code=403, detail="Moderators cannot kick moderators, admins or owners")
    if req_role == "admin" and target_role in ["owner", "admin"]:
        raise HTTPException(status_code=403, detail="Admins cannot kick admins or owners")
    if req_role == "member":
        raise HTTPException(status_code=403, detail="Members cannot kick anyone")

    # Remove member
    remove_query = """
        DELETE FROM public.chat_members 
        WHERE chat_id = :chat_id AND profile_id = :user_id
    """
    await db.execute(remove_query, {"chat_id": chat_id, "user_id": user_id})
    await db.commit()

    return {"message": "User successfully removed from group"}

@router.post("/join/{invite_token}")
async def join_group_via_invite(
    invite_token: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Join a group channel via invite link token
    """
    # 1. Fetch group by token
    group_query = "SELECT id, title FROM public.chats WHERE invite_token = :token"
    result = await db.execute(group_query, {"token": invite_token})
    row = result.first()
    if not row:
        raise HTTPException(status_code=404, detail="Invalid or expired group invite token")

    chat_id = row[0]

    # 2. Insert member as member
    member_query = """
        INSERT INTO public.chat_members (chat_id, profile_id, role)
        VALUES (:chat_id, :user_id, 'member')
        ON CONFLICT (chat_id, profile_id) DO NOTHING
    """
    await db.execute(member_query, {"chat_id": chat_id, "user_id": user_id})
    await db.commit()

    return {"chat_id": chat_id, "title": row[1], "message": "Successfully joined group"}

@router.post("/{chat_id}/pin")
async def pin_group_message(
    chat_id: str,
    payload: PinMessageRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Pins a message inside the group channel (Requires owner, admin, or moderator)
    """
    role = await check_user_role(chat_id, user_id, db)
    if role not in ["owner", "admin", "moderator"]:
        raise HTTPException(status_code=403, detail="Insufficient permissions to pin messages")

    pin_query = """
        UPDATE public.chats 
        SET pinned_message_id = :message_id 
        WHERE id = :chat_id
    """
    await db.execute(pin_query, {"message_id": payload.message_id, "chat_id": chat_id})
    await db.commit()

    return {"message": "Message successfully pinned"}

@router.post("/{chat_id}/polls")
async def create_poll(
    chat_id: str,
    payload: PollCreateRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Create a new interactive poll in the group chat
    """
    await check_user_role(chat_id, user_id, db)
    poll_id = str(uuid.uuid4())

    # 1. Create poll
    poll_query = """
        INSERT INTO public.polls (id, chat_id, creator_id, question, is_anonymous, allow_multiple_answers)
        VALUES (:id, :chat_id, :creator_id, :question, :is_anon, :allow_mult)
    """
    await db.execute(
        poll_query,
        {
            "id": poll_id,
            "chat_id": chat_id,
            "creator_id": user_id,
            "question": payload.question,
            "is_anon": payload.is_anonymous,
            "allow_mult": payload.allow_multiple_answers
        }
    )

    # 2. Create options
    for option_text in payload.options:
        opt_id = str(uuid.uuid4())
        opt_query = "INSERT INTO public.poll_options (id, poll_id, option_text) VALUES (:id, :poll_id, :text)"
        await db.execute(opt_query, {"id": opt_id, "poll_id": poll_id, "text": option_text})

    await db.commit()

    return {"poll_id": poll_id, "question": payload.question}

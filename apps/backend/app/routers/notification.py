from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from typing import List

from app.core.database import get_db
from app.core.security import get_current_user_id
from app.schemas.notification import NotificationSettingsUpdateRequest, NotificationSettingsResponse, MarkReadRequest

router = APIRouter(prefix="/notifications", tags=["Notification Settings"])

@router.get("/settings", response_model=NotificationSettingsResponse)
async def get_notification_settings(
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Get user Quiet Hours and module mute settings
    """
    query = """
        SELECT user_id, quiet_hours_enabled, quiet_hours_start, quiet_hours_end,
               mute_groups, mute_nearby, mute_help, mute_marketplace
        FROM public.user_notification_settings
        WHERE user_id = :user_id
    """
    result = await db.execute(text(query), {"user_id": user_id})
    row = result.first()
    if not row:
        # Fallback default insertion
        await db.execute(
            text("INSERT INTO public.user_notification_settings (user_id) VALUES (:user_id) ON CONFLICT DO NOTHING"),
            {"user_id": user_id}
        )
        await db.commit()
        
        result = await db.execute(text(query), {"user_id": user_id})
        row = result.first()

    return NotificationSettingsResponse(
        user_id=str(row.user_id),
        quiet_hours_enabled=row.quiet_hours_enabled,
        quiet_hours_start=row.quiet_hours_start,
        quiet_hours_end=row.quiet_hours_end,
        mute_groups=row.mute_groups,
        mute_nearby=row.mute_nearby,
        mute_help=row.mute_help,
        mute_marketplace=row.mute_marketplace
    )


@router.put("/settings", response_model=NotificationSettingsResponse)
async def update_notification_settings(
    payload: NotificationSettingsUpdateRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Update user Quiet Hours scheduling or toggle category mute filters
    """
    # Build dynamic update SQL statement based on defined fields
    updates = []
    params = {"user_id": user_id}
    
    for key, value in payload.dict(exclude_unset=True).items():
        updates.append(f"{key} = :{key}")
        params[key] = value

    if not updates:
        return await get_notification_settings(user_id, db)

    update_query = f"""
        UPDATE public.user_notification_settings 
        SET {', '.join(updates)}
        WHERE user_id = :user_id
    """
    await db.execute(text(update_query), params)
    await db.commit()

    return await get_notification_settings(user_id, db)


@router.get("/history")
async def get_notification_history(
    is_read: Optional[bool] = None,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Fetch user notification history logs, optionally filtered by read/unread state
    """
    query_str = """
        SELECT id, title, body, category, metadata, is_read, created_at
        FROM public.notification_history
        WHERE user_id = :user_id
    """
    params = {"user_id": user_id}
    
    if is_read is not None:
        query_str += " AND is_read = :is_read"
        params["is_read"] = is_read
        
    query_str += " ORDER BY created_at DESC LIMIT 50"
    
    result = await db.execute(text(query_str), params)
    rows = result.all()
    
    return [
        {
            "id": str(r.id),
            "title": r.title,
            "body": r.body,
            "category": r.category,
            "metadata": r.metadata,
            "is_read": r.is_read,
            "created_at": r.created_at
        }
        for r in rows
    ]


@router.post("/read")
async def mark_notifications_as_read(
    payload: MarkReadRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Mark unread notifications as read
    """
    if not payload.notification_ids:
        return {"message": "No notification IDs provided"}

    update_query = """
        UPDATE public.notification_history 
        SET is_read = true 
        WHERE user_id = :user_id AND id = ANY(:ids)
    """
    await db.execute(text(update_query), {"user_id": user_id, "ids": payload.notification_ids})
    await db.commit()

    return {"message": "Notifications marked as read successfully"}

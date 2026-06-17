import uuid
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from typing import List

from app.core.database import get_db
from app.core.security import get_current_user_id
from app.schemas.help import (
    HelperRegisterRequest,
    ReviewSubmitRequest,
    BookingCreateRequest,
    BookingStatusUpdateRequest,
    HelperSearchResponse
)

router = APIRouter(prefix="/help", tags=["Verified Help"])

@router.post("/helpers", status_code=status.HTTP_201_CREATED)
async def register_as_helper(
    payload: HelperRegisterRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Register user profile as a verified helper category provider
    """
    register_query = """
        INSERT INTO public.helper_profiles (id, category, experience_years, skills_description)
        VALUES (:id, :category, :exp, :desc)
        ON CONFLICT (id) DO UPDATE 
        SET category = EXCLUDED.category,
            experience_years = EXCLUDED.experience_years,
            skills_description = EXCLUDED.skills_description
    """
    await db.execute(
        text(register_query),
        {
            "id": user_id,
            "category": payload.category,
            "exp": payload.experience_years,
            "desc": payload.skills_description
        }
    )
    await db.commit()
    return {"message": "Helper profile registered successfully"}


@router.get("/helpers/search", response_model=List[HelperSearchResponse])
async def search_helpers(
    category: str,
    latitude: float,
    longitude: float,
    radius_meters: int = 5000,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Query nearby verified helpers by category, sorted by verification level, rating, and distance
    """
    search_query = """
        SELECT 
          hp.id, p.display_name, p.avatar_url, hp.category, hp.experience_years,
          hp.verification_level, hp.average_rating, hp.review_count,
          ST_Distance(
            p.location_coordinates, 
            ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography
          ) AS distance_meters
        FROM public.helper_profiles hp
        JOIN public.profiles p ON p.id = hp.id
        WHERE hp.category = :category
          AND ST_DWithin(
                p.location_coordinates, 
                ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography, 
                :radius
              )
        ORDER BY 
          CASE hp.verification_level 
            WHEN 'gold' THEN 1 
            WHEN 'silver' THEN 2 
            WHEN 'bronze' THEN 3 
            ELSE 4 
          END ASC,
          hp.average_rating DESC,
          distance_meters ASC
    """
    result = await db.execute(
        text(search_query),
        {
            "category": category,
            "lat": latitude,
            "lng": longitude,
            "radius": radius_meters
        }
    )
    rows = result.all()
    
    helpers = []
    for r in rows:
        helpers.append(
            HelperSearchResponse(
                id=str(r.id),
                display_name=r.display_name,
                avatar_url=r.avatar_url,
                category=r.category,
                experience_years=r.experience_years,
                verification_level=r.verification_level,
                average_rating=float(r.average_rating),
                review_count=r.review_count,
                distance_meters=r.distance_meters
            )
        )
    return helpers


@router.post("/helpers/{helper_id}/reviews", status_code=status.HTTP_201_CREATED)
async def submit_helper_review(
    helper_id: str,
    payload: ReviewSubmitRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Write a recommendation review for a helper profile. Auto-recalculates ratings via DB triggers.
    """
    # Prevent self-reviewing
    if helper_id == user_id:
        raise HTTPException(status_code=400, detail="Users cannot write reviews for their own helper profiles")

    review_query = """
        INSERT INTO public.helper_reviews (helper_id, reviewer_id, rating, review_text)
        VALUES (:helper_id, :reviewer_id, :rating, :text)
        ON CONFLICT (helper_id, reviewer_id)
        DO UPDATE SET rating = EXCLUDED.rating, review_text = EXCLUDED.review_text
    """
    await db.execute(
        text(review_query),
        {
            "helper_id": helper_id,
            "reviewer_id": user_id,
            "rating": payload.rating,
            "text": payload.review_text
        }
    )
    await db.commit()
    return {"message": "Review submitted successfully"}


@router.post("/helpers/{helper_id}/bookings", status_code=status.HTTP_201_CREATED)
async def book_helper(
    helper_id: str,
    payload: BookingCreateRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Book a helper. Automatically creates a direct chat channel for integrated communication.
    """
    # 1. Check if a direct chat between helper and customer already exists
    chat_check_query = """
        SELECT c.id FROM public.chats c
        JOIN public.chat_members cm1 ON cm1.chat_id = c.id
        JOIN public.chat_members cm2 ON cm2.chat_id = c.id
        WHERE c.type = 'direct'
          AND cm1.profile_id = :cust_id
          AND cm2.profile_id = :help_id
    """
    result = await db.execute(text(chat_check_query), {"cust_id": user_id, "help_id": helper_id})
    row = result.first()
    
    chat_id = row[0] if row else None

    # 2. If no chat exists, create one
    if not chat_id:
        chat_id = str(uuid.uuid4())
        # Insert Chat
        await db.execute(
            text("INSERT INTO public.chats (id, type) VALUES (:id, 'direct')"),
            {"id": chat_id}
        )
        # Add members
        await db.execute(
            text("INSERT INTO public.chat_members (chat_id, profile_id, role) VALUES (:cid, :pid, 'member')"),
            {"cid": chat_id, "pid": user_id}
        )
        await db.execute(
            text("INSERT INTO public.chat_members (chat_id, profile_id, role) VALUES (:cid, :pid, 'member')"),
            {"cid": chat_id, "pid": helper_id}
        )

    # 3. Create Booking
    booking_id = str(uuid.uuid4())
    booking_query = """
        INSERT INTO public.helper_bookings (id, helper_id, customer_id, chat_id, booking_date)
        VALUES (:id, :helper_id, :cust_id, :chat_id, :date)
    """
    await db.execute(
        text(booking_query),
        {
            "id": booking_id,
            "helper_id": helper_id,
            "cust_id": user_id,
            "chat_id": chat_id,
            "date": payload.booking_date
        }
    )
    
    await db.commit()

    return {
        "booking_id": booking_id,
        "chat_id": chat_id,
        "status": "requested",
        "message": "Helper booking created. Direct chat initiated."
    }

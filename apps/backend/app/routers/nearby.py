import uuid
import logging
from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from typing import List

from app.core.database import get_db
from app.core.security import get_current_user_id, check_rate_limit
from app.schemas.nearby import NearbyPostCreateRequest, NearbyPostResponse, NearbyVoteRequest

router = APIRouter(prefix="/nearby", tags=["Nearby Right Now"])
logger = logging.getLogger("NearbyRouter")

@router.post("/posts", status_code=status.HTTP_201_CREATED)
async def create_nearby_post(
    request: Request,
    payload: NearbyPostCreateRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    # 1. Rate Limiting (max 3 posts per 10 minutes per user)
    await check_rate_limit(f"nearby_post:{user_id}", limit=3, window_seconds=600)

    # 2. Location Spoof Detection
    # Fetch user's previous location and update time
    user_query = """
        SELECT location_coordinates, location_updated_at 
        FROM public.profiles 
        WHERE id = :user_id
    """
    user_result = await db.execute(text(user_query), {"user_id": user_id})
    user_row = user_result.first()
    
    if user_row and user_row[0] is not None and user_row[1] is not None:
        # Compute distance and elapsed time using PostGIS
        check_query = """
            SELECT 
              ST_Distance(
                :prev_loc::geography, 
                ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography
              ) AS distance_meters,
              EXTRACT(EPOCH FROM (NOW() - :prev_time::timestamptz)) AS elapsed_seconds
        """
        check_result = await db.execute(
            text(check_query), 
            {
                "prev_loc": user_row[0], 
                "prev_time": user_row[1],
                "lat": payload.latitude,
                "lng": payload.longitude
            }
        )
        check_row = check_result.first()
        if check_row:
            dist_m = check_row[0]
            time_s = max(1.0, check_row[1]) # avoid divide by zero
            velocity = dist_m / time_s # meters per second
            
            # If velocity exceeds 33.3 m/s (~120 km/h) and time window is short, flag as spoofing
            if velocity > 33.3 and time_s < 600:
                logger.warning(f"Spoof detected for user {user_id}: Travelled {dist_m:.1f}m in {time_s:.1f}s (Speed: {velocity*3.6:.1f} km/h)")
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="Location spoofing detected. Travel speed between location updates too high."
                )

    # 3. Duplicate Detection / Spam Guard
    # Check if similar alert exists in 200m radius posted in last 15 minutes
    dup_query = """
        SELECT COUNT(*) FROM public.nearby_posts
        WHERE category = :category
          AND ST_DWithin(
                location, 
                ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography, 
                200
              )
          AND created_at > NOW() - INTERVAL '15 minutes'
    """
    dup_result = await db.execute(
        text(dup_query), 
        {
            "category": payload.category, 
            "lat": payload.latitude, 
            "lng": payload.longitude
        }
    )
    dup_count = dup_result.scalar()
    if dup_count and dup_count > 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Duplicate alert detected in your immediate vicinity. Please upvote the existing alert instead."
        )

    # 4. Insert Post
    post_id = str(uuid.uuid4())
    insert_query = """
        INSERT INTO public.nearby_posts (id, creator_id, title, feed_type, category, description, location, attachment_url)
        VALUES (
            :id, :creator, :title, :feed_type, :category, :description,
            ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography,
            :attachment
        )
    """
    await db.execute(
        text(insert_query),
        {
            "id": post_id,
            "creator": user_id,
            "title": payload.title,
            "feed_type": payload.feed_type,
            "category": payload.category,
            "description": payload.description,
            "lat": payload.latitude,
            "lng": payload.longitude,
            "attachment": payload.attachment_url
        }
    )

    # 5. Update user's profile location coordinates
    update_profile_query = """
        UPDATE public.profiles 
        SET location_coordinates = ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography,
            location_updated_at = NOW()
        WHERE id = :user_id
    """
    await db.execute(text(update_profile_query), {"lat": payload.latitude, "lng": payload.longitude, "user_id": user_id})
    await db.commit()

    return {"post_id": post_id, "message": "Alert posted successfully"}


@router.get("/feed", response_model=List[NearbyPostResponse])
async def get_nearby_feed(
    latitude: float,
    longitude: float,
    radius_meters: int = 1000, // default 1km
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Retrieves nearby feed alerts within the selected radius (500m, 1km, 5km, 10km),
    filtering out highly flagged spam posts.
    """
    if radius_meters not in [500, 1000, 5000, 10000]:
        raise HTTPException(status_code=400, detail="Invalid feed radius. Supported: 500, 1000, 5000, 10000 meters.")

    feed_query = """
        SELECT 
          id, creator_id, title, feed_type, category, description,
          ST_Y(location::geometry) AS latitude, 
          ST_X(location::geometry) AS longitude,
          attachment_url, reputation_score, created_at,
          ST_Distance(
            location, 
            ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography
          ) AS distance_meters
        FROM public.nearby_posts
        WHERE ST_DWithin(
                location, 
                ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography, 
                :radius
              )
          AND reputation_score >= -5 -- Filter out highly flagged posts
        ORDER BY created_at DESC, reputation_score DESC
    """
    result = await db.execute(
        text(feed_query), 
        {
            "lat": latitude, 
            "lng": longitude, 
            "radius": radius_meters
        }
    )
    rows = result.all()
    
    feed = []
    for r in rows:
        feed.append(
            NearbyPostResponse(
                id=str(r.id),
                creator_id=str(r.creator_id),
                title=r.title,
                feed_type=r.feed_type,
                category=r.category,
                description=r.description,
                latitude=r.latitude,
                longitude=r.longitude,
                attachment_url=r.attachment_url,
                reputation_score=r.reputation_score,
                created_at=r.created_at,
                distance_meters=r.distance_meters
            )
        )
    return feed


@router.post("/posts/{post_id}/vote")
async def vote_nearby_post(
    post_id: str,
    payload: NearbyVoteRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Upvote or flag a nearby alert. Upvotes increase visibility; flags report abuse.
    """
    vote_query = """
        INSERT INTO public.nearby_post_votes (post_id, user_id, vote_type)
        VALUES (:post_id, :user_id, :vote_type)
        ON CONFLICT (post_id, user_id) 
        DO UPDATE SET vote_type = EXCLUDED.vote_type
    """
    await db.execute(
        text(vote_query), 
        {
            "post_id": post_id, 
            "user_id": user_id, 
            "vote_type": payload.vote_type
        }
    )
    await db.commit()

    return {"message": f"Successfully registered {payload.vote_type} on post"}

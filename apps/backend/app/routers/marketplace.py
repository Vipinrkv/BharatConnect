import uuid
import logging
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from typing import List

from app.core.database import get_db
from app.core.security import get_current_user_id
from app.schemas.marketplace import RequestCreateRequest, BidCreateRequest, MarketplaceRequestSearchResponse
from app.services.fcm_notifier import fcm_notifier

router = APIRouter(prefix="/marketplace", tags=["Need It Now Marketplace"])
logger = logging.getLogger("MarketplaceRouter")

@router.post("/requests", status_code=status.HTTP_201_CREATED)
async def create_marketplace_request(
    payload: RequestCreateRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Publish an urgent hyperlocal request. Expiration default is 2 hours.
    """
    request_id = str(uuid.uuid4())
    insert_query = """
        INSERT INTO public.need_it_now_requests (id, requester_id, title, description, category, budget_estimate, location, expires_at)
        VALUES (
            :id, :requester, :title, :description, :category, :budget,
            ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography,
            NOW() + CAST(:duration || ' hours' AS INTERVAL)
        )
    """
    await db.execute(
        text(insert_query),
        {
            "id": request_id,
            "requester": user_id,
            "title": payload.title,
            "description": payload.description,
            "category": payload.category,
            "budget": payload.budget_estimate,
            "lat": payload.latitude,
            "lng": payload.longitude,
            "duration": payload.duration_hours
        }
    )
    await db.commit()
    return {"request_id": request_id, "message": "Marketplace request published successfully"}


@router.get("/requests/search", response_model=List[MarketplaceRequestSearchResponse])
async def search_marketplace_requests(
    latitude: float,
    longitude: float,
    radius_meters: int = 1000,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Query nearby active, unexpired gig demands matching radius
    """
    if radius_meters not in [500, 1000, 5000, 10000]:
        raise HTTPException(status_code=400, detail="Invalid search radius. Supported: 500, 1000, 5000, 10000 meters.")

    search_query = """
        SELECT 
          id, requester_id, title, description, category, budget_estimate,
          ST_Y(location::geometry) AS latitude, 
          ST_X(location::geometry) AS longitude,
          status, expires_at, created_at,
          ST_Distance(
            location, 
            ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography
          ) AS distance_meters
        FROM public.need_it_now_requests
        WHERE status = 'active'
          AND expires_at > NOW()
          AND ST_DWithin(
                location, 
                ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography, 
                :radius
              )
        ORDER BY distance_meters ASC, created_at DESC
    """
    result = await db.execute(
        text(search_query),
        {
            "lat": latitude,
            "lng": longitude,
            "radius": radius_meters
        }
    )
    rows = result.all()
    
    requests = []
    for r in rows:
        requests.append(
            MarketplaceRequestSearchResponse(
                id=str(r.id),
                requester_id=str(r.requester_id),
                title=r.title,
                description=r.description,
                category=r.category,
                budget_estimate=float(r.budget_estimate) if r.budget_estimate else None,
                latitude=r.latitude,
                longitude=r.longitude,
                status=r.status,
                expires_at=r.expires_at,
                created_at=r.created_at,
                distance_meters=r.distance_meters
            )
        )
    return requests


@router.post("/requests/{request_id}/bids", status_code=status.HTTP_201_CREATED)
async def submit_gig_bid(
    request_id: str,
    payload: BidCreateRequest,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Submit a service bid for an active gig
    """
    # Verify request is active and unexpired
    req_check = """
        SELECT requester_id, status, expires_at FROM public.need_it_now_requests 
        WHERE id = :req_id
    """
    result = await db.execute(text(req_check), {"req_id": request_id})
    row = result.first()
    if not row:
        raise HTTPException(status_code=404, detail="Marketplace request not found")
    if row[1] != "active" or row[2] < db.execute(text("SELECT NOW()")).scalar(): # expired
        raise HTTPException(status_code=400, detail="This marketplace request is no longer active")
    if row[0] == user_id:
        raise HTTPException(status_code=400, detail="You cannot place bids on your own request")

    bid_id = str(uuid.uuid4())
    bid_query = """
        INSERT INTO public.need_it_now_bids (id, request_id, bidder_id, bid_amount, message)
        VALUES (:id, :req_id, :bidder, :amount, :msg)
    """
    await db.execute(
        text(bid_query),
        {
            "id": bid_id,
            "req_id": request_id,
            "bidder": user_id,
            "amount": payload.bid_amount,
            "msg": payload.message
        }
    )
    await db.commit()

    return {"bid_id": bid_id, "message": "Bid submitted successfully"}


@router.post("/bids/{bid_id}/accept")
async def accept_gig_bid(
    bid_id: str,
    user_id: str = Depends(get_current_user_id),
    db: AsyncSession = Depends(get_db)
):
    """
    Accept a service provider's bid. Auto-establishes direct chat and updates other bids.
    """
    # 1. Fetch bid details and verify requester is the owner of the request
    bid_query = """
        SELECT b.id, b.request_id, b.bidder_id, b.bid_amount, r.requester_id, r.title, p.phone
        FROM public.need_it_now_bids b
        JOIN public.need_it_now_requests r ON r.id = b.request_id
        JOIN public.profiles p ON p.id = b.bidder_id
        WHERE b.id = :bid_id
    """
    result = await db.execute(text(bid_query), {"bid_id": bid_id})
    row = result.first()
    if not row:
        raise HTTPException(status_code=404, detail="Bid not found")
        
    request_id = row[1]
    bidder_id = row[2]
    bid_amount = row[3]
    requester_id = row[4]
    request_title = row[5]

    if requester_id != user_id:
        raise HTTPException(status_code=403, detail="Only the publisher can accept bids")

    # 2. Check if direct chat already exists, otherwise create it
    chat_check_query = """
        SELECT c.id FROM public.chats c
        JOIN public.chat_members cm1 ON cm1.chat_id = c.id
        JOIN public.chat_members cm2 ON cm2.chat_id = c.id
        WHERE c.type = 'direct'
          AND cm1.profile_id = :req_id
          AND cm2.profile_id = :bidder_id
    """
    chat_result = await db.execute(text(chat_check_query), {"req_id": user_id, "bidder_id": bidder_id})
    chat_row = chat_result.first()
    chat_id = chat_row[0] if chat_row else None

    if not chat_id:
        chat_id = str(uuid.uuid4())
        # Insert Chat
        await db.execute(text("INSERT INTO public.chats (id, type) VALUES (:id, 'direct')"), {"id": chat_id})
        # Add members
        await db.execute(text("INSERT INTO public.chat_members (chat_id, profile_id, role) VALUES (:cid, :pid, 'member')"), {"cid": chat_id, "pid": user_id})
        await db.execute(text("INSERT INTO public.chat_members (chat_id, profile_id, role) VALUES (:cid, :pid, 'member')"), {"cid": chat_id, "pid": bidder_id})

    # 3. Update bid status to 'accepted' (Trigger handles other bids rejection and request status change)
    update_bid = """
        UPDATE public.need_it_now_bids 
        SET status = 'accepted', chat_id = :chat_id 
        WHERE id = :bid_id
    """
    await db.execute(text(update_bid), {"chat_id": chat_id, "bid_id": bid_id})
    await db.commit()

    # 4. Dispatch FCM Push notification to bidder
    # Fetch bidder token (mock helper fetches token from DB in production)
    bidder_token = "mock_device_token"
    fcm_notifier.send_push_notification(
        token=bidder_token,
        title="Bid Accepted! 🎉",
        body=f"Your bid of ₹{bid_amount:.0f} on '{request_title}' has been accepted. Tap to open chat.",
        data={"chat_id": chat_id, "type": "bid_acceptance"}
    )

    return {
        "status": "fulfilled",
        "chat_id": chat_id,
        "message": "Bid accepted. Direct chat channel established."
    }

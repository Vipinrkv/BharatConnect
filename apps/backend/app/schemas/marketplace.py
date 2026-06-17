from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime

class RequestCreateRequest(BaseModel):
    title: str = Field(..., max_length=150)
    description: str
    category: str = Field(..., pattern="^(Emergency|Transport|Borrow|Tools|Tickets|Services)$")
    budget_estimate: Optional[float] = Field(None, ge=0.0)
    latitude: float = Field(..., ge=-90.0, le=90.0)
    longitude: float = Field(..., ge=-180.0, le=180.0)
    duration_hours: Optional[int] = Field(2, ge=1, le=24, description="Expiry duration in hours")

class BidCreateRequest(BaseModel):
    bid_amount: float = Field(..., ge=0.0)
    message: Optional[str] = None

class MarketplaceRequestSearchResponse(BaseModel):
    id: str
    requester_id: str
    title: str
    description: str
    category: str
    budget_estimate: Optional[float]
    latitude: float
    longitude: float
    status: str
    expires_at: datetime
    created_at: datetime
    distance_meters: float

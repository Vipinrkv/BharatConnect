from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class NearbyPostCreateRequest(BaseModel):
    title: str = Field(..., max_length=150)
    feed_type: str = Field(..., pattern="^(alert|discussion|observation)$")
    category: str = Field(..., description="Category (traffic, power_cut, police, water_supply, etc.)")
    description: str
    latitude: float = Field(..., ge=-90.0, le=90.0)
    longitude: float = Field(..., ge=-180.0, le=180.0)
    attachment_url: Optional[str] = None

class NearbyPostResponse(BaseModel):
    id: str
    creator_id: str
    title: str
    feed_type: str
    category: str
    description: str
    latitude: float
    longitude: float
    attachment_url: Optional[str]
    reputation_score: int
    created_at: datetime
    distance_meters: float

class NearbyVoteRequest(BaseModel):
    vote_type: str = Field(..., pattern="^(upvote|flag)$")

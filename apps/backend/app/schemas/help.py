from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import date, datetime

class HelperRegisterRequest(BaseModel):
    category: str = Field(..., pattern="^(Maid|Cook|Tutor|Driver|Electrician|Plumber)$")
    experience_years: int = Field(0, ge=0)
    skills_description: Optional[str] = None

class ReviewSubmitRequest(BaseModel):
    rating: int = Field(..., ge=1, le=5)
    review_text: Optional[str] = None

class BookingCreateRequest(BaseModel):
    booking_date: date

class BookingStatusUpdateRequest(BaseModel):
    status: str = Field(..., pattern="^(confirmed|completed|cancelled)$")

class HelperSearchResponse(BaseModel):
    id: str
    display_name: Optional[str]
    avatar_url: Optional[str]
    category: str
    experience_years: int
    verification_level: str
    average_rating: float
    review_count: int
    distance_meters: float

from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime

class GroupCreateRequest(BaseModel):
    title: str = Field(..., max_length=100)
    description: Optional[str] = None
    avatar_url: Optional[str] = None
    banner_url: Optional[str] = None

class AddMemberRequest(BaseModel):
    user_id: str
    role: Optional[str] = "member"

class UpdateMemberRoleRequest(BaseModel):
    role: str = Field(..., pattern="^(owner|admin|moderator|member)$")

class PinMessageRequest(BaseModel):
    message_id: str

class PollOptionRequest(BaseModel):
    option_text: str

class PollCreateRequest(BaseModel):
    question: str
    options: List[str] = Field(..., min_items=2, max_items=10)
    is_anonymous: Optional[bool] = True
    allow_multiple_answers: Optional[bool] = False

class VoteRequest(BaseModel):
    option_id: str

from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime

class UserBlockRequest(BaseModel):
    is_blocked: bool

class VerificationReviewRequest(BaseModel):
    status: str = Field(..., pattern="^(approved|rejected)$")
    rejection_reason: Optional[str] = None

class ReportResolveRequest(BaseModel):
    status: str = Field(..., pattern="^(resolved|dismissed)$")
    action_taken: Optional[str] = None

class AdminDashboardStatsResponse(BaseModel):
    total_users: int
    active_sessions: int
    pending_verifications: int
    unresolved_reports: int
    spoof_alerts_count: int

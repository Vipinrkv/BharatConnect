from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import time

class NotificationSettingsUpdateRequest(BaseModel):
    quiet_hours_enabled: Optional[bool] = None
    quiet_hours_start: Optional[time] = None
    quiet_hours_end: Optional[time] = None
    mute_groups: Optional[bool] = None
    mute_nearby: Optional[bool] = None
    mute_help: Optional[bool] = None
    mute_marketplace: Optional[bool] = None

class NotificationSettingsResponse(BaseModel):
    user_id: str
    quiet_hours_enabled: bool
    quiet_hours_start: time
    quiet_hours_end: time
    mute_groups: bool
    mute_nearby: bool
    mute_help: bool
    mute_marketplace: bool

    class Config:
        from_attributes = True

class MarkReadRequest(BaseModel):
    notification_ids: List[str]

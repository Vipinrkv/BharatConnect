from pydantic import BaseModel, Field, EmailStr
from typing import Optional, List
from datetime import datetime

class EmailLoginRequest(BaseModel):
  email: EmailStr
  password: str
  device_id: str
  device_name: Optional[str] = "Unknown Device"

class GoogleLoginRequest(BaseModel):
  id_token: str
  device_id: str
  device_name: Optional[str] = "Unknown Device"

class TokenResponse(BaseModel):
  access_token: str
  refresh_token: str
  token_type: str = "bearer"

class TokenRefreshRequest(BaseModel):
  refresh_token: str

class UserSessionResponse(BaseModel):
  id: str
  device_id: str
  device_name: Optional[str]
  ip_address: Optional[str]
  is_active: bool
  created_at: datetime
  last_active_at: datetime

  class Config:
    from_attributes = True

class ContactDiscoveryRequest(BaseModel):
  phone_hashes: List[str] = Field(..., description="List of SHA-256 phone hashes to match")

class ContactMatch(BaseModel):
  id: str
  display_name: Optional[str]
  avatar_url: Optional[str]
  username: Optional[str]

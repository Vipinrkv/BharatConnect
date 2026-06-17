from sqlalchemy import Column, String, Boolean, Numeric, DateTime
from sqlalchemy.sql import func
from geoalchemy2 import Geometry
from app.core.database import Base

class Profile(Base):
    __tablename__ = "profiles"

    id = Column(String, primary_key=True, index=True)
    phone = Column(String, unique=True, nullable=False, index=True)
    display_name = Column(String, nullable=True)
    
    # Media Columns
    avatar_url = Column(String, nullable=True)
    avatar_thumbnail_url = Column(String, nullable=True)
    banner_url = Column(String, nullable=True)
    banner_thumbnail_url = Column(String, nullable=True)
    
    avatar_updated_at = Column(DateTime(timezone=True), nullable=True)
    banner_updated_at = Column(DateTime(timezone=True), nullable=True)

    # Location & Vetting
    location_coordinates = Column(Geometry(geometry_type='POINT', srid=4326), nullable=True)
    location_updated_at = Column(DateTime(timezone=True), nullable=True)
    is_verified_helper = Column(Boolean, default=False)
    helper_trust_score = Column(Numeric(3, 2), default=5.00)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

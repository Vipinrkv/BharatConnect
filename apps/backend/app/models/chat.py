from sqlalchemy import Column, String, DateTime, ForeignKey
from sqlalchemy.sql import func
from geoalchemy2 import Geometry
from app.core.database import Base

class Chat(Base):
    __tablename__ = "chats"

    id = Column(String, primary_key=True, index=True)
    type = Column(String, nullable=False) # 'direct' or 'group'
    title = Column(String, nullable=True)
    avatar_url = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

class Message(Base):
    __tablename__ = "messages"

    id = Column(String, primary_key=True, index=True)
    chat_id = Column(String, ForeignKey("chats.id", ondelete="CASCADE"), nullable=False)
    sender_id = Column(String, ForeignKey("profiles.id", ondelete="SET NULL"), nullable=True)
    
    content_type = Column(String, nullable=False) # 'text', 'image', 'video', 'audio', 'location'
    text_content = Column(String, nullable=True)
    attachment_url = Column(String, nullable=True)
    location_content = Column(Geometry(geometry_type='POINT', srid=4326), nullable=True)
    
    # E2EE Encrypted Media Specifications
    media_size = Column(String, nullable=True) # size in bytes
    checksum = Column(String(64), nullable=True) # SHA-256 hash checksum
    encryption_key_reference = Column(String, nullable=True) # Serialized IV/Key info

    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

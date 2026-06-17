import logging
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.core.websocket import manager
from app.routers.auth import router as auth_router
from app.routers.chat import router as chat_router
from app.routers.group import router as group_router
from app.routers.nearby import router as nearby_router
from app.routers.help import router as help_router
from app.routers.marketplace import router as marketplace_router
from app.routers.notification import router as notification_router
from app.routers.admin import router as admin_router

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("BharatConnect")

app = FastAPI(
    title="BharatConnect Backend API",
    description="Production-ready communication layer API for BharatConnect PWA/Android clients.",
    version="1.0.0"
)

app.include_router(auth_router, prefix="/api/v1")
app.include_router(chat_router, prefix="/api/v1")
app.include_router(group_router, prefix="/api/v1")
app.include_router(nearby_router, prefix="/api/v1")
app.include_router(help_router, prefix="/api/v1")
app.include_router(marketplace_router, prefix="/api/v1")
app.include_router(notification_router, prefix="/api/v1")
app.include_router(admin_router, prefix="/api/v1")

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health_check():
    return {"status": "healthy", "environment": settings.ENVIRONMENT}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket, token: str = None):
    """
    Centralized WebSocket channel for real-time messages, presence updates, and typing alerts.
    """
    # 1. Resolve JWT Token query parameter
    # 2. Accept connection and register with connection manager
    # 3. Enter event routing loop
    user_id = token or "anonymous" # Placeholder logic
    await manager.connect(websocket, user_id)
    logger.info(f"Client connected: {user_id}")
    
    try {
        while True:
            data = await websocket.receive_json()
            # Publish event packet to Redis Pub/Sub for cross-server routing
            await manager.broadcast_event_packet(user_id, data)
    }
    except WebSocketDisconnect:
        manager.disconnect(websocket, user_id)
        logger.info(f"Client disconnected: {user_id}")

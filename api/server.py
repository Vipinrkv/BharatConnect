"""
BharatConnect Python FastAPI Server & Gateway
"""

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from api.routes import router as api_router
from api.ws import ws_manager
from database.db import db_engine

app = FastAPI(
    title="BharatConnect 🇮🇳 API Gateway",
    description="High-Throughput Realtime Text Messaging & Developer Hub API",
    version="2.0.0"
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include REST Routes
app.include_router(api_router)


@app.get("/")
def root():
    return {
        "status": "ONLINE",
        "platform": "BharatConnect 🇮🇳 (Python Edition)",
        "version": "2.0.0",
        "storage_engine": "O(1) Hash Map Indexing",
        "docs_url": "/docs"
    }


@app.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: str):
    await ws_manager.connect(user_id, websocket)
    try:
        while True:
            data = await websocket.receive_json()
            event_type = data.get("type")
            payload = data.get("payload", {})

            if event_type == "message.send":
                chat_id = payload.get("chat_id")
                content = payload.get("content")
                msg = db_engine.send_message(chat_id, content, sender_id=user_id)

                chat = db_engine.chats.get(chat_id, {})
                participants = chat.get("participants", [user_id])

                await ws_manager.broadcast_to_chat(
                    participants,
                    {"type": "message.received", "payload": msg}
                )

            elif event_type == "typing.start":
                chat_id = payload.get("chat_id")
                chat = db_engine.chats.get(chat_id, {})
                participants = [p for p in chat.get("participants", []) if p != user_id]
                await ws_manager.broadcast_to_chat(
                    participants,
                    {"type": "typing.indicator", "payload": {"user_id": user_id, "chat_id": chat_id, "is_typing": True}}
                )

    except WebSocketDisconnect:
        ws_manager.disconnect(user_id, websocket)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("api.server:app", host="0.0.0.0", port=5000, reload=True)

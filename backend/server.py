import os
import time
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="BharatConnect Cloud Service",
    description="BharatConnect API, Health Check & Webhook Dispatcher",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {
        "app": "BharatConnect 🇮🇳",
        "status": "online",
        "version": "2.0.0-native",
        "description": "Pure Native Jetpack Compose + Supabase Serverless Architecture",
        "docs": "/docs",
        "health": "/health"
    }

@app.get("/health")
def health():
    return {
        "status": "healthy",
        "timestamp": int(time.time()),
        "service": "BharatConnect Backend & Webhook Service"
    }

@app.get("/api/info")
def info():
    return {
        "architecture": "Serverless Supabase + Native Android",
        "storage": "Cloudinary Unsigned CDN",
        "auth": "Supabase Auth (OTP / SMTP / Deep Linking)",
        "database": "Supabase PostgreSQL with Realtime Channels"
    }

# Mock high-throughput endpoints for load testing and local server mode
@app.get("/rest/v1/conversations")
def get_conversations(limit: int = 20):
    return [
        {
            "id": f"conv_{i}",
            "type": "direct",
            "title": f"User {i}",
            "last_message": "ENC:iiLNU8yyOVLpKK9TAvW",
            "last_message_time": "2026-10-31T21:40:00Z"
        }
        for i in range(min(limit, 50))
    ]

@app.get("/rest/v1/messages")
def get_messages(limit: int = 50):
    return [
        {
            "id": f"msg_{i}",
            "conversation_id": "conv_1",
            "sender_id": f"user_{i}",
            "sender_name": f"User {i}",
            "content": "ENC:iiLNU8yyOVLpKK9TAvW",
            "status": "delivered",
            "created_at": "2026-10-31T21:40:00Z"
        }
        for i in range(min(limit, 100))
    ]

@app.post("/rest/v1/messages")
def post_message(payload: dict):
    return {
        "status": "sent",
        "id": payload.get("id", "msg_gen"),
        "received_at": time.time()
    }

@app.get("/rest/v1/posts")
def get_posts(limit: int = 20):
    return [
        {
            "id": f"post_{i}",
            "author_name": f"Citizen {i}",
            "content": "Connecting India with BharatConnect!",
            "likes_count": i * 12,
            "comments_count": i * 2,
            "created_at": "2026-10-31T21:30:00Z"
        }
        for i in range(min(limit, 50))
    ]

@app.get("/rest/v1/profiles")
def get_profiles(limit: int = 20):
    return [
        {
            "id": f"user_{i}",
            "username": f"user_{i}",
            "full_name": f"Bharat User {i}",
            "is_online": (i % 2 == 0)
        }
        for i in range(min(limit, 50))
    ]

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="127.0.0.1", port=port)

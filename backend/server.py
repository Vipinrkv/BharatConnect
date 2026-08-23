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

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("backend.server:app", host="0.0.0.0", port=port, reload=False)

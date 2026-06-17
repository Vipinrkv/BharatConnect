import redis.asyncio as aioredis
from app.config import settings

class RedisManager:
    def __init__(self):
        self.client = None

    def initialize(self):
        """
        Initialize the async Redis client pool
        """
        self.client = aioredis.from_url(
            settings.REDIS_URL,
            encoding="utf-8",
            decode_responses=True
        )

    async def get_client(self) -> aioredis.Redis:
        if not self.client:
            self.initialize()
        return self.client

    async def publish_event(self, channel: str, message: str):
        client = await self.get_client()
        await client.publish(channel, message)

    async def set_presence(self, user_id: str, status: str, expiry_seconds: int = 60):
        client = await self.get_client()
        await client.set(f"presence:{user_id}", status, ex=expiry_seconds)

    async def get_presence(self, user_id: str) -> str:
        client = await self.get_client()
        return await client.get(f"presence:{user_id}") or "offline"

redis_manager = RedisManager()

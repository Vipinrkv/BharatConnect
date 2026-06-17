import json
import logging
import asyncio
from typing import Dict, List
from fastapi import WebSocket
from app.core.redis import redis_manager

logger = logging.getLogger("WebSocketManager")

class ConnectionManager:
    def __init__(self):
        # Maps user_id -> list of active websockets on this instance
        self.active_connections: Dict[str, List[WebSocket]] = {}
        # Single shared background task for pattern subscription
        self.global_pubsub_task: Optional[asyncio.Task] = None
        self.pubsub = None

    async def connect(self, websocket: WebSocket, user_id: str):
        await websocket.accept()
        if user_id not in self.active_connections:
            self.active_connections[user_id] = []
        self.active_connections[user_id].append(websocket)
        
        # Set user presence as online in Redis
        await redis_manager.set_presence(user_id, "online", expiry_seconds=300)
        await self.broadcast_presence(user_id, "online")

        # Lazily start the single shared Redis pub/sub listener on the first connection
        if not self.global_pubsub_task or self.global_pubsub_task.done():
            self.global_pubsub_task = asyncio.create_task(self._start_global_pubsub_listener())

    def disconnect(self, websocket: WebSocket, user_id: str):
        if user_id in self.active_connections:
            self.active_connections[user_id].remove(websocket)
            if not self.active_connections[user_id]:
                del self.active_connections[user_id]
                # Trigger offline presence state update
                asyncio.create_task(self._handle_offline_presence(user_id))

    async def _handle_offline_presence(self, user_id: str):
        # Grace period of 10s to prevent flickering
        await asyncio.sleep(10)
        if user_id not in self.active_connections:
            await redis_manager.set_presence(user_id, "offline")
            await self.broadcast_presence(user_id, "offline")

    async def broadcast_presence(self, user_id: str, status: str):
        event = {
            "event_type": "presence_update",
            "payload": {
                "profile_id": user_id,
                "status": status
            }
        }
        await redis_manager.publish_event("global_presence", json.dumps(event))

    async def broadcast_event_packet(self, sender_id: str, packet: dict):
        """
        Routes message packets, typing tags, and receipts across server channels
        """
        from app.core.database import AsyncSessionLocal

        event_type = packet.get("event_type")
        payload = packet.get("payload", {})
        recipient_id = payload.get("recipient_id") or payload.get("chat_id")

        if not recipient_id:
            return

        # Enforce sender_id into payload to prevent spoofing
        payload["sender_id"] = sender_id
        packet["payload"] = payload

        # Check group membership routing
        async with AsyncSessionLocal() as session:
            try:
                # Query members of the chat_id
                query = "SELECT profile_id FROM public.chat_members WHERE chat_id = :chat_id"
                result = await session.execute(text(query), {"chat_id": recipient_id})
                members = [str(row[0]) for row in result.all()]
            except Exception as e:
                logger.error(f"Error querying members for chat {recipient_id}: {e}")
                members = []

        if members:
            # Publish to each member's private channel
            for member_id in members:
                if member_id == sender_id and event_type == "message_send":
                    continue
                channel = f"user_events:{member_id}"
                await redis_manager.publish_event(channel, json.dumps(packet))
        else:
            # Direct chat (recipient_id is peer's user_id)
            channel = f"user_events:{recipient_id}"
            await redis_manager.publish_event(channel, json.dumps(packet))

    async def _start_global_pubsub_listener(self):
        """
        Production Hardened Redis Listener: Single process-level subscription connection
        Subscribes to 'user_events:*' using psubscribe to handle all routed traffic.
        """
        try:
            client = await redis_manager.get_client()
            self.pubsub = client.pubsub()
            await self.pubsub.psubscribe("user_events:*")
            
            logger.info("Shared Process-Level Redis Pub/Sub pattern listener started successfully.")
            
            while True:
                # Poll pattern channel
                message = await self.pubsub.get_message(ignore_subscribe_messages=True, timeout=1.0)
                if message:
                    channel = message["channel"] # e.g. "user_events:user-uuid"
                    user_id = channel.replace("user_events:", "")
                    data = json.loads(message["data"])
                    
                    # Fetch active local sockets for this user ID on this FastAPI instance
                    websockets = self.active_connections.get(user_id, [])
                    for ws in websockets:
                        try:
                            await ws.send_json(data)
                        except Exception:
                            # Socket failed, skip. Disconnect handler will clean up.
                            pass
                await asyncio.sleep(0.01)
        except asyncio.CancelledError:
            logger.info("Global Redis Pub/Sub listener task stopped.")
            if self.pubsub:
                await self.pubsub.punsubscribe("user_events:*")
        except Exception as e:
            logger.error(f"Critical exception in global Redis listener: {e}")
            # Schedule restart in 5 seconds
            await asyncio.sleep(5)
            self.global_pubsub_task = asyncio.create_task(self._start_global_pubsub_listener())

manager = ConnectionManager()

"""
BharatConnect Python WebSocket Connection Manager
Provides real-time sub-50ms message broadcasting and presence synchronization.
"""

from typing import List, Dict
import json


class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[str, List[object]] = {}

    async def connect(self, user_id: str, websocket):
        await websocket.accept()
        if user_id not in self.active_connections:
            self.active_connections[user_id] = []
        self.active_connections[user_id].append(websocket)
        print(f"[WebSocket] User {user_id} connected.")

    def disconnect(self, user_id: str, websocket):
        if user_id in self.active_connections:
            if websocket in self.active_connections[user_id]:
                self.active_connections[user_id].remove(websocket)
            if not self.active_connections[user_id]:
                del self.active_connections[user_id]
        print(f"[WebSocket] User {user_id} disconnected.")

    async def broadcast_to_user(self, user_id: str, message: dict):
        if user_id in self.active_connections:
            payload = json.dumps(message)
            for connection in self.active_connections[user_id]:
                await connection.send_text(payload)

    async def broadcast_to_chat(self, participant_ids: list, message: dict):
        payload = json.dumps(message)
        for user_id in participant_ids:
            if user_id in self.active_connections:
                for connection in self.active_connections[user_id]:
                    await connection.send_text(payload)


ws_manager = ConnectionManager()

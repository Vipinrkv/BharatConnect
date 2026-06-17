import asyncio
import json
import time
import pytest
from app.core.redis import redis_manager
from app.core.websocket import manager

@pytest.mark.asyncio
async def test_redis_pubsub_broadcast_latency():
    """
    Real-time Test: Measures Redis Pub/Sub event distribution latency
    """
    await redis_manager.initialize()
    client = await redis_manager.get_client()
    
    pubsub = client.pubsub()
    channel = "user_events:test_recipient"
    await pubsub.subscribe(channel)
    
    # Send event packet with high-precision timestamp
    send_time = time.perf_counter()
    packet = {
        "event_type": "message_receive",
        "payload": {
            "text": "latency_check",
            "timestamp": send_time
        }
    }
    
    await redis_manager.publish_event(channel, json.dumps(packet))
    
    # Read packet back from Redis
    message = None
    for _ in range(50): # Poll up to 50 times
        message = await pubsub.get_message(ignore_subscribe_messages=True, timeout=0.1)
        if message:
            break
        await asyncio.sleep(0.01)
        
    assert message is not None
    receive_time = time.perf_counter()
    
    latency_ms = (receive_time - send_time) * 1000
    print(f"\nRedis Pub/Sub Latency: {latency_ms:.3f} ms")
    assert latency_ms < 100.0 # Delivery should complete under 100ms on localhost

@pytest.mark.asyncio
async def test_websocket_manager_registration():
    """
    Real-time Test: Validates WebSocket connection manager registers sessions 
    and sets online presence caches correctly
    """
    await redis_manager.initialize()
    user_id = "test-session-user"
    
    class MockWebSocket:
        def __init__(self):
            self.accepted = False
            self.closed = False
            
        async def accept(self):
            self.accepted = True
            
        async def send_json(self, data):
            pass
            
    mock_ws = MockWebSocket()
    
    # 1. Connect
    await manager.connect(mock_ws, user_id)
    assert user_id in manager.active_connections
    assert mock_ws in manager.active_connections[user_id]
    
    # Check presence cache
    presence = await redis_manager.get_presence(user_id)
    assert presence == "online"
    
    # 2. Disconnect
    manager.disconnect(mock_ws, user_id)
    assert user_id not in manager.active_connections
    
    # Cleanup tasks
    if user_id in manager.pubsub_tasks:
        manager.pubsub_tasks[user_id].cancel()

import os
import sys
import time
import json
import asyncio
import threading

sys.path.insert(0, os.path.abspath(os.path.dirname(__file__) + "/.."))

def test_websocket_live_stream():
    print("============================================================")
    print("       TESTING WEBSOCKET INSTANT LIVE CHAT BROADCAST        ")
    print("============================================================")

    import websockets

    async def connect_and_listen():
        uri = "ws://127.0.0.1:8000/ws/stream"
        try:
            async with websockets.connect(uri) as websocket:
                print("[WS Client] Connected to real-time stream!")
                while True:
                    msg = await websocket.recv()
                    print(f"[WS Client] Received live event: {msg}")
        except Exception as e:
            print(f"[WS Client] Error: {e}")

    # Run listener in background thread
    t = threading.Thread(target=lambda: asyncio.run(connect_and_listen()), daemon=True)
    t.start()
    time.sleep(1)

    print("[*] WS Listener started. Done test setup.")

if __name__ == "__main__":
    test_websocket_live_stream()

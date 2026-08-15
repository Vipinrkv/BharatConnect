import os
import sys
import time
import threading

sys.path.insert(0, os.path.abspath(os.path.dirname(__file__) + "/.."))

def test_instant_ws():
    print("============================================================")
    print("      TESTING INSTANT REAL-TIME WEBSOCKET LISTENER          ")
    print("============================================================")

    from app.sync_engine import HybridSyncEngine

    sync = HybridSyncEngine()
    
    received_events = []

    def on_live_msg(data):
        print(f"[TEST CALLBACK] Instant message received! Data: {data}")
        received_events.append(data)

    test_chat_id = "c-instant-test"
    sync.register_chat_listener(test_chat_id, on_live_msg)

    print(f"[*] Registered listener for chat_id={test_chat_id}")
    time.sleep(1)

    # Simulate message notification trigger
    sync.notify_chat_listeners(test_chat_id, {"text": "Instant Hi!", "sender_id": "u-user-2"})

    time.sleep(0.5)
    assert len(received_events) == 1, "Should have received 1 instant message event"
    print(f"[+] Success! Received instant event: {received_events[0]}")

    print("============================================================")
    print("      INSTANT REAL-TIME WEBSOCKET TEST PASSED CLEANLY!      ")
    print("============================================================")

if __name__ == "__main__":
    test_instant_ws()

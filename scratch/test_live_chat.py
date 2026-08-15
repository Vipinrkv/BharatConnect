import os
import sys
import time
import threading

sys.path.insert(0, os.path.abspath(os.path.dirname(__file__) + "/.."))


def test_live_chat_flow():
    print("============================================================")
    print("       TESTING BHARATCONNECT LIVE CHAT SYNCHRONIZATION      ")
    print("============================================================")

    # 1. Test local database message storage
    from database.database import SQLiteDatabaseEngine
    db = SQLiteDatabaseEngine()
    
    test_chat_id = "test-live-chat-101"
    
    print("\n--- 1. Testing Local Message Insertion ---")
    msg1 = db.send_chat_message(test_chat_id, "Hello from Sender!")
    print(f"Sent Message: {msg1}")

    local_msgs = db.get_chat_messages(test_chat_id)
    print(f"Retrieved Messages Count: {len(local_msgs)}")
    assert len(local_msgs) >= 1, "Local message count should be at least 1"

    # 2. Test Sync Engine Online Fetching & Merging
    from app.sync_engine import sync_engine
    
    print("\n--- 2. Testing Sync Engine Messages Interface ---")
    sync_msgs = sync_engine.get_chat_messages(test_chat_id)
    print(f"Sync Engine Messages Count: {len(sync_msgs)}")
    assert len(sync_msgs) >= 1, "Sync engine message count should be at least 1"

    print("\n============================================================")
    print("       LIVE CHAT SYNCHRONIZATION TEST COMPLETED SUCCESSFULLY!  ")
    print("============================================================")

if __name__ == "__main__":
    test_live_chat_flow()

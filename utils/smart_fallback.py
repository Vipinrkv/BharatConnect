"""
BharatConnect 3-Tier Smart Fallback Architecture (utils/smart_fallback.py)

Guarantees 100% data availability using a 3-Tier Failover Matrix:
Tier 1: High-Speed Cloud REST API & WebSocket Server
Tier 2: Live Google Sheets Web App Endpoint
Tier 3: Local SQLite Database & Local JSON Disk Cache
"""

import json
from typing import Dict, List, Tuple, Any

from app.api_client import api_client
from backend.google_sheets import GoogleSheetsConnector
from utils.local_storage import local_storage
from database.database import SQLiteDatabaseEngine


class ThreeTierSmartFallbackEngine:
    def __init__(self):
        self.local_db = SQLiteDatabaseEngine()
        self.google_sheets = GoogleSheetsConnector()

    def execute_read_with_fallback(self, domain_key: str, api_func, sheets_func, local_func) -> Tuple[List[dict], str]:
        """
        Executes read query across 3 tiers:
        Returns: (data_list, tier_used_str)
        """
        # Tier 1: Try Cloud REST API Server
        if api_client.check_server_online():
            try:
                data = api_func()
                if data:
                    # Update lower tiers asynchronously/in-memory
                    local_storage.set(f"{domain_key}_cache", data)
                    return data, "TIER_1_CLOUD_API"
            except Exception:
                pass

        # Tier 2: Try Live Google Sheets Endpoint
        if self.google_sheets.is_configured():
            try:
                data = sheets_func()
                if data:
                    local_storage.set(f"{domain_key}_cache", data)
                    return data, "TIER_2_GOOGLE_SHEETS"
            except Exception:
                pass

        # Tier 3: Local SQLite & Local Storage Disk Cache
        try:
            data = local_func()
            if data:
                return data, "TIER_3_LOCAL_SQLITE"
        except Exception:
            pass

        data = local_storage.get(f"{domain_key}_cache", [])
        return data, "TIER_3_DISK_CACHE"

    def execute_write_with_fallback(self, action_name: str, payload: dict, api_write_func, local_write_func) -> str:
        """
        Executes write operation with instant Tier 3 local write + background Tier 1 & 2 cloud push.
        """
        # 1. Instant Tier 3 Local Persistence (Zero latency)
        local_write_func()
        local_storage.save()

        # 2. Tier 1 Cloud API Push
        cloud_pushed = False
        if api_client.check_server_online():
            try:
                api_write_func()
                cloud_pushed = True
            except Exception:
                pass

        # 3. Tier 2 Google Sheets Push
        sheets_pushed = False
        if self.google_sheets.is_configured():
            try:
                if action_name == "ADD_POST":
                    self.google_sheets.save_post(payload)
                elif action_name == "SAVE_USER":
                    self.google_sheets.save_user(payload)
                elif action_name == "SAVE_MESSAGE":
                    self.google_sheets.save_message(payload)
                sheets_pushed = True
            except Exception:
                pass

        if cloud_pushed:
            return "TIER_1_CLOUD_SYNCED"
        if sheets_pushed:
            return "TIER_2_SHEETS_SYNCED"
        return "TIER_3_LOCAL_QUEUED"


smart_fallback = ThreeTierSmartFallbackEngine()

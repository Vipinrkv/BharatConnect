"""
BharatConnect Hybrid Database Facade Module
Exposes the Hybrid Sync Engine (`db_engine`) which seamlessly bridges
local offline SQLite persistence and online REST API server sync.
"""

from app.sync_engine import sync_engine as db_engine, HybridSyncEngine as SQLiteDatabaseEngine

__all__ = ["db_engine", "SQLiteDatabaseEngine"]


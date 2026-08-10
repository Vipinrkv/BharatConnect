/**
 * BharatConnect Central Configuration & Cloud Sync Manager
 * 
 * Future-proof configuration file embedded inside the APK.
 * Allows instant endpoint updates, schema migrations, and sync controls.
 */

window.BHARATCONNECT_CONFIG = {
    APP_NAME: 'BharatConnect',
    APP_VERSION: '2.0.0-Opt1',
    // Universal Standalone FastAPI REST & WebSocket Endpoints
    API_BASE_URL: 'http://localhost:8000/api/v1',
    WS_BASE_URL: 'ws://localhost:8000/ws',
    // Firebase Cloud Messaging
    FCM_PROJECT_ID: 'bharatconnect-fcm',
    FCM_SENDER_ID: '247753000307',
    // Supabase Endpoints (Loaded dynamically or set via backend)
    SUPABASE_URL: 'https://ykbfynoofjvibnyfkifi.supabase.co',
    SUPABASE_PUBLISHABLE_KEY: '',
    DB_KEY: 'bharatconnect_db_v6_config',
    SESSION_KEY: 'bharatconnect_session_v6_config',
    ENABLE_E2EE: true,
    AUTO_RETRY_OFFLINE_SYNC: true,
    
    TABLE_SCHEMAS: {
        users: ["id", "username", "display_name", "email", "phone_number", "dob", "user_avatar", "password_hash", "bio", "created_at"],
        messages: ["id", "chat_id", "sender_id", "sender_name", "recipient_id", "text", "time", "is_me", "timestamp"],
        posts: ["id", "author_id", "author_name", "user_avatar", "avatar_color", "content", "image_title", "likes_count", "comments_count", "timestamp"],
        groups: ["id", "group_name", "members_count", "online_count", "pinned_message", "created_at"],
        communities: ["id", "community_name", "members_count", "topic", "pinned_announcement", "created_at"]
    }
};


import json
import logging
import datetime
import firebase_admin
from firebase_admin import credentials, messaging
from sqlalchemy import text

from app.config import settings
from app.core.database import AsyncSessionLocal
from app.core.redis import redis_manager

logger = logging.getLogger("FCMNotifier")

class FCMNotifier:
    def __init__(self):
        self.initialized = False
        try:
            if settings.FIREBASE_SERVICE_ACCOUNT_JSON and settings.FIREBASE_SERVICE_ACCOUNT_JSON != "{}":
                service_account_info = json.loads(settings.FIREBASE_SERVICE_ACCOUNT_JSON)
                cred = credentials.Certificate(service_account_info)
                firebase_admin.initialize_app(cred)
                self.initialized = True
                logger.info("Firebase Admin SDK initialized successfully.")
            else:
                logger.warning("FCM credentials missing. Push notifications will run in mock mode.")
        except Exception as e:
            logger.error(f"FCM initialization failed: {e}")

    async def send_user_notification(
        self,
        recipient_id: str,
        category: str, -- 'message', 'group', 'nearby', 'help', 'marketplace'
        title: str,
        body: str,
        data: dict = None
    ) -> bool:
        """
        Sends an FCM push notification, checking Quiet Hours, applying Smart Grouping, 
        and caching the item in the user's notification history.
        """
        async with AsyncSessionLocal() as session:
            # 1. Resolve recipient settings and device token
            # We select settings and the last active session token
            settings_query = """
                SELECT quiet_hours_enabled, quiet_hours_start, quiet_hours_end,
                       mute_groups, mute_nearby, mute_help, mute_marketplace
                FROM public.user_notification_settings
                WHERE user_id = :rec_id
            """
            settings_res = await session.execute(text(settings_query), {"rec_id": recipient_id})
            settings_row = settings_res.first()

            token_query = """
                SELECT refresh_token_hash FROM public.user_sessions 
                WHERE user_id = :rec_id AND is_active = true 
                ORDER BY last_active_at DESC LIMIT 1
            """
            token_res = await session.execute(text(token_query), {"rec_id": recipient_id})
            token_row = token_res.first()
            device_token = token_row[0] if token_row else "mock_device_token"

            # 2. Check Category Mute Filters
            if settings_row:
                if category == "group" and settings_row[3]: return False # mute_groups
                if category == "nearby" and settings_row[4]: return False # mute_nearby
                if category == "help" and settings_row[5]: return False # mute_help
                if category == "marketplace" and settings_row[6]: return False # mute_marketplace

            # 3. Check Quiet Hours Schedule
            is_silent = False
            if settings_row and settings_row[0]: # quiet_hours_enabled
                now_time = datetime.datetime.utcnow().time()
                start_time = settings_row[1]
                end_time = settings_row[2]
                
                # Handling window wraparound (e.g. 22:00 to 07:00)
                if start_time < end_time:
                    in_window = start_time <= now_time <= end_time
                else:
                    in_window = now_time >= start_time or now_time <= end_time
                
                if in_window:
                    # Suppress alert to a silent background data payload
                    is_silent = True
                    logger.info(f"Quiet hours active for user {recipient_id}. Downgrading to silent payload.")

            # 4. Smart Grouping (Redis Counters)
            # Accumulate unread counts for E2EE chats/categories
            chat_id = (data or {}).get("chat_id", "generic")
            redis_key = f"unread_notifs:{recipient_id}:{category}:{chat_id}"
            
            client = await redis_manager.get_client()
            unread_count = await client.incr(redis_key)
            await client.expire(redis_key, 3600) # Expire counter after 1 hour

            display_title = title
            display_body = body
            
            if unread_count > 1 and category in ["message", "group"]:
                display_title = title
                display_body = f"You have {unread_count} new messages in this thread."

            # 5. Insert notification record into postgres history
            history_insert = """
                INSERT INTO public.notification_history (user_id, title, body, category, metadata)
                VALUES (:user_id, :title, :body, :category, :meta)
            """
            await session.execute(
                text(history_insert),
                {
                    "user_id": recipient_id,
                    "title": display_title,
                    "body": display_body,
                    "category": category,
                    "meta": json.dumps(data or {})
                }
            )
            await session.commit()

            # 6. Dispatch via FCM
            if not self.initialized:
                logger.info(f"[Mock FCM Notification] User: {recipient_id[:8]}, Category: {category}, Title: '{display_title}', Body: '{display_body}', Silent: {is_silent}")
                return True

            # Silent notifications exclude the alert/notification block
            message = messaging.Message(
                data={**(data or {}), "silent": "true" if is_silent else "false"},
                token=device_token,
            )
            
            if not is_silent:
                message.notification = messaging.Notification(
                    title=display_title,
                    body=display_body,
                )

            try:
                response = messaging.send(message)
                logger.info(f"FCM Notification successfully delivered: {response}")
                return True
            except Exception as e:
                logger.error(f"FCM delivery exception: {e}")
                return False

fcm_notifier = FCMNotifier()

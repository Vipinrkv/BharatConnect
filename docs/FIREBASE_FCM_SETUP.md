# 🔥 BharatConnect — Firebase Cloud Messaging (FCM) Setup

**FCM Project ID:** `bharatconnect-fcm`  
**Sender ID:** `247753000307`  

---

## 1. Client-Side `google-services.json` Placement

1. Open your [Firebase Console](https://console.firebase.google.com/).
2. Select project **`bharatconnect-fcm`** (or create a project with package name `com.bharatconnect.app`).
3. Under **Project Settings** ➔ **Your Apps** ➔ Add Android App:
   * **Package name:** `com.bharatconnect.app`
   * **App nickname:** `BharatConnect Android`
4. Download **`google-services.json`**.
5. Place the file at:
   ```
   BharatConnect/android_native/app/google-services.json
   ```

---

## 2. Token Registration Lifecycle

When the app launches or a new FCM token is assigned:
1. [`BharatConnectFirebaseMessagingService.kt`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/android_native/app/src/main/java/com/bharatconnect/app/core/notifications/BharatConnectFirebaseMessagingService.kt) captures `onNewToken(token)`.
2. Inserts the user ID + FCM token into Supabase `public.device_tokens` table.
3. When another user sends a chat message, a Supabase Database Webhook or Edge Function calls Firebase HTTP v1 API using [`firebase_service_account.json`](file:///c:/Users/Vipin/OneDrive/Desktop/WebAplications/BharatConnect/firebase_service_account.json) located securely on the backend server.
4. The user's device receives the push notification even when the app is in the background or killed.

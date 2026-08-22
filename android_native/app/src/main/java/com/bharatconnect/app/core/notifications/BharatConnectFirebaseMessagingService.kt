package com.bharatconnect.app.core.notifications

import com.bharatconnect.app.core.network.SupabaseClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenDto(
    val user_id: String,
    val fcm_token: String,
    val device_model: String
)

class BharatConnectFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Sync FCM device token to Supabase for secure server-side push notifications
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                
                SupabaseClient.client.postgrest["device_tokens"].insert(
                    DeviceTokenDto(
                        user_id = userId,
                        fcm_token = token,
                        device_model = deviceModel
                    )
                )
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "BharatConnect"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "New message received"
        val conversationId = remoteMessage.data["conversation_id"]

        NotificationHelper.showMessageNotification(
            context = applicationContext,
            title = title,
            body = body,
            conversationId = conversationId
        )
    }
}

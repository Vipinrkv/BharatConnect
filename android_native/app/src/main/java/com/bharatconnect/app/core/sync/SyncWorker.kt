package com.bharatconnect.app.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bharatconnect.app.data.repository.ChatRepositoryImpl

import com.bharatconnect.app.core.notifications.NotificationHelper

class SyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val chatRepo = ChatRepositoryImpl()
            chatRepo.retryPendingMessages()
            chatRepo.fetchConversations()
            val notifResult = chatRepo.fetchNotifications()
            val unreadNotifs = notifResult.getOrNull()?.filter { !it.isRead && it.category == "messages" }.orEmpty()
            if (unreadNotifs.isNotEmpty()) {
                val latest = unreadNotifs.first()
                NotificationHelper.showMessageNotification(
                    context = appContext,
                    title = latest.title,
                    body = latest.description
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

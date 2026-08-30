package com.bharatconnect.app.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bharatconnect.app.data.repository.ChatRepositoryImpl

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val chatRepo = ChatRepositoryImpl()
            chatRepo.retryPendingMessages()
            chatRepo.fetchConversations()
            chatRepo.fetchNotifications()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

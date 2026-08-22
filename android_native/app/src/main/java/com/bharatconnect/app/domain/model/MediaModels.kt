package com.bharatconnect.app.domain.model

import kotlinx.serialization.Serializable

enum class UploadStatus {
    IDLE,
    SELECTED,
    VALIDATING,
    COMPRESSING,
    UPLOADING,
    PROCESSING,
    UPLOADED,
    DB_RECORD_CREATED,
    READY,
    FAILED
}

@Serializable
data class MediaAttachment(
    val id: String,
    val ownerId: String,
    val conversationId: String? = null,
    val cloudinaryPublicId: String,
    val mediaType: String, // "image", "video", "document"
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
    val fileSize: Long? = null,
    val secureUrl: String,
    val thumbnailUrl: String? = null,
    val status: String = "ready",
    val createdAt: String? = null
)

data class MediaUploadTask(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val status: UploadStatus = UploadStatus.IDLE,
    val progress: Float = 0f,
    val compressedSizeBytes: Long = 0,
    val originalSizeBytes: Long = 0,
    val secureUrl: String? = null,
    val thumbnailUrl: String? = null,
    val publicId: String? = null,
    val errorMessage: String? = null
)

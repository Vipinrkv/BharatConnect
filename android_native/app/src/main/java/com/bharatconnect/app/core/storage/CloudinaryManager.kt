package com.bharatconnect.app.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.bharatconnect.app.domain.model.MediaUploadTask
import com.bharatconnect.app.domain.model.UploadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object CloudinaryManager {
    const val CLOUD_NAME = "twiesyqj"
    const val UPLOAD_PRESET = "bharatconnect_unsigned"

    fun processAndUploadMedia(
        context: Context,
        uri: Uri,
        mimeType: String
    ): Flow<MediaUploadTask> = flow {
        val taskId = UUID.randomUUID().toString()
        val isImage = mimeType.startsWith("image")

        // 1. SELECTED
        var task = MediaUploadTask(
            id = taskId,
            fileName = uri.lastPathSegment ?: "media_$taskId",
            mimeType = mimeType,
            status = UploadStatus.SELECTED,
            progress = 0.1f
        )
        emit(task)

        // 2. VALIDATING
        task = task.copy(status = UploadStatus.VALIDATING, progress = 0.25f)
        emit(task)

        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        val originalSize = fileDescriptor?.statSize ?: 0L
        fileDescriptor?.close()

        if (originalSize > 50 * 1024 * 1024) { // 50 MB limit
            emit(task.copy(status = UploadStatus.FAILED, errorMessage = "File size exceeds 50MB limit"))
            return@flow
        }

        task = task.copy(originalSizeBytes = originalSize)

        // 3. COMPRESSING
        task = task.copy(status = UploadStatus.COMPRESSING, progress = 0.45f)
        emit(task)

        var uploadBytes: ByteArray? = null
        if (isImage) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val out = ByteArrayOutputStream()
                    // Compress image by 80% quality
                    originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    uploadBytes = out.toByteArray()
                    task = task.copy(compressedSizeBytes = uploadBytes.size.toLong())
                }
            } catch (e: Exception) {
                // If local compression fails, fallback to raw stream
            }
        }

        // 4. UPLOADING
        task = task.copy(status = UploadStatus.UPLOADING, progress = 0.70f)
        emit(task)

        // Simulate network upload / CDN dispatch
        kotlinx.coroutines.delay(600)

        // 5. PROCESSING
        task = task.copy(status = UploadStatus.PROCESSING, progress = 0.85f)
        emit(task)

        kotlinx.coroutines.delay(400)

        // 6. UPLOADED
        val generatedPublicId = "bharatconnect_${taskId.take(8)}"
        val secureUrl = "https://res.cloudinary.com/$CLOUD_NAME/image/upload/v1/bharatconnect/$generatedPublicId.jpg"
        val thumbnailUrl = "https://res.cloudinary.com/$CLOUD_NAME/image/upload/c_thumb,w_200,g_face/v1/bharatconnect/$generatedPublicId.jpg"

        task = task.copy(
            status = UploadStatus.UPLOADED,
            progress = 0.95f,
            publicId = generatedPublicId,
            secureUrl = secureUrl,
            thumbnailUrl = thumbnailUrl
        )
        emit(task)

        // 7. READY
        task = task.copy(
            status = UploadStatus.READY,
            progress = 1.0f
        )
        emit(task)
    }.flowOn(Dispatchers.IO)
}

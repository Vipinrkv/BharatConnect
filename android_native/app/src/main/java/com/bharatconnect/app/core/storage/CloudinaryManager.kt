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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object CloudinaryManager {
    const val CLOUD_NAME = "twiesyqj"
    const val UPLOAD_PRESET = "bharatconnect_unsigned"
    private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/auto/upload"

    /**
     * Ultra-fast, direct profile picture compression and upload.
     * Downscales to 512x512 max square avatar and compresses to JPEG 85% (~40-80KB).
     */
    suspend fun uploadProfilePicture(
        context: Context,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val compressedBytes = compressImageForUpload(context, uri, maxDimension = 512, quality = 85)
                ?: return@withContext Result.failure(Exception("Failed to decode and compress avatar image"))

            val fileName = "avatar_${System.currentTimeMillis()}.jpg"
            uploadDirect(compressedBytes, fileName, "image/jpeg")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Direct media upload with retry capability and fast response parsing.
     */
    suspend fun uploadMedia(
        context: Context,
        uri: Uri,
        mimeType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isImage = mimeType.startsWith("image")
            val bytes = if (isImage) {
                compressImageForUpload(context, uri, maxDimension = 1280, quality = 82)
                    ?: context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(Exception("Could not read media file"))
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext Result.failure(Exception("Could not read media file"))
            }

            val fileName = "media_${System.currentTimeMillis()}.${if (isImage) "jpg" else "bin"}"
            uploadDirect(bytes, fileName, mimeType)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reactive flow upload with step-by-step progress tracking for rich UI sheets.
     */
    fun processAndUploadMedia(
        context: Context,
        uri: Uri,
        mimeType: String
    ): Flow<MediaUploadTask> = flow {
        val taskId = UUID.randomUUID().toString()
        val isImage = mimeType.startsWith("image")

        var task = MediaUploadTask(
            id = taskId,
            fileName = uri.lastPathSegment ?: "media_$taskId",
            mimeType = mimeType,
            status = UploadStatus.SELECTED,
            progress = 0.1f
        )
        emit(task)

        task = task.copy(status = UploadStatus.VALIDATING, progress = 0.25f)
        emit(task)

        val originalBytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }

        if (originalBytes == null || originalBytes.isEmpty()) {
            emit(task.copy(status = UploadStatus.FAILED, errorMessage = "Selected file is empty or inaccessible"))
            return@flow
        }

        if (originalBytes.size > 50 * 1024 * 1024) {
            emit(task.copy(status = UploadStatus.FAILED, errorMessage = "File size exceeds 50MB limit"))
            return@flow
        }

        task = task.copy(originalSizeBytes = originalBytes.size.toLong(), status = UploadStatus.COMPRESSING, progress = 0.45f)
        emit(task)

        val uploadBytes = if (isImage) {
            compressImageForUpload(context, uri, maxDimension = 1280, quality = 82) ?: originalBytes
        } else {
            originalBytes
        }
        task = task.copy(compressedSizeBytes = uploadBytes.size.toLong())

        task = task.copy(status = UploadStatus.UPLOADING, progress = 0.70f)
        emit(task)

        val uploadResult = uploadDirect(uploadBytes, task.fileName, mimeType)
        if (uploadResult.isFailure) {
            emit(task.copy(
                status = UploadStatus.FAILED,
                errorMessage = uploadResult.exceptionOrNull()?.message ?: "Upload to Cloudinary failed"
            ))
            return@flow
        }

        val secureUrl = uploadResult.getOrThrow()
        task = task.copy(status = UploadStatus.PROCESSING, progress = 0.90f)
        emit(task)

        task = task.copy(
            status = UploadStatus.UPLOADED,
            progress = 0.95f,
            secureUrl = secureUrl,
            thumbnailUrl = secureUrl
        )
        emit(task)

        task = task.copy(status = UploadStatus.READY, progress = 1.0f)
        emit(task)
    }.flowOn(Dispatchers.IO)

    private fun uploadDirect(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String> {
        var lastException: Exception? = null

        // 2-attempt fast retry for maximum reliability
        for (attempt in 1..2) {
            var connection: HttpURLConnection? = null
            try {
                val boundary = "===BharatConnect_${System.currentTimeMillis()}==="
                val apiUrl = URL(UPLOAD_URL)
                connection = (apiUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 12000
                    readTimeout = 25000
                    useCaches = false
                    setChunkedStreamingMode(64 * 1024) // 64KB chunked stream
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    setRequestProperty("Connection", "Keep-Alive")
                    setRequestProperty("User-Agent", "BharatConnect-Native/2.0")
                }

                DataOutputStream(connection.outputStream).use { dos ->
                    // Upload preset parameter
                    dos.writeBytes("--$boundary\r\n")
                    dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                    dos.writeBytes("$UPLOAD_PRESET\r\n")

                    // File parameter
                    dos.writeBytes("--$boundary\r\n")
                    dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                    dos.writeBytes("Content-Type: $mimeType\r\n\r\n")
                    dos.write(bytes)
                    dos.writeBytes("\r\n")

                    dos.writeBytes("--$boundary--\r\n")
                    dos.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val responseText = BufferedInputStream(connection.inputStream).bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val secureUrl = json.optString("secure_url")
                    if (secureUrl.isNotEmpty()) {
                        return Result.success(secureUrl)
                    }
                } else {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                    lastException = Exception("Cloudinary Error ($responseCode): $errorText")
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        return Result.failure(lastException ?: Exception("Network connection failed during media upload"))
    }

    private fun compressImageForUpload(
        context: Context,
        uri: Uri,
        maxDimension: Int,
        quality: Int
    ): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var sampleSize = 1
            while ((options.outWidth / sampleSize > maxDimension) || (options.outHeight / sampleSize > maxDimension)) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val out = ByteArrayOutputStream()
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val result = out.toByteArray()
            decodedBitmap.recycle()
            result
        } catch (_: Exception) {
            null
        }
    }
}

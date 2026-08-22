package com.bharatconnect.app

import com.bharatconnect.app.domain.model.MediaUploadTask
import com.bharatconnect.app.domain.model.UploadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStateMachineTest {

    @Test
    fun testInitialUploadTaskState() {
        val task = MediaUploadTask(
            id = "test_1",
            fileName = "india_gate.jpg",
            mimeType = "image/jpeg"
        )

        assertEquals(UploadStatus.IDLE, task.status)
        assertEquals(0f, task.progress, 0.001f)
    }

    @Test
    fun testStateMachineProgression() {
        var task = MediaUploadTask(
            id = "task_progress",
            fileName = "post_media.png",
            mimeType = "image/png",
            status = UploadStatus.SELECTED,
            progress = 0.1f
        )

        assertEquals(UploadStatus.SELECTED, task.status)

        // Progress to COMPRESSING
        task = task.copy(status = UploadStatus.COMPRESSING, progress = 0.45f, compressedSizeBytes = 200 * 1024)
        assertEquals(UploadStatus.COMPRESSING, task.status)
        assertTrue(task.compressedSizeBytes > 0)

        // Progress to UPLOADED
        task = task.copy(status = UploadStatus.UPLOADED, progress = 0.95f, secureUrl = "https://res.cloudinary.com/twiesyqj/image/upload/sample.jpg")
        assertEquals(UploadStatus.UPLOADED, task.status)
        assertTrue(task.secureUrl?.contains("cloudinary.com") == true)

        // Final READY state
        task = task.copy(status = UploadStatus.READY, progress = 1.0f)
        assertEquals(UploadStatus.READY, task.status)
        assertEquals(1.0f, task.progress, 0.001f)
    }
}

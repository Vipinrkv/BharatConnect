package com.bharatconnect.app.presentation.media

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bharatconnect.app.core.storage.CloudinaryManager
import com.bharatconnect.app.domain.model.MediaUploadTask
import com.bharatconnect.app.domain.model.UploadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentTask = MutableStateFlow<MediaUploadTask?>(null)
    val currentTask: StateFlow<MediaUploadTask?> = _currentTask.asStateFlow()

    private val _uploadedGallery = MutableStateFlow<List<MediaUploadTask>>(emptyList())
    val uploadedGallery: StateFlow<List<MediaUploadTask>> = _uploadedGallery.asStateFlow()

    fun uploadMedia(uri: Uri, mimeType: String = "image/jpeg") {
        viewModelScope.launch {
            CloudinaryManager.processAndUploadMedia(getApplication(), uri, mimeType).collect { task ->
                _currentTask.value = task
                if (task.status == UploadStatus.READY) {
                    _uploadedGallery.value = listOf(task) + _uploadedGallery.value
                }
            }
        }
    }

    fun clearCurrentTask() {
        _currentTask.value = null
    }
}

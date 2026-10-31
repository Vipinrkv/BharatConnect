package com.bharatconnect.app.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * =========================================================================================
 * BHARATCONNECT VOICE RECORDER & AUDIO ENGINE (WHATSAPP PARITY)
 * =========================================================================================
 *
 * Implements WhatsApp-style native audio voice notes:
 * 1. High efficiency AAC in MPEG-4 (.m4a) container at 64kbps, optimized for Indian network conditions.
 * 2. Automatic duration tracking, clean start/stop/cancel lifecycles.
 * 3. Unified AudioPlayer with live playback progress tracking for interactive waveforms.
 */
object AudioRecordManager {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordStartTime: Long = 0L
    private var isRecording = false

    fun isCurrentlyRecording(): Boolean = isRecording

    @Synchronized
    fun startRecording(context: Context): Result<File> {
        return try {
            if (isRecording) {
                cancelRecording()
            }

            val cacheDir = File(context.cacheDir, "voice_notes").apply { if (!exists()) mkdirs() }
            val outputFile = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordStartTime = System.currentTimeMillis()
            isRecording = true

            Result.success(outputFile)
        } catch (e: Exception) {
            cancelRecording()
            Result.failure(e)
        }
    }

    @Synchronized
    fun stopRecording(): Result<Pair<File, Long>> {
        if (!isRecording || mediaRecorder == null || currentOutputFile == null) {
            return Result.failure(IllegalStateException("No active audio recording session"))
        }

        return try {
            val durationMs = (System.currentTimeMillis() - recordStartTime).coerceAtLeast(500L)
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            val file = currentOutputFile!!
            currentOutputFile = null
            Result.success(Pair(file, durationMs))
        } catch (e: Exception) {
            cancelRecording()
            Result.failure(e)
        }
    }

    @Synchronized
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false

        try {
            currentOutputFile?.let { if (it.exists()) it.delete() }
        } catch (_: Exception) {}
        currentOutputFile = null
    }
}

/**
 * Singleton Audio Player for voice note playback across chat messages.
 */
object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    private val _currentPlayingUrl = MutableStateFlow<String?>(null)
    val currentPlayingUrl: StateFlow<String?> = _currentPlayingUrl.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun cyclePlaybackSpeed(): Float {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        setPlaybackSpeed(nextSpeed)
        return nextSpeed
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.playbackParams = player.playbackParams.setSpeed(speed)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun play(
        audioUrl: String,
        onCompletion: () -> Unit = {}
    ) {
        if (_currentPlayingUrl.value == audioUrl && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                pause()
                return
            } else {
                resume()
                return
            }
        }

        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && _playbackSpeed.value != 1.0f) {
                        try {
                            mp.playbackParams = mp.playbackParams.setSpeed(_playbackSpeed.value)
                        } catch (_: Exception) {}
                    }
                    mp.start()
                    _currentPlayingUrl.value = audioUrl
                    _isPlaying.value = true
                    startProgressTracker()
                }
                setOnCompletionListener {
                    stopProgressTracker()
                    _isPlaying.value = false
                    _playbackProgress.value = 0f
                    _currentPositionMs.value = 0L
                    _currentPlayingUrl.value = null
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            stopProgressTracker()
        } catch (_: Exception) {}
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracker()
        } catch (_: Exception) {}
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPlayingUrl.value = null
        _playbackProgress.value = 0f
        _currentPositionMs.value = 0L
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressRunnable = object : Runnable {
            override fun run() {
                val player = mediaPlayer
                if (player != null && _isPlaying.value) {
                    try {
                        val current = player.currentPosition
                        val total = player.duration.coerceAtLeast(1)
                        _currentPositionMs.value = current.toLong()
                        _playbackProgress.value = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    } catch (_: Exception) {}
                    handler.postDelayed(this, 100L)
                }
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressTracker() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }
}

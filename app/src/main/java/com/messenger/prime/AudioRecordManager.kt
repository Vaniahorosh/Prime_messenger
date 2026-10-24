package com.messenger.prime

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

object AudioRecordManager {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false

    private val _amplitudes = MutableStateFlow<List<Int>>(emptyList())
    val amplitudes: StateFlow<List<Int>> = _amplitudes

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration

    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startRecording(context: Context): File? {
        Log.d("VoiceRecord", "AudioRecordManager.startRecording called")
        if (isRecording) {
            Log.w("VoiceRecord", "Already recording, cancelling previous")
            cancelRecording()
        }
        
        val cacheDir = File(context.cacheDir, "voices")
        if (!cacheDir.exists()) {
            val created = cacheDir.mkdirs()
            Log.d("VoiceRecord", "Created cacheDir/voices: $created")
        }
        
        currentOutputFile = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        Log.d("VoiceRecord", "OutputFile path: ${currentOutputFile?.absolutePath}")

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(currentOutputFile?.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            _amplitudes.value = emptyList()
            _recordingDuration.value = 0L
            startPolling()
            Log.d("VoiceRecord", "MediaRecorder started successfully")
        } catch (e: Throwable) {
            Log.e("VoiceRecord", "MediaRecorder start failed", e)
            release()
            return null
        }
        
        return currentOutputFile
    }

    private fun startPolling() {
        recordJob?.cancel()
        recordJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive && isRecording) {
                val maxAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }
                val normalized = ((maxAmp / 32767f) * 100f).toInt().coerceIn(2, 100)
                _amplitudes.value = _amplitudes.value + normalized
                _recordingDuration.value = System.currentTimeMillis() - startTime
                delay(70)
            }
        }
    }

    fun stopRecording(): Pair<File?, List<Int>> {
        Log.d("VoiceRecord", "AudioRecordManager.stopRecording called")
        val resultFile = currentOutputFile
        val resultAmplitudes = _amplitudes.value
        release()
        return Pair(resultFile, resultAmplitudes)
    }

    fun cancelRecording() {
        Log.d("VoiceRecord", "AudioRecordManager.cancelRecording called")
        release()
        currentOutputFile?.delete()
        currentOutputFile = null
    }

    private fun release() {
        isRecording = false
        recordJob?.cancel()
        try {
            mediaRecorder?.stop()
        } catch (e: Throwable) {
            Log.w("VoiceRecord", "MediaRecorder stop failed or wasn't running")
        }
        try {
            mediaRecorder?.release()
        } catch (e: Throwable) {}
        mediaRecorder = null
    }
}

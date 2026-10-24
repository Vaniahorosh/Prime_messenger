package com.messenger.prime

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class PrimeVoiceRecorderController(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val recordBar: View,
    private val etMessage: View,
    private val tvTimer: TextView,
    private val liveWaveform: VoiceMessageItemView,
    private val btnCancel: ImageButton,
    private val btnSend: ImageButton,
    private val btnAttach: View? = null,
    private val listener: VoiceRecordListener
) {
    interface VoiceRecordListener {
        fun onVoiceRecorded(file: File, amplitudes: List<Int>)
        fun onRecordingStateChanged(isRecording: Boolean)
    }

    private var isRecording = false
    private var timerJob: Job? = null

    init {
        setupButtons()
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener {
            cancelRecording()
        }

        btnSend.setOnClickListener {
            stopAndSendRecording()
        }
    }

    fun isRecording(): Boolean = isRecording

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecordingProcess() {
        Log.d("VoiceDebug", "PrimeVoiceRecorderController.startRecordingProcess called")
        
        if (!checkAudioPermission()) {
            Toast.makeText(activity, "Предоставьте разрешение на запись аудио", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                201
            )
            PrimePredictiveBack.isGestureLocked = false
            return
        }

        // Hide keyboard
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        activity.currentFocus?.let {
            imm?.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }

        val file = AudioRecordManager.startRecording(activity)
        if (file == null) {
            Toast.makeText(activity, "Ошибка запуска записи звука", Toast.LENGTH_SHORT).show()
            PrimePredictiveBack.isGestureLocked = false
            return
        }

        isRecording = true
        PrimePredictiveBack.isGestureLocked = true
        listener.onRecordingStateChanged(true)

        // Show record bar UI & hide input bar + attach button
        etMessage.visibility = View.GONE
        btnAttach?.visibility = View.GONE
        recordBar.visibility = View.VISIBLE
        recordBar.bringToFront()
        liveWaveform.visibility = View.VISIBLE
        liveWaveform.setWaveform(emptyList())
        tvTimer.text = "0:00"

        timerJob = lifecycleOwner.lifecycleScope.launch {
            launch {
                AudioRecordManager.recordingDuration.collect { durationMs ->
                    val sec = (durationMs / 1000) % 60
                    val min = (durationMs / 1000) / 60
                    tvTimer.text = String.format(Locale.US, "%d:%02d", min, sec)
                }
            }
            launch {
                AudioRecordManager.amplitudes.collect { amps ->
                    liveWaveform.setWaveform(amps)
                }
            }
        }
    }

    fun retryRecording() {
        Log.d("VoiceDebug", "PrimeVoiceRecorderController.retryRecording called")
        timerJob?.cancel()
        timerJob = null

        AudioRecordManager.cancelRecording()
        isRecording = false
        liveWaveform.setWaveform(emptyList())
        tvTimer.text = "0:00"

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startRecordingProcess()
        }, 80)
    }

    fun stopAndSendRecording() {
        if (!isRecording) return
        Log.d("VoiceDebug", "PrimeVoiceRecorderController.stopAndSendRecording called")
        
        timerJob?.cancel()
        timerJob = null

        val (file, amplitudes) = AudioRecordManager.stopRecording()
        isRecording = false
        PrimePredictiveBack.isGestureLocked = false
        listener.onRecordingStateChanged(false)
        resetUI()

        if (file != null && file.exists() && file.length() > 0) {
            listener.onVoiceRecorded(file, amplitudes)
        }
    }

    fun cancelRecording() {
        if (!isRecording) return
        Log.d("VoiceDebug", "PrimeVoiceRecorderController.cancelRecording called")
        
        timerJob?.cancel()
        timerJob = null

        AudioRecordManager.cancelRecording()
        isRecording = false
        PrimePredictiveBack.isGestureLocked = false
        listener.onRecordingStateChanged(false)
        resetUI()
    }

    fun resetUI() {
        recordBar.visibility = View.GONE
        etMessage.visibility = View.VISIBLE
        btnAttach?.visibility = View.VISIBLE
        liveWaveform.visibility = View.GONE
        liveWaveform.setWaveform(emptyList())
        PrimePredictiveBack.isGestureLocked = false
    }
}

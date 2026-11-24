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
            PrimeNotification.show(activity, "Предоставьте разрешение на запись аудио")
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
            PrimeNotification.show(activity, "Ошибка запуска записи звука")
            PrimePredictiveBack.isGestureLocked = false
            return
        }

        isRecording = true
        PrimePredictiveBack.isGestureLocked = true
        listener.onRecordingStateChanged(true)

        val blurInputAttach = activity.findViewById<View>(R.id.blurInputAttach)
        val btnFormat = activity.findViewById<View>(R.id.btnFormat)
        val btnEmojiToggle = activity.findViewById<View>(R.id.btnEmojiToggle)

        // Cancel any pending animations to prevent views from unexpectedly becoming GONE
        etMessage.animate().cancel()
        btnAttach?.animate()?.cancel()
        blurInputAttach?.animate()?.cancel()
        btnFormat?.animate()?.cancel()
        btnEmojiToggle?.animate()?.cancel()
        recordBar.animate().cancel()

        val attachTarget = blurInputAttach ?: btnAttach

        // Smoothly fade & slide out input controls
        etMessage.animate().alpha(0f).translationX(-30f).setDuration(180).withEndAction {
            etMessage.visibility = View.GONE
            etMessage.translationX = 0f
        }.start()

        btnEmojiToggle?.animate()?.alpha(0f)?.translationX(-30f)?.setDuration(180)?.withEndAction {
            btnEmojiToggle.visibility = View.GONE
            btnEmojiToggle.translationX = 0f
        }?.start()

        btnFormat?.animate()?.alpha(0f)?.translationX(-30f)?.setDuration(180)?.withEndAction {
            btnFormat.visibility = View.GONE
            btnFormat.translationX = 0f
        }?.start()

        attachTarget?.animate()?.alpha(0f)?.translationX(-30f)?.setDuration(180)?.withEndAction {
            attachTarget.visibility = View.GONE
            attachTarget.translationX = 0f
        }?.start()

        // Show record bar UI with smooth animation
        recordBar.visibility = View.VISIBLE
        recordBar.bringToFront()
        recordBar.alpha = 0f
        recordBar.translationX = 30f
        recordBar.animate().alpha(1f).translationX(0f).setDuration(220).start()

        liveWaveform.visibility = View.VISIBLE
        liveWaveform.setWaveform(emptyList())

        tvTimer.text = "0:00"
        tvTimer.alpha = 1f
        tvTimer.translationY = 0f

        timerJob = lifecycleOwner.lifecycleScope.launch {
            launch {
                AudioRecordManager.recordingDuration.collect { durationMs ->
                    val sec = (durationMs / 1000) % 60
                    val min = (durationMs / 1000) / 60
                    val newTime = String.format(Locale.US, "%d:%02d", min, sec)
                    if (tvTimer.text != newTime) {
                        tvTimer.text = newTime
                        tvTimer.alpha = 1f
                        tvTimer.translationY = 0f
                    }
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

        val blurInputAttach = activity.findViewById<View>(R.id.blurInputAttach)
        val btnFormat = activity.findViewById<View>(R.id.btnFormat)
        val btnEmojiToggle = activity.findViewById<View>(R.id.btnEmojiToggle)

        etMessage.animate().cancel()
        btnAttach?.animate()?.cancel()
        blurInputAttach?.animate()?.cancel()
        btnFormat?.animate()?.cancel()
        btnEmojiToggle?.animate()?.cancel()
        recordBar.animate().cancel()

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
        val blurInputAttach = activity.findViewById<View>(R.id.blurInputAttach)
        val btnFormat = activity.findViewById<View>(R.id.btnFormat)
        val btnEmojiToggle = activity.findViewById<View>(R.id.btnEmojiToggle)

        etMessage.animate().cancel()
        btnAttach?.animate()?.cancel()
        blurInputAttach?.animate()?.cancel()
        btnFormat?.animate()?.cancel()
        btnEmojiToggle?.animate()?.cancel()
        recordBar.animate().cancel()

        val attachTarget = blurInputAttach ?: btnAttach

        recordBar.animate().alpha(0f).translationX(30f).setDuration(180).withEndAction {
            recordBar.visibility = View.GONE
            recordBar.translationX = 0f
            liveWaveform.visibility = View.GONE

            etMessage.visibility = View.VISIBLE
            etMessage.animate().cancel()
            etMessage.animate().alpha(1f).translationX(0f).setDuration(200).start()

            btnEmojiToggle?.visibility = View.VISIBLE
            btnEmojiToggle?.animate()?.cancel()
            btnEmojiToggle?.animate()?.alpha(1f)?.translationX(0f)?.setDuration(200)?.start()

            val etText = (etMessage as? android.widget.EditText)?.text
            if (!etText.isNullOrEmpty()) {
                btnFormat?.visibility = View.VISIBLE
                btnFormat?.animate()?.cancel()
                btnFormat?.animate()?.alpha(1f)?.scaleX(1f)?.scaleY(1f)?.translationX(0f)?.setDuration(200)?.start()
            } else {
                btnFormat?.visibility = View.GONE
            }

            val btnCancelEdit = (btnAttach?.parent as? android.view.ViewGroup)?.findViewById<View>(R.id.btnCancelEdit)
            if (btnCancelEdit == null || btnCancelEdit.visibility != View.VISIBLE) {
                attachTarget?.visibility = View.VISIBLE
                attachTarget?.animate()?.cancel()
                attachTarget?.animate()?.alpha(1f)?.translationX(0f)?.setDuration(200)?.start()
            } else {
                attachTarget?.visibility = View.GONE
            }
        }.start()
        
        liveWaveform.setWaveform(emptyList())
    }
}

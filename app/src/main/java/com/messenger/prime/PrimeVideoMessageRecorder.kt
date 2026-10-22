package com.messenger.prime

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Outline
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.slider.RangeSlider
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PrimeVideoMessageRecorder(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val overlayView: FrameLayout,
    private val previewContainer: FrameLayout,
    private val btnVoiceVideoNote: ImageButton,
    private val tvTimer: TextView,
    private val pbVideoRecordTime: PrimeWaterLoadingView,
    private val layoutPreviewControls: View,
    private val btnCancel: ImageButton,
    private val btnRetry: ImageButton,
    private val btnSend: ImageButton,
    private val btnSettings: ImageButton,
    private val btnStopLocked: ImageButton,
    private val listener: VideoRecordListener
) {
    interface VideoRecordListener {
        fun onVideoRecorded(file: File)
        fun onOpenSettings()
        fun isVideoMode(): Boolean
        fun isConnectionActive(): Boolean
        fun onRecordingStateChanged(isRecording: Boolean)
    }

    private var previewView: PreviewView? = null
    private var videoPlaybackView: TextureVideoView? = null
    private var sliderVideoTrim: RangeSlider? = null
    private var tvTrimStart: TextView? = null
    private var tvTrimEnd: TextView? = null
    private var layoutVideoLockIndicator: View? = null
    private var ivVideoLockIcon: ImageView? = null
    private var layoutVideoCancelContainer: View? = null
    private var ivVideoTrashIcon: ImageView? = null
    private var tvZoomBadge: TextView? = null
    private var sliderVideoZoom: com.google.android.material.slider.Slider? = null
    private var layoutVideoZoomControls: View? = null
    private var btnCancelMid: ImageButton? = null
    private var btnFlipMid: ImageButton? = null
    private var btnPauseResume: ImageButton? = null
    private var btnFinishLocked: ImageButton? = null
    private var layoutVideoCenterControls: View? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var blurView: BlurView? = null

    private var isLocked = false
    private var isRecording = false
    private var isRecordingStarting = false
    private var isRecordingPaused = false
    private var isFlippingCamera = false
    private var shouldStopPending = false
    private var isInCancelZone = false
    private var startX = 0f
    private var startY = 0f
    private var currentVideoFile: File? = null
    private val recordedSegments = mutableListOf<File>()
    private var accumulatedDurationMs = 0L
    private var segmentStartTimestamp = 0L

    private var trimStartMs: Int = 0
    private var trimEndMs: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPressTriggered = false

    private val recordingProgressRunnable = object : Runnable {
        override fun run() {
            if (isRecording && !isRecordingPaused) {
                val now = System.currentTimeMillis()
                val currentSegmentMs = if (segmentStartTimestamp > 0) now - segmentStartTimestamp else 0L
                val totalMs = (accumulatedDurationMs + currentSegmentMs).toInt()

                val seconds = totalMs / 1000
                tvTimer.text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
                pbVideoRecordTime.max = 120000
                pbVideoRecordTime.progress = totalMs.coerceIn(0, 120000)

                if (totalMs >= 120000) {
                    stopRecording()
                    return
                }
                handler.postDelayed(this, 30)
            }
        }
    }

    private val checkTrimLoopRunnable = object : Runnable {
        override fun run() {
            videoPlaybackView?.let { vv ->
                if (vv.visibility == View.VISIBLE && vv.isPlaying) {
                    val cur = vv.currentPosition
                    val dur = vv.duration
                    if (dur > 0) {
                        pbVideoRecordTime.max = dur
                        pbVideoRecordTime.progress = cur.coerceIn(0, dur)
                    }

                    if (trimEndMs > 0 && cur >= trimEndMs - 50) {
                        vv.pause()
                        vv.seekTo(trimStartMs)
                        vv.start()
                    }
                }
                handler.postDelayed(this, 30)
            }
        }
    }

    init {
        ViewCompat.setOnApplyWindowInsetsListener(overlayView) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val topControls = overlayView.findViewById<View>(R.id.layoutVideoTopControls)
            val lp = topControls?.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                lp.topMargin = statusBars.top + (12 * activity.resources.displayMetrics.density).toInt()
                topControls.layoutParams = lp
            }

            layoutPreviewControls.setPadding(
                layoutPreviewControls.paddingLeft,
                layoutPreviewControls.paddingTop,
                layoutPreviewControls.paddingRight,
                navBars.bottom + (16 * activity.resources.displayMetrics.density).toInt()
            )
            insets
        }

        ProcessCameraProvider.getInstance(activity).addListener({
            try {
                cameraProvider = ProcessCameraProvider.getInstance(activity).get()
            } catch (e: Exception) {
                Log.e("PrimeVideo", "Failed to cache camera provider", e)
            }
        }, ContextCompat.getMainExecutor(activity))

        previewView = PreviewView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        videoPlaybackView = previewContainer.findViewById(R.id.vvVideoRecordPlayback)
        sliderVideoTrim = layoutPreviewControls.findViewById(R.id.sliderVideoTrim)
        tvTrimStart = layoutPreviewControls.findViewById(R.id.tvTrimStart)
        tvTrimEnd = layoutPreviewControls.findViewById(R.id.tvTrimEnd)
        layoutVideoLockIndicator = overlayView.findViewById(R.id.layoutVideoLockIndicator)
        ivVideoLockIcon = overlayView.findViewById(R.id.ivVideoLockIcon)
        layoutVideoCancelContainer = overlayView.findViewById(R.id.layoutVideoCancelContainer)
        ivVideoTrashIcon = overlayView.findViewById(R.id.ivVideoTrashIcon)
        tvZoomBadge = overlayView.findViewById(R.id.tvVideoZoomBadge)
        layoutVideoZoomControls = overlayView.findViewById(R.id.layoutVideoZoomControls)
        sliderVideoZoom = overlayView.findViewById(R.id.sliderVideoZoom)

        sliderVideoZoom?.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                camera?.cameraControl?.setZoomRatio(value)
                tvZoomBadge?.text = String.format(Locale.US, "%.1fx", value)
            }
        }
        layoutVideoCenterControls = overlayView.findViewById(R.id.layoutVideoCenterControls)
        btnCancelMid = overlayView.findViewById(R.id.btnVideoRecordCancelMid)
        btnFlipMid = overlayView.findViewById(R.id.btnVideoRecordFlipMid)
        btnPauseResume = overlayView.findViewById(R.id.btnVideoRecordPauseResume)
        btnFinishLocked = overlayView.findViewById(R.id.btnVideoRecordFinishLocked)

        previewContainer.removeAllViews()
        previewContainer.addView(previewView)
        videoPlaybackView?.let { previewContainer.addView(it) }

        setupBlurView()
        setupListeners()
    }

    private fun triggerHaptic(feedbackType: Int) {
        try {
            btnVoiceVideoNote.performHapticFeedback(feedbackType)
        } catch (ignored: Exception) {}
    }

    private fun setupBlurView() {
        blurView = overlayView.findViewById(R.id.blurVideoRecord)
        blurView?.let { bv ->
            try {
                val decorView = activity.window.decorView
                val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
                val windowBackground = decorView.background

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bv.setupWith(rootView, RenderEffectBlur())
                        .setFrameClearDrawable(windowBackground)
                        .setBlurRadius(16f)
                        .setBlurAutoUpdate(false)
                } else {
                    try {
                        bv.setupWith(rootView, RenderScriptBlur(activity))
                            .setFrameClearDrawable(windowBackground)
                            .setBlurRadius(16f)
                            .setBlurAutoUpdate(false)
                    } catch (t: Throwable) {
                        Log.e("PrimeVideo", "RenderScriptBlur unavailable", t)
                    }
                }
            } catch (t: Throwable) {
                Log.e("PrimeVideo", "BlurView setup failed", t)
            }
        }
    }

    private fun animate3DCameraFlip(onHalfway: () -> Unit) {
        val density = activity.resources.displayMetrics.density
        previewContainer.cameraDistance = 8000f * density

        previewContainer.animate()
            .rotationY(90f)
            .setDuration(150)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                onHalfway()
                previewContainer.rotationY = -90f
                previewContainer.animate()
                    .rotationY(0f)
                    .setDuration(150)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun flipCameraProcess() {
        triggerHaptic(HapticFeedbackConstants.KEYBOARD_TAP)

        val nextSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val provider = cameraProvider ?: return
        try {
            if (!provider.hasCamera(nextSelector)) {
                Toast.makeText(activity, "Камера недоступна", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            return
        }

        animate3DCameraFlip {
            cameraSelector = nextSelector

            if (isRecording) {
                isFlippingCamera = true
                recording?.stop()
            } else if (overlayView.visibility == View.VISIBLE) {
                startCamera {}
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        val togglePlayback = View.OnClickListener {
            videoPlaybackView?.let { vv ->
                if (vv.visibility == View.VISIBLE) {
                    if (vv.isPlaying) {
                        vv.pause()
                    } else {
                        vv.start()
                    }
                }
            }
        }

        previewContainer.setOnClickListener(togglePlayback)
        videoPlaybackView?.setOnClickListener(togglePlayback)

        btnCancelMid?.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
            cancelRecording()
        }
        btnFlipMid?.setOnClickListener { flipCameraProcess() }

        btnPauseResume?.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
            recording?.let { rec ->
                if (isRecordingPaused) {
                    rec.resume()
                    isRecordingPaused = false
                    segmentStartTimestamp = System.currentTimeMillis()
                    btnPauseResume?.setImageResource(R.drawable.ic_media_pause)
                    handler.post(recordingProgressRunnable)
                } else {
                    rec.pause()
                    isRecordingPaused = true
                    if (segmentStartTimestamp > 0) {
                        accumulatedDurationMs += (System.currentTimeMillis() - segmentStartTimestamp)
                        segmentStartTimestamp = 0L
                    }
                    handler.removeCallbacks(recordingProgressRunnable)
                    btnPauseResume?.setImageResource(R.drawable.ic_media_play)
                }
            }
        }

        btnFinishLocked?.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.VIRTUAL_KEY)
            if (isRecording || isRecordingStarting) {
                stopRecording()
            }
        }

        btnVoiceVideoNote.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    isLongPressTriggered = false
                    isInCancelZone = false

                    if (!listener.isConnectionActive()) {
                        return@setOnTouchListener false
                    }

                    if (listener.isVideoMode()) {
                        longPressRunnable = Runnable {
                            isLongPressTriggered = true
                            triggerHaptic(HapticFeedbackConstants.LONG_PRESS)
                            if (checkPermissions()) {
                                startRecordingProcess()
                            } else {
                                requestPermissions()
                            }
                        }
                        handler.postDelayed(longPressRunnable!!, 150)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isRecording && !isLocked) {
                        val dy = startY - event.rawY
                        val dx = startX - event.rawX
                        val screenWidth = activity.resources.displayMetrics.widthPixels

                        // 1. Interactive Lock indicator (Swipe UP)
                        if (dy > 30) {
                            layoutVideoLockIndicator?.visibility = View.VISIBLE
                            val animY = (-dy * 0.5f).coerceAtLeast(-120f)
                            val scale = 0.8f + (dy / 120f) * 0.4f
                            layoutVideoLockIndicator?.translationY = animY
                            ivVideoLockIcon?.scaleX = scale.coerceAtMost(1.4f)
                            ivVideoLockIcon?.scaleY = scale.coerceAtMost(1.4f)

                            if (dy > 120) {
                                triggerHaptic(HapticFeedbackConstants.CONTEXT_CLICK)
                                lockRecording()
                                layoutVideoLockIndicator?.visibility = View.GONE
                            }
                        } else {
                            layoutVideoLockIndicator?.visibility = View.GONE
                        }

                        // 2. Interactive Trash Container (Drag LEFT)
                        if (dx > 80 || event.rawX < screenWidth * 0.35f) {
                            isInCancelZone = true
                            layoutVideoCancelContainer?.visibility = View.VISIBLE
                            val trashScale = 1.0f + ((dx - 80) / 100f).coerceIn(0f, 0.5f)
                            ivVideoTrashIcon?.scaleX = trashScale
                            ivVideoTrashIcon?.scaleY = trashScale
                        } else {
                            isInCancelZone = false
                            layoutVideoCancelContainer?.visibility = View.GONE
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }

                    if (isLongPressTriggered) {
                        triggerHaptic(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (isInCancelZone) {
                            cancelRecording()
                        } else if ((isRecording || isRecordingStarting) && !isLocked) {
                            stopRecording()
                        }
                    } else {
                        btnVoiceVideoNote.performClick()
                    }
                    isLongPressTriggered = false
                    isInCancelZone = false
                    layoutVideoLockIndicator?.visibility = View.GONE
                    layoutVideoCancelContainer?.visibility = View.GONE
                    tvZoomBadge?.visibility = View.GONE
                    true
                }
                else -> false
            }
        }

        btnStopLocked.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.VIRTUAL_KEY)
            if (isRecording || isRecordingStarting) {
                stopRecording()
            } else {
                resetUI()
            }
        }

        btnCancel.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
            cancelRecording()
        }

        btnRetry.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
            cancelRecording()
            startRecordingProcess()
        }

        btnSend.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
            currentVideoFile?.let { file ->
                if (trimStartMs > 0 || (trimEndMs > 0 && trimEndMs < (videoPlaybackView?.duration ?: Int.MAX_VALUE) - 500)) {
                    val trimmedFile = File(activity.externalCacheDir, "trimmed_" + file.name)
                    VideoTrimmer.trimVideo(file, trimmedFile, trimStartMs.toLong(), trimEndMs.toLong())
                    listener.onVideoRecorded(trimmedFile)
                } else {
                    listener.onVideoRecorded(file)
                }
            }
            resetUI()
        }

        btnSettings.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.KEYBOARD_TAP)
            cancelRecording()
            listener.onOpenSettings()
        }
    }

    private fun checkPermissions(): Boolean {
        val hasCamera = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        return hasCamera && hasAudio
    }

    private fun requestPermissions() {
        Toast.makeText(activity, "Предоставьте разрешения на Камеру и Запись звука", Toast.LENGTH_SHORT).show()
        ActivityCompat.requestPermissions(
            activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    private fun applyShapeClipping(container: FrameLayout) {
        val sharedPrefs = activity.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val radiusPercent = sharedPrefs.getInt("video_message_corner_radius", 50)

        container.clipToOutline = true
        container.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val cornerRadiusPx = view.width * (radiusPercent / 100f)
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
            }
        }
        container.invalidateOutline()
    }

    private fun startRecordingProcess() {
        recordedSegments.clear()
        accumulatedDurationMs = 0L
        segmentStartTimestamp = 0L

        val sharedPrefs = activity.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val shape = sharedPrefs.getString("video_message_shape", "circle") ?: "circle"

        val accentColor = ColorAccentManager.getCurrentAccentColor(activity)
        pbVideoRecordTime.isBorderProgressMode = true
        pbVideoRecordTime.staticShapeType = shape
        pbVideoRecordTime.setIndicatorColor(accentColor)
        pbVideoRecordTime.max = 120000
        pbVideoRecordTime.progress = 0

        applyShapeClipping(previewContainer)

        overlayView.bringToFront()
        overlayView.visibility = View.VISIBLE
        blurView?.setBlurAutoUpdate(true)

        videoPlaybackView?.visibility = View.GONE
        previewView?.visibility = View.VISIBLE

        layoutPreviewControls.visibility = View.GONE
        btnStopLocked.visibility = View.GONE
        layoutVideoCenterControls?.visibility = View.GONE
        layoutVideoZoomControls?.visibility = View.VISIBLE
        sliderVideoZoom?.value = 1.0f
        tvZoomBadge?.text = "1.0x"
        btnVoiceVideoNote.visibility = View.INVISIBLE
        tvTimer.text = "00:00"

        listener.onRecordingStateChanged(true)

        startCamera {
            startRecording()
        }
    }

    private fun lockRecording() {
        isLocked = true
        layoutVideoCenterControls?.bringToFront()
        layoutVideoCenterControls?.visibility = View.VISIBLE
        btnStopLocked.visibility = View.GONE
        btnVoiceVideoNote.visibility = View.GONE
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val videoCapture = this.videoCapture ?: return
        if (isRecording || isRecordingStarting) return

        isRecordingStarting = true
        shouldStopPending = false
        isRecordingPaused = false
        btnPauseResume?.setImageResource(R.drawable.ic_media_pause)

        val videoFile = File(
            activity.externalCacheDir,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + "_seg" + recordedSegments.size + ".mp4"
        )
        currentVideoFile = videoFile

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        try {
            recording = videoCapture.output
                .prepareRecording(activity, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(activity)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            isRecordingStarting = false
                            isRecording = true
                            segmentStartTimestamp = System.currentTimeMillis()
                            handler.removeCallbacks(recordingProgressRunnable)
                            handler.post(recordingProgressRunnable)

                            if (shouldStopPending) {
                                shouldStopPending = false
                                stopRecording()
                            }
                        }
                        is VideoRecordEvent.Status -> {
                            // High precision live progress handled via 30ms smooth recordingProgressRunnable!
                        }
                        is VideoRecordEvent.Finalize -> {
                            isRecordingStarting = false
                            isRecording = false
                            handler.removeCallbacks(recordingProgressRunnable)

                            if (segmentStartTimestamp > 0) {
                                val segmentMs = System.currentTimeMillis() - segmentStartTimestamp
                                accumulatedDurationMs += segmentMs
                                segmentStartTimestamp = 0L
                            }

                            currentVideoFile?.let { file ->
                                if (file.exists() && file.length() > 500) {
                                    recordedSegments.add(file)
                                }
                            }

                            if (isFlippingCamera) {
                                isFlippingCamera = false
                                startCamera {
                                    startRecording()
                                }
                            } else {
                                processFinalRecordingsAndShowPreview()
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            isRecordingStarting = false
            isRecording = false
            handler.removeCallbacks(recordingProgressRunnable)
            Log.e("PrimeVideo", "Failed to start recording", e)
            resetUI()
        }
    }

    private fun processFinalRecordingsAndShowPreview() {
        if (recordedSegments.isEmpty()) {
            resetUI()
            return
        }

        if (recordedSegments.size == 1) {
            currentVideoFile = recordedSegments[0]
            showPreviewControls()
        } else {
            val joinedFile = File(
                activity.externalCacheDir,
                "joined_" + System.currentTimeMillis() + ".mp4"
            )
            VideoTrimmer.concatenateVideos(recordedSegments, joinedFile)
            currentVideoFile = joinedFile
            showPreviewControls()
        }
    }

    private fun stopRecording() {
        if (isRecordingStarting) {
            shouldStopPending = true
            return
        }
        try {
            recording?.stop()
        } catch (e: Exception) {
            Log.e("PrimeVideo", "Error stopping recording", e)
        }
        recording = null
    }

    private fun showPreviewControls() {
        handler.removeCallbacks(recordingProgressRunnable)
        layoutPreviewControls.bringToFront()
        layoutPreviewControls.visibility = View.VISIBLE
        btnStopLocked.visibility = View.GONE
        layoutVideoCenterControls?.visibility = View.GONE
        layoutVideoZoomControls?.visibility = View.GONE
        btnVoiceVideoNote.visibility = View.GONE

        handler.removeCallbacks(checkTrimLoopRunnable)
        handler.post(checkTrimLoopRunnable)

        currentVideoFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                previewView?.visibility = View.GONE
                videoPlaybackView?.apply {
                    visibility = View.VISIBLE
                    setVideoPath(file.absolutePath)
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        val durationMs = mediaPlayer.duration
                        trimStartMs = 0
                        trimEndMs = durationMs
                        start()

                        pbVideoRecordTime.max = durationMs
                        pbVideoRecordTime.progress = 0

                        val formatMs = { ms: Float ->
                            val sec = (ms / 1000).toInt()
                            String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60)
                        }

                        tvTrimStart?.text = formatMs(0f)
                        tvTrimEnd?.text = formatMs(durationMs.toFloat())

                        sliderVideoTrim?.apply {
                            valueFrom = 0f
                            valueTo = durationMs.toFloat().coerceAtLeast(100f)
                            values = listOf(0f, durationMs.toFloat().coerceAtLeast(100f))

                            clearOnChangeListeners()
                            clearOnSliderTouchListeners()

                            addOnChangeListener { slider, value, fromUser ->
                                val vals = slider.values
                                if (vals.size >= 2) {
                                    trimStartMs = vals[0].toInt()
                                    trimEndMs = vals[1].toInt()
                                    tvTrimStart?.text = formatMs(vals[0])
                                    tvTrimEnd?.text = formatMs(vals[1])

                                    if (fromUser) {
                                        videoPlaybackView?.pause()
                                        videoPlaybackView?.seekTo(value.toInt())
                                    }
                                }
                            }

                            addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
                                override fun onStartTrackingTouch(slider: RangeSlider) {
                                    videoPlaybackView?.pause()
                                }

                                override fun onStopTrackingTouch(slider: RangeSlider) {
                                    videoPlaybackView?.seekTo(trimStartMs)
                                    videoPlaybackView?.start()
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    private fun resetUI() {
        handler.removeCallbacks(recordingProgressRunnable)
        handler.removeCallbacks(checkTrimLoopRunnable)
        videoPlaybackView?.apply {
            stopPlayback()
            visibility = View.GONE
        }
        blurView?.setBlurAutoUpdate(false)
        overlayView.visibility = View.GONE
        layoutVideoLockIndicator?.visibility = View.GONE
        layoutVideoCancelContainer?.visibility = View.GONE
        layoutVideoCenterControls?.visibility = View.GONE
        layoutVideoZoomControls?.visibility = View.GONE
        tvZoomBadge?.visibility = View.GONE
        btnStopLocked.visibility = View.GONE
        btnVoiceVideoNote.visibility = View.VISIBLE
        isLocked = false
        isRecording = false
        isRecordingStarting = false
        isRecordingPaused = false
        isFlippingCamera = false
        shouldStopPending = false
        isInCancelZone = false

        for (f in recordedSegments) {
            try { if (f.exists()) f.delete() } catch (ignored: Exception) {}
        }
        recordedSegments.clear()
        accumulatedDurationMs = 0L
        segmentStartTimestamp = 0L

        currentVideoFile = null
        listener.onRecordingStateChanged(false)
    }

    fun isRecordingOrPreviewing(): Boolean {
        return overlayView.visibility == View.VISIBLE
    }

    fun cancelRecording() {
        if (isRecording || isRecordingStarting) {
            try {
                recording?.stop()
            } catch (ignored: Exception) {}
            recording = null
        }
        resetUI()
    }

    private fun startCamera(onCameraReady: () -> Unit) {
        val provider = cameraProvider
        if (provider != null) {
            bindCameraUseCases(provider, onCameraReady)
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
            cameraProviderFuture.addListener({
                try {
                    val p = cameraProviderFuture.get()
                    cameraProvider = p
                    bindCameraUseCases(p, onCameraReady)
                } catch (e: Exception) {
                    Log.e("PrimeVideo", "CameraX initialization failed", e)
                    Toast.makeText(activity, "Ошибка камеры: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(activity))
        }
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider, onCameraReady: () -> Unit) {
        val displayRotation = previewView?.display?.rotation
            ?: activity.windowManager.defaultDisplay.rotation

        val captureTargetRotation = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            when (displayRotation) {
                android.view.Surface.ROTATION_0 -> android.view.Surface.ROTATION_180
                android.view.Surface.ROTATION_180 -> android.view.Surface.ROTATION_0
                android.view.Surface.ROTATION_90 -> android.view.Surface.ROTATION_270
                android.view.Surface.ROTATION_270 -> android.view.Surface.ROTATION_90
                else -> android.view.Surface.ROTATION_180
            }
        } else {
            displayRotation
        }

        val preview = Preview.Builder()
            .setTargetRotation(displayRotation)
            .build()
            .also { p ->
                previewView?.let { pv ->
                    p.setSurfaceProvider(pv.surfaceProvider)
                }
            }

        val sharedPrefs = activity.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val savedQuality = sharedPrefs.getInt("video_message_quality", 1)
        val qualitySelector = when (savedQuality) {
            0 -> QualitySelector.from(Quality.SD)
            1 -> QualitySelector.from(Quality.HD)
            else -> QualitySelector.from(Quality.FHD)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        videoCapture = VideoCapture.Builder(recorder)
            .setTargetRotation(captureTargetRotation)
            .build()

        try {
            if (!cameraProvider.hasCamera(cameraSelector)) {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }
        } catch (e: Exception) {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, videoCapture
            )
            onCameraReady()
        } catch (exc: Exception) {
            Log.e("PrimeVideo", "CameraX binding failed", exc)
            Toast.makeText(activity, "Ошибка камеры: " + exc.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun destroy() {
        cameraExecutor.shutdown()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}

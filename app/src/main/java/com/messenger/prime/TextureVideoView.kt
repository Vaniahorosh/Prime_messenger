package com.messenger.prime

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView

/**
 * Custom TextureView for video playback that supports 100% reliable hardware clipping
 * (ViewOutlineProvider / setClipToOutline) and internal Matrix CENTER_CROP scaling.
 * Correctly respects video orientation rotation metadata (90° / 270°) to prevent any sideways rotation.
 * Supports silent looping previews with custom loop window duration for chat message bubbles.
 */
class TextureVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    fun interface OnPreparedListener {
        fun onPrepared(mp: MediaPlayer)
    }

    fun interface OnCompletionListener {
        fun onCompletion(mp: MediaPlayer)
    }

    fun interface OnProgressUpdateListener {
        fun onProgress(currentPositionMs: Int, totalDurationMs: Int)
    }

    fun interface OnFirstFrameRenderedListener {
        fun onFirstFrameRendered()
    }

    fun interface OnPlaybackErrorListener {
        fun onPlaybackError(what: Int, extra: Int)
    }

    private var mediaPlayer: MediaPlayer? = null
    private var videoPath: String? = null
    private var isPrepared = false
    private var isPreparing = false
    private var effectiveLoopDurationMs: Int = 0
    private var playbackSpeed: Float = 1.0f
    private var onPreparedListener: OnPreparedListener? = null
    private var onCompletionListener: OnCompletionListener? = null

    private var onFirstFrameRenderedListener: OnFirstFrameRenderedListener? = null
    private var onPlaybackErrorListener: OnPlaybackErrorListener? = null
    private var hasRenderedFirstFrame = false
    private var nativeSurface: Surface? = null

    @JvmField
    var onProgressUpdateListener: OnProgressUpdateListener? = null

    @JvmField
    var isMuted: Boolean = false

    @JvmField
    var loopDurationMs: Int = 0

    @JvmField
    var loopVideo: Boolean = false

    @JvmField
    var autoPlayOnPrepared: Boolean = true

    private var isSeeking = false
    private val loopHandler = Handler(Looper.getMainLooper())
    private val loopRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && isPrepared) {
                try {
                    val cur = if (isSeeking) 0 else mp.currentPosition
                    val dur = effectiveLoopDurationMs
                    val total = mp.duration
                    onProgressUpdateListener?.onProgress(cur, total)
                    if (dur > 0 && cur >= dur && !isSeeking) {
                        isSeeking = true
                        mp.seekTo(0)
                    }
                } catch (_: Exception) {}
            }
            val shouldTick = mediaPlayer != null && isPrepared &&
                (effectiveLoopDurationMs > 0 || loopVideo || onProgressUpdateListener != null)
            if (shouldTick) {
                loopHandler.removeCallbacks(this)
                loopHandler.postDelayed(this, 100)
            }
        }
    }

    init {
        surfaceTextureListener = this
    }

    fun setMuted(muted: Boolean) {
        this.isMuted = muted
        mediaPlayer?.let { mp ->
            try {
                if (muted) {
                    mp.setVolume(0f, 0f)
                } else {
                    mp.setVolume(1f, 1f)
                }
            } catch (_: Exception) {}
        }
    }

    fun setVideoPath(path: String) {
        if (this.videoPath == path && mediaPlayer != null && (isPrepared || isPreparing)) {
            if (isPrepared) {
                mediaPlayer?.let { mp ->
                    try {
                        updateCenterCropMatrix(mp.videoWidth, mp.videoHeight)
                        if (autoPlayOnPrepared && !mp.isPlaying) {
                            start()
                        } else if (mp.isPlaying && (effectiveLoopDurationMs > 0 || loopVideo || onProgressUpdateListener != null)) {
                            loopHandler.removeCallbacks(loopRunnable)
                            loopHandler.post(loopRunnable)
                        }
                    } catch (_: Exception) {}
                }
                if (hasRenderedFirstFrame) {
                    post { onFirstFrameRenderedListener?.onFirstFrameRendered() }
                }
            }
            return
        }
        this.videoPath = path
        if (isAvailable) {
            openVideo()
        }
    }

    fun setOnPreparedListener(listener: OnPreparedListener?) {
        this.onPreparedListener = listener
    }

    fun setOnCompletionListener(listener: OnCompletionListener?) {
        this.onCompletionListener = listener
    }

    fun setOnProgressUpdateListener(listener: OnProgressUpdateListener?) {
        this.onProgressUpdateListener = listener
    }

    fun setOnFirstFrameRenderedListener(listener: OnFirstFrameRenderedListener?) {
        this.onFirstFrameRenderedListener = listener
    }

    fun setOnPlaybackErrorListener(listener: OnPlaybackErrorListener?) {
        this.onPlaybackErrorListener = listener
    }

    private fun openVideo() {
        val path = videoPath ?: return
        val surfaceTexture = surfaceTexture ?: return

        stopPlayback(clearPath = false)
        hasRenderedFirstFrame = false
        isPreparing = true

        try {
            val surf = Surface(surfaceTexture)
            nativeSurface = surf
            mediaPlayer = MediaPlayer().apply {
                if (path.startsWith("content://")) {
                    val uri = Uri.parse(path)
                    var opened = false
                    try {
                        val afd = context.contentResolver.openAssetFileDescriptor(uri, "r")
                        if (afd != null) {
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                            opened = true
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TextureVideoView", "AFD open error: ${e.message}")
                    }
                    if (!opened) {
                        setDataSource(context, uri)
                    }
                } else if (path.startsWith("file://")) {
                    val cleanPath = Uri.parse(path).path ?: path.removePrefix("file://")
                    setDataSource(cleanPath)
                } else if (path.startsWith("http://") || path.startsWith("https://")) {
                    setDataSource(context, Uri.parse(path))
                } else {
                    setDataSource(path)
                }
                setSurface(surf)
                isLooping = false
                if (isMuted) {
                    try { setVolume(0f, 0f) } catch (_: Exception) {}
                }
                setOnVideoSizeChangedListener { _, w, h ->
                    updateCenterCropMatrix(w, h)
                    if (w > 0 && h > 0) {
                        ChatAdapter.cacheVideoDimensions(path, w, h)
                    }
                }
                setOnPreparedListener { mp ->
                    isPreparing = false
                    isPrepared = true
                    val vw = mp.videoWidth
                    val vh = mp.videoHeight
                    updateCenterCropMatrix(vw, vh)
                    if (vw > 0 && vh > 0) {
                        ChatAdapter.cacheVideoDimensions(path, vw, vh)
                    }
                    if (isMuted) {
                        try { mp.setVolume(0f, 0f) } catch (_: Exception) {}
                    }
                    if (playbackSpeed != 1.0f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        try {
                            val params = mp.playbackParams
                            params.speed = playbackSpeed
                            mp.playbackParams = params
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val totalDur = mp.duration
                    effectiveLoopDurationMs = if (loopDurationMs == -1) {
                        when {
                            totalDur <= 0 -> 0
                            totalDur <= 5000 -> totalDur
                            else -> 5000
                        }
                    } else {
                        loopDurationMs
                    }
                    if (autoPlayOnPrepared) {
                        start()
                    }
                    onPreparedListener?.onPrepared(mp)
                }
                setOnSeekCompleteListener { mp ->
                    isSeeking = false
                    try {
                        if (autoPlayOnPrepared && !mp.isPlaying) {
                            mp.start()
                        }
                        if (effectiveLoopDurationMs > 0 || loopVideo || onProgressUpdateListener != null) {
                            loopHandler.removeCallbacks(loopRunnable)
                            loopHandler.post(loopRunnable)
                        }
                    } catch (_: Exception) {}
                }
                setOnCompletionListener { mp ->
                    isSeeking = false
                    if (loopVideo || effectiveLoopDurationMs > 0) {
                        try {
                            isSeeking = true
                            mp.seekTo(0)
                            if (autoPlayOnPrepared && !mp.isPlaying) {
                                mp.start()
                            }
                        } catch (_: Exception) {}
                    } else {
                        onCompletionListener?.onCompletion(mp)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("TextureVideoView", "MediaPlayer error: what=$what extra=$extra path=$videoPath")
                    stopPlayback()
                    post { onPlaybackErrorListener?.onPlaybackError(what, extra) }
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopPlayback()
            post { onPlaybackErrorListener?.onPlaybackError(-1, -1) }
        }
    }

    fun start() {
        try {
            mediaPlayer?.start()
            if (effectiveLoopDurationMs > 0 || loopVideo || onProgressUpdateListener != null) {
                loopHandler.removeCallbacks(loopRunnable)
                loopHandler.post(loopRunnable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        loopHandler.removeCallbacks(loopRunnable)
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmOverloads
    fun stopPlayback(clearPath: Boolean = true) {
        loopHandler.removeCallbacksAndMessages(null)
        isSeeking = false
        isPreparing = false
        hasRenderedFirstFrame = false
        effectiveLoopDurationMs = 0
        if (clearPath) {
            videoPath = null
        }
        mediaPlayer?.let { mp ->
            try {
                mp.setOnPreparedListener(null)
                mp.setOnCompletionListener(null)
                mp.setOnSeekCompleteListener(null)
                mp.setOnErrorListener(null)
                mp.setOnVideoSizeChangedListener(null)
                mp.stop()
                mp.release()
            } catch (e: Exception) {
                // ignored
            }
        }
        mediaPlayer = null
        isPrepared = false
        try {
            nativeSurface?.release()
        } catch (_: Exception) {}
        nativeSurface = null
    }

    val isPlaying: Boolean
        get() = try { mediaPlayer?.isPlaying == true } catch (e: Exception) { false }

    val duration: Int
        get() = try { mediaPlayer?.duration ?: 0 } catch (e: Exception) { 0 }

    val currentPosition: Int
        get() = try { mediaPlayer?.currentPosition ?: 0 } catch (e: Exception) { 0 }

    fun seekTo(msec: Int) {
        try {
            mediaPlayer?.seekTo(msec)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSpeed(speed: Float) {
        this.playbackSpeed = speed
        mediaPlayer?.let { mp ->
            if (!isPrepared) return@let
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val params = mp.playbackParams
                    params.speed = speed
                    mp.playbackParams = params
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private val rotationCache = java.util.concurrent.ConcurrentHashMap<String, Int>()
    }

    private fun getRotationDegrees(): Int {
        val path = videoPath ?: return 0
        rotationCache[path]?.let { return it }

        var retriever: MediaMetadataRetriever? = null
        val rot = try {
            retriever = MediaMetadataRetriever()
            if (path.startsWith("content://") || path.startsWith("file://") || path.startsWith("http://") || path.startsWith("https://")) {
                retriever.setDataSource(context, Uri.parse(path))
            } else {
                retriever.setDataSource(path)
            }
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            rotationStr?.toInt() ?: 0
        } catch (e: Exception) {
            0
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
        }
        rotationCache[path] = rot
        return rot
    }

    private fun updateCenterCropMatrix(rawVideoWidth: Int, rawVideoHeight: Int) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (rawVideoWidth <= 0 || rawVideoHeight <= 0) return
        if (viewWidth <= 0 || viewHeight <= 0) {
            post { updateCenterCropMatrix(rawVideoWidth, rawVideoHeight) }
            return
        }

        val rotation = getRotationDegrees()
        val isSwapped = (rotation == 90 || rotation == 270)

        val displayedVideoWidth = if (isSwapped) rawVideoHeight.toFloat() else rawVideoWidth.toFloat()
        val displayedVideoHeight = if (isSwapped) rawVideoWidth.toFloat() else rawVideoHeight.toFloat()

        val scale = maxOf(viewWidth / displayedVideoWidth, viewHeight / displayedVideoHeight)

        val scaleX = (rawVideoWidth.toFloat() * scale) / viewWidth
        val scaleY = (rawVideoHeight.toFloat() * scale) / viewHeight

        val cx = viewWidth / 2f
        val cy = viewHeight / 2f

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, cx, cy)
        if (rotation != 0) {
            matrix.postRotate(rotation.toFloat(), cx, cy)
        }

        setTransform(matrix)
        invalidate()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        openVideo()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        mediaPlayer?.let { mp ->
            updateCenterCropMatrix(mp.videoWidth, mp.videoHeight)
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopPlayback()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        if (!hasRenderedFirstFrame) {
            hasRenderedFirstFrame = true
            post {
                onFirstFrameRenderedListener?.onFirstFrameRendered()
            }
        }
    }
}

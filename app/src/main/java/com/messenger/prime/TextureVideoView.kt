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

    private var mediaPlayer: MediaPlayer? = null
    private var videoPath: String? = null
    private var isPrepared = false
    private var playbackSpeed: Float = 1.0f
    private var onPreparedListener: OnPreparedListener? = null
    private var onCompletionListener: OnCompletionListener? = null

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
                    val cur = mp.currentPosition
                    val dur = loopDurationMs
                    val total = mp.duration
                    onProgressUpdateListener?.onProgress(cur, total)
                    if (dur > 0 && cur >= dur && !isSeeking) {
                        isSeeking = true
                        mp.seekTo(0)
                    }
                } catch (_: Exception) {}
            }
            if (loopDurationMs > 0 && mediaPlayer != null && isPrepared) {
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
        this.videoPath = path
        if (isAvailable) {
            openVideo()
        }
    }

    fun setOnPreparedListener(listener: OnPreparedListener) {
        this.onPreparedListener = listener
    }

    fun setOnCompletionListener(listener: OnCompletionListener) {
        this.onCompletionListener = listener
    }

    fun setOnProgressUpdateListener(listener: OnProgressUpdateListener) {
        this.onProgressUpdateListener = listener
    }

    private fun openVideo() {
        val path = videoPath ?: return
        val surfaceTexture = surfaceTexture ?: return

        stopPlayback()

        try {
            val surface = Surface(surfaceTexture)
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
                setSurface(surface)
                isLooping = false
                if (isMuted) {
                    try { setVolume(0f, 0f) } catch (_: Exception) {}
                }
                setOnVideoSizeChangedListener { _, w, h ->
                    updateCenterCropMatrix(w, h)
                }
                setOnPreparedListener { mp ->
                    isPrepared = true
                    updateCenterCropMatrix(mp.videoWidth, mp.videoHeight)
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
                    if (loopDurationMs == -1) {
                        val totalDur = mp.duration
                        loopDurationMs = when {
                            totalDur <= 3000 -> totalDur
                            totalDur <= 7000 -> 2000
                            else -> 2800
                        }
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
                    } catch (_: Exception) {}
                }
                setOnCompletionListener { mp ->
                    isSeeking = false
                    if (loopVideo || loopDurationMs > 0) {
                        try {
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
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun start() {
        try {
            mediaPlayer?.start()
            if (loopDurationMs > 0) {
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

    fun stopPlayback() {
        loopHandler.removeCallbacks(loopRunnable)
        isSeeking = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        isPrepared = false
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

    private fun getRotationDegrees(): Int {
        val path = videoPath ?: return 0
        var retriever: MediaMetadataRetriever? = null
        return try {
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
        val isSwapped = (rotation == 90 || rotation == 270) && (rawVideoWidth > rawVideoHeight)

        val realWidth = if (isSwapped) rawVideoHeight.toFloat() else rawVideoWidth.toFloat()
        val realHeight = if (isSwapped) rawVideoWidth.toFloat() else rawVideoHeight.toFloat()

        val videoAspect = realWidth / realHeight
        val viewAspect = viewWidth / viewHeight

        val scaleX: Float
        val scaleY: Float

        if (videoAspect > viewAspect) {
            scaleX = videoAspect / viewAspect
            scaleY = 1.0f
        } else {
            scaleX = 1.0f
            scaleY = viewAspect / videoAspect
        }

        val cx = viewWidth / 2f
        val cy = viewHeight / 2f

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, cx, cy)

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

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}

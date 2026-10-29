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

    private var mediaPlayer: MediaPlayer? = null
    private var videoPath: String? = null
    private var isPrepared = false
    private var playbackSpeed: Float = 1.0f
    private var onPreparedListener: OnPreparedListener? = null
    private var onCompletionListener: OnCompletionListener? = null

    @JvmField
    var isMuted: Boolean = false

    @JvmField
    var loopDurationMs: Int = 0

    @JvmField
    var loopVideo: Boolean = false

    private val loopHandler = Handler(Looper.getMainLooper())
    private val loopRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && isPrepared && isPlaying) {
                val dur = loopDurationMs
                if (dur > 0 && currentPosition >= dur) {
                    seekTo(0)
                    try {
                        mp.start()
                    } catch (_: Exception) {}
                }
            }
            if (loopDurationMs > 0 && isPlaying) {
                loopHandler.postDelayed(this, 150)
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

    private fun openVideo() {
        val path = videoPath ?: return
        val surfaceTexture = surfaceTexture ?: return

        stopPlayback()

        try {
            val surface = Surface(surfaceTexture)
            mediaPlayer = MediaPlayer().apply {
                if (path.startsWith("content://")) {
                    setDataSource(context, Uri.parse(path))
                } else {
                    setDataSource(path)
                }
                setSurface(surface)
                isLooping = false
                if (isMuted) {
                    setVolume(0f, 0f)
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
                    onPreparedListener?.onPrepared(mp)
                }
                setOnCompletionListener { mp ->
                    if (loopVideo || loopDurationMs > 0) {
                        seekTo(0)
                        try {
                            mp.start()
                        } catch (_: Exception) {}
                    } else {
                        onCompletionListener?.onCompletion(mp)
                    }
                }
                setOnErrorListener { _, _, _ ->
                    true // Silently handle preview decode errors
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
                loopHandler.postDelayed(loopRunnable, 150)
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
            if (path.startsWith("content://")) {
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

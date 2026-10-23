package com.messenger.prime

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView

/**
 * Custom TextureView for video playback that supports 100% reliable hardware clipping
 * (ViewOutlineProvider / setClipToOutline) and internal Matrix CENTER_CROP scaling.
 * Correctly respects video orientation rotation metadata (90° / 270°) to prevent any sideways rotation.
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

    init {
        surfaceTextureListener = this
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
                setDataSource(path)
                setSurface(surface)
                isLooping = false
                setOnVideoSizeChangedListener { _, w, h ->
                    updateCenterCropMatrix(w, h)
                }
                setOnPreparedListener { mp ->
                    isPrepared = true
                    updateCenterCropMatrix(mp.videoWidth, mp.videoHeight)
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
                    onCompletionListener?.onCompletion(mp)
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPlayback() {
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
        return try {
            val retriever = MediaMetadataRetriever()
            if (path.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(path))
            } else {
                retriever.setDataSource(path)
            }
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            retriever.release()
            rotationStr?.toInt() ?: 0
        } catch (e: Exception) {
            0
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

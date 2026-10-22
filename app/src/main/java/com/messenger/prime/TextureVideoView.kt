package com.messenger.prime

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView

/**
 * Custom TextureView for video playback that supports 100% reliable hardware clipping
 * (ViewOutlineProvider / setClipToOutline) and internal Matrix CENTER_CROP scaling
 * so video never overflows, stretches wide, or distorts.
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

    private fun updateCenterCropMatrix(videoWidth: Int, videoHeight: Int) {
        post {
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()

            if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return@post

            val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
            val viewAspect = viewWidth / viewHeight

            val scaleX: Float
            val scaleY: Float

            if (videoAspect > viewAspect) {
                scaleX = videoAspect / viewAspect
                scaleY = 1f
            } else {
                scaleX = 1f
                scaleY = viewAspect / videoAspect
            }

            val matrix = Matrix()
            matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
            setTransform(matrix)
            invalidate()
        }
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

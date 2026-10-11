package com.messenger.prime

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.messenger.prime.databinding.DialogMediaPlayerSheetBinding

class PrimeMusicPlayerDialog(private val context: Context) : PrimeMusicManager.PlayerListener {

    private var dialog: BottomSheetDialog? = null
    private var binding: DialogMediaPlayerSheetBinding? = null
    private var playlistAdapter: MusicTrackAdapter? = null
    private var isUserTrackingSeekBar = false

    fun show(initialTrack: TrackItem? = null, playlist: List<TrackItem> = emptyList()) {
        if (dialog != null && dialog?.isShowing == true) {
            if (initialTrack != null) {
                PrimeMusicManager.playTrack(context, initialTrack, playlist)
            }
            return
        }

        val b = DialogMediaPlayerSheetBinding.inflate(LayoutInflater.from(context))
        binding = b

        val d = BottomSheetDialog(context, R.style.Theme_Prime_BottomSheetDialog)
        d.setContentView(b.root)

        d.setOnShowListener {
            val bottomSheetView = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheetView != null) {
                bottomSheetView.setBackgroundColor(Color.TRANSPARENT)
                val lp = bottomSheetView.layoutParams
                if (lp != null) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    bottomSheetView.layoutParams = lp
                }
                val behavior = BottomSheetBehavior.from(bottomSheetView)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = true
            }
        }

        if (d.window != null) {
            d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            d.window?.setDimAmount(0.4f)
        }

        // Setup BlurView
        var activityRoot: ViewGroup? = null
        if (context is android.app.Activity) {
            activityRoot = context.window.decorView.findViewById(android.R.id.content)
        }
        
        if (activityRoot != null) {
            val windowBackground = (context as android.app.Activity).window.decorView.background
            val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) Color.parseColor("#400F172A") else Color.parseColor("#40154B87")
            
            b.blurViewPlayerSheet.setupBlur(activityRoot, 20f, overlayColor, windowBackground)
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(context)
        val colorStateList = ColorStateList.valueOf(accentColor)

        b.sbPlayerProgress.progressTintList = colorStateList
        b.sbPlayerProgress.thumbTintList = colorStateList
        b.btnPlayerPlayPauseContainer.backgroundTintList = colorStateList
        b.btnPlayerSaveCollection.imageTintList = colorStateList

        // Enable Marquee text animation
        b.tvPlayerTitle.isSelected = true

        playlistAdapter = MusicTrackAdapter(
            tracks = PrimeMusicManager.getCurrentPlaylist(),
            onTrackClick = { track ->
                PrimeMusicManager.playTrack(context, track, PrimeMusicManager.getCurrentPlaylist())
            },
            onSelectionChanged = { _ -> }
        )
        b.rvPlayerPlaylist.adapter = playlistAdapter

        b.btnPlayerPlayPauseContainer.setOnClickListener {
            PrimeMusicManager.togglePlayPause(context)
        }

        b.btnPlayerPrev.setOnClickListener {
            PrimeMusicManager.playPreviousTrack(context)
        }

        b.btnPlayerNext.setOnClickListener {
            PrimeMusicManager.playNextTrack(context)
        }

        b.btnPlayerSaveCollection.setOnClickListener {
            val track = PrimeMusicManager.getCurrentTrack()
            if (track != null) {
                val saved = PrimeMusicManager.saveTrackToCollection(context, track.path, track.title, track.artist)
                if (saved != null) {
                    Toast.makeText(context, "Трек сохранен в коллекцию", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Трек уже в коллекции", Toast.LENGTH_SHORT).show()
                }
            }
        }

        b.sbPlayerProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    b.tvPlayerCurrentTime.text = PrimeMusicManager.formatDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserTrackingSeekBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserTrackingSeekBar = false
                val pos = seekBar?.progress ?: 0
                PrimeMusicManager.seekTo(pos)
            }
        })

        d.setOnDismissListener {
            PrimeMusicManager.removeListener(this)
            dialog = null
            binding = null
        }

        dialog = d
        PrimeMusicManager.addListener(this)

        d.show()

        if (initialTrack != null) {
            PrimeMusicManager.playTrack(context, initialTrack, playlist)
        } else if (PrimeMusicManager.getCurrentTrack() == null && playlist.isNotEmpty()) {
            PrimeMusicManager.playTrack(context, playlist[0], playlist)
        } else {
            updateUI(PrimeMusicManager.getCurrentTrack())
        }
    }

    private fun updateUI(track: TrackItem?) {
        val b = binding ?: return
        if (track == null) {
            b.tvPlayerTitle.text = "Трек не выбран"
            b.tvPlayerArtist.text = "Выберите трек из списка"
            b.ivPlayerCover.setImageResource(R.drawable.ic_music)
            return
        }

        b.tvPlayerTitle.text = track.title
        b.tvPlayerArtist.text = track.artist

        val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        if (!track.coverPath.isNullOrEmpty()) {
            try {
                val bmp = BitmapFactory.decodeFile(track.coverPath)
                if (bmp != null) {
                    b.ivPlayerCover.setImageBitmap(bmp)
                    val adaptiveTint = extractAdaptiveOverlayColor(bmp, isDark)
                    b.blurViewPlayerSheet.setOverlayColor(adaptiveTint)
                } else {
                    b.ivPlayerCover.setImageResource(R.drawable.ic_music)
                    val defaultTint = if (isDark) Color.parseColor("#400F172A") else Color.parseColor("#40154B87")
                    b.blurViewPlayerSheet.setOverlayColor(defaultTint)
                }
            } catch (_: Exception) {
                b.ivPlayerCover.setImageResource(R.drawable.ic_music)
            }
        } else {
            b.ivPlayerCover.setImageResource(R.drawable.ic_music)
            val defaultTint = if (isDark) Color.parseColor("#400F172A") else Color.parseColor("#40154B87")
            b.blurViewPlayerSheet.setOverlayColor(defaultTint)
        }

        playlistAdapter?.updateTracks(PrimeMusicManager.getCurrentPlaylist())
        playlistAdapter?.setActiveTrack(track.id, PrimeMusicManager.isPlaying())
    }

    private fun extractAdaptiveOverlayColor(bitmap: Bitmap, isDarkTheme: Boolean): Int {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return if (isDarkTheme) Color.parseColor("#400F172A") else Color.parseColor("#40154B87")
        }

        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        var sampleCount = 0

        val stepX = (width / 12).coerceAtLeast(1)
        val stepY = (height / 12).coerceAtLeast(1)

        for (x in 0 until width step stepX) {
            for (y in 0 until height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                redSum += Color.red(pixel)
                greenSum += Color.green(pixel)
                blueSum += Color.blue(pixel)
                sampleCount++
            }
        }

        if (sampleCount == 0) {
            return if (isDarkTheme) Color.parseColor("#400F172A") else Color.parseColor("#40154B87")
        }

        val avgRed = (redSum / sampleCount).toInt()
        val avgGreen = (greenSum / sampleCount).toInt()
        val avgBlue = (blueSum / sampleCount).toInt()

        val alpha = if (isDarkTheme) 0x65 else 0x48
        return Color.argb(alpha, avgRed, avgGreen, avgBlue)
    }

    override fun onTrackChanged(track: TrackItem?) {
        updateUI(track)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        val b = binding ?: return
        b.ivPlayerPlayPause.setImageResource(if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play)
        val track = PrimeMusicManager.getCurrentTrack()
        playlistAdapter?.setActiveTrack(track?.id, isPlaying)
    }

    override fun onProgressUpdated(positionMs: Long, durationMs: Long) {
        val b = binding ?: return
        if (!isUserTrackingSeekBar) {
            b.sbPlayerProgress.max = durationMs.toInt()
            b.sbPlayerProgress.progress = positionMs.toInt()
            b.tvPlayerCurrentTime.text = PrimeMusicManager.formatDuration(positionMs)
            b.tvPlayerTotalTime.text = PrimeMusicManager.formatDuration(durationMs)
        }
    }
}

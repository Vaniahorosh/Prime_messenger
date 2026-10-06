package com.messenger.prime

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.messenger.prime.databinding.ActivityChatPlaceholderBinding

class ChatPlaceholderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatPlaceholderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = resources.configuration
        val isTablet = config.smallestScreenWidthDp >= 600
        val isEmbedded = try {
            androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)
        } catch (_: Exception) {
            false
        }

        // Safety guard: ChatPlaceholderActivity must ONLY exist as a secondary (right-pane) embedded window on tablets.
        // If it is ever brought to the front standalone or as primary on the left side, finish it immediately!
        if (!isTablet || !isEmbedded) {
            finish()
            return
        }

        binding = ActivityChatPlaceholderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val wallpaper = binding.vRightPaneWallpaper
        wallpaper.updateThemeColors()
        wallpaper.startEntranceAnimation()

        val blur = binding.blurRightPaneEmpty
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val overlayColor = if (isDark) Color.parseColor("#400F172A") else Color.parseColor("#40154B87")
        blur.setupBlur(binding.root, 16f, overlayColor, wallpaper.background)
    }
}

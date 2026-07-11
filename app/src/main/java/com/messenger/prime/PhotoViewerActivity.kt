package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.messenger.prime.databinding.ActivityPhotoViewerBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

/**
 * Полноэкранный просмотр аватарки в исходном качестве.
 * Открывается по второму "дотягиванию" вниз на SettingsActivity.
 */
class PhotoViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoViewerBinding

    companion object {
        private const val EXTRA_AVATAR_URI = "extra_avatar_uri"

        fun start(context: Context, avatarUri: String?) {
            val intent = Intent(context, PhotoViewerActivity::class.java)
            intent.putExtra(EXTRA_AVATAR_URI, avatarUri)
            context.startActivity(intent)
            if (context is AppCompatActivity) {
                context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK

        val avatarUriString = intent.getStringExtra(EXTRA_AVATAR_URI)
        if (avatarUriString != null) {
            binding.ivFullPhoto.setImageURI(Uri.parse(avatarUriString))
        } else {
            binding.ivFullPhoto.setImageResource(R.drawable.ic_person)
        }

        binding.btnBack.setOnClickListener { finishWithSlide() }

        // Свайп для закрытия — как и на остальных активити приложения
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)
    }

    override fun onBackPressed() {
        finishWithSlide()
    }

    private fun finishWithSlide() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
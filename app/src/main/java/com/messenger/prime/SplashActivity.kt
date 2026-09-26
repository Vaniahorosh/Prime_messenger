package com.messenger.prime

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupEdgeToEdge()

        setContent {
            val darkTheme = isSystemInDarkTheme()
            PrimeTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LavaBackgroundState.onActivityResumed()
                    AnimatedBackground(darkTheme = darkTheme)

                    AndroidView(
                        factory = { context ->
                            PrimeSplashView(context).apply {
                                setOnAnimationFinishedListener {
                                    checkLoginAndNavigate()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun checkLoginAndNavigate() {
        val sharedPreferences = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        val nextIntent = if (isLoggedIn) {
            val banExpiry = sharedPreferences.getLong("ban_expiry", 0)
            val isBanned = if (banExpiry == -1L) true else System.currentTimeMillis() < banExpiry

            if (isBanned) {
                Intent(this, BanActivity::class.java).apply {
                    val currentUser = sharedPreferences.getString("current_user", "") ?: ""
                    val userName = sharedPreferences.getString("${currentUser}_name", "Пользователь")
                    putExtra("EXTRA_USER_NAME", userName)
                    putExtra("EXTRA_REASON", sharedPreferences.getString("ban_reason", "Нарушение"))
                    putExtra("EXTRA_VALUE", sharedPreferences.getLong("ban_value", 0))
                    putExtra("EXTRA_UNIT", sharedPreferences.getString("ban_unit", "S"))
                }
            } else {
                Intent(this, ChatListActivity::class.java)
            }
        } else {
            Intent(this, HiActivity::class.java)
        }

        startActivity(nextIntent)
        overridePendingTransition(R.anim.fade_in_slow, R.anim.stay_slow)
        finish()
    }
}

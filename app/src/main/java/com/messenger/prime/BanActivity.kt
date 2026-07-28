package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.messenger.prime.databinding.ActivityBanBinding
import java.text.SimpleDateFormat
import java.util.*

class BanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBanBinding
    private val handler = Handler(Looper.getMainLooper())
    
    private var banExpiryTimestamp: Long = 0
    private var isPermanent = false
    private var unit: String = ""

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (updateTimeText()) {
                handler.postDelayed(this, 1000)
            } else {
                returnToApp()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Параметры из интента
        val userName = intent.getStringExtra("EXTRA_USER_NAME") ?: "Пользователь"
        val reason = intent.getStringExtra("EXTRA_REASON") ?: "Нарушение правил"
        val value = intent.getLongExtra("EXTRA_VALUE", 0)
        unit = intent.getStringExtra("EXTRA_UNIT") ?: "S"

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        banExpiryTimestamp = sharedPrefs.getLong("ban_expiry", 0)

        isPermanent = value <= 0
        setupUI(userName, reason)
    }

    override fun onResume() {
        super.onResume()
        if (!isPermanent && (unit == "S" || unit == "M")) {
            handler.post(timerRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)
    }

    private fun setupUI(userName: String, reason: String) {
        binding.tvBanTitle.text = "$userName, ваш аккаунт заблокирован."
        binding.tvBanReason.text = "Причина: $reason"

        if (binding.tsBanTime.childCount == 0) {
            binding.tsBanTime.setFactory {
                TextView(this).apply {
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            }
        }

        if (isPermanent) {
            binding.tsBanTime.setText("Навсегда")
            binding.tvBanExpiry.visibility = View.GONE
            binding.btnBanAction.text = "Удалить аккаунт"
            binding.tvDeleteWarning.visibility = View.VISIBLE
            
            binding.btnBanAction.setOnClickListener { deleteAccountAndFinish() }
        } else {
            updateTimeText()
            
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            binding.tvBanExpiry.text = "Блокировка спадет: ${sdf.format(Date(banExpiryTimestamp))}"
            
            binding.btnBanAction.text = "Выйти из приложения"
            binding.btnBanAction.setOnClickListener { finishAffinity() }
        }
    }


    private fun updateTimeText(): Boolean {
        val currentTime = System.currentTimeMillis()
        val totalSecondsLeft = (banExpiryTimestamp - currentTime) / 1000

        if (totalSecondsLeft <= 0) {
            binding.tsBanTime.setText("0 сек.")
            return false
        }

        val text = when {
            totalSecondsLeft >= 86400 -> "${totalSecondsLeft / 86400} дн."
            totalSecondsLeft >= 3600 -> "${totalSecondsLeft / 3600} ч."
            totalSecondsLeft >= 60 -> "${totalSecondsLeft / 60} мин."
            else -> "$totalSecondsLeft сек."
        }
        binding.tsBanTime.setText(text)
        return true
    }

    private fun returnToApp() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            remove("ban_reason")
            remove("ban_value")
            remove("ban_unit")
            remove("ban_expiry")
            apply()
        }

        val intent = Intent(this, ChatListActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun deleteAccountAndFinish() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        
        sharedPrefs.edit().apply {
            remove("is_logged_in")
            remove("current_user")
            remove(currentUser)
            remove("${currentUser}_name")
            remove("${currentUser}_avatar")
            remove("ban_reason")
            remove("ban_value")
            remove("ban_unit")
            remove("ban_expiry")
            apply()
        }

        val intent = Intent(this, HiActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}

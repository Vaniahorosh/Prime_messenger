package com.messenger.prime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.messenger.prime.databinding.ActivityHiBinding

class HiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHiBinding
    private val handler = Handler(Looper.getMainLooper())
    private var currentDataIndex = -1
    private var isSequenceRunning = false

    private data class DynamicContent(val slogan: String, val button: String)

    private val contents = listOf(
        DynamicContent("Всегда будь в", "Прайме!"),
        DynamicContent("Всегда сообщение", "Быстрее!"),
        DynamicContent("Всегда будь на", "Связи!"),
        DynamicContent("Всегда будь в", "Приватности!"),
        DynamicContent("Всегда мы", "Кастомнее!")
    )

    private val dynamicRunnable = object : Runnable {
        override fun run() {
            if (!isSequenceRunning) return
            var nextIndex: Int
            do {
                nextIndex = (contents.indices).random()
            } while (nextIndex == currentDataIndex)

            currentDataIndex = nextIndex
            val content = contents[currentDataIndex]

            binding.textSwitcherSlogan.setText(content.slogan)
            binding.textSwitcherButton.setText(content.button)

            handler.postDelayed(this, 3000)
        }
    }

    private val allViews: List<View> by lazy {
        listOf(
            binding.btnExit,
            binding.ivLogo,
            binding.textSwitcherSlogan,
            binding.btnPrime,
            binding.btnPermissions,
            binding.tvLicense
        )
    }

    private var isPendingNavigationAfterPermissions = false

    private val requestAllPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        updatePermissionsButtonUi()
        val missing = getMissingPermissions()
        if (missing.isEmpty()) {
            PrimeNotification.show(this, "Разрешения приняты! Переходим...")
            if (isPendingNavigationAfterPermissions) {
                isPendingNavigationAfterPermissions = false
                binding.btnPrime.isEnabled = false
                binding.btnExit.isEnabled = false
                binding.btnPermissions.isEnabled = false
                stopDynamicSequence()
                fadeOutAndNavigateToLogin()
            }
        } else {
            isPendingNavigationAfterPermissions = false
            PrimeNotification.show(this, "Для перехода в приложение требуются разрешения")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.fade_in_slow,
                R.anim.stay_slow
            )
        }

        val sharedPreferences = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            val banExpiry = sharedPreferences.getLong("ban_expiry", 0)
            val isBanned = if (banExpiry == -1L) true else System.currentTimeMillis() < banExpiry
            
            if (isBanned) {
                val intent = Intent(this, BanActivity::class.java).apply {
                    val currentUser = sharedPreferences.getString("current_user", "") ?: ""
                    val userName = sharedPreferences.getString("${currentUser}_name", "Пользователь")
                    putExtra("EXTRA_USER_NAME", userName)
                    putExtra("EXTRA_REASON", sharedPreferences.getString("ban_reason", "Нарушение"))
                    putExtra("EXTRA_VALUE", sharedPreferences.getLong("ban_value", 0))
                    putExtra("EXTRA_UNIT", sharedPreferences.getString("ban_unit", "S"))
                }
                startActivity(intent)
            } else {
                startActivity(Intent(this, ChatListActivity::class.java))
            }
            finish()
            return
        }

        binding = ActivityHiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge()

        binding.composeBackground.setContent {
            PrimeTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LavaBackgroundState.onActivityResumed()
                    AnimatedBackground(
                        darkTheme = isSystemInDarkTheme()
                    )
                }
            }
        }

        setupTextSwitcher()
        binding.tvLicense.setTextColor(Color.WHITE)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val headerParams = binding.topHeader.layoutParams as ConstraintLayout.LayoutParams
            headerParams.height = systemBarsInsets.top + (32 * resources.displayMetrics.density).toInt()
            binding.topHeader.layoutParams = headerParams

            val backParams = binding.btnExit.layoutParams as ConstraintLayout.LayoutParams
            backParams.topMargin = systemBarsInsets.top + (8 * resources.displayMetrics.density).toInt()
            binding.btnExit.layoutParams = backParams

            binding.root.setPadding(0, 0, 0, systemBarsInsets.bottom)

            windowInsets
        }

        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        binding.ivLogo.startAnimation(fadeIn)
        binding.textSwitcherSlogan.startAnimation(fadeIn)
        binding.btnPrime.startAnimation(fadeIn)
        binding.btnPermissions.startAnimation(fadeIn)

        updatePermissionsButtonUi()

        binding.btnPermissions.setOnClickListener {
            isPendingNavigationAfterPermissions = false
            showPermissionsDialog()
        }

        binding.btnPrime.setOnClickListener {
            val missing = getMissingPermissions()
            if (missing.isNotEmpty()) {
                isPendingNavigationAfterPermissions = true
                showPermissionsDialog()
            } else {
                binding.btnPrime.isEnabled = false
                binding.btnExit.isEnabled = false
                binding.btnPermissions.isEnabled = false
                stopDynamicSequence()
                fadeOutAndNavigateToLogin()
            }
        }

        binding.btnExit.setOnClickListener {
            finishAffinity()
        }
    }

    private fun getAllRequiredPermissions(): Array<String> {
        val perms = mutableListOf<String>()
        perms.add(Manifest.permission.CAMERA)
        perms.add(Manifest.permission.READ_CONTACTS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(Manifest.permission.READ_MEDIA_VIDEO)
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        return perms.toTypedArray()
    }

    private fun getMissingPermissions(): List<String> {
        return getAllRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun updatePermissionsButtonUi() {
        val missing = getMissingPermissions()
        if (missing.isEmpty()) {
            binding.btnPermissions.text = "Все разрешения приняты ✓"
            binding.btnPermissions.strokeColor = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            binding.btnPermissions.setTextColor(Color.parseColor("#4CAF50"))
            binding.btnPermissions.setIconTintResource(R.color.prime_success)
        } else {
            binding.btnPermissions.text = "Разрешения приложения (${missing.size})"
            binding.btnPermissions.strokeColor = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))
            binding.btnPermissions.setTextColor(Color.WHITE)
            binding.btnPermissions.setIconTintResource(R.color.white)
        }
    }

    private fun showPermissionsDialog() {
        val missing = getMissingPermissions()
        if (missing.isEmpty()) return

        MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
            .setTitle("Проверка готовности к переходу")
            .setMessage("Для перехода в Prime Messenger необходимо предоставить разрешительные доступы (Bluetooth, уведомления, камера, галерея, контакты).")
            .setPositiveButton("Предоставить все") { _, _ ->
                requestAllPermissionsLauncher.launch(missing.toTypedArray())
            }
            .setNegativeButton("Отмена") { _, _ ->
                isPendingNavigationAfterPermissions = false
            }
            .show()
    }

    private fun setupTextSwitcher() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val generalTextColor = Color.WHITE
        val buttonTextColor = if (isDark) Color.WHITE else Color.parseColor("#154B87")

        binding.textSwitcherSlogan.setFactory {
            TextView(this).apply {
                gravity = Gravity.CENTER
                textSize = 20f
                setTextColor(generalTextColor)
                setTypeface(null, Typeface.BOLD)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
        }

        binding.textSwitcherButton.setFactory {
            TextView(this).apply {
                gravity = Gravity.CENTER
                textSize = 22f
                setTextColor(buttonTextColor)
                setTypeface(null, Typeface.BOLD)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }

        val inAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_top_alpha)
        val outAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom)

        binding.textSwitcherSlogan.inAnimation = inAnim
        binding.textSwitcherSlogan.outAnimation = outAnim
        
        binding.textSwitcherButton.inAnimation = inAnim
        binding.textSwitcherButton.outAnimation = outAnim
    }

    private fun startDynamicSequence() {
        if (isSequenceRunning) return
        isSequenceRunning = true
        handler.removeCallbacks(dynamicRunnable)
        handler.post(dynamicRunnable)
    }

    private fun stopDynamicSequence() {
        isSequenceRunning = false
        handler.removeCallbacks(dynamicRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            allViews.forEach { view ->
                view.animate().cancel()
                view.alpha = 1f
            }
            binding.btnPrime.isEnabled = true
            binding.btnExit.isEnabled = true
            binding.btnPermissions.isEnabled = true
            updatePermissionsButtonUi()
            startDynamicSequence()
        }
    }

    override fun onPause() {
        super.onPause()
        stopDynamicSequence()
    }

    private fun fadeOutAndNavigateToLogin() {
        LavaBackgroundState.onTransitionStart()
        val fadeOutDuration = 600L

        allViews.forEach { view ->
            view.animate()
                .alpha(0f)
                .setDuration(fadeOutDuration)
                .start()
        }

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_slow, R.anim.stay_slow)
    }

    override fun finish() {
        LavaBackgroundState.onTransitionStart()
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

package com.messenger.prime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.button.MaterialButton
import com.messenger.prime.databinding.ActivityHiBinding
import eightbitlab.com.blurview.BlurView

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
            binding.composePromoCard,
            binding.btnPrime,
            binding.tvLicense
        )
    }

    private var isPendingNavigationAfterPermissions = false

    private val requestAllPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val missing = getMissingPermissions()
        if (missing.isEmpty()) {
            PrimeNotification.show(this, "Разрешения приняты! Переходим...")
            if (isPendingNavigationAfterPermissions) {
                isPendingNavigationAfterPermissions = false
                binding.btnPrime.isEnabled = false
                binding.btnExit.isEnabled = false
                stopDynamicSequence()
                fadeOutAndNavigateToLogin()
            }
        } else {
            isPendingNavigationAfterPermissions = false
            PrimeNotification.show(this, "Для перехода в приложение требуются разрешения")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)

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

        binding.composePromoCard.setContent {
            PrimeTheme {
                PrimeFeaturePromoCard(
                    onSlideChanged = { prefix, button ->
                        binding.textSwitcherSlogan.setText(prefix)
                        binding.textSwitcherButton.setText(button)
                    }
                )
            }
        }

        setupTextSwitcher()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        binding.tvLicense.setTextColor(if (isDark) Color.WHITE else Color.parseColor("#64748B"))
        binding.btnExit.setColorFilter(if (isDark) Color.WHITE else Color.parseColor("#1E293B"))

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

        runIntroHeroAnimation()

        binding.btnPrime.setOnClickListener {
            val missing = getMissingPermissions()
            if (missing.isNotEmpty()) {
                showPermissionsBlurDialog()
            } else {
                binding.btnPrime.isEnabled = false
                binding.btnExit.isEnabled = false
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
        
        // Камера и Контакты
        perms.add(Manifest.permission.CAMERA)
        perms.add(Manifest.permission.READ_CONTACTS)

        // Геолокация (требуется для поиска устройств поблизости по Bluetooth)
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Разрешения для Bluetooth на Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        // Уведомления и медиафайлы на Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(Manifest.permission.READ_MEDIA_VIDEO)
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            // Память для ранних версий Android
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        return perms.toTypedArray()
    }

    private fun getMissingPermissions(): List<String> {
        return getAllRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showPermissionsBlurDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_permissions_blur, null)
        val blurCard = dialogView.findViewById<BlurView>(R.id.blurPermissionsCard)
        val btnAcceptAll = dialogView.findViewById<MaterialButton>(R.id.btnAcceptAllPermissions)
        val btnContinueWithout = dialogView.findViewById<MaterialButton>(R.id.btnContinueWithoutPermissions)
        val btnExitApp = dialogView.findViewById<MaterialButton>(R.id.btnExitApp)

        if (blurCard != null) {
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
                ?: window.decorView as ViewGroup
            val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) Color.parseColor("#450F172A") else Color.parseColor("#45154B87")
            blurCard.setupBlur(rootView, 20f, overlayColor, window.decorView.background)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dialog.window != null) {
            try {
                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val params = dialog.window?.attributes
                if (params != null) {
                    params.blurBehindRadius = 60
                    dialog.window?.attributes = params
                }
            } catch (_: Throwable) {}
        }

        btnAcceptAll?.setOnClickListener {
            dialog.dismiss()
            isPendingNavigationAfterPermissions = true
            val missing = getMissingPermissions()
            if (missing.isNotEmpty()) {
                requestAllPermissionsLauncher.launch(missing.toTypedArray())
            } else {
                fadeOutAndNavigateToLogin()
            }
        }

        btnContinueWithout?.setOnClickListener {
            dialog.dismiss()
            binding.btnPrime.isEnabled = false
            binding.btnExit.isEnabled = false
            stopDynamicSequence()
            fadeOutAndNavigateToLogin()
        }

        btnExitApp?.setOnClickListener {
            dialog.dismiss()
            finishAffinity()
        }

        blurCard?.apply {
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
            translationY = 50f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(300L)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()
        }

        dialog.show()
    }

    private fun runIntroHeroAnimation() {
        // Изначально гарантируем полную невидимость до начала анимации
        binding.btnExit.visibility = View.INVISIBLE
        binding.btnExit.alpha = 0f
        binding.btnExit.translationX = -60f

        binding.centerContainer.visibility = View.INVISIBLE
        binding.centerContainer.alpha = 0f
        binding.centerContainer.translationY = 80f

        binding.tvLicense.visibility = View.INVISIBLE
        binding.tvLicense.alpha = 0f
        binding.tvLicense.translationY = 50f

        binding.ivLogo.visibility = View.INVISIBLE
        binding.ivLogo.alpha = 0f
        binding.ivLogo.scaleX = 0.5f
        binding.ivLogo.scaleY = 0.5f

        binding.root.post {
            val screenHeight = resources.displayMetrics.heightPixels.toFloat()
            val logoCenterInTop = binding.ivLogo.top + binding.ivLogo.height / 2f
            val deltaY = (screenHeight / 2f) - logoCenterInTop

            binding.ivLogo.translationY = deltaY
            binding.ivLogo.visibility = View.VISIBLE

            // 1. Появление логотипа в центре
            binding.ivLogo.animate()
                .alpha(1f)
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(550L)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction {
                    // 2. Плавный отъезд логотипа наверх к стрелке назад
                    binding.ivLogo.animate()
                        .translationY(0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(750L)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .withEndAction {
                            // 3. Выезд остального интерфейса из тени:
                            binding.btnExit.visibility = View.VISIBLE
                            binding.btnExit.animate()
                                .alpha(1f)
                                .translationX(0f)
                                .setDuration(500L)
                                .setInterpolator(DecelerateInterpolator())
                                .start()

                            binding.centerContainer.visibility = View.VISIBLE
                            binding.centerContainer.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(600L)
                                .setInterpolator(DecelerateInterpolator())
                                .start()

                            binding.tvLicense.visibility = View.VISIBLE
                            binding.tvLicense.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setStartDelay(120L)
                                .setDuration(550L)
                                .setInterpolator(DecelerateInterpolator())
                                .start()
                        }
                        .start()
                }
                .start()
        }
    }

    private fun setupTextSwitcher() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val generalTextColor = if (isDark) Color.WHITE else Color.parseColor("#1E293B")
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
        // Оставляем управление текстом промо-блоку для 100% синхронизации
        isSequenceRunning = false
        handler.removeCallbacks(dynamicRunnable)
    }

    private fun stopDynamicSequence() {
        isSequenceRunning = false
        handler.removeCallbacks(dynamicRunnable)
    }

    override fun onResume() {
        super.onResume()
        LavaBackgroundState.onActivityResumed()
        if (::binding.isInitialized) {
            allViews.forEach { view ->
                view.animate().cancel()
                view.alpha = 1f
            }
            binding.btnPrime.isEnabled = true
            binding.btnExit.isEnabled = true
            startDynamicSequence()
        }
    }

    override fun onPause() {
        super.onPause()
        stopDynamicSequence()
    }

    override fun onDestroy() {
        stopDynamicSequence()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
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
        PrimeTransitions.applyOpenTransition(this)
    }

    override fun finish() {
        LavaBackgroundState.onTransitionStart()
        super.finish()
        PrimeTransitions.applyCloseTransition(this)
    }
}

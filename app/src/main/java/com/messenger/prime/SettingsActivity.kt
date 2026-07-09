package com.messenger.prime

import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.messenger.prime.databinding.ActivitySettingsBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private var currentAvatarUri: String? = null
    private var isAppBarFullyExpanded = false
    private var isButtonsCompact = false
    private val argbEvaluator = ArgbEvaluator()

    // Цвета для морфинга кнопок между "развёрнутым" и "свёрнутым" видом
    private val colorBlue by lazy { ContextCompat.getColor(this, R.color.prime_background_blue) }
    private val colorRed = Color.parseColor("#D32F2F")
    private val colorWhite = Color.WHITE

    // ===== "Летающий" текст имени/статуса =====
    private var expandedNamePos = PointF(0f, 0f)
    private var collapsedNamePos = PointF(0f, 0f)
    private var expandedStatusPos = PointF(0f, 0f)
    private var collapsedStatusPos = PointF(0f, 0f)
    private var titlePositionsReady = false

    // ===== Pull-жест до fullscreen-фото =====
    private val pullThresholdPx by lazy { resources.displayMetrics.density * 110f }
    private var photoDragStartRawY = 0f
    private var isDraggingPhoto = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ==========================================
        // 1. ДЕЛАЕМ СТАТУС-БАР ПРОЗРАЧНЫМ
        // ==========================================
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        // Настраиваем кнопку назад внутри родного Тулбара
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // ==========================================
        // 2. СТАРТУЕМ В СВЁРНУТОМ СОСТОЯНИИ + ИЗМЕРЯЕМ
        //    крайние позиции текста для "перелёта"
        // ==========================================
        setupFloatingTitleMeasurement()

        // ==========================================
        // 3. АНИМАЦИЯ ЦВЕТА СТАТУС-БАРА / КНОПОК / ПОЗИЦИИ ТЕКСТА
        // ==========================================
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@addOnOffsetChangedListener

            // Процент сжатия: 0 = полностью развёрнуто, 1 = полностью свёрнуто
            val percentage = kotlin.math.abs(verticalOffset).toFloat() / totalScrollRange.toFloat()

            // Плавное появление аватарки в тулбаре
            binding.collapsedTitleLayout.alpha = if (percentage > 0.8f) (percentage - 0.8f) / 0.2f else 0f

            // Перелёт имени/статуса между позицией у фото и позицией в тулбаре
            updateFloatingTitlePosition(percentage)

            // Перекрашиваем статус-бар, когда шапка полностью свернулась
            if (kotlin.math.abs(verticalOffset) >= totalScrollRange - 10) {
                window.statusBarColor = ContextCompat.getColor(this, R.color.prime_background_blue)
            } else {
                window.statusBarColor = Color.TRANSPARENT
            }

            // "Морфинг" кнопок Фото/Настройки/Выход между большим и компактным видом
            updateButtonsAppearance(percentage)

            // Overscroll-логика (переход в fullscreen) разрешена ТОЛЬКО когда
            // шапка полностью развёрнута
            isAppBarFullyExpanded = verticalOffset == 0
        }

        // ==========================================
        // 4. ПОДКЛЮЧЕНИЕ СВАЙПА ПО ЭКРАНУ (SLIDR)
        // ==========================================
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)

        // ==========================================
        // 5. PULL ВНИЗ → FULLSCREEN ФОТО В ИСХОДНОМ КАЧЕСТВЕ
        //    Работает двумя способами: тянуть за контент под фото
        //    ИЛИ тянуть прямо по самому фото.
        // ==========================================
        binding.nestedScrollView.isPullEnabled = { isAppBarFullyExpanded }
        binding.nestedScrollView.onPullProgress = { dragPx -> handlePullProgress(dragPx) }
        binding.nestedScrollView.onPullReleased = { reached -> handlePullReleased(reached) }
        binding.nestedScrollView.onPullThresholdReached = { handlePullThresholdReached() }

        setupPhotoDragToOpen()

        // ==========================================
        // 6. ЗАГРУЗКА И УСТАНОВКА ДАННЫХ
        // ==========================================
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""

        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь")
        val savedPassword = sharedPrefs.getString(currentUser, "")
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        currentAvatarUri = savedAvatarUri

        binding.tvUserNameExpanded.text = savedName
        binding.tvUserNameCollapsed.text = savedName
        binding.tvUserNameFloating.text = savedName
        binding.etSettingsLogin.setText(currentUser)
        binding.etSettingsPassword.setText(savedPassword)

        if (savedAvatarUri != null) {
            val uri = Uri.parse(savedAvatarUri)
            binding.ivProfilePhoto.setImageURI(uri)
            binding.ivProfilePhotoCollapsed.setImageURI(uri)
        } else {
            binding.ivProfilePhoto.setImageResource(R.drawable.ic_person)
            binding.ivProfilePhotoCollapsed.setImageResource(R.drawable.ic_person)
        }

        // ==========================================
        // 7. КНОПКА ВЫХОДА
        // ==========================================
        binding.btnLogout.setOnClickListener {
            sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    // ==========================================
    // ЛЕТАЮЩИЙ ТЕКСТ: измерение крайних позиций
    // ==========================================

    /**
     * Чтобы TextView мог плавно "перелетать" между позицией у фото и
     * позицией в тулбаре, нам нужны точные пиксельные координаты обоих
     * состояний. Хардкодить их нельзя (разная плотность экрана, разный
     * отступ статус-бара), поэтому измеряем реальные anchor-вью:
     * 1) на кадре разворачиваем шапку и снимаем координаты expandedTitleLayout
     * 2) сворачиваем обратно и снимаем координаты collapsedTextAnchor
     * Всё это скрыто от пользователя (root invisible), занимает доли кадра.
     */
    private fun setupFloatingTitleMeasurement() {
        binding.root.visibility = View.INVISIBLE

        binding.appBarLayout.post {
            binding.appBarLayout.setExpanded(true, false)

            binding.root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.root.viewTreeObserver.removeOnPreDrawListener(this)

                    expandedNamePos = relativePosition(binding.tvUserNameExpanded)
                    expandedStatusPos = relativePosition(binding.tvStatusExpanded)

                    binding.appBarLayout.setExpanded(false, false)

                    binding.root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            binding.root.viewTreeObserver.removeOnPreDrawListener(this)

                            collapsedNamePos = relativePosition(binding.tvUserNameCollapsed)
                            collapsedStatusPos = relativePosition(binding.tvStatusCollapsed)

                            titlePositionsReady = true
                            updateFloatingTitlePosition(1f) // мы сейчас в свёрнутом состоянии
                            binding.root.visibility = View.VISIBLE
                            return true
                        }
                    })
                    return false
                }
            })
        }
    }

    private fun relativePosition(view: View): PointF {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val rootLoc = IntArray(2)
        binding.root.getLocationOnScreen(rootLoc)
        return PointF((loc[0] - rootLoc[0]).toFloat(), (loc[1] - rootLoc[1]).toFloat())
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

    private fun updateFloatingTitlePosition(percentage: Float) {
        if (!titlePositionsReady) return

        binding.tvUserNameFloating.x = lerp(expandedNamePos.x, collapsedNamePos.x, percentage)
        binding.tvUserNameFloating.y = lerp(expandedNamePos.y, collapsedNamePos.y, percentage)
        binding.tvUserNameFloating.textSize = lerp(28f, 16f, percentage)

        binding.tvStatusFloating.x = lerp(expandedStatusPos.x, collapsedStatusPos.x, percentage)
        binding.tvStatusFloating.y = lerp(expandedStatusPos.y, collapsedStatusPos.y, percentage)
        binding.tvStatusFloating.textSize = lerp(14f, 12f, percentage)
    }

    // ==========================================
    // PULL-ЖЕСТ (общая логика для NestedScrollView и для фото)
    // ==========================================

    private fun handlePullProgress(dragPx: Float) {
        val translation = dragPx.coerceAtMost(140f)
        binding.ivProfilePhoto.translationY = translation * 0.5f
        binding.ivProfilePhoto.scaleX = 1f + (translation / 1400f)
        binding.ivProfilePhoto.scaleY = 1f + (translation / 1400f)
    }

    private fun handlePullReleased(reachedThreshold: Boolean) {
        binding.ivProfilePhoto.animate()
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun handlePullThresholdReached() {
        PhotoViewerActivity.start(this, currentAvatarUri)
    }

    /**
     * Позволяет тянуть вниз прямо по самой фотографии (не только по
     * контенту под ней), чтобы открыть fullscreen с оригинальным качеством.
     * Работает, только когда шапка уже полностью развёрнута.
     * Используем rawY, а не y, потому что сама вью в процессе жеста
     * получает translationY — если считать по локальному y, координаты
     * "поедут" вслед за собственной анимацией.
     */
    private fun setupPhotoDragToOpen() {
        binding.ivProfilePhoto.setOnTouchListener { _, event ->
            if (!isAppBarFullyExpanded) return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    photoDragStartRawY = event.rawY
                    isDraggingPhoto = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = (event.rawY - photoDragStartRawY) * 0.5f
                    if (delta > 0) {
                        isDraggingPhoto = true
                        handlePullProgress(delta)
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDraggingPhoto) {
                        val delta = (event.rawY - photoDragStartRawY) * 0.5f
                        val reached = delta >= pullThresholdPx
                        handlePullReleased(reached)
                        if (reached) handlePullThresholdReached()
                        isDraggingPhoto = false
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    /**
     * Плавно перекрашивает кнопки Фото/Настройки/Выход по мере сворачивания шапки
     * (percentage: 0 = развёрнуто, 1 = свёрнуто), а на пороге переключает их
     * между "полной" (иконка+текст, цветной фон) и "компактной" (только иконка,
     * белый фон) версией.
     */
    private fun updateButtonsAppearance(percentage: Float) {
        val photoBg = argbEvaluator.evaluate(percentage, colorBlue, colorWhite) as Int
        val settingsBg = argbEvaluator.evaluate(percentage, colorBlue, colorWhite) as Int
        val logoutBg = argbEvaluator.evaluate(percentage, colorRed, colorWhite) as Int

        binding.btnChangePhoto.backgroundTintList = ColorStateList.valueOf(photoBg)
        binding.btnExtraSettings.backgroundTintList = ColorStateList.valueOf(settingsBg)
        binding.btnLogout.backgroundTintList = ColorStateList.valueOf(logoutBg)

        val photoIconColor = argbEvaluator.evaluate(percentage, colorWhite, colorBlue) as Int
        val settingsIconColor = argbEvaluator.evaluate(percentage, colorWhite, colorBlue) as Int
        val logoutIconColor = argbEvaluator.evaluate(percentage, colorWhite, colorRed) as Int

        binding.btnChangePhoto.iconTint = ColorStateList.valueOf(photoIconColor)
        binding.btnExtraSettings.iconTint = ColorStateList.valueOf(settingsIconColor)
        binding.btnLogout.iconTint = ColorStateList.valueOf(logoutIconColor)

        val shouldBeCompact = percentage > 0.6f
        val shouldBeExpanded = percentage < 0.4f

        if (shouldBeCompact && !isButtonsCompact) {
            isButtonsCompact = true
            binding.btnChangePhoto.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
            binding.btnExtraSettings.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
            binding.btnLogout.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
        } else if (shouldBeExpanded && isButtonsCompact) {
            isButtonsCompact = false
            binding.btnChangePhoto.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_TOP
            binding.btnExtraSettings.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_TOP
            binding.btnLogout.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_TOP
        }
    }
}
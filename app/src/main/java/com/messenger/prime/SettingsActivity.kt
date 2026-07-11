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
import androidx.activity.result.contract.ActivityResultContracts
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
    private var activeDialogBinding: com.messenger.prime.databinding.DialogEditAccountBinding? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            currentAvatarUri = it.toString()
            binding.ivProfilePhoto.setImageURI(it)
            
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", "") ?: ""
            sharedPrefs.edit().putString("${currentUser}_avatar", currentAvatarUri).apply()
            
            android.widget.Toast.makeText(this, "Фото обновлено", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

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
    private val pullThresholdPx by lazy { resources.displayMetrics.density * 80f }
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
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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
        //    Работает как по самому фото, так и по списку настроек.
        // ==========================================
        binding.nestedScrollView.isPullEnabled = { isAppBarFullyExpanded }
        binding.nestedScrollView.onPullProgress = { handlePullProgress(it) }
        binding.nestedScrollView.onPullReleased = { handlePullReleased(it) }
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
        } else {
            binding.ivProfilePhoto.setImageResource(R.drawable.ic_person)
        }

        // ==========================================
        // 7. КНОПКА ВЫХОДА
        // ==========================================
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // ==========================================
        // 8. НАСТРОЙКИ (КНОПКИ И ТУМБЛЕРЫ)
        // ==========================================
        binding.btnChangePhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnExtraSettings.setOnClickListener {
            // Чтобы точно скроллилось, берем позицию относительно родителя внутри ScrollView
            val rect = android.graphics.Rect()
            binding.tvSettingsTitle.getDrawingRect(rect)
            binding.nestedScrollView.offsetDescendantRectToMyCoords(binding.tvSettingsTitle, rect)
            
            val toolbarHeight = binding.toolbar.height
            val scrollTarget = rect.top - toolbarHeight - (16 * resources.displayMetrics.density).toInt()
            
            binding.nestedScrollView.smoothScrollTo(0, scrollTarget)
        }

        // ==========================================
        // 9. РЕДАКТИРОВАНИЕ ДАННЫХ (ДИАЛОГ)
        // ==========================================
        val openDialogListener = View.OnClickListener {
            showAccountEditDialog()
        }
        
        // Делаем поля кликабельными
        binding.etSettingsLogin.setOnClickListener(openDialogListener)
        binding.etSettingsPassword.setOnClickListener(openDialogListener)
        
        // Также делаем контейнеры (TextInputLayout) кликабельными для удобства
        (binding.etSettingsLogin.parent.parent as View).setOnClickListener(openDialogListener)
        (binding.etSettingsPassword.parent.parent as View).setOnClickListener(openDialogListener)

        binding.tvUserNameFloating.setOnClickListener {
            binding.appBarLayout.setExpanded(true, true)
        }

        // Состояние тумблеров
        binding.switchAnimations.isChecked = sharedPrefs.getBoolean("settings_animations", true)
        binding.switchBlocked.isChecked = sharedPrefs.getBoolean("settings_show_blocked", false)
        binding.switchSearch.isChecked = sharedPrefs.getBoolean("settings_hide_search", false)

        binding.switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_animations", isChecked).apply()
        }
        binding.switchBlocked.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_show_blocked", isChecked).apply()
        }
        binding.switchSearch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_hide_search", isChecked).apply()
        }

        startShimmerAnimation()
    }

    private fun startShimmerAnimation() {
        val animator = android.animation.ValueAnimator.ofFloat(0.4f, 0.8f)
        animator.duration = 2000
        animator.repeatMode = android.animation.ValueAnimator.REVERSE
        animator.repeatCount = android.animation.ValueAnimator.INFINITE
        animator.addUpdateListener { anim ->
            binding.photoShimmer.alpha = anim.animatedValue as Float
        }
        animator.start()
    }

    private fun showAccountEditDialog() {
        val dialogBinding = com.messenger.prime.databinding.DialogEditAccountBinding.inflate(layoutInflater)
        activeDialogBinding = dialogBinding
        binding.dialogContainer.removeAllViews()
        binding.dialogContainer.addView(dialogBinding.root)
        binding.dialogContainer.visibility = View.VISIBLE

        // Начальное состояние для анимации
        dialogBinding.cardContainer.scaleX = 0.8f
        dialogBinding.cardContainer.scaleY = 0.8f
        dialogBinding.cardContainer.alpha = 0f
        dialogBinding.dialogRoot.alpha = 0f

        // Анимация появления
        dialogBinding.dialogRoot.animate().alpha(1f).setDuration(300).start()
        dialogBinding.cardContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Загрузка текущих данных
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val currentPass = sharedPrefs.getString(currentUser, "") ?: ""

        dialogBinding.etNewLogin.setText(currentUser)
        dialogBinding.etNewPassword.setText(currentPass)

        // Кнопка назад
        dialogBinding.btnBack.setOnClickListener {
            hideAccountEditDialog(dialogBinding)
        }

        // Кнопка сохранить
        dialogBinding.btnSave.setOnClickListener {
            val newLogin = dialogBinding.etNewLogin.text.toString().trim()
            val newPass = dialogBinding.etNewPassword.text.toString()

            // Сохраняем новые данные и УДАЛЯЕМ старые (если логин изменился)
            sharedPrefs.edit().apply {
                if (newLogin != currentUser) {
                    val name = sharedPrefs.getString("${currentUser}_name", "Пользователь")
                    val avatar = sharedPrefs.getString("${currentUser}_avatar", null)
                    
                    // Записываем новые данные
                    putString("current_user", newLogin)
                    putString(newLogin, newPass)
                    putString("${newLogin}_name", name)
                    if (avatar != null) putString("${newLogin}_avatar", avatar)

                    // Удаляем старый профиль из базы
                    remove(currentUser)
                    remove("${currentUser}_name")
                    remove("${currentUser}_avatar")
                } else {
                    putString(currentUser, newPass)
                }
                apply()
            }

            // Обновляем UI в активити
            binding.etSettingsLogin.setText(newLogin)
            binding.etSettingsPassword.setText(newPass)
            
            android.widget.Toast.makeText(this, "Данные обновлены", android.widget.Toast.LENGTH_SHORT).show()
            hideAccountEditDialog(dialogBinding)
        }
        
        // Закрытие при клике на фон
        dialogBinding.dialogRoot.setOnClickListener {
            hideAccountEditDialog(dialogBinding)
        }
        
        // Свайп вправо для закрытия карточки
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        isDragging = false
                        // Возвращаем false, чтобы дети (кнопки/поля) могли получить клик
                        return false 
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - startX
                        if (deltaX > 30f && !isDragging) {
                            isDragging = true
                        }
                        if (isDragging) {
                            v.translationX = deltaX.coerceAtLeast(0f)
                            dialogBinding.dialogRoot.alpha = 1f - (deltaX / screenWidth).coerceIn(0f, 0.5f)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            val deltaX = event.rawX - startX
                            if (deltaX > screenWidth / 4) {
                                hideAccountEditDialog(dialogBinding)
                            } else {
                                v.animate().translationX(0f).setDuration(200).start()
                                dialogBinding.dialogRoot.animate().alpha(1f).setDuration(200).start()
                            }
                            isDragging = false
                            return true
                        }
                    }
                }
                return false
            }
        })

        dialogBinding.cardContainer.setOnClickListener { /* предотвращаем проброс клика на фон */ }
    }

    private fun hideAccountEditDialog(dialogBinding: com.messenger.prime.databinding.DialogEditAccountBinding) {
        dialogBinding.dialogRoot.animate().alpha(0f).setDuration(300).start()
        
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.animate()
            .translationX(screenWidth)
            .alpha(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.dialogContainer.visibility = View.GONE
                binding.dialogContainer.removeAllViews()
                activeDialogBinding = null
            }
            .start()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            activeDialogBinding?.let { dialogBinding ->
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()

                // Список вьюх, клик по которым НЕ должен скрывать клавиатуру
                val protectedViews = listOf(
                    dialogBinding.btnBack,
                    dialogBinding.etNewLogin,
                    dialogBinding.etNewPassword,
                    dialogBinding.btnSave
                )

                var hitProtected = false
                val rect = android.graphics.Rect()
                for (view in protectedViews) {
                    view.getGlobalVisibleRect(rect)
                    if (rect.contains(x, y)) {
                        hitProtected = true
                        break
                    }
                }

                if (!hitProtected) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    currentFocus?.let { focusedView ->
                        imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
                        focusedView.clearFocus()
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun showLogoutDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
            .setTitle("Выход")
            .setMessage("Сделать выход из аккаунта?")
            .setPositiveButton("Да") { _, _ ->
                getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_logged_in", false)
                    .apply()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finishAffinity()
            }
            .setNegativeButton("Нет", null)
            .show()
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
        binding.root.alpha = 0f

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
                            binding.root.alpha = 1f
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
        val maxTranslation = 250f
        val translation = dragPx.coerceAtMost(maxTranslation)
        binding.ivProfilePhoto.translationY = translation * 0.4f
        binding.ivProfilePhoto.scaleX = 1f + (translation / 1000f)
        binding.ivProfilePhoto.scaleY = 1f + (translation / 1000f)
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
        binding.ivProfilePhoto.setOnTouchListener { v, event ->
            if (!isAppBarFullyExpanded) return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    photoDragStartRawY = event.rawY
                    isDraggingPhoto = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val delta = (event.rawY - photoDragStartRawY) * 0.5f
                    if (delta > 10f) {
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
                    } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                        // Если сдвига почти не было — это клик
                        v.performClick()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        binding.ivProfilePhoto.setOnClickListener {
            handlePullThresholdReached()
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
            binding.btnChangePhoto.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
            binding.btnExtraSettings.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
            binding.btnLogout.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
        }

        // Анимация подписей: при percentage -> 1 (сворачивание) они уходят вверх и гаснут
        val labelAlpha = (1f - percentage * 2.5f).coerceIn(0f, 1f)
        val labelTranslationY = -percentage * 50f // уходят вверх на 50px

        binding.tvLabelPhoto.alpha = labelAlpha
        binding.tvLabelPhoto.translationY = labelTranslationY
        
        binding.tvLabelSettings.alpha = labelAlpha
        binding.tvLabelSettings.translationY = labelTranslationY
        
        binding.tvLabelLogout.alpha = labelAlpha
        binding.tvLabelLogout.translationY = labelTranslationY
    }
}
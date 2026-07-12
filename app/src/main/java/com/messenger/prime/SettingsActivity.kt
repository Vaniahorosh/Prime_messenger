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
    private var activePhotoDialogBinding: com.messenger.prime.databinding.DialogPhotoActionsBinding? = null
    
    private var isFullScreenMode = false
    private var currentAppBarOffset = 0
    private var swipeDetector: android.view.GestureDetector? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            currentAvatarUri = it.toString()
            
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", "") ?: ""
            sharedPrefs.edit().putString("${currentUser}_avatar", currentAvatarUri).apply()
            
            // Сразу применяем изменения
            applyAvatarState(currentAvatarUri)
            
            PrimeNotification.show(this, "Фото обновлено")
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

        // setupFloatingTitleMeasurement() - перенесено в блок загрузки данных

        // ==========================================
        // 3. АНИМАЦИЯ ЦВЕТА СТАТУС-БАРА / КНОПОК / ПОЗИЦИИ ТЕКСТА
        // ==========================================
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            currentAppBarOffset = verticalOffset
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
        // 5. ЛОГИКА ФОТО (УДАЛЕНО СТАРОЕ)
        // ==========================================
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

        binding.tvUserNameFloating.text = savedName
        binding.etSettingsLogin.setText(currentUser)
        binding.etSettingsPassword.setText(savedPassword)

        applyAvatarState(savedAvatarUri)

        binding.btnCloseFullPhoto.setOnClickListener {
            closeFullScreenMode()
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

    private fun applyAvatarState(avatarUri: String?) {
        if (avatarUri != null) {
            val uri = Uri.parse(avatarUri)
            binding.ivProfilePhoto.setImageURI(uri)
            binding.ivProfilePhoto.visibility = View.VISIBLE
            binding.photoShimmer.visibility = View.VISIBLE
            
            // Возвращаем стандартную высоту и скролл
            val appBarParams = binding.appBarLayout.layoutParams
            appBarParams.height = (350 * resources.displayMetrics.density).toInt()
            binding.appBarLayout.layoutParams = appBarParams
            binding.appBarLayout.setBackgroundColor(Color.TRANSPARENT)
            
            val params = binding.collapsingToolbar.layoutParams as com.google.android.material.appbar.AppBarLayout.LayoutParams
            params.scrollFlags = com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            binding.collapsingToolbar.layoutParams = params
            
            setupFloatingTitleMeasurement()
        } else {
            binding.ivProfilePhoto.visibility = View.GONE
            binding.photoShimmer.visibility = View.GONE

            // Если аватарки нет — делаем шапку компактной и синей
            val params = binding.collapsingToolbar.layoutParams as com.google.android.material.appbar.AppBarLayout.LayoutParams
            params.scrollFlags = 0 
            binding.collapsingToolbar.layoutParams = params
            
            val appBarParams = binding.appBarLayout.layoutParams
            appBarParams.height = (160 * resources.displayMetrics.density).toInt()
            binding.appBarLayout.layoutParams = appBarParams
            binding.appBarLayout.setBackgroundColor(colorBlue)

            // Позиционируем текст статично (в левый верхний угол по макету)
            binding.tvUserNameFloating.post {
                val density = resources.displayMetrics.density
                binding.tvUserNameFloating.x = 80 * density // Смещаем вправо от стрелки назад
                binding.tvUserNameFloating.y = 52 * density // В красную область по макету
                binding.tvUserNameFloating.textSize = 28f
                
                binding.tvStatusFloating.x = 80 * density
                binding.tvStatusFloating.y = 90 * density
                binding.tvStatusFloating.textSize = 14f
                
                titlePositionsReady = true
                binding.root.alpha = 1f
            }
            
            // Сбрасываем прозрачность кнопок (всегда видны)
            binding.tvLabelPhoto.alpha = 1f
            binding.tvLabelSettings.alpha = 1f
            binding.tvLabelLogout.alpha = 1f
            binding.tvLabelPhoto.translationY = 0f
            binding.tvLabelSettings.translationY = 0f
            binding.tvLabelLogout.translationY = 0f
        }
    }

    private fun showPhotoActionDialog() {
        val dialogBinding = com.messenger.prime.databinding.DialogPhotoActionsBinding.inflate(layoutInflater)
        activePhotoDialogBinding = dialogBinding
        binding.dialogContainer.removeAllViews()
        binding.dialogContainer.addView(dialogBinding.root)
        binding.dialogContainer.visibility = View.VISIBLE

        dialogBinding.cardContainer.scaleX = 0.8f
        dialogBinding.cardContainer.scaleY = 0.8f
        dialogBinding.cardContainer.alpha = 0f
        dialogBinding.dialogRoot.alpha = 0f

        dialogBinding.dialogRoot.animate().alpha(1f).setDuration(300).start()
        dialogBinding.cardContainer.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        dialogBinding.btnOpenPhoto.setOnClickListener {
            hidePhotoActionDialog(dialogBinding)
            if (currentAvatarUri != null) {
                PhotoViewerActivity.start(this, currentAvatarUri)
            }
        }

        dialogBinding.btnChangePhoto.setOnClickListener {
            hidePhotoActionDialog(dialogBinding)
            pickImage.launch("image/*")
        }

        dialogBinding.btnDeletePhoto.setOnClickListener {
            hidePhotoActionDialog(dialogBinding)
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", "") ?: ""
            sharedPrefs.edit().remove("${currentUser}_avatar").apply()
            currentAvatarUri = null
            applyAvatarState(null)
            PrimeNotification.show(this, "Фото удалено")
        }

        dialogBinding.btnClose.setOnClickListener {
            hidePhotoActionDialog(dialogBinding)
        }

        dialogBinding.dialogRoot.setOnClickListener { hidePhotoActionDialog(dialogBinding) }
        
        // Свайп для закрытия
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        
        // Переинициализируем детектор для фото (классическая механика)
        swipeDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaY = e2.y - e1.y
                if (!isFullScreenMode) {
                    if (deltaY > 100 && velocityY > 800 && currentAppBarOffset == 0) {
                        openFullScreenMode()
                        return true
                    }
                } else {
                    if (kotlin.math.abs(deltaY) > 100 && kotlin.math.abs(velocityY) > 800) {
                        closeFullScreenMode()
                        return true
                    }
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!isFullScreenMode) showPhotoActionDialog()
                return true
            }
        })

        dialogBinding.cardContainer.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            private var isDragging = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { startX = event.rawX; isDragging = false; return false }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - startX
                        if (deltaX > 30f && !isDragging) isDragging = true
                        if (isDragging) {
                            v.translationX = deltaX.coerceAtLeast(0f)
                            dialogBinding.dialogRoot.alpha = 1f - (deltaX / screenWidth).coerceIn(0f, 0.5f)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            if (event.rawX - startX > screenWidth / 4) hidePhotoActionDialog(dialogBinding)
                            else {
                                v.animate().translationX(0f).setDuration(200).start()
                                dialogBinding.dialogRoot.animate().alpha(1f).setDuration(200).start()
                            }
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    private fun hidePhotoActionDialog(dialogBinding: com.messenger.prime.databinding.DialogPhotoActionsBinding) {
        dialogBinding.dialogRoot.animate().alpha(0f).setDuration(300).start()
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.animate()
            .translationX(screenWidth).alpha(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.dialogContainer.visibility = View.GONE
                binding.dialogContainer.removeAllViews()
                activePhotoDialogBinding = null
            }
            .start()
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
            
            PrimeNotification.show(this, "Данные обновлены")
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
                    
                    // Если открыт фото-диалог (где нет полей), просто закрываем его
                    activePhotoDialogBinding?.let { hidePhotoActionDialog(it) }
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


    private fun openFullScreenMode() {
        if (currentAvatarUri == null || isFullScreenMode) return
        isFullScreenMode = true

        binding.ivProfilePhoto.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

        val params = binding.collapsingToolbar.layoutParams as com.google.android.material.appbar.AppBarLayout.LayoutParams
        val startHeight = binding.collapsingToolbar.height
        val targetHeight = resources.displayMetrics.heightPixels

        val animator = android.animation.ValueAnimator.ofInt(startHeight, targetHeight).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.height = animation.animatedValue as Int
                binding.collapsingToolbar.layoutParams = params
                binding.appBarLayout.requestLayout()
            }
        }

        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: android.animation.Animator) {
                params.scrollFlags = 0
                binding.appBarLayout.setExpanded(true, true)
                binding.ivProfilePhoto.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                
                binding.root.setBackgroundColor(Color.BLACK)
                binding.viewDimmer.visibility = View.VISIBLE
                binding.viewDimmer.alpha = 1f
                binding.btnCloseFullPhoto.visibility = View.VISIBLE
                binding.toolbar.visibility = View.GONE
                binding.nestedScrollView.visibility = View.GONE
                binding.tvUserNameFloating.visibility = View.GONE
                binding.tvStatusFloating.visibility = View.GONE
            }
        })
        animator.start()
    }

    private fun closeFullScreenMode() {
        if (!isFullScreenMode) return
        isFullScreenMode = false

        binding.btnCloseFullPhoto.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

        val params = binding.collapsingToolbar.layoutParams as com.google.android.material.appbar.AppBarLayout.LayoutParams
        val startHeight = binding.collapsingToolbar.height
        val defaultHeight = (350 * resources.displayMetrics.density).toInt()

        val animator = android.animation.ValueAnimator.ofInt(startHeight, defaultHeight).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.height = animation.animatedValue as Int
                binding.collapsingToolbar.layoutParams = params
                binding.appBarLayout.requestLayout()
            }
        }

        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: android.animation.Animator) {
                binding.ivProfilePhoto.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                binding.btnCloseFullPhoto.visibility = View.GONE
                binding.root.setBackgroundColor(Color.parseColor("#F7F8FA"))
                binding.viewDimmer.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.viewDimmer.visibility = View.GONE
                }.start()
            }
            override fun onAnimationEnd(animation: android.animation.Animator) {
                params.scrollFlags = com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                binding.collapsingToolbar.layoutParams = params

                binding.toolbar.visibility = View.VISIBLE
                binding.nestedScrollView.visibility = View.VISIBLE
                binding.tvUserNameFloating.visibility = View.VISIBLE
                binding.tvStatusFloating.visibility = View.VISIBLE
            }
        })
        animator.start()
    }

    /**
     * Позволяет тянуть вниз прямо по самой фотографии...
     */
    private fun setupPhotoDragToOpen() {
        // Инициализируем детектор жестов
        val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaY = e2.y - e1.y
                if (!isFullScreenMode && deltaY > 100 && velocityY > 800 && currentAppBarOffset == 0) {
                    openFullScreenMode()
                    return true
                } else if (isFullScreenMode && kotlin.math.abs(deltaY) > 100 && kotlin.math.abs(velocityY) > 800) {
                    closeFullScreenMode()
                    return true
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                showPhotoActionDialog()
                return true
            }
        })

        binding.ivProfilePhoto.setOnTouchListener { v, event ->
            if (isFullScreenMode) return@setOnTouchListener true
            
            val handled = gestureDetector.onTouchEvent(event)
            
            if (currentAppBarOffset == 0) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
            } else {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
            
            handled || currentAppBarOffset == 0
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
package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.messenger.prime.databinding.ActivityChatListBinding

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var adapter: ChatAdapter
    private lateinit var allChats: List<ChatModel>

    private lateinit var connectivityManager: ConnectivityManager
    private var isNetworkConnected = true

    // ==========================================
    // ПЕРЕМЕННЫЕ ДЛЯ СВАЙПА
    // ==========================================
    private var startY = 0f
    private var isPulling = false
    private var isThresholdCrossed = false
    private val PULL_THRESHOLD = 250f // Более отзывчивый порог

    private var isIslandHidden = false
    private var isTransitioning = false

    // Слушатель изменения состояния сети
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            isNetworkConnected = true
            runOnUiThread {
                animateSearchHint("ПОИСК")
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            isNetworkConnected = false
            runOnUiThread {
                animateSearchHint("Ожидание сети...")
            }
        }
    }

    private fun animateSearchHint(newHint: String) {
        if (binding.inputLayoutSearch.hint == newHint) return
        
        animateViewHint(binding.inputLayoutSearch, newHint)
        adapter.updateNetworkHint(newHint)
    }

    private fun animateViewHint(view: com.google.android.material.textfield.TextInputLayout, newHint: String) {
        view.animate()
            .alpha(0f)
            .translationY(-30f)
            .setDuration(150)
            .withEndAction {
                view.hint = newHint
                view.translationY = 30f
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .start()
            }
            .start()
    }

    // ==========================================
    // ГЛОБАЛЬНЫЙ ПЕРЕХВАТ КАСАНИЙ
    // ==========================================
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // 1. Закрытие клавиатуры при клике мимо поля
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextInputEditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                isPulling = false
                isThresholdCrossed = false
                isTransitioning = false
            }
            MotionEvent.ACTION_MOVE -> {
                // Если мы уже в процессе перехода, ничего не делаем
                if (isTransitioning) return true
                
                // Если активен поиск, блокируем жест оттягивания к профилю
                if (adapter.isSearchActive) return super.dispatchTouchEvent(event)

                if (!binding.recyclerViewChats.canScrollVertically(-1)) {
                    val dy = event.y - startY

                    if (dy > 30f && !isPulling) {
                        isPulling = true
                    }

                    if (isPulling) {
                        if (dy > 0) {
                            // Эффект пружины для списка
                            binding.recyclerViewChats.translationY = dy * 0.35f

                            val progress = (dy / PULL_THRESHOLD).coerceIn(0f, 1.2f)

                            // Масштабирование верхнего островка
                            binding.recyclerViewChats.layoutManager?.findViewByPosition(0)?.let { headerView ->
                                headerView.pivotY = 0f
                                headerView.scaleX = 1f + (progress * 0.1f) // +10%
                                headerView.scaleY = 1f + (progress * 0.2f) // +20%
                            }


                            if (dy > PULL_THRESHOLD) {
                                if (!isThresholdCrossed) {
                                    isThresholdCrossed = true
                                    isTransitioning = true // Блокируем повторные входы
                                    
                                    binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    
                                    // Мгновенный переход
                                    startActivity(android.content.Intent(this, SettingsActivity::class.java))
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                    
                                    // Сбрасываем визуально
                                    resetPullUiInstant()
                                    isPulling = false
                                }
                            }
                        } else {
                            // Если dy <= 0, значит пользователь свайпнул обратно вверх - отменяем действие
                            cancelPullToProfile()
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPulling) {
                    val dy = event.y - startY
                    
                    // Открытие настроек, если порог пройден
                    if (dy > PULL_THRESHOLD) {
                        isPulling = false
                        binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        startActivity(android.content.Intent(this, SettingsActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        
                        // Сбрасываем визуально без анимации (так как мы уже уходим на другой экран)
                        resetPullUiInstant()
                    } else {
                        cancelPullToProfile()
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun cancelPullToProfile() {
        isPulling = false
        isThresholdCrossed = false

        // Плавный отскок списка
        binding.recyclerViewChats.animate()
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .start()

        // Сброс масштаба верхнего островка
        binding.recyclerViewChats.layoutManager?.findViewByPosition(0)?.animate()
            ?.scaleX(1f)?.scaleY(1f)
            ?.setDuration(400)
            ?.setInterpolator(android.view.animation.OvershootInterpolator())
            ?.start()

        // Текст больше не показываем
    }

    private fun resetPullUiInstant() {
        isPulling = false
        isThresholdCrossed = false
        binding.recyclerViewChats.translationY = 0f
        binding.recyclerViewChats.layoutManager?.findViewByPosition(0)?.let {
            it.scaleX = 1f
            it.scaleY = 1f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ==========================================
        // 1. СТАТУС-БАР И ОТСТУПЫ
        // ==========================================
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // Устанавливаем темные иконки статус-бара (для светлого фона)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())

            // Островок теперь внизу, учитываем отступ навигационной панели и клавиатуры
            val islandParams = binding.islandHeader.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            val bottomMargin = if (imeInsets.bottom > 0) {
                imeInsets.bottom + (16 * resources.displayMetrics.density).toInt()
            } else {
                systemBarsInsets.bottom + (16 * resources.displayMetrics.density).toInt()
            }
            islandParams.bottomMargin = bottomMargin
            binding.islandHeader.layoutParams = islandParams

            // Верхний отступ для списка (статус-бар)
            val topPadding = systemBarsInsets.top + (16 * resources.displayMetrics.density).toInt()
            // Нижний отступ для списка (островок + навигационная панель)
            val bottomPadding = systemBarsInsets.bottom + (100 * resources.displayMetrics.density).toInt()
            binding.recyclerViewChats.setPadding(0, topPadding, 0, bottomPadding)


            // view.setPadding(0, 0, 0, imeInsets.bottom) // Это больше не нужно, так как островок сам подпрыгивает
            windowInsets
        }

        // ==========================================
        // 2. АВАТАРКА ПОЛЬЗОВАТЕЛЯ
        // ==========================================
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"

        // ==========================================
        // 3. СПИСОК ЧАТОВ
        // ==========================================
        allChats = listOf(
            ChatModel("1", "Дмитрий", "Привет! Как успехи с приложением?", "14:23", null, OnlineStatus.ONLINE, MessageStatus.READ, 0, false),
            ChatModel("2", "Анна", "Скинула новые макеты на ревью", "12:05", null, OnlineStatus.OFFLINE, MessageStatus.SENT, 12, false),
            ChatModel("3", "Команда разработки", "Завтра созвон в 11:00", "Вчера", null, OnlineStatus.OFFLINE, MessageStatus.NONE, 13, true),
            ChatModel("4", "Максим", "Слушай, не могу загрузить файл...", "Вчера", null, OnlineStatus.ONLINE, MessageStatus.ERROR, 0, false),
            ChatModel("5", "Неизвестный", "Вы выиграли приз, перейдите по ссылке", "Пн", null, OnlineStatus.BLOCKED, MessageStatus.NONE, 0, false),
            ChatModel("6", "Елена", "Ок", "Пн", null, OnlineStatus.OFFLINE, MessageStatus.READ, 0, false),
            ChatModel("7", "Алексей", "Давай обсудим это чуть позже", "10 Мая", null, OnlineStatus.ONLINE, MessageStatus.SENT, 0, false),
            ChatModel("8", "Мама", "Купи фламиши с ветчиной по пути домой", "09 Мая", null, OnlineStatus.ONLINE, MessageStatus.READ, 0, false),
            ChatModel("9", "Староста П2-23", "Скиньте лабы по питону до пятницы!", "09 Мая", null, OnlineStatus.OFFLINE, MessageStatus.NONE, 5, false),
            ChatModel("10", "Влад", "Какую термопасту лучше взять для нового кулера?", "08 Мая", null, OnlineStatus.ONLINE, MessageStatus.NONE, 2, false),
            ChatModel("11", "Саня", "Го вечером в CS2, я скин на ТП продал", "08 Мая", null, OnlineStatus.OFFLINE, MessageStatus.READ, 0, false),
            ChatModel("12", "OpenWrt Community", "Как настроить блокировку рекламы на роутере Cudy?", "07 Мая", null, OnlineStatus.OFFLINE, MessageStatus.NONE, 45, true),
            ChatModel("13", "Доставка", "Ваш заказ (Биг Хит) будет доставлен через 15 минут", "06 Мая", null, OnlineStatus.OFFLINE, MessageStatus.READ, 0, false),
            ChatModel("14", "Проект Prime", "Кнопка отправки готова, лого телеграма убрал", "05 Мая", null, OnlineStatus.ONLINE, MessageStatus.SENT, 0, false),
            ChatModel("15", "Вика", "Билеты на поезд до Анапы уже у тебя?", "04 Мая", null, OnlineStatus.OFFLINE, MessageStatus.NONE, 1, false)
        )

        adapter = ChatAdapter(
            allChats,
            savedAvatarUri,
            savedName,
            onStartChatClick = {
                PrimeNotification.show(this, "Поиск контактов...")
            },
            onAvatarClick = {
                startActivity(android.content.Intent(this, SettingsActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            },
            onHeaderSearchClick = {
                activateIslandSearch()
            },
            onNameClick = {
                showNameEditDialog()
            }
        )
        binding.recyclerViewChats.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChats.adapter = adapter

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                animateShowSearchClear()
            } else if (binding.etSearch.text.isNullOrEmpty()) {
                animateHideSearchClear()
            }
        }

        binding.btnSearchClear.setOnClickListener {
            animateHideSearchClear()
            binding.etSearch.text?.clear()
            hideKeyboardAndClearFocus()
        }

        binding.recyclerViewChats.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                
                // Если поиск активен, островок должен быть всегда виден
                if (adapter.isSearchActive) {
                    showIsland()
                    return
                }

                // Если первый элемент (хедер) виден, скрываем островок
                if (firstVisiblePos == 0) {
                    hideIsland()
                } else {
                    showIsland()
                }
            }
        })

        if (savedAvatarUri != null) {
            binding.ivToolbarAvatar.setImageURI(Uri.parse(savedAvatarUri))
        } else {
            binding.ivToolbarAvatar.setImageResource(R.drawable.ic_person)
        }

        binding.ivToolbarAvatar.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.ivToolbarAvatar.setOnLongClickListener {
            binding.recyclerViewChats.smoothScrollToPosition(0)
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            true
        }

        // ==========================================
        // 4. ГЛАССМОРФИЗМ (BLUR)
        // ==========================================
        binding.islandBlurBackground.applyGlassBlur(25f)

        // ==========================================
        // 5. ПОИСК ЧАТОВ
        // ==========================================
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Если ввели пробел в пустом поле - скрываем клавиатуру
                if (s?.toString() == " ") {
                    binding.etSearch.text?.clear()
                    hideKeyboardAndClearFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                if (query.isEmpty()) {
                    adapter.updateList(allChats)
                    adapter.setSearchActive(false)
                    // Если мы в самом верху, при окончании поиска прячем островок
                    if (!binding.recyclerViewChats.canScrollVertically(-1)) {
                        hideIsland()
                    }
                } else {
                    adapter.setSearchActive(true)
                    showIsland()
                    val filteredChats = allChats.filter { chat ->
                        chat.name.lowercase().contains(query) || chat.lastMessage.lowercase().contains(query)
                    }
                    adapter.updateList(filteredChats)
                }
            }
        })

        // Обработка удаления (Backspace) в пустом поле
        binding.etSearch.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
                if (binding.etSearch.text.isNullOrEmpty()) {
                    hideKeyboardAndClearFocus()
                    return@setOnKeyListener true
                }
            }
            false
        }

        // ==========================================
        // 5. МОНИТОРИНГ СЕТИ
        // ==========================================
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        binding.inputLayoutSearch.hint = if (isNetworkConnected) "ПОИСК" else "Ожидание сети..."

        val networkRequest = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Показ одноразового совета
        showScrollTopHintOnce(sharedPrefs)
    }

    private fun showScrollTopHintOnce(sharedPrefs: android.content.SharedPreferences) {
        val hintShown = sharedPrefs.getBoolean("hint_scroll_top_shown", false)
        if (!hintShown) {
            PrimeNotification.show(this, "Зажмите, для подтягивание к верху экрана")
            sharedPrefs.edit().putBoolean("hint_scroll_top_shown", true).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        // 1. Синхронизируем аватарку и имя (могли измениться в настройках)
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"

        if (savedAvatarUri != null) {
            binding.ivToolbarAvatar.setImageURI(Uri.parse(savedAvatarUri))
            adapter.updateAvatar(savedAvatarUri)
        } else {
            binding.ivToolbarAvatar.setImageResource(R.drawable.ic_person)
            adapter.updateAvatar(null)
        }
        adapter.updateUserName(savedName)

        // 2. Синхронизируем видимость островка в зависимости от позиции скролла
        binding.recyclerViewChats.post {
            val layoutManager = binding.recyclerViewChats.layoutManager as? LinearLayoutManager
            if (layoutManager != null) {
                val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                
                // Если мы в самом верху и поиск не активен - прячем нижний островок
                if (firstVisiblePos == 0 && !adapter.isSearchActive) {
                    if (!isIslandHidden) {
                        isIslandHidden = true
                        binding.islandHeader.translationY = 300f
                        binding.islandHeader.alpha = 0f
                    }
                } else {
                    // Если прокручено ниже - показываем
                    if (isIslandHidden) {
                        isIslandHidden = false
                        binding.islandHeader.translationY = 0f
                        binding.islandHeader.alpha = 1f
                    }
                }
            }
        }

        // 3. Сбрасываем флаги свайпа (на всякий случай)
        isPulling = false
        isThresholdCrossed = false
    }

    private fun showIsland() {
        if (!isIslandHidden) return
        isIslandHidden = false
        binding.islandHeader.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun hideIsland() {
        if (isIslandHidden) return
        isIslandHidden = true
        binding.islandHeader.animate()
            .translationY(300f) // Уходит вниз
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .start()
    }

    private fun activateIslandSearch() {
        adapter.setSearchActive(true) // Скрываем статичный хедер сразу
        showIsland()
        binding.etSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showNameEditDialog() {
        val dialogBinding = com.messenger.prime.databinding.DialogEditNameBinding.inflate(layoutInflater)
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
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val currentName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"

        dialogBinding.etNewName.setText(currentName)
        
        // Изначально кнопка сохранить выключена, т.к. имя еще не изменено
        dialogBinding.btnSave.isEnabled = false
        dialogBinding.btnSave.alpha = 0.5f

        dialogBinding.etNewName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newName = s.toString().trim()
                val isChanged = newName != currentName && newName.isNotEmpty()
                dialogBinding.btnSave.isEnabled = isChanged
                dialogBinding.btnSave.alpha = if (isChanged) 1.0f else 0.5f
                dialogBinding.inputLayoutName.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialogBinding.btnSave.setOnClickListener {
            val newName = dialogBinding.etNewName.text.toString().trim()
            val oldName = currentName
            
            sharedPrefs.edit().putString("${currentUser}_name", newName).apply()
            adapter.updateUserName(newName)
            
            PrimeNotification.show(this, "Имя обновлено") {
                sharedPrefs.edit().putString("${currentUser}_name", oldName).apply()
                adapter.updateUserName(oldName)
            }
            hideNameEditDialog(dialogBinding)
        }

        dialogBinding.btnBack.setOnClickListener { hideNameEditDialog(dialogBinding) }
        dialogBinding.dialogRoot.setOnClickListener { hideNameEditDialog(dialogBinding) }
    }

    private fun hideNameEditDialog(dialogBinding: com.messenger.prime.databinding.DialogEditNameBinding) {
        dialogBinding.dialogRoot.animate().alpha(0f).setDuration(300).start()
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        dialogBinding.cardContainer.animate()
            .translationX(screenWidth).alpha(0f)
            .setDuration(350)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                binding.dialogContainer.visibility = View.GONE
                binding.dialogContainer.removeAllViews()
            }
            .start()
    }

    private fun animateShowSearchClear() {
        if (binding.btnSearchClear.visibility == View.VISIBLE && binding.btnSearchClear.alpha == 1f) return
        
        binding.btnSearchClear.visibility = View.VISIBLE
        binding.btnSearchClear.translationY = -50f * resources.displayMetrics.density
        binding.btnSearchClear.alpha = 0f
        binding.btnSearchClear.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun animateHideSearchClear() {
        if (binding.btnSearchClear.visibility != View.VISIBLE) return
        
        binding.btnSearchClear.animate()
            .translationY(50f * resources.displayMetrics.density)
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                binding.btnSearchClear.visibility = View.INVISIBLE
                binding.btnSearchClear.translationY = 0f
            }
            .start()
    }

    private fun hideKeyboardAndClearFocus() {
        binding.etSearch.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        
        // Сбрасываем состояние если поле пустое
        if (binding.etSearch.text.isNullOrEmpty()) {
            adapter.setSearchActive(false)
            if (!binding.recyclerViewChats.canScrollVertically(-1)) {
                hideIsland()
            }
            animateHideSearchClear()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
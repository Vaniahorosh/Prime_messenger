package com.messenger.prime

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.messenger.prime.databinding.ActivityChatListContentBinding
import com.messenger.prime.databinding.LayoutIslandBinding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListContentBinding
    private lateinit var islandBinding: LayoutIslandBinding
    private lateinit var adapter: ChatAdapter
    private lateinit var allChats: List<ChatModel>

    private lateinit var connectivityManager: ConnectivityManager
    private var isNetworkConnected = true

    // Состояние видимости островка для Compose
    private var isIslandVisibleState = mutableStateOf(true)

    // ==========================================
    // ПЕРЕМЕННЫЕ ДЛЯ СВАЙПА
    // ==========================================
    private var startY = 0f
    private var isPulling = false
    private var isThresholdCrossed = false
    private val PULL_THRESHOLD = 250f
    private var isTransitioning = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val isCurrentlyConnected = connectivityManager.getNetworkCapabilities(network)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            if (isCurrentlyConnected) {
                isNetworkConnected = true
                runOnUiThread { animateSearchHint("ПОИСК") }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            val activeNetwork = connectivityManager.activeNetwork
            val hasInternet = connectivityManager.getNetworkCapabilities(activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            if (!hasInternet) {
                isNetworkConnected = false
                runOnUiThread { animateSearchHint("Ожидание сети...") }
            }
        }
    }

    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        val currentUser = sharedPreferences.getString("current_user", "") ?: ""
        if (key == "${currentUser}_avatar" || key == "${currentUser}_name") {
            runOnUiThread { refreshUserUi() }
        }
    }

    private fun animateSearchHint(newHint: String) {
        if (!::islandBinding.isInitialized) return
        if (islandBinding.inputLayoutSearch.hint == newHint) return
        animateViewHint(islandBinding.inputLayoutSearch, newHint)
        adapter.updateNetworkHint(newHint)
    }

    private fun animateViewHint(view: com.google.android.material.textfield.TextInputLayout, newHint: String) {
        view.animate()
            .alpha(0f)
            .translationY(-30f)
            .setDuration(300)
            .withEndAction {
                view.hint = newHint
                view.translationY = 30f
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .start()
            }
            .start()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (!::binding.isInitialized) return super.dispatchTouchEvent(event)
        
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
                if (isTransitioning) return true
                if (adapter.isSearchActive) return super.dispatchTouchEvent(event)

                if (!binding.recyclerViewChats.canScrollVertically(-1)) {
                    val dy = event.y - startY
                    if (dy > 30f && !isPulling) isPulling = true

                    if (isPulling) {
                        if (dy > 0) {
                            binding.recyclerViewChats.translationY = dy * 0.35f
                            val progress = (dy / PULL_THRESHOLD).coerceIn(0f, 1.2f)

                            val headerView = binding.recyclerViewChats.layoutManager?.findViewByPosition(0)
                            if (headerView != null) {
                                headerView.pivotY = 0f
                                headerView.scaleX = 1f + (progress * 0.1f)
                                headerView.scaleY = 1f + (progress * 0.2f)
                            }

                            if (dy > PULL_THRESHOLD && !isThresholdCrossed) {
                                isThresholdCrossed = true
                                isTransitioning = true
                                binding.photoRootContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                                startActivity(Intent(this, SettingsActivity::class.java))
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

                                resetPullUiInstant()
                                isPulling = false
                            }
                        } else {
                            cancelPullToProfile()
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPulling) {
                    val dy = event.y - startY
                    if (dy > PULL_THRESHOLD) {
                        isPulling = false
                        binding.photoRootContainer.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        startActivity(Intent(this, SettingsActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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
        if (!::binding.isInitialized) return
        isPulling = false
        isThresholdCrossed = false
        binding.recyclerViewChats.animate().translationY(0f).setDuration(400)
            .setInterpolator(android.view.animation.OvershootInterpolator()).start()

        binding.recyclerViewChats.layoutManager?.findViewByPosition(0)?.animate()
            ?.scaleX(1f)?.scaleY(1f)?.setDuration(400)
            ?.setInterpolator(android.view.animation.OvershootInterpolator())?.start()
    }

    private fun resetPullUiInstant() {
        if (!::binding.isInitialized) return
        isPulling = false
        isThresholdCrossed = false
        binding.recyclerViewChats.translationY = 0f
        val headerView = binding.recyclerViewChats.layoutManager?.findViewByPosition(0)
        if (headerView != null) {
            headerView.scaleX = 1f
            headerView.scaleY = 1f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
        
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)
        val savedName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"

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
            allChats, savedAvatarUri, savedName,
            onStartChatClick = { PrimeNotification.show(this, "Поиск контактов...") },
            onAvatarClick = {
                startActivity(Intent(this, SettingsActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            },
            onHeaderSearchClick = { activateIslandSearch() },
            onNameClick = { showNameEditDialog() },
            onChatClick = { chat ->
                if (chat.id == "block_test_contact") {
                    if (::islandBinding.isInitialized) {
                        val parts = islandBinding.etSearch.text.toString().split(" ")
                        if (parts.size >= 4) {
                            val reason = parts.subList(1, parts.size - 2).joinToString(" ")
                            val value = parts[parts.size - 2].toLongOrNull() ?: 0
                            val unit = parts[parts.size - 1]

                            val expiry = if (value <= 0) -1L else System.currentTimeMillis() + calculateMillis(value, unit)
                            sharedPrefs.edit().apply {
                                putString("ban_reason", reason)
                                putLong("ban_value", value)
                                putString("ban_unit", unit)
                                putLong("ban_expiry", expiry)
                                apply()
                            }

                            val intent = Intent(this, BanActivity::class.java).apply {
                                putExtra("EXTRA_USER_NAME", savedName)
                                putExtra("EXTRA_REASON", reason)
                                putExtra("EXTRA_VALUE", value)
                                putExtra("EXTRA_UNIT", unit)
                            }
                            startActivity(intent)
                        }
                    }
                }
            }
        )

        // Интеграция Compose и Haze
        findViewById<ComposeView>(R.id.composeRoot).setContent {
            val hazeState = remember { HazeState() }
            val isIslandVisible by isIslandVisibleState
            
            Box(modifier = Modifier.fillMaxSize()) {
                // Фоновый контент (View-based RecyclerView)
                AndroidView(
                    factory = { context ->
                        val view = layoutInflater.inflate(R.layout.activity_chat_list_content, null)
                        binding = ActivityChatListContentBinding.bind(view)
                        
                        binding.recyclerViewChats.layoutManager = LinearLayoutManager(context)
                        binding.recyclerViewChats.adapter = adapter
                        
                        view
                    },
                    modifier = Modifier.fillMaxSize().hazeSource(hazeState)
                )

                // Островок (Haze)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding() // Автоматический подъем при появлении клавиатуры
                        .navigationBarsPadding() // Учитывает системную панель навигации
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    AnimatedVisibility(
                        visible = isIslandVisible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .hazeEffect(
                                    state = hazeState,
                                    style = HazeStyle(
                                        blurRadius = 40.dp,
                                        noiseFactor = 0.15f,
                                        tint = dev.chrisbanes.haze.HazeTint(Color(0xB3154B87)) 
                                    )
                                )
                        ) {
                            AndroidView(
                                factory = { context ->
                                    val view = layoutInflater.inflate(R.layout.layout_island, null)
                                    islandBinding = LayoutIslandBinding.bind(view)
                                    
                                    setupLegacyListeners()
                                    updateToolbarInitialUi(savedAvatarUri, savedName)
                                    
                                    view
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isNetworkConnected = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), networkCallback)

        showScrollTopHintOnce(sharedPrefs)
    }

    private fun setupLegacyListeners() {
        if (!::islandBinding.isInitialized) return
        
        islandBinding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) animateShowSearchClear() else if (islandBinding.etSearch.text.isNullOrEmpty()) animateHideSearchClear()
        }

        islandBinding.btnSearchClear.setOnClickListener {
            animateHideSearchClear()
            islandBinding.etSearch.text?.clear()
            hideKeyboardAndClearFocus()
        }

        binding.recyclerViewChats.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                if (adapter.isSearchActive) { showIsland(); return }
                if (firstVisiblePos == 0) hideIsland() else showIsland()
            }
        })

        islandBinding.ivToolbarAvatar.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        islandBinding.tvToolbarInitials.setOnClickListener {
            islandBinding.ivToolbarAvatar.performClick()
        }

        islandBinding.ivToolbarAvatar.setOnLongClickListener {
            binding.recyclerViewChats.smoothScrollToPosition(0)
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            true
        }

        islandBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.toString() == " ") { islandBinding.etSearch.text?.clear(); hideKeyboardAndClearFocus() }
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                if (query.startsWith("/blocktestme")) {
                    adapter.setSearchActive(true)
                    showIsland()
                    val testContact = ChatModel("block_test_contact", "Тестирование активити блока", "Нажмите, чтобы протестировать", "сейчас", null, OnlineStatus.ONLINE)
                    adapter.updateList(listOf(testContact))
                    return
                }

                if (query.isEmpty()) {
                    adapter.updateList(allChats)
                } else {
                    adapter.setSearchActive(true)
                    showIsland()
                    val filtered = allChats.filter { it.name.lowercase().contains(query) || it.lastMessage.lowercase().contains(query) }
                    adapter.updateList(filtered)
                }
            }
        })
    }

    private fun updateToolbarInitialUi(savedAvatarUri: String?, savedName: String) {
        if (!::islandBinding.isInitialized) return
        if (savedAvatarUri != null) {
            islandBinding.ivToolbarAvatar.setImageURI(Uri.parse(savedAvatarUri))
            islandBinding.tvToolbarInitials.visibility = View.GONE
            islandBinding.ivToolbarAvatar.visibility = View.VISIBLE
        } else {
            val initial = savedName.take(1).uppercase()
            islandBinding.tvToolbarInitials.text = initial
            islandBinding.tvToolbarInitials.visibility = View.VISIBLE
            islandBinding.ivToolbarAvatar.visibility = View.INVISIBLE

            val color = getAvatarColor(savedName)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 15 * resources.displayMetrics.density
                setColor(color)
            }
            islandBinding.tvToolbarInitials.setBackground(bg)
        }
    }

    private fun calculateMillis(value: Long, unit: String): Long {
        val seconds = when (unit.uppercase()) {
            "S", "SECOND" -> value
            "M", "MIN", "MINUTE" -> value * 60
            "H", "HOUR" -> value * 3600
            "D", "DAYS" -> value * 86400
            "MO", "MOUNTH" -> value * 2592000
            "Y", "YEAR" -> value * 31536000
            else -> value
        }
        return seconds * 1000
    }

    private fun showScrollTopHintOnce(sharedPrefs: android.content.SharedPreferences) {
        if (!sharedPrefs.getBoolean("hint_scroll_top_shown", false)) {
            PrimeNotification.show(this, "Зажмите, для подтягивание к верху экрана")
            sharedPrefs.edit().putBoolean("hint_scroll_top_shown", true).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserUi()
        isPulling = false; isThresholdCrossed = false
    }

    private fun refreshUserUi() {
        if (!::binding.isInitialized || !::islandBinding.isInitialized) return
        
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val avatar = sharedPrefs.getString("${currentUser}_avatar", null)
        val name = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"

        if (avatar != null) {
            islandBinding.ivToolbarAvatar.setImageURI(null)
            islandBinding.ivToolbarAvatar.setImageURI(Uri.parse(avatar))
            islandBinding.tvToolbarInitials.visibility = View.GONE
            islandBinding.ivToolbarAvatar.visibility = View.VISIBLE
            adapter.updateAvatar(avatar)
        } else {
            val initial = name.take(1).uppercase()
            islandBinding.tvToolbarInitials.text = initial
            islandBinding.tvToolbarInitials.visibility = View.VISIBLE
            islandBinding.ivToolbarAvatar.visibility = View.INVISIBLE

            val color = getAvatarColor(name)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 15 * resources.displayMetrics.density
                setColor(color)
            }
            islandBinding.tvToolbarInitials.setBackground(bg)

            adapter.updateAvatar(null)
        }
        adapter.updateUserName(name)

        binding.recyclerViewChats.post {
            val layoutManager = binding.recyclerViewChats.layoutManager as? LinearLayoutManager
            if (layoutManager != null) {
                val first = layoutManager.findFirstVisibleItemPosition()
                if (first == 0 && !adapter.isSearchActive) {
                    hideIsland()
                } else {
                    showIsland()
                }
            }
        }
    }

    private fun showIsland() {
        isIslandVisibleState.value = true
    }

    private fun hideIsland() {
        isIslandVisibleState.value = false
    }

    private fun activateIslandSearch() {
        if (!::islandBinding.isInitialized) return
        adapter.setSearchActive(true)
        showIsland()
        islandBinding.etSearch.requestFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(islandBinding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showNameEditDialog() {
        if (!::binding.isInitialized) return
        val dialogBinding = com.messenger.prime.databinding.DialogEditNameBinding.inflate(layoutInflater)
        binding.dialogContainer.removeAllViews()
        binding.dialogContainer.addView(dialogBinding.root)
        binding.dialogContainer.visibility = View.VISIBLE

        dialogBinding.cardContainer.scaleX = 0.8f; dialogBinding.cardContainer.scaleY = 0.8f
        dialogBinding.cardContainer.alpha = 0f; dialogBinding.dialogRoot.alpha = 0f
        dialogBinding.dialogRoot.animate().alpha(1f).setDuration(300).start()
        dialogBinding.cardContainer.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(400).setInterpolator(android.view.animation.DecelerateInterpolator()).start()

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val currentName = sharedPrefs.getString("${currentUser}_name", "Пользователь") ?: "Пользователь"

        dialogBinding.etNewName.setText(currentName)
        dialogBinding.btnSave.isEnabled = false; dialogBinding.btnSave.alpha = 0.5f

        dialogBinding.etNewName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val new = s.toString().trim()
                val changed = new != currentName && new.isNotEmpty()
                dialogBinding.btnSave.isEnabled = changed; dialogBinding.btnSave.alpha = if (changed) 1f else 0.5f
                dialogBinding.inputLayoutName.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialogBinding.btnSave.setOnClickListener {
            val new = dialogBinding.etNewName.text.toString().trim()
            val error = ValidationUtils.getValidationError(new, false)
            if (error != null) {
                dialogBinding.inputLayoutName.error = error
                dialogBinding.cardContainer.shake()
                return@setOnClickListener
            }

            sharedPrefs.edit().putString("${currentUser}_name", new).apply()
            adapter.updateUserName(new)
            PrimeNotification.show(this, "Имя обновлено") {
                sharedPrefs.edit().putString("${currentUser}_name", currentName).apply()
                adapter.updateUserName(currentName)
            }
            hideNameEditDialog(dialogBinding)
        }
        dialogBinding.btnBack.setOnClickListener { hideNameEditDialog(dialogBinding) }
        dialogBinding.dialogRoot.setOnClickListener { hideNameEditDialog(dialogBinding) }
    }

    private fun hideNameEditDialog(dialogBinding: com.messenger.prime.databinding.DialogEditNameBinding) {
        dialogBinding.dialogRoot.animate().alpha(0f).setDuration(300).start()
        dialogBinding.cardContainer.animate().translationX(resources.displayMetrics.widthPixels.toFloat()).alpha(0f).setDuration(350)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).withEndAction {
                binding.dialogContainer.visibility = View.GONE; binding.dialogContainer.removeAllViews()
            }.start()
    }

    private fun animateShowSearchClear() {
        if (!::islandBinding.isInitialized) return
        if (islandBinding.btnSearchClear.visibility == View.VISIBLE && islandBinding.btnSearchClear.alpha == 1f) return
        islandBinding.btnSearchClear.visibility = View.VISIBLE
        islandBinding.btnSearchClear.translationY = -50f * resources.displayMetrics.density
        islandBinding.btnSearchClear.alpha = 0f
        islandBinding.btnSearchClear.animate().translationY(0f).alpha(1f).setDuration(300).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }

    private fun animateHideSearchClear() {
        if (!::islandBinding.isInitialized) return
        if (islandBinding.btnSearchClear.visibility != View.VISIBLE) return
        islandBinding.btnSearchClear.animate().translationY(50f * resources.displayMetrics.density).alpha(0f).setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator()).withEndAction {
                islandBinding.btnSearchClear.visibility = View.INVISIBLE; islandBinding.btnSearchClear.translationY = 0f
            }.start()
    }

    private fun hideKeyboardAndClearFocus() {
        if (!::islandBinding.isInitialized) return
        islandBinding.etSearch.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(islandBinding.etSearch.windowToken, 0)
        if (islandBinding.etSearch.text.isNullOrEmpty()) {
            adapter.setSearchActive(false)
            if (!binding.recyclerViewChats.canScrollVertically(-1)) hideIsland()
            animateHideSearchClear()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    private fun getAvatarColor(name: String): Int {
        val colors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722")
        val index = Math.abs(name.hashCode()) % colors.size
        return android.graphics.Color.parseColor(colors[index])
    }
}

package com.messenger.prime

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

        binding.inputLayoutSearch.animate()
            .alpha(0f)
            .translationY(-30f)
            .setDuration(150)
            .withEndAction {
                binding.inputLayoutSearch.hint = newHint
                binding.inputLayoutSearch.translationY = 30f
                binding.inputLayoutSearch.animate()
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

        // 2. Механика оттягивания списка (Pull-to-Profile)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                isPulling = false
                isThresholdCrossed = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!binding.recyclerViewChats.canScrollVertically(-1)) {
                    val dy = event.y - startY

                    if (dy > 30f && !isPulling) {
                        isPulling = true
                    }

                    if (isPulling) {
                        if (dy > 0) {
                            // Эффект пружины для списка
                            binding.recyclerViewChats.translationY = dy * 0.35f

                            // Прогресс оттягивания (0.0 до 1.0)
                            val progress = (dy / PULL_THRESHOLD).coerceIn(0f, 1.2f)

                            // Анимация пузырька
                            binding.tvPullIndicator.alpha = (progress * 2).coerceIn(0f, 1f)
                            binding.tvPullIndicator.scaleX = 0.5f + (progress * 0.6f).coerceIn(0f, 0.7f)
                            binding.tvPullIndicator.scaleY = 0.5f + (progress * 0.6f).coerceIn(0f, 0.7f)
                            
                            // Вылет из верхней точки (район камеры)
                            val startOffset = -100f * resources.displayMetrics.density
                            val endOffset = 40f * resources.displayMetrics.density
                            binding.tvPullIndicator.translationY = startOffset + (progress * (endOffset - startOffset))

                            if (dy > PULL_THRESHOLD) {
                                if (!isThresholdCrossed) {
                                    isThresholdCrossed = true
                                    binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    binding.tvPullIndicator.text = "ОТПУСТИТЕ ДЛЯ ПРОФИЛЯ"
                                    
                                    // Легкая пульсация при достижении порога
                                    binding.tvPullIndicator.animate()
                                        .scaleX(1.3f).scaleY(1.3f)
                                        .setDuration(100)
                                        .withEndAction {
                                            binding.tvPullIndicator.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                                        }.start()
                                }
                            } else {
                                if (isThresholdCrossed) {
                                    isThresholdCrossed = false
                                    binding.tvPullIndicator.text = "ПОТЯНИТЕ ДЛЯ ПРОФИЛЯ"
                                } else {
                                    if (binding.tvPullIndicator.text != "ПОТЯНИТЕ ДЛЯ ПРОФИЛЯ") {
                                        binding.tvPullIndicator.text = "ПОТЯНИТЕ ДЛЯ ПРОФИЛЯ"
                                    }
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

        // Текст эффектно улетает наверх
        binding.tvPullIndicator.animate()
            .alpha(0f)
            .translationY(-150f * resources.displayMetrics.density) // Улетает выше для акцента
            .scaleX(0.4f)
            .scaleY(0.4f)
            .setDuration(400)
            .setInterpolator(android.view.animation.AnticipateInterpolator())
            .start()
    }

    private fun resetPullUiInstant() {
        isPulling = false
        isThresholdCrossed = false
        binding.recyclerViewChats.translationY = 0f
        binding.tvPullIndicator.alpha = 0f
        binding.tvPullIndicator.translationY = -150f * resources.displayMetrics.density
        binding.tvPullIndicator.scaleX = 0.5f
        binding.tvPullIndicator.scaleY = 0.5f
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

            // Задаем базовый отступ тексту индикатора, чтобы он всегда был под тулбаром
            val layoutParams = binding.tvPullIndicator.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.topMargin = (20 * resources.displayMetrics.density).toInt() // Небольшой отступ от самого верха
            binding.tvPullIndicator.layoutParams = layoutParams

            // view.setPadding(0, 0, 0, imeInsets.bottom) // Это больше не нужно, так как островок сам подпрыгивает
            windowInsets
        }

        // ==========================================
        // 2. АВАТАРКА ПОЛЬЗОВАТЕЛЯ
        // ==========================================
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)

        if (savedAvatarUri != null) {
            binding.ivToolbarAvatar.setImageURI(Uri.parse(savedAvatarUri))
        } else {
            binding.ivToolbarAvatar.setImageResource(R.drawable.ic_person)
        }

        binding.ivToolbarAvatar.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

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

        adapter = ChatAdapter(allChats) {
            // Клик по кнопке "Начать общение" в футере
            PrimeNotification.show(this, "Поиск контактов...")
        }
        binding.recyclerViewChats.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChats.adapter = adapter

        // ==========================================
        // 4. ГЛАССМОРФИЗМ (BLUR)
        // ==========================================
        binding.islandBlurBackground.applyGlassBlur(25f)

        // ==========================================
        // 5. ПОИСК ЧАТОВ
        // ==========================================
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                if (query.isEmpty()) {
                    adapter.updateList(allChats)
                } else {
                    val filteredChats = allChats.filter { chat ->
                        chat.name.lowercase().contains(query) || chat.lastMessage.lowercase().contains(query)
                    }
                    adapter.updateList(filteredChats)
                }
            }
        })

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
    }

    override fun onResume() {
        super.onResume()
        // Синхронизируем аватарку при возвращении на экран
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)

        if (savedAvatarUri != null) {
            binding.ivToolbarAvatar.setImageURI(Uri.parse(savedAvatarUri))
        } else {
            binding.ivToolbarAvatar.setImageResource(R.drawable.ic_person)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
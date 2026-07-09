package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.messenger.prime.databinding.ActivityChatListBinding

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var adapter: ChatAdapter
    private lateinit var allChats: List<ChatModel> // Храним полный список для поиска

    private lateinit var connectivityManager: ConnectivityManager

    // Создаем слушатель изменения состояния сети
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // Вызывается, когда интернет появился
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            runOnUiThread {
                binding.inputLayoutSearch.hint = "ПОИСК"
            }
        }

        // Вызывается, когда интернет пропал
        override fun onLost(network: Network) {
            super.onLost(network)
            runOnUiThread {
                binding.inputLayoutSearch.hint = "Ожидание сети..."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setDynamicStatusBar(R.color.white, true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(0, 0, 0, imeInsets.bottom)
            windowInsets
        }

        // ==========================================
        // 1. ЗАГРУЗКА АВАТАРКИ ПОЛЬЗОВАТЕЛЯ
        // ==========================================
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentUser = sharedPrefs.getString("current_user", "") ?: ""
        val savedAvatarUri = sharedPrefs.getString("${currentUser}_avatar", null)

        if (savedAvatarUri != null) {
            binding.ivToolbarAvatar.setImageURI(Uri.parse(savedAvatarUri))
        } else {
            binding.ivToolbarAvatar.setImageResource(R.drawable.ic_person)
        }

        // ==========================================
        // 2. ИНИЦИАЛИЗАЦИЯ СПИСКА ЧАТОВ
        // ==========================================
        allChats = listOf(
            ChatModel("1", "Дмитрий", "Привет! Как успехи с приложением?", "14:23", null, OnlineStatus.ONLINE, MessageStatus.READ, 0, false),
            ChatModel("2", "Анна", "Скинула новые макеты на ревью", "12:05", null, OnlineStatus.OFFLINE, MessageStatus.SENT, 12, false),
            ChatModel("3", "Команда разработки", "Завтра созвон в 11:00", "Вчера", null, OnlineStatus.OFFLINE, MessageStatus.NONE, 13, true),
            ChatModel("4", "Максим", "Слушай, не могу загрузить файл...", "Вчера", null, OnlineStatus.ONLINE, MessageStatus.ERROR, 0, false),
            ChatModel("5", "Неизвестный", "Вы выиграли приз, перейдите по ссылке", "Пн", null, OnlineStatus.BLOCKED, MessageStatus.NONE, 0, false),
            ChatModel("6", "Елена", "Ок", "Пн", null, OnlineStatus.OFFLINE, MessageStatus.READ, 0, false),
            ChatModel("7", "Алексей", "Давай обсудим это чуть позже", "10 Мая", null, OnlineStatus.ONLINE, MessageStatus.SENT, 0, false),

            // --- ДОПОЛНИТЕЛЬНЫЕ ЧАТЫ ДЛЯ ТЕСТА СКРОЛЛА ---
            ChatModel("8", "Мама", "Купи фламиши с ветчиной по пути домой", "09 Мая", null, OnlineStatus.ONLINE, MessageStatus.READ, 0, false),
            ChatModel("9", "Староста П2-23", "Скиньте лабы по питону до пятницы!", "09 Мая", null, OnlineStatus.OFFLINE, MessageStatus.NONE, 5, false),
            ChatModel("10", "Влад", "Какую термопасту лучше взять для нового кулера?", "08 Мая", null, OnlineStatus.ONLINE, MessageStatus.NONE, 2, false),
            ChatModel("11", "Саня", "Го вечером в CS2, я скин на ТП продал", "08 Мая", null, OnlineStatus.OFFLINE, MessageStatus.READ, 0, false),
            ChatModel("12", "OpenWrt Community", "Как настроить блокировку рекламы на роутере Cudy?", "07 Мая", null, OnlineStatus.OFFLINE, MessageStatus.NONE, 45, true),
            ChatModel("13", "Доставка", "Ваш заказ (Биг Хит) будет доставлен через 15 минут", "06 Мая", null, OnlineStatus.OFFLINE, MessageStatus.READ, 0, false),
            ChatModel("14", "Проект Prime", "Кнопка отправки готова, лого телеграма убрал", "05 Мая", null, OnlineStatus.ONLINE, MessageStatus.SENT, 0, false),
            ChatModel("15", "Вика", "Билеты на поезд до Анапы уже у тебя?", "04 Мая", null, OnlineStatus.OFFLINE, MessageStatus.NONE, 1, false)
        )

        adapter = ChatAdapter(allChats)
        binding.recyclerViewChats.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChats.adapter = adapter

        // ==========================================
        // 3. ЛОГИКА ПОИСКА (ФИЛЬТРАЦИЯ)
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
                        chat.name.lowercase().contains(query) ||
                                chat.lastMessage.lowercase().contains(query)
                    }
                    adapter.updateList(filteredChats)
                }
            }
        })

        // ==========================================
        // 4. НАСТРОЙКА МОНИТОРИНГА ИНТЕРНЕТА
        // ==========================================
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Проверяем состояние сети прямо при запуске экрана
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isInitiallyConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        // Ставим начальный текст в зависимости от наличия сети
        binding.inputLayoutSearch.hint = if (isInitiallyConnected) "ПОИСК" else "Ожидание сети..."

        // Регистрируем наш слушатель для отслеживания изменений в реальном времени
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // ==========================================
        // 5. ОТКРЫТИЕ НАСТРОЕК (КЛИК И СВАЙП)
        // ==========================================

        // Вариант 1: Простое открытие по клику на аватарку в тулбаре
        binding.ivToolbarAvatar.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Вариант 2: Магия свайпа (шторка вниз)
        var startY = 0f
        binding.recyclerViewChats.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // Запоминаем точку, куда изначально коснулся палец
                    startY = event.y
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val endY = event.y
                    val distance = endY - startY

                    // Проверяем 2 условия одновременно:
                    // 1. Палец проехал вниз больше чем на 150 пикселей (защита от случайных дрожаний)
                    // 2. Список чатов НЕ может прокручиваться вверх (-1), то есть он в самом начале!
                    if (distance > 150 && !binding.recyclerViewChats.canScrollVertically(-1)) {
                        startActivity(Intent(this@ChatListActivity, SettingsActivity::class.java))
                        // ДОБАВЛЯЕМ ВОТ ЭТУ СТРОЧКУ (Магия выезда сверху):
                        overridePendingTransition(R.anim.slide_in_top, R.anim.stay)
                    }
                }
            }
            // Обязательно возвращаем false, чтобы RecyclerView продолжал нормально листать чаты!
            false
        }
    }

    // Очень важно убирать слушатели, когда экран закрывается, чтобы не было утечек памяти
    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

}
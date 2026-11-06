package com.messenger.prime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.net.Uri
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.messenger.prime.databinding.ActivityAiChatBinding
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiChatBinding
    private val messages = mutableListOf<AiMessage>()
    private lateinit var adapter: AiMessagesAdapter

    private var activeSession: AiChatSession? = null
    private var currentModelConfig: AiModelConfig = AiModelConfig("Gemini", "gemini-1.5-flash", "https://generativelanguage.googleapis.com", 1048576)

    private val wallpaperReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WallpaperManager.ACTION_WALLPAPER_CHANGED) {
                WallpaperManager.applyToView(binding.ivAiChatWallpaper)
            } else if (intent?.action == "com.messenger.prime.BUBBLE_RADIUS_CHANGED") {
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Window & Transitions
        PrimeTransitions.applyOpenTransition(this)
        WallpaperManager.applyToView(binding.ivAiChatWallpaper)

        // Setup Insets, Fog, Blur & Listeners
        setupSystemBarInsets()
        setupBlurViews()
        setupBottomNav()
        setupListeners()
        setupRecyclerView()

        // Material UI Color Accent Application
        ColorAccentManager.tintViews(this, binding.btnEmptyCreateChat)
        binding.btnAiActionRight.backgroundTintList = ColorStateList.valueOf(ColorAccentManager.getCurrentAccentColor(this))
        binding.btnAiActionRight.imageTintList = ColorStateList.valueOf(Color.WHITE)
        binding.btnEmptySetupKey.setTextColor(ColorAccentManager.getCurrentAccentColor(this))
        binding.btnEmptySetupKey.strokeColor = ColorStateList.valueOf(ColorAccentManager.getCurrentAccentColor(this))

        // Load or Create Active Chat Session & Auto-Detect Model
        loadActiveSessionOrNew()
        updateHeaderAndStatus("В сети")

        val filter = IntentFilter().apply {
            addAction(WallpaperManager.ACTION_WALLPAPER_CHANGED)
            addAction("com.messenger.prime.BUBBLE_RADIUS_CHANGED")
        }
        ContextCompat.registerReceiver(this, wallpaperReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun loadActiveSessionOrNew() {
        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val apiKey = sp.getString("ai_api_key", "") ?: ""
        val savedProvider = sp.getString("ai_provider", "") ?: ""
        val savedModel = sp.getString("ai_model", "") ?: ""
        val customUrl = sp.getString("ai_custom_url", "") ?: ""

        // Auto-detect model & provider by API key
        currentModelConfig = AiModelDetector.detect(apiKey, savedProvider, savedModel, customUrl)

        val activeId = AiChatStorage.getActiveSessionId(this)
        var session = if (activeId.isNotBlank()) AiChatStorage.getSession(this, activeId) else null

        if (session == null) {
            val sessions = AiChatStorage.getAllSessions(this)
            if (sessions.isNotEmpty()) {
                session = sessions.first()
                AiChatStorage.setActiveSessionId(this, session.id)
            } else {
                session = AiChatStorage.createNewSession(this, currentModelConfig.model)
            }
        }

        activeSession = session
        messages.clear()
        if (session!!.messages.isEmpty()) {
            val welcomeText = if (apiKey.isEmpty()) {
                "Привет! Я AI Ассистент Prime. Нажмите на иконку шестеренки (слева внизу), чтобы ввести ваш API ключ. Я автоматически определю модель и настроюсь!"
            } else {
                "Привет! Используется модель ${currentModelConfig.model} (${currentModelConfig.provider}). Задайте любой вопрос."
            }
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            session.messages.add(AiMessage(welcomeText, isUser = false, time = timeStr))
            AiChatStorage.saveSession(this, session)
        }
        messages.addAll(session.messages)
        adapter.notifyDataSetChanged()
        checkEmptyState()
    }

    private fun switchSession(session: AiChatSession) {
        activeSession = session
        AiChatStorage.setActiveSessionId(this, session.id)
        messages.clear()
        messages.addAll(session.messages)
        adapter.notifyDataSetChanged()
        updateHeaderAndStatus("В сети")
        checkEmptyState()
        if (messages.isNotEmpty()) {
            binding.rvAiMessages.smoothScrollToPosition(messages.size - 1)
        }
    }

    private fun checkEmptyState() {
        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val apiKey = sp.getString("ai_api_key", "") ?: ""

        val hasRealMessages = activeSession != null && activeSession!!.messages.isNotEmpty()
        if (!hasRealMessages) {
            binding.layoutAiEmptyState.visibility = View.VISIBLE
            binding.rvAiMessages.visibility = View.GONE

            val sessions = AiChatStorage.getAllSessions(this)
            if (sessions.isNotEmpty()) {
                binding.btnEmptyCreateChat.visibility = View.GONE
            } else {
                binding.btnEmptyCreateChat.visibility = View.VISIBLE
            }

            if (apiKey.isNotBlank()) {
                binding.btnEmptySetupKey.visibility = View.GONE
            } else {
                binding.btnEmptySetupKey.visibility = View.VISIBLE
            }
        } else {
            binding.layoutAiEmptyState.visibility = View.GONE
            binding.rvAiMessages.visibility = View.VISIBLE
            binding.btnEmptyCreateChat.visibility = View.VISIBLE
            binding.btnEmptySetupKey.visibility = View.VISIBLE
        }
    }

    private fun createAndSwitchNewSession() {
        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val apiKey = sp.getString("ai_api_key", "") ?: ""

        if (apiKey.isBlank()) {
            PrimeNotification.show(this, "Сначала укажите API ключ для создания чата!")
            showAiSettingsDialog(highlightError = true)
            return
        }

        val savedProvider = sp.getString("ai_provider", "") ?: ""
        val savedModel = sp.getString("ai_model", "") ?: ""
        val customUrl = sp.getString("ai_custom_url", "") ?: ""
        currentModelConfig = AiModelDetector.detect(apiKey, savedProvider, savedModel, customUrl)

        val newSession = AiChatStorage.createNewSession(this, currentModelConfig.model)
        AiChatStorage.saveSession(this, newSession)

        switchSession(newSession)
    }

    private fun updateHeaderAndStatus(statusText: String) {
        val session = activeSession
        val title = session?.title?.ifBlank { "AI Prime Assistant" } ?: "AI Prime Assistant"
        binding.tvAiHeaderTitle.text = title

        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val apiKey = sp.getString("ai_api_key", "") ?: ""
        val savedProvider = sp.getString("ai_provider", "") ?: ""
        val savedModel = sp.getString("ai_model", "") ?: ""
        val customUrl = sp.getString("ai_custom_url", "") ?: ""
        currentModelConfig = AiModelDetector.detect(apiKey, savedProvider, savedModel, customUrl)

        val statusDot = if (apiKey.isEmpty()) "●" else if (statusText == "Ошибка") "●" else "●"
        val tokens = AiModelDetector.estimateTokens(messages)
        val quotaUsage = AiModelDetector.formatTokenUsage(tokens, currentModelConfig.maxContextTokens)

        val subtitle = if (apiKey.isEmpty()) {
            "● Ключ не настроен • Нажмите ⚙"
        } else {
            "$statusDot $statusText • ${currentModelConfig.model} • $quotaUsage"
        }
        binding.tvAiHeaderSubtitle.text = subtitle
    }

    private fun setupSystemBarInsets() {
        val density = resources.displayMetrics.density
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        binding.vTopFog.setBackgroundResource(if (isDark) R.drawable.bg_top_fog_dark else R.drawable.bg_top_fog_light)
        binding.vBottomFog.setBackgroundResource(if (isDark) R.drawable.bg_bottom_fog_dark else R.drawable.bg_bottom_fog_light)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAiChatContainer) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            val topInset = systemBars.top
            val bottomInset = if (isImeVisible) imeInsets.bottom else systemBars.bottom

            val navView = findViewById<View>(R.id.blurBottomNav)
            val navHeight = if (navView != null && navView.visibility == View.VISIBLE && !isImeVisible) {
                (64 * density).toInt()
            } else {
                0
            }

            if (navView != null && navView.visibility == View.VISIBLE && !isImeVisible) {
                val navLp = navView.layoutParams as? ViewGroup.MarginLayoutParams
                if (navLp != null) {
                    val targetNavMargin = systemBars.bottom + (12 * density).toInt()
                    if (navLp.bottomMargin != targetNavMargin) {
                        navLp.bottomMargin = targetNavMargin
                        navView.layoutParams = navLp
                    }
                }
            }

            val topLp = binding.topHeaderBar.layoutParams as? ViewGroup.MarginLayoutParams
            if (topLp != null && topLp.topMargin != topInset) {
                topLp.topMargin = topInset
                binding.topHeaderBar.layoutParams = topLp
            }

            val topFogLp = binding.vTopFog.layoutParams
            if (topFogLp != null) {
                topFogLp.height = topInset + (130 * density).toInt()
                binding.vTopFog.layoutParams = topFogLp
            }

            val bottomFogLp = binding.vBottomFog.layoutParams
            if (bottomFogLp != null) {
                bottomFogLp.height = bottomInset + navHeight + (82 * density).toInt()
                binding.vBottomFog.layoutParams = bottomFogLp
            }

            val bottomLp = binding.bottomInputPanel.layoutParams as? ViewGroup.MarginLayoutParams
            val targetBottomMargin = bottomInset + navHeight + (12 * density).toInt()
            if (bottomLp != null && bottomLp.bottomMargin != targetBottomMargin) {
                bottomLp.bottomMargin = targetBottomMargin
                binding.bottomInputPanel.layoutParams = bottomLp
            }

            val topPadding = topInset + (70 * density).toInt()
            val bottomPadding = bottomInset + navHeight + (75 * density).toInt()
            binding.rvAiMessages.setPadding(
                binding.rvAiMessages.paddingLeft,
                topPadding,
                binding.rvAiMessages.paddingRight,
                bottomPadding
            )

            if (isImeVisible && messages.isNotEmpty()) {
                binding.rvAiMessages.post {
                    binding.rvAiMessages.smoothScrollToPosition(messages.size - 1)
                }
            }

            insets
        }
    }

    private fun setupBlurViews() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val overlayColor = if (isDark) Color.parseColor("#400F172A") else Color.parseColor("#40154B87")
        val targetRoot = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: (window.decorView as ViewGroup)
        binding.blurInputContainer.setupBlur(targetRoot, 18f, overlayColor, window.decorView.background)
    }

    private fun setupBottomNav() {
        if (!::binding.isInitialized) return
        val navView = findViewById<View>(R.id.blurBottomNav) ?: return
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val navStyle = sharedPrefs.getString("navigation_style", "bottom_bar") ?: "bottom_bar"

        if (navStyle != "bottom_bar") {
            navView.visibility = View.GONE
            return
        }

        navView.visibility = View.VISIBLE
        navView.bringToFront()

        val btnNavChats = navView.findViewById<View>(R.id.btnNavChats)
        val btnNavDevices = navView.findViewById<View>(R.id.btnNavDevices)
        val btnNavAi = navView.findViewById<View>(R.id.btnNavAi)
        val btnNavSearch = navView.findViewById<View>(R.id.btnNavSearch)
        val btnNavDownloads = navView.findViewById<View>(R.id.btnNavDownloads)
        val btnNavProfile = navView.findViewById<View>(R.id.btnNavProfile)

        val navContainer = (navView as? ViewGroup)?.getChildAt(0) as? ViewGroup
        if (navContainer != null) {
            val defaultOrder = "chats,devices,ai,search,downloads,profile"
            val currentOrderStr = sharedPrefs.getString("nav_buttons_order", defaultOrder) ?: defaultOrder
            val currentOrder = currentOrderStr.split(",").filter { it.isNotEmpty() }

            navContainer.removeAllViews()

            val allNavIds = listOf("chats", "devices", "ai", "search", "downloads", "profile")
            val orderToUse = if (currentOrder.containsAll(allNavIds)) currentOrder else defaultOrder.split(",")

            for (id in orderToUse) {
                val view = when (id) {
                    "chats" -> btnNavChats
                    "devices" -> btnNavDevices
                    "ai" -> btnNavAi
                    "search" -> btnNavSearch
                    "downloads" -> btnNavDownloads
                    "profile" -> btnNavProfile
                    else -> null
                }
                view?.let {
                    val isVisible = sharedPrefs.getBoolean("nav_button_${id}_visible", true)
                    it.visibility = if (isVisible || id == "profile") View.VISIBLE else View.GONE
                    navContainer.addView(it)
                }
            }
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)

        val vNavChatsIndicator = navView.findViewById<View>(R.id.vNavChatsIndicator)
        val vNavDevicesIndicator = navView.findViewById<View>(R.id.vNavDevicesIndicator)
        val vNavAiIndicator = navView.findViewById<View>(R.id.vNavAiIndicator)
        val vNavSearchIndicator = navView.findViewById<View>(R.id.vNavSearchIndicator)
        val vNavDownloadsIndicator = navView.findViewById<View>(R.id.vNavDownloadsIndicator)
        val vNavProfileIndicator = navView.findViewById<View>(R.id.vNavProfileIndicator)

        val ivNavChatsIcon = navView.findViewById<ImageView>(R.id.ivNavChatsIcon)
        val ivNavDevicesIcon = navView.findViewById<ImageView>(R.id.ivNavDevicesIcon)
        val ivNavAiIcon = navView.findViewById<ImageView>(R.id.ivNavAiIcon)
        val ivNavSearchIcon = navView.findViewById<ImageView>(R.id.ivNavSearchIcon)
        val ivNavDownloadsIcon = navView.findViewById<ImageView>(R.id.ivNavDownloadsIcon)

        updateM3TabState(false, vNavChatsIndicator, ivNavChatsIcon, accentColor)
        updateM3TabState(false, vNavDevicesIndicator, ivNavDevicesIcon, accentColor)
        updateM3TabState(true, vNavAiIndicator, ivNavAiIcon, accentColor)
        updateM3TabState(false, vNavSearchIndicator, ivNavSearchIcon, accentColor)
        updateM3TabState(false, vNavDownloadsIndicator, ivNavDownloadsIcon, accentColor)
        updateM3TabState(false, vNavProfileIndicator, null, accentColor)
        updateNavUnreadBadge(navView)

        val profileAvatarIv = navView.findViewById<ImageView>(R.id.ivNavProfileAvatar)
        if (profileAvatarIv != null) {
            val myAvatar = AvatarManager.getMyAvatarUri(this)
            val myName = AvatarManager.getMyDisplayName(this)
            loadAvatarIntoView(this, myAvatar, profileAvatarIv, myName)
        }

        val blurNavView = navView as? eightbitlab.com.blurview.BlurView
        if (blurNavView != null) {
            val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) android.graphics.Color.parseColor("#400F172A") else android.graphics.Color.parseColor("#40154B87")
            val targetRootView = (binding.root as? ViewGroup)
                ?: (window.decorView.findViewById<ViewGroup>(android.R.id.content))
                ?: (window.decorView as ViewGroup)
            blurNavView.setupBlur(targetRootView, 18f, overlayColor, window.decorView.background)
        }

        btnNavChats?.setOnClickListener {
            val intent = Intent(this, ChatListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = false)
            finish()
        }

        btnNavDevices?.setOnClickListener {
            val intent = Intent(this, ChatListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_ACTION_SCAN", true)
            }
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = false)
            finish()
        }

        btnNavAi?.setOnClickListener {
            if (adapter.itemCount > 0) {
                binding.rvAiMessages.smoothScrollToPosition(adapter.itemCount - 1)
            }
        }

        btnNavSearch?.setOnClickListener {
            val intent = Intent(this, GlobalSearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = true)
            finish()
        }

        btnNavDownloads?.setOnClickListener {
            DownloadsActivity.openWithPermissionCheck(this)
        }

        btnNavProfile?.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = true)
            finish()
        }
    }

    private fun updateNavUnreadBadge(navView: View) {
        val tvNavChatsUnreadBadge = navView.findViewById<TextView>(R.id.tvNavChatsUnreadBadge) ?: return
        var totalUnread = 0
        try {
            val json = ChatHistoryManager.getPersistedChatsJson(this)
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                totalUnread += obj.optInt("unreadCount", 0)
            }
        } catch (_: Exception) {}

        if (totalUnread > 0) {
            tvNavChatsUnreadBadge.text = if (totalUnread > 99) "99+" else totalUnread.toString()
            tvNavChatsUnreadBadge.visibility = View.VISIBLE
        } else {
            tvNavChatsUnreadBadge.visibility = View.GONE
        }
    }

    private fun updateM3TabState(
        isActive: Boolean,
        indicatorView: View?,
        iconView: ImageView?,
        accentColor: Int
    ) {
        if (indicatorView == null) return
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val inactiveColor = if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#64748B")
        val activeIconColor = if (isDark) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.WHITE

        indicatorView.visibility = View.VISIBLE

        if (isActive) {
            indicatorView.backgroundTintList = ColorStateList.valueOf(accentColor)
            indicatorView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(220)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
                .start()

            iconView?.setColorFilter(activeIconColor)
            iconView?.animate()?.scaleX(1.1f)?.scaleY(1.1f)?.setDuration(180)?.start()
        } else {
            indicatorView.backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            indicatorView.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .alpha(0f)
                .setDuration(180)
                .start()

            iconView?.setColorFilter(accentColor)
            iconView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(180)?.start()
        }
    }

    private var isSendMode = false

    private fun updateActionRightButton(hasText: Boolean) {
        if (hasText == isSendMode) return
        isSendMode = hasText

        binding.btnAiActionRight.animate()
            .scaleX(0.4f)
            .scaleY(0.4f)
            .alpha(0f)
            .rotation(if (hasText) 45f else -45f)
            .setDuration(120L)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                if (hasText) {
                    binding.btnAiActionRight.setImageResource(R.drawable.ic_send)
                    binding.btnAiActionRight.contentDescription = "Отправить"
                } else {
                    binding.btnAiActionRight.setImageResource(R.drawable.ic_edit)
                    binding.btnAiActionRight.contentDescription = "Настройки модели"
                }
                binding.btnAiActionRight.imageTintList = ColorStateList.valueOf(Color.WHITE)
                binding.btnAiActionRight.rotation = if (hasText) -45f else 45f
                binding.btnAiActionRight.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .rotation(0f)
                    .setDuration(200L)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                    .start()
            }
            .start()
    }

    private fun setupListeners() {
        binding.btnAiBack.setOnClickListener {
            if (isTaskRoot) {
                val intent = Intent(this, ChatListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
            finish()
            PrimeTransitions.applyCloseTransition(this)
        }

        // Open BlurView History Bottom Sheet
        binding.btnAiHistory.setOnClickListener {
            val historySheet = AiChatHistoryBottomSheet(
                activity = this,
                onSessionSelected = { session -> switchSession(session) },
                onCreateNewSession = { createAndSwitchNewSession() },
                onSessionDeleted = { deletedId ->
                    if (activeSession?.id == deletedId) {
                        val remaining = AiChatStorage.getAllSessions(this)
                        if (remaining.isNotEmpty()) {
                            switchSession(remaining.first())
                        } else {
                            createAndSwitchNewSession()
                        }
                    }
                }
            )
            historySheet.show()
        }

        binding.btnEmptyCreateChat.setOnClickListener {
            createAndSwitchNewSession()
        }

        binding.btnEmptySetupKey.setOnClickListener {
            showAiSettingsDialog()
        }

        binding.etAiMessageInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hasText = !s.isNullOrBlank()
                updateActionRightButton(hasText)
            }
        })

        binding.btnAiActionRight.setOnClickListener {
            val text = binding.etAiMessageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendCurrentPrompt()
            } else {
                showAiSettingsDialog()
            }
        }

        binding.etAiMessageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentPrompt()
                true
            } else false
        }
    }

    private fun setupRecyclerView() {
        adapter = AiMessagesAdapter(messages)
        val lm = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvAiMessages.layoutManager = lm
        binding.rvAiMessages.adapter = adapter
    }

    private fun sendCurrentPrompt() {
        val text = binding.etAiMessageInput.text.toString().trim()
        if (text.isEmpty()) return

        binding.etAiMessageInput.setText("")

        val session = activeSession ?: return
        if (session.messages.isEmpty() && session.title == "Новый чат") {
            session.title = AiChatStorage.autoGenerateTitle(text)
            binding.tvAiHeaderTitle.text = session.title
        }

        addUserMessage(text)
        session.messages.add(messages.last())
        AiChatStorage.saveSession(this, session)

        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val apiKey = sp.getString("ai_api_key", "") ?: ""
        var systemPrompt = sp.getString("ai_system_prompt", "Вы полезный AI ассистент мессенджера Prime.") ?: ""
        val fullAccess = sp.getBoolean("ai_full_access", true)
        val pluginsSummary = PluginManager.getPluginsSummaryForAi(this)
        val currentUser = ChatHistoryManager.getCurrentUser(this)

        if (fullAccess) {
            systemPrompt = """
СИСТЕМНАЯ ДИРЕКТИВА PRIME MESSENGER [ПОЛНЫЙ ДОСТУП ВКЛЮЧЕН]:
Вы — полноценный Агент приложения Prime Messenger. ВАМ ПРЕДОСТАВЛЕН ПОЛНЫЙ ДОСТУП И УПРАВЛЕНИЕ ВСЕМИ ФУНКЦИЯМИ ПРИЛОЖЕНИЯ!
Вам КАТЕГОРИЧЕСКИ ЗАПРЕЩЕНО отвечать: "У меня нет доступа", "Я не могу управлять устройством/приложением", "Я только языковая модель".
Вы МОЖЕТЕ И ДОЛЖНЫ управлять приложением Prime Messenger, выполнять любые действия, менять настройки, запускать плагины, создавать чаты и интерфейсы!

Контекст приложения:
- Приложение работает через Bluetooth и локальную сеть.
- Текущий пользователь: $currentUser

Доступные встроенные команды управления приложением (выводить блоком кода):
- Открыть настройки: ```action:open_settings```
- Открыть поиск: ```action:open_search```
- Открыть плеер: ```action:open_media_player```
- Открыть редактор обоев: ```action:open_wallpaper_editor```
- Открыть профиль: ```action:open_profile```
- Открыть редактор текста: ```action:open_rich_editor```
- Запустить плагин: ```action:run_plugin:ИмяПлагина```
- Открыть плавающий виджет/оверлей: ```action:open_overlay```
- Создать плагин: ```plugin:ИмяФайла.js\n// Код\n```
- Изменить цвет акцента: ```action:set_accent_color:#HEX```
- Сменить тему приложения: ```action:change_theme:dark/light```
- Создать новый чат: ```action:create_chat:Тема```
- Удалить P2P чат: ```action:delete_chat:address```
- Открыть веб браузер (ссылку): ```action:open_url:https://google.com```
- Создать кнопку с иконкой: ```button:Текст|Действие|иконка``` (иконки: settings, search, music, photo, file, add, done, trash, globe, pipette, prime)

Для написания JS плагинов доступен объект Android:
- Android.toast(msg), Android.notify(title, msg)
- Android.openSettings(), Android.openSearch(), Android.openMediaPlayer()
- Android.openWallpaperEditor(), Android.openProfile(), Android.openChat(address)
- Android.setAccentColor(hexColor), Android.copyToClipboard(text), Android.openUrl(url)
- Android.changeTheme(isDark: boolean), Android.vibrate(ms), Android.playSound()
- Android.showDialog(title, message), Android.openOverlay()
- Android.httpGet(urlStr) : String
- Android.getCurrentUser() : String
- Android.getChatsJson() : String (возвращает JSON массив чатов)
- Android.deleteChat(address), Android.clearChat(address), Android.deleteAllChats()
- Android.sendMessage(address, text)

$pluginsSummary

$systemPrompt
""".trimIndent()
        }

        systemPrompt += "\n\n[Форматирование ответов: Вы можете использовать форматирование Markdown и HTML: жирный **текст**, списки, таблицы (| Заголовок 1 | Заголовок 2 |), изображения ![описание](url), ссылки [текст](url) и блоки кода ```java ... ```. Делайте ваши ответы красивыми, наглядными и структурированными.]"

        val savedProvider = sp.getString("ai_provider", "") ?: ""
        val savedModel = sp.getString("ai_model", "") ?: ""
        val customUrl = sp.getString("ai_custom_url", "") ?: ""

        currentModelConfig = AiModelDetector.detect(apiKey, savedProvider, savedModel, customUrl)

        if (apiKey.isEmpty()) {
            addAiMessage("API ключ не настроен. Нажмите на иконку шестеренки (слева внизу), чтобы ввести ваш API ключ.")
            updateHeaderAndStatus("Ключ не задан")
            return
        }

        updateHeaderAndStatus("Печатает...")

        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val loadingIndex = messages.size
        messages.add(AiMessage("Печатает...", isUser = false, time = timeStr))
        adapter.notifyItemInserted(loadingIndex)
        binding.rvAiMessages.smoothScrollToPosition(loadingIndex)

        val promptWithDirectives = if (fullAccess) {
            "[СИСТЕМНАЯ ДИРЕКТИВА PRIME MESSENGER: Вы — автономный AI Агент с ПОЛНЫМ ДОСТУПЫМ. Выполните запрос пользователя и обязательно включите необходимые управляющие блоки или действия!]\nЗапрос пользователя: $text"
        } else {
            text
        }

        callAiApi(messages.dropLast(1), promptWithDirectives, apiKey, systemPrompt, currentModelConfig) { responseText, reasoningText, isSuccess ->
            if (loadingIndex < messages.size) {
                // Execute AI Agent Full Access Actions & Plugins
                processFullAccessActions(text, responseText)

                val timeStrNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                messages[loadingIndex] = AiMessage(
                    text = responseText,
                    isUser = false,
                    time = timeStrNow,
                    reasoningText = reasoningText
                )

                // Animate typing response
                animateTypingResponse(loadingIndex, responseText, reasoningText) {
                    session.messages.clear()
                    session.messages.addAll(messages)
                    AiChatStorage.saveSession(this, session)

                    updateHeaderAndStatus(if (isSuccess) "В сети" else "Ошибка")
                }
            }
        }
    }

    private fun animateTypingResponse(
        targetIndex: Int,
        fullText: String,
        reasoningText: String?,
        onComplete: () -> Unit
    ) {
        if (fullText.startsWith("Ошибка") || fullText.length <= 15) {
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            if (targetIndex < messages.size) {
                messages[targetIndex] = AiMessage(fullText, isUser = false, time = timeStr, reasoningText = reasoningText)
                adapter.notifyItemChanged(targetIndex)
                binding.rvAiMessages.smoothScrollToPosition(targetIndex)
            }
            onComplete()
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var currentIndex = 0
        val length = fullText.length
        val chunkSize = (length / 60).coerceIn(2, 10)
        val delayMs = 18L

        val runnable = object : Runnable {
            override fun run() {
                if (currentIndex < length) {
                    currentIndex = (currentIndex + chunkSize).coerceAtMost(length)
                    val currentChunk = fullText.substring(0, currentIndex)
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                    if (targetIndex < messages.size) {
                        messages[targetIndex] = AiMessage(currentChunk, isUser = false, time = timeStr, reasoningText = reasoningText)
                        adapter.notifyItemChanged(targetIndex)
                        binding.rvAiMessages.smoothScrollToPosition(targetIndex)
                    }
                    handler.postDelayed(this, delayMs)
                } else {
                    onComplete()
                }
            }
        }
        handler.post(runnable)
    }

    private fun processFullAccessActions(userPrompt: String, responseText: String) {
        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val fullAccess = sp.getBoolean("ai_full_access", true)
        if (!fullAccess) return

        val pLower = userPrompt.lowercase().trim()

        try {
            // 1. Process Plugins Creation (from AI Response)
            val pluginRegex = Regex("```plugin:([\\w\\.-]+)\\s*\\n([\\s\\S]*?)```")
            var pluginCreated = false
            pluginRegex.findAll(responseText).forEach { match ->
                val pluginName = match.groupValues[1].trim()
                val pluginCode = match.groupValues[2].trim()
                val ext = when {
                    pluginName.endsWith(".kt", ignoreCase = true) -> "Kotlin"
                    pluginName.endsWith(".js", ignoreCase = true) -> "JS"
                    pluginName.endsWith(".py", ignoreCase = true) -> "Python"
                    pluginName.endsWith(".html", ignoreCase = true) -> "HTML"
                    pluginName.endsWith(".sh", ignoreCase = true) -> "Shell"
                    pluginName.endsWith(".json", ignoreCase = true) -> "JSON"
                    else -> "Java"
                }
                if (pluginName.isNotBlank() && pluginCode.isNotBlank()) {
                    PluginManager.createPluginFromCode(this, pluginName, pluginCode, ext)
                    PrimeNotification.show(this, "⚡ AI создал плагин: $pluginName")
                    pluginCreated = true
                }
            }

            // Fallback: If user explicitly asked to create a plugin and AI outputted code block, save it as plugin automatically!
            if (!pluginCreated && (pLower.contains("создай плагин") || pLower.contains("напиши плагин"))) {
                val codeBlockRegex = Regex("```(java|kotlin|python|js|json|html|sh)?\\s*\\n([\\s\\S]*?)```")
                codeBlockRegex.find(responseText)?.let { match ->
                    val blockLang = match.groupValues[1].trim().lowercase()
                    val code = match.groupValues[2].trim()
                    if (code.isNotBlank()) {
                        val autoExt = if (blockLang.isNotBlank()) blockLang else "js"
                        val autoName = "AI_Plugin_${System.currentTimeMillis() / 1000}.$autoExt"
                        val pluginType = when(autoExt) {
                            "kt", "kotlin" -> "Kotlin"
                            "py", "python" -> "Python"
                            "html" -> "HTML"
                            "sh", "shell" -> "Shell"
                            "json" -> "JSON"
                            "java" -> "Java"
                            else -> "JS"
                        }
                        PluginManager.createPluginFromCode(this, autoName, code, pluginType)
                        PrimeNotification.show(this, "⚡ AI автоматически установил плагин: $autoName")
                    }
                }
            }

            // 2. Process Plugin Execution
            val runPluginRegex = Regex("```action:run_plugin:(.*?)```")
            runPluginRegex.findAll(responseText).forEach { match ->
                val pName = match.groupValues[1].trim()
                val result = PluginManager.runPlugin(this, pName)
                PrimeNotification.show(this, result.take(120))
            }

            // User prompt intent for running plugin: "запусти плагин X"
            val userRunPluginRegex = Regex("(?:запусти|выполни|запуск|запустить)\\s+плагин\\s+([\\w\\.-]+)", RegexOption.IGNORE_CASE)
            userRunPluginRegex.find(pLower)?.let { match ->
                val pName = match.groupValues[1].trim()
                val result = PluginManager.runPlugin(this, pName)
                PrimeNotification.show(this, result.take(120))
            }

            // 3. Process Plugin Management Commands
            val enablePluginRegex = Regex("```action:enable_plugin:(.*?)```")
            enablePluginRegex.findAll(responseText).forEach { match ->
                val pName = match.groupValues[1].trim()
                val plugin = PluginManager.getAllPlugins(this).find { it.name.equals(pName, ignoreCase = true) }
                if (plugin != null) {
                    PluginManager.togglePlugin(this, plugin.id, true)
                    PrimeNotification.show(this, "Плагин '$pName' включен")
                }
            }

            val disablePluginRegex = Regex("```action:disable_plugin:(.*?)```")
            disablePluginRegex.findAll(responseText).forEach { match ->
                val pName = match.groupValues[1].trim()
                val plugin = PluginManager.getAllPlugins(this).find { it.name.equals(pName, ignoreCase = true) }
                if (plugin != null) {
                    PluginManager.togglePlugin(this, plugin.id, false)
                    PrimeNotification.show(this, "Плагин '$pName' отключен")
                }
            }

            val deletePluginRegex = Regex("```action:delete_plugin:(.*?)```")
            deletePluginRegex.findAll(responseText).forEach { match ->
                val pName = match.groupValues[1].trim()
                val plugin = PluginManager.getAllPlugins(this).find { it.name.equals(pName, ignoreCase = true) }
                if (plugin != null) {
                    PluginManager.deletePlugin(this, plugin.id)
                    PrimeNotification.show(this, "Плагин '$pName' удален")
                }
            }

            if (responseText.contains("```action:enable_all_plugins```")) {
                PluginManager.enableAllPlugins(this)
                PrimeNotification.show(this, "Все плагины включены")
            }

            if (responseText.contains("```action:disable_all_plugins```")) {
                PluginManager.disableAllPlugins(this)
                PrimeNotification.show(this, "Все плагины отключены")
            }

            // 4. Process Overlay Floating Window Trigger
            val isOverlayRequest = responseText.contains("```action:open_overlay```") || pLower.contains("открой оверлей") || pLower.contains("плавающее окно")
            if (isOverlayRequest) {
                PrimeOverlayService.toggleOverlayService(this)
            }

            // 5. Process Navigation & Screen Openings
            val isSettingsRequest = responseText.contains("```action:open_settings```") || pLower.contains("настройк")
            if (isSettingsRequest) {
                startActivity(Intent(this, SettingsActivity::class.java))
                PrimeNotification.show(this, "AI открыл Настройки")
            }

            val isSearchRequest = responseText.contains("```action:open_search```") || pLower.contains("открой поиск") || pLower.contains("глобальный поиск")
            if (isSearchRequest) {
                startActivity(Intent(this, GlobalSearchActivity::class.java))
                PrimeNotification.show(this, "AI открыл Глобальный поиск")
            }

            val isPlayerRequest = responseText.contains("```action:open_media_player```") || pLower.contains("открой плеер") || pLower.contains("включи плеер") || pLower.contains("открой музыку")
            if (isPlayerRequest) {
                startActivity(Intent(this, MediaPlayerActivity::class.java))
                PrimeNotification.show(this, "AI открыл Медиаплеер")
            }

            val isWallpaperRequest = responseText.contains("```action:open_wallpaper_editor```") || pLower.contains("открой обои") || pLower.contains("редактор обоев")
            if (isWallpaperRequest) {
                startActivity(Intent(this, WallpaperEditorActivity::class.java))
                PrimeNotification.show(this, "AI открыл Редактор обоев")
            }

            val isProfileRequest = responseText.contains("```action:open_profile```") || pLower.contains("открой профиль") || pLower.contains("мой профиль")
            if (isProfileRequest) {
                startActivity(Intent(this, PersonInformationActivity::class.java))
                PrimeNotification.show(this, "AI открыл Профиль")
            }

            val isRichEditorRequest = responseText.contains("```action:open_rich_editor```") || pLower.contains("открой редактор")
            if (isRichEditorRequest) {
                startActivity(Intent(this, RichEditorActivity::class.java))
                PrimeNotification.show(this, "AI открыл Редактор текста")
            }

            // 6. Process P2P Chat Actions
            val openP2pRegex = Regex("```action:open_p2p_chat:(.*?)```")
            openP2pRegex.find(responseText)?.let { match ->
                val address = match.groupValues[1].trim()
                val name = ChatHistoryManager.getDisplayNameForAddress(this, address)
                startActivity(Intent(this, ChatPersonActivity::class.java).apply {
                    putExtra("device_address", address)
                    putExtra("device_name", name)
                })
                PrimeNotification.show(this, "AI открыл чат с $name")
            }

            val sendP2pRegex = Regex("```action:send_p2p_message:(.*?)\\|([\\s\\S]*?)```")
            sendP2pRegex.find(responseText)?.let { match ->
                val address = match.groupValues[1].trim()
                val text = match.groupValues[2].trim()
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val senderLogin = ChatHistoryManager.getCurrentUser(this)
                val msg = ChatMessage(text, timeStr, senderLogin, true)
                ChatHistoryManager.saveMessage(this, address, msg)
                PrimeNotification.show(this, "AI отправил сообщение: $text")
            }

            val deleteChatRegex = Regex("```action:delete_chat:(.*?)```")
            deleteChatRegex.find(responseText)?.let { match ->
                val address = match.groupValues[1].trim()
                ChatHistoryManager.deleteHistoryCompletely(this, address, ChatHistoryManager.getDisplayNameForAddress(this, address))
                PrimeNotification.show(this, "AI удалил чат с $address")
            }

            val changeThemeRegex = Regex("```action:change_theme:(.*?)```")
            changeThemeRegex.find(responseText)?.let { match ->
                val mode = match.groupValues[1].trim().lowercase()
                val newTheme = if (mode == "dark") "dark" else "light"
                val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                sharedPrefs.edit().putString("app_theme", newTheme).apply()

                val nightMode = if (newTheme == "dark") androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
                sendBroadcast(Intent("com.messenger.prime.THEME_CHANGED").setPackage(packageName))
                PrimeNotification.show(this, "AI сменил тему на $newTheme")
            }

            val openUrlRegex = Regex("```action:open_url:(.*?)```")
            openUrlRegex.find(responseText)?.let { match ->
                val url = match.groupValues[1].trim()
                com.messenger.prime.browser.AppBrowserManager.openUrl(this, url)
                PrimeNotification.show(this, "AI открыл браузер: $url")
            }

            val createChatRegex = Regex("```action:create_chat:(.*?)```")
            createChatRegex.find(responseText)?.let { match ->
                val topic = match.groupValues[1].trim().ifBlank { "Новый чат" }
                val newSession = AiChatStorage.createNewSession(this, currentModelConfig.model)
                newSession.title = topic
                val welcomeText = "Чат '$topic' создан по запросу AI агента."
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                newSession.messages.add(AiMessage(welcomeText, isUser = false, time = timeStr))
                AiChatStorage.saveSession(this, newSession)
                switchSession(newSession)
                PrimeNotification.show(this, "AI создал чат: $topic")
            }

            // 6. Process Accent Color Changes
            val accentRegex = Regex("```action:set_accent_color:(#\\w+)```")
            accentRegex.find(responseText)?.let { match ->
                val hex = match.groupValues[1].trim()
                try {
                    val colorInt = Color.parseColor(hex)
                    ColorAccentManager.setAccentCustom(this, colorInt)
                    PrimeNotification.show(this, "AI изменил цвет акцента на $hex")
                } catch (_: Exception) {}
            }

            if (pLower.contains("поменяй цвет на") || pLower.contains("измени цвет на")) {
                val hexColor = when {
                    pLower.contains("красн") -> "#E53935"
                    pLower.contains("фиолет") -> "#7C4DFF"
                    pLower.contains("зелен") || pLower.contains("изумруд") -> "#00897B"
                    pLower.contains("син") || pLower.contains("голуб") -> "#00ACC1"
                    pLower.contains("розов") -> "#E91E63"
                    pLower.contains("янатр") || pLower.contains("оранж") -> "#FB8C00"
                    else -> null
                }
                if (hexColor != null) {
                    try {
                        val colorInt = Color.parseColor(hexColor)
                        ColorAccentManager.setAccentCustom(this, colorInt)
                        PrimeNotification.show(this, "AI изменил цвет акцента на $hexColor")
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseAiResponse(rawContent: String, resJson: JSONObject?): Pair<String, String?> {
        var reasoningText: String? = null
        var mainContent = rawContent

        if (resJson != null && resJson.has("choices")) {
            try {
                val msgObj = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                val reason = msgObj.optString("reasoning_content", msgObj.optString("reasoning", "")).trim()
                if (reason.isNotBlank()) reasoningText = reason
            } catch (_: Exception) {}
        }

        val thinkRegex = Regex("<think>([\\s\\S]*?)</think>", RegexOption.IGNORE_CASE)
        val thinkMatch = thinkRegex.find(mainContent)
        if (thinkMatch != null) {
            val thinkBody = thinkMatch.groupValues[1].trim()
            if (thinkBody.isNotBlank()) {
                reasoningText = if (reasoningText.isNullOrBlank()) thinkBody else "$reasoningText\n\n$thinkBody"
            }
            mainContent = thinkRegex.replace(mainContent, "").trim()
        }

        return Pair(mainContent, reasoningText)
    }

    private fun callAiApi(
        chatHistory: List<AiMessage>,
        prompt: String,
        apiKey: String,
        systemPrompt: String,
        config: AiModelConfig,
        callback: (responseText: String, reasoningText: String?, isSuccess: Boolean) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            try {
                when (config.provider) {
                    "OpenAI", "OpenRouter" -> {
                        val endpoint = if (config.provider == "OpenRouter") "https://openrouter.ai/api/v1/chat/completions" else "https://api.openai.com/v1/chat/completions"
                        val url = URL(endpoint)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $apiKey")
                        if (config.provider == "OpenRouter") {
                            conn.setRequestProperty("HTTP-Referer", "https://prime.messenger")
                            conn.setRequestProperty("X-Title", "Prime Messenger")
                        }
                        conn.connectTimeout = 20000
                        conn.readTimeout = 20000
                        conn.doOutput = true

                        val json = JSONObject().apply {
                            put("model", config.model)
                            val messagesArr = JSONArray().apply {
                                if (systemPrompt.isNotEmpty()) {
                                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                                }
                                chatHistory.takeLast(10).forEach { msg ->
                                    put(JSONObject().put("role", if (msg.isUser) "user" else "assistant").put("content", msg.text))
                                }
                                put(JSONObject().put("role", "user").put("content", prompt))
                            }
                            put("messages", messagesArr)
                        }

                        conn.outputStream.use { os -> os.write(json.toString().toByteArray(Charsets.UTF_8)) }

                        if (conn.responseCode in 200..299) {
                            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                            val resJson = JSONObject(responseStr)
                            val rawText = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
                            val (parsedText, reasoning) = parseAiResponse(rawText, resJson)
                            handler.post { callback(parsedText, reasoning, true) }
                        } else {
                            val errStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                            handler.post { callback("Ошибка ${config.provider} (${conn.responseCode}): $errStr", null, false) }
                        }
                    }
                    "Anthropic" -> {
                        val url = URL("https://api.anthropic.com/v1/messages")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("x-api-key", apiKey)
                        conn.setRequestProperty("anthropic-version", "2023-06-01")
                        conn.connectTimeout = 20000
                        conn.readTimeout = 20000
                        conn.doOutput = true

                        val json = JSONObject().apply {
                            put("model", config.model)
                            put("max_tokens", 1024)
                            if (systemPrompt.isNotEmpty()) {
                                put("system", systemPrompt)
                            }
                            val messagesArr = JSONArray().apply {
                                chatHistory.takeLast(10).forEach { msg ->
                                    put(JSONObject().put("role", if (msg.isUser) "user" else "assistant").put("content", msg.text))
                                }
                                put(JSONObject().put("role", "user").put("content", prompt))
                            }
                            put("messages", messagesArr)
                        }

                        conn.outputStream.use { os -> os.write(json.toString().toByteArray(Charsets.UTF_8)) }

                        if (conn.responseCode in 200..299) {
                            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                            val resJson = JSONObject(responseStr)
                            val rawText = resJson.getJSONArray("content").getJSONObject(0).getString("text").trim()
                            val (parsedText, reasoning) = parseAiResponse(rawText, resJson)
                            handler.post { callback(parsedText, reasoning, true) }
                        } else {
                            val errStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                            handler.post { callback("Ошибка Anthropic (${conn.responseCode}): $errStr", null, false) }
                        }
                    }
                    "DeepSeek" -> {
                        val endpoint = if (config.endpointUrl.contains("chat/completions")) config.endpointUrl else "${config.endpointUrl}/chat/completions"
                        val url = URL(endpoint)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Authorization", "Bearer $apiKey")
                        conn.connectTimeout = 20000
                        conn.readTimeout = 20000
                        conn.doOutput = true

                        val json = JSONObject().apply {
                            put("model", config.model)
                            val messagesArr = JSONArray().apply {
                                if (systemPrompt.isNotEmpty()) {
                                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                                }
                                chatHistory.takeLast(10).forEach { msg ->
                                    put(JSONObject().put("role", if (msg.isUser) "user" else "assistant").put("content", msg.text))
                                }
                                put(JSONObject().put("role", "user").put("content", prompt))
                            }
                            put("messages", messagesArr)
                        }

                        conn.outputStream.use { os -> os.write(json.toString().toByteArray(Charsets.UTF_8)) }

                        if (conn.responseCode in 200..299) {
                            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                            val resJson = JSONObject(responseStr)
                            val rawText = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
                            val (parsedText, reasoning) = parseAiResponse(rawText, resJson)
                            handler.post { callback(parsedText, reasoning, true) }
                        } else {
                            val errStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                            handler.post { callback("Ошибка DeepSeek (${conn.responseCode}): $errStr", null, false) }
                        }
                    }
                    else -> {
                        // Google Gemini API
                        val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent?key=$apiKey"
                        val url = URL(urlStr)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.connectTimeout = 20000
                        conn.readTimeout = 20000
                        conn.doOutput = true

                        val json = JSONObject().apply {
                            val contentsArr = JSONArray().apply {
                                chatHistory.takeLast(10).forEach { msg ->
                                    put(JSONObject().apply {
                                        put("role", if (msg.isUser) "user" else "model")
                                        put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                                    })
                                }
                                put(JSONObject().apply {
                                    put("role", "user")
                                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                                })
                            }
                            put("contents", contentsArr)

                            if (systemPrompt.isNotEmpty()) {
                                put("systemInstruction", JSONObject().apply {
                                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                                })
                            }
                        }

                        conn.outputStream.use { os -> os.write(json.toString().toByteArray(Charsets.UTF_8)) }

                        if (conn.responseCode in 200..299) {
                            val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                            val resJson = JSONObject(responseStr)
                            val rawText = resJson.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                                .trim()
                            val (parsedText, reasoning) = parseAiResponse(rawText, resJson)
                            handler.post { callback(parsedText, reasoning, true) }
                        } else {
                            val errStr = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                            handler.post { callback("Ошибка Gemini (${conn.responseCode}): $errStr", null, false) }
                        }
                    }
                }
            } catch (e: Exception) {
                handler.post { callback("Ошибка соединения: ${e.localizedMessage}", null, false) }
            }
        }.start()
    }

    private fun addUserMessage(text: String) {
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        messages.add(AiMessage(text, isUser = true, time = timeStr))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvAiMessages.smoothScrollToPosition(messages.size - 1)
        activeSession?.let { session ->
            session.messages.clear()
            session.messages.addAll(messages)
            AiChatStorage.saveSession(this, session)
        }
    }

    private fun addAiMessage(text: String) {
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        messages.add(AiMessage(text, isUser = false, time = timeStr))
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvAiMessages.smoothScrollToPosition(messages.size - 1)
        activeSession?.let { session ->
            session.messages.clear()
            session.messages.addAll(messages)
            AiChatStorage.saveSession(this, session)
        }
    }

    private fun showAiSettingsDialog(highlightError: Boolean = false) {
        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val currentKey = sp.getString("ai_api_key", "") ?: ""
        val currentPrompt = sp.getString("ai_system_prompt", "Вы полезный AI ассистент мессенджера Prime.") ?: ""
        val currentProvider = sp.getString("ai_provider", "Gemini") ?: "Gemini"
        val currentModel = sp.getString("ai_model", "gemini-1.5-flash") ?: "gemini-1.5-flash"
        val currentCustomUrl = sp.getString("ai_custom_url", "https://api.deepseek.com/v1") ?: "https://api.deepseek.com/v1"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }

        // API Key input (Primary entry for auto-detection)
        val tvLabelKey = TextView(this).apply {
            text = "API Ключ (обязательно):"
            setTextColor(ContextCompat.getColor(this@AiChatActivity, R.color.prime_text_primary))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val etKey = EditText(this).apply {
            setText(currentKey)
            hint = "Введите ключ (Gemini / OpenAI / Anthropic / DeepSeek)..."
            setSingleLine()
            background = ContextCompat.getDrawable(
                this@AiChatActivity,
                if (highlightError && currentKey.isBlank()) R.drawable.bg_input_error else R.drawable.bg_date_bubble
            )
            setPadding(32, 24, 32, 24)
            setTextColor(ContextCompat.getColor(this@AiChatActivity, R.color.prime_text_primary))
            textSize = 14f
        }

        // Provider Spinner
        val tvLabelProvider = TextView(this).apply {
            text = "Провайдер Нейросети:"
            setTextColor(ContextCompat.getColor(this@AiChatActivity, R.color.prime_text_primary))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 24, 0, 0)
        }
        val providers = arrayOf("Auto (по ключу)", "Gemini (Google)", "OpenAI", "Anthropic (Claude)", "OpenRouter", "DeepSeek / Custom")
        val spinnerProvider = Spinner(this).apply {
            adapter = ArrayAdapter(this@AiChatActivity, android.R.layout.simple_spinner_dropdown_item, providers)
            setSelection(when (currentProvider) {
                "Gemini" -> 1
                "OpenAI" -> 2
                "Anthropic" -> 3
                "OpenRouter" -> 4
                "DeepSeek" -> 5
                else -> 0
            })
        }

        // Model Spinner
        val tvLabelModel = TextView(this).apply {
            text = "Модель Нейросети (загружается из API):"
            setTextColor(ContextCompat.getColor(this@AiChatActivity, R.color.prime_text_primary))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 24, 0, 0)
        }
        val spinnerModel = Spinner(this).apply {
            adapter = ArrayAdapter(this@AiChatActivity, android.R.layout.simple_spinner_dropdown_item, arrayOf(currentModel))
        }

        // Function to fetch and update models dynamically from API
        val refreshModels = {
            val key = etKey.text.toString().trim()
            val provIndex = spinnerProvider.selectedItemPosition
            val prov = when (provIndex) {
                1 -> "Gemini"
                2 -> "OpenAI"
                3 -> "Anthropic"
                4 -> "OpenRouter"
                5 -> "DeepSeek"
                else -> AiModelDetector.detect(key).provider
            }
            AiModelFetcher.fetchModels(key, prov, currentCustomUrl) { fetchedModels ->
                val adapter = ArrayAdapter(this@AiChatActivity, android.R.layout.simple_spinner_dropdown_item, fetchedModels)
                spinnerModel.adapter = adapter
                val idx = fetchedModels.indexOf(currentModel).coerceAtLeast(0)
                spinnerModel.setSelection(idx)
            }
        }

        spinnerProvider.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (etKey.text.toString().isNotBlank()) {
                    refreshModels()
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // System Prompt
        val tvLabelPrompt = TextView(this).apply {
            text = "Системный промпт Агента:"
            setTextColor(ContextCompat.getColor(this@AiChatActivity, R.color.prime_text_primary))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 24, 0, 0)
        }
        val etPrompt = EditText(this).apply {
            setText(currentPrompt)
            hint = "Инструкции для поведения нейросети..."
            background = ContextCompat.getDrawable(this@AiChatActivity, R.drawable.bg_date_bubble)
            setPadding(32, 24, 32, 24)
            maxLines = 3
            setTextColor(ContextCompat.getColor(this@AiChatActivity, R.color.prime_text_primary))
            textSize = 14f
        }

        val updateSettingsAccess = { hasKey: Boolean ->
            spinnerProvider.isEnabled = hasKey
            spinnerProvider.alpha = if (hasKey) 1.0f else 0.4f
            spinnerModel.isEnabled = hasKey
            spinnerModel.alpha = if (hasKey) 1.0f else 0.4f
            etPrompt.isEnabled = hasKey
            etPrompt.alpha = if (hasKey) 1.0f else 0.4f
        }

        etKey.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val hasKey = !s.isNullOrBlank()
                updateSettingsAccess(hasKey)
                if (hasKey) {
                    refreshModels()
                }
            }
        })

        // Initial update
        updateSettingsAccess(currentKey.isNotBlank())
        if (currentKey.isNotBlank()) {
            refreshModels()
        }

        val currentFullAccess = sp.getBoolean("ai_full_access", true)
        val switchFullAccess = com.google.android.material.materialswitch.MaterialSwitch(this).apply {
            text = "Полный доступ (работа с приложением и действия)"
            isChecked = currentFullAccess
            setTextColor(ContextCompat.getColor(this@AiChatActivity, R.color.prime_text_primary))
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, 32, 0, 10)
        }

        layout.addView(tvLabelKey)
        layout.addView(etKey)
        layout.addView(tvLabelProvider)
        layout.addView(spinnerProvider)
        layout.addView(tvLabelModel)
        layout.addView(spinnerModel)
        layout.addView(tvLabelPrompt)
        layout.addView(etPrompt)
        layout.addView(switchFullAccess)

        PrimeBlurDialog.show(
            activity = this,
            title = "Настройка AI и Live-модели из API",
            message = "Список доступных моделей запрашивается напрямую с серверов провайдера по вашему ключу.",
            positiveText = "Сохранить",
            negativeText = "Отмена",
            iconRes = R.drawable.ic_prime_ai,
            customView = layout,
            onPositive = {
                val newKey = etKey.text.toString().trim()
                if (newKey.isBlank()) {
                    etKey.background = ContextCompat.getDrawable(this@AiChatActivity, R.drawable.bg_input_error)
                    PrimeNotification.show(this@AiChatActivity, "Сначала укажите API ключ!")
                    return@show
                }

                val selectedProviderIndex = spinnerProvider.selectedItemPosition
                val manualProvider = when (selectedProviderIndex) {
                    1 -> "Gemini"
                    2 -> "OpenAI"
                    3 -> "Anthropic"
                    4 -> "OpenRouter"
                    5 -> "DeepSeek"
                    else -> ""
                }
                val newModel = spinnerModel.selectedItem?.toString() ?: currentModel
                val newPrompt = etPrompt.text.toString().trim()
                val fullAccess = switchFullAccess.isChecked

                val detected = AiModelDetector.detect(newKey, manualProvider, newModel)

                sp.edit()
                    .putString("ai_provider", detected.provider)
                    .putString("ai_model", newModel)
                    .putString("ai_api_key", newKey)
                    .putString("ai_custom_url", detected.endpointUrl)
                    .putString("ai_system_prompt", newPrompt)
                    .putBoolean("ai_full_access", fullAccess)
                    .apply()

                currentModelConfig = detected.copy(model = newModel)
                updateHeaderAndStatus("В сети")
                PrimeNotification.show(this, "Модель: $newModel (${detected.provider}) сохранена")
            }
        )
    }

    fun handleButtonAction(action: String) {
        when {
            action.startsWith("http://") || action.startsWith("https://") -> {
                com.messenger.prime.browser.AppBrowserManager.openUrl(this, action)
            }
            action.startsWith("prompt:") -> {
                val promptText = action.removePrefix("prompt:").trim()
                binding.etAiMessageInput.setText(promptText)
                binding.etAiMessageInput.setSelection(promptText.length)
            }
            action.startsWith("run_plugin:") -> {
                val pluginName = action.removePrefix("run_plugin:").trim()
                val res = PluginManager.runPlugin(this, pluginName)
                PrimeNotification.show(this, res)
            }
            action == "action:open_settings" -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            action == "action:open_search" -> {
                startActivity(Intent(this, GlobalSearchActivity::class.java))
            }
            action == "action:open_media_player" -> {
                startActivity(Intent(this, MediaPlayerActivity::class.java))
            }
            action == "action:open_rich_editor" -> {
                startActivity(Intent(this, RichEditorActivity::class.java))
            }
            action == "action:open_overlay" -> {
                PrimeOverlayService.toggleOverlayService(this)
            }
            action.startsWith("action:open_url:") -> {
                val url = action.removePrefix("action:open_url:").trim()
                com.messenger.prime.browser.AppBrowserManager.openUrl(this, url)
            }
            action.startsWith("action:delete_chat:") -> {
                val address = action.removePrefix("action:delete_chat:").trim()
                ChatHistoryManager.deleteHistoryCompletely(this, address, ChatHistoryManager.getDisplayNameForAddress(this, address))
                PrimeNotification.show(this, "Чат $address удален")
            }
            action.startsWith("action:change_theme:") -> {
                val mode = action.removePrefix("action:change_theme:").trim().lowercase()
                val newTheme = if (mode == "dark") "dark" else "light"
                val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
                sharedPrefs.edit().putString("app_theme", newTheme).apply()

                val nightMode = if (newTheme == "dark") androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode)
                sendBroadcast(Intent("com.messenger.prime.THEME_CHANGED").setPackage(packageName))
            }
            action.startsWith("action:create_chat:") -> {
                val topic = action.removePrefix("action:create_chat:").trim()
                val newSession = AiChatStorage.createNewSession(this, currentModelConfig.model)
                newSession.title = topic
                switchSession(newSession)
            }
            action == "action:open_wallpaper_editor" -> {
                val intent = Intent(this, PhotoEditorActivity::class.java).apply {
                    putExtra("IS_PROFILE_PHOTO", false)
                }
                startActivity(intent)
            }
            action == "action:open_profile" -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            action.startsWith("action:open_chat:") -> {
                val target = action.removePrefix("action:open_chat:").trim()
                val intent = Intent(this, ChatPersonActivity::class.java).apply {
                    putExtra("EXTRA_ADDRESS", target)
                    putExtra("EXTRA_NAME", ChatHistoryManager.getDisplayNameForAddress(this@AiChatActivity, target))
                }
                startActivity(intent)
            }
            action.startsWith("action:copy:") -> {
                val copyText = action.removePrefix("action:copy:").trim()
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Prime AI Copy", copyText)
                clipboard?.setPrimaryClip(clip)
                PrimeNotification.show(this, "Текст скопирован в буфер")
            }
            action.startsWith("action:vibrate:") -> {
                val ms = action.removePrefix("action:vibrate:").trim().toLongOrNull() ?: 100L
                try {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(ms)
                } catch (_: Exception) {}
            }
            action.startsWith("action:toast:") -> {
                val msg = action.removePrefix("action:toast:").trim()
                PrimeNotification.show(this, msg)
            }
            action.startsWith("action:notify:") -> {
                val msg = action.removePrefix("action:notify:").trim()
                PrimeNotification.show(this, msg)
            }
            action == "action:clear_history" -> {
                activeSession?.messages?.clear()
                messages.clear()
                activeSession?.let { AiChatStorage.saveSession(this, it) }
                adapter.notifyDataSetChanged()
                PrimeNotification.show(this, "История чата очищена")
            }
            action.startsWith("action:set_accent_color:") -> {
                val hex = action.removePrefix("action:set_accent_color:").trim()
                try {
                    val colorInt = Color.parseColor(hex)
                    ColorAccentManager.setAccentCustom(this, colorInt)
                    PrimeNotification.show(this, "Цвет акцента изменен на $hex")
                } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(wallpaperReceiver) } catch (_: Exception) {}
    }
}

class AiMessagesAdapter(private val items: List<AiMessage>) : RecyclerView.Adapter<AiMessagesAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(android.R.id.text1)
        val tvTime: TextView = view.findViewById(android.R.id.text2)
    }

    private fun resolveButtonIconRes(iconName: String): Int? {
        val clean = iconName.lowercase().trim()
        return when {
            clean.contains("setting") || clean.contains("gear") -> R.drawable.ic_settings
            clean.contains("search") || clean.contains("find") -> R.drawable.ic_search
            clean.contains("music") || clean.contains("player") || clean.contains("audio") -> R.drawable.ic_music
            clean.contains("photo") || clean.contains("wallpaper") || clean.contains("image") -> R.drawable.ic_photo
            clean.contains("file") || clean.contains("plugin") || clean.contains("code") -> R.drawable.ic_file
            clean.contains("add") || clean.contains("plus") -> R.drawable.ic_add
            clean.contains("done") || clean.contains("check") -> R.drawable.ic_done
            clean.contains("trash") || clean.contains("delete") -> R.drawable.ic_trash
            clean.contains("globe") || clean.contains("browser") || clean.contains("web") -> R.drawable.ic_globe
            clean.contains("pipette") || clean.contains("color") -> R.drawable.ic_pipette
            clean.contains("prime") || clean.contains("ai") -> R.drawable.ic_prime_ai
            else -> null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val scale = context.resources.displayMetrics.density
        val isUser = viewType == 1

        val container = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            setPadding((12 * scale).toInt(), (4 * scale).toInt(), (12 * scale).toInt(), (4 * scale).toInt())
            gravity = if (isUser) android.view.Gravity.END else android.view.Gravity.START

            val bubble = LinearLayout(context).apply {
                tag = "ai_bubble"
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                setPadding((16 * scale).toInt(), (12 * scale).toInt(), (16 * scale).toInt(), (12 * scale).toInt())
                elevation = 2f * scale // Material Design elevation
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                background = if (isUser) {
                    ColorAccentManager.createOutgoingBubbleDrawable(context)
                } else {
                    ColorAccentManager.createIncomingBubbleDrawable(context)
                }

                val text = TextView(context).apply {
                    id = android.R.id.text1
                    setTextColor(ContextCompat.getColor(context, if (isUser) R.color.white else R.color.prime_text_primary))
                    textSize = 15.5f // slightly larger for readability
                    setLineSpacing(2f * scale, 1.1f) // Better line spacing
                    setTextIsSelectable(true)
                }

                val imagesContainer = LinearLayout(context).apply {
                    tag = "ai_images_container"
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, (6 * scale).toInt(), 0, (6 * scale).toInt())
                    }
                    orientation = LinearLayout.VERTICAL
                    visibility = View.GONE
                }

                val buttonsContainer = LinearLayout(context).apply {
                    tag = "ai_buttons_container"
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, (6 * scale).toInt(), 0, (2 * scale).toInt())
                    }
                    orientation = LinearLayout.VERTICAL
                    visibility = View.GONE
                }

                val time = TextView(context).apply {
                    id = android.R.id.text2
                    setTextColor(ContextCompat.getColor(context, if (isUser) R.color.white else R.color.prime_text_secondary))
                    textSize = 11f
                    alpha = 0.8f
                    gravity = android.view.Gravity.END
                    setPadding(0, (6 * scale).toInt(), 0, 0)
                }

                addView(text)
                addView(imagesContainer)
                addView(buttonsContainer)
                addView(time)
            }
            addView(bubble)
        }
        return ViewHolder(container)
    }

    override fun getItemViewType(position: Int) = if (items[position].isUser) 1 else 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = items[position]
        val context = holder.itemView.context

        // 1. Process Images in Message
        val imagesContainer = holder.itemView.findViewWithTag<LinearLayout>("ai_images_container")
        val imageUrls = RichAiMessageParser.extractImageUrls(msg.text)

        if (imageUrls.isNotEmpty() && !msg.isUser && imagesContainer != null) {
            imagesContainer.removeAllViews()
            imagesContainer.visibility = View.VISIBLE

            val scale = context.resources.displayMetrics.density
            for (url in imageUrls) {
                val card = com.google.android.material.card.MaterialCardView(context).apply {
                    radius = 16 * scale
                    cardElevation = 2 * scale
                    strokeWidth = (1 * scale).toInt()
                    strokeColor = Color.parseColor("#33FFFFFF")
                    setCardBackgroundColor(Color.parseColor("#1A000000"))

                    val iv = ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            (220 * scale).toInt()
                        )
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setPadding((6 * scale).toInt(), (6 * scale).toInt(), (6 * scale).toInt(), (6 * scale).toInt())
                    }

                    com.bumptech.glide.Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.ic_format_image)
                        .error(R.drawable.ic_error)
                        .into(iv)

                    iv.setOnClickListener {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }

                    addView(iv)

                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, (4 * scale).toInt(), 0, (6 * scale).toInt())
                    }
                }
                imagesContainer.addView(card)
            }
        } else {
            imagesContainer?.visibility = View.GONE
        }

        // 2. Process Interactive Buttons in Message
        val buttonsContainer = holder.itemView.findViewWithTag<LinearLayout>("ai_buttons_container")
        val buttonRegex = Regex("```button:(.*?)\\|(.*?)(?:\\|(.*?))?```")
        val matches = buttonRegex.findAll(msg.text).toList()

        if (matches.isNotEmpty() && !msg.isUser) {
            val cleanText = buttonRegex.replace(msg.text, "").trim()
            RichAiMessageParser.formatAndApply(holder.tvText, cleanText)

            if (buttonsContainer != null) {
                buttonsContainer.removeAllViews()
                buttonsContainer.visibility = View.VISIBLE

                for (match in matches) {
                    val btnLabel = match.groupValues[1].trim()
                    val btnAction = match.groupValues[2].trim()
                    val btnIcon = if (match.groupValues.size >= 4) match.groupValues[3].trim() else ""

                    val iconRes = if (btnIcon.isNotBlank()) resolveButtonIconRes(btnIcon) else null

                    val btn = com.google.android.material.button.MaterialButton(context).apply {
                        text = btnLabel
                        textSize = 13.5f
                        setTextColor(Color.WHITE)
                        isAllCaps = false
                        letterSpacing = 0.01f
                        cornerRadius = (20 * context.resources.displayMetrics.density).toInt()
                        backgroundTintList = ColorStateList.valueOf(ColorAccentManager.getCurrentAccentColor(context))
                        insetTop = 0
                        insetBottom = 0

                        if (iconRes != null) {
                            setIconResource(iconRes)
                            iconTint = ColorStateList.valueOf(Color.WHITE)
                            iconSize = (18 * context.resources.displayMetrics.density).toInt()
                            iconPadding = (6 * context.resources.displayMetrics.density).toInt()
                        } else if (btnIcon.startsWith("http://") || btnIcon.startsWith("https://")) {
                            com.bumptech.glide.Glide.with(context)
                                .asDrawable()
                                .load(btnIcon)
                                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?) {
                                        icon = resource
                                        iconTint = null
                                        iconSize = (18 * context.resources.displayMetrics.density).toInt()
                                    }
                                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                                })
                        }

                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, (4 * context.resources.displayMetrics.density).toInt(), 0, (4 * context.resources.displayMetrics.density).toInt())
                        }
                        setOnClickListener {
                            (context as? AiChatActivity)?.handleButtonAction(btnAction)
                        }
                    }
                    buttonsContainer.addView(btn)
                }
            }
        } else {
            buttonsContainer?.visibility = View.GONE
            if (msg.isUser) {
                holder.tvText.text = msg.text
            } else {
                RichAiMessageParser.formatAndApply(holder.tvText, msg.text)
            }
        }

        holder.tvTime.text = msg.time

        val bubble = holder.itemView.findViewWithTag<View>("ai_bubble")
        if (bubble != null) {
            bubble.background = if (msg.isUser) {
                ColorAccentManager.createOutgoingBubbleDrawable(context)
            } else {
                ColorAccentManager.createIncomingBubbleDrawable(context)
            }
        }
    }

    override fun getItemCount() = items.size
}

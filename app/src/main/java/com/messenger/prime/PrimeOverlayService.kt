package com.messenger.prime

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrimeOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null
    private var floatingCardView: View? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var cardParams: WindowManager.LayoutParams? = null

    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (checkOverlayPermission(this)) {
            setupFloatingBubble()
            setupFloatingCard()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!checkOverlayPermission(this)) {
            requestOverlayPermission(this)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingBubble() {
        val density = resources.displayMetrics.density
        val sizePx = (52 * density).toInt()

        val bubble = ImageView(this).apply {
            setImageResource(R.drawable.ic_prime_ai)
            background = ContextCompat.getDrawable(this@PrimeOverlayService, R.drawable.bg_circular_mode_active)
            backgroundTintList = android.content.res.ColorStateList.valueOf(ColorAccentManager.getCurrentAccentColor(this@PrimeOverlayService))
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * scaleDensity(this@PrimeOverlayService)).toInt(), (12 * scaleDensity(this@PrimeOverlayService)).toInt())
            elevation = 16f
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (20 * density).toInt()
            y = (200 * density).toInt()
        }

        bubbleParams = params
        floatingBubbleView = bubble

        var dX = 0f
        var dY = 0f
        var initialX = 0f
        var initialY = 0f

        bubble.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    initialY = event.rawY
                    dX = params.x - event.rawX
                    dY = params.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (event.rawX + dX).toInt()
                    params.y = (event.rawY + dY).toInt()
                    try { windowManager?.updateViewLayout(v, params) } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dist = Math.hypot((event.rawX - initialX).toDouble(), (event.rawY - initialY).toDouble())
                    if (dist < 15) {
                        toggleExpanded()
                    } else {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val targetX = if (params.x + sizePx / 2 < screenWidth / 2) (12 * density).toInt() else screenWidth - sizePx - (12 * density).toInt()
                        params.x = targetX
                        try { windowManager?.updateViewLayout(v, params) } catch (_: Exception) {}
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(bubble, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scaleDensity(context: Context): Float = context.resources.displayMetrics.density

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingCard() {
        val density = resources.displayMetrics.density
        val cardWidth = (310 * density).toInt()
        val cardHeight = (420 * density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            cardWidth,
            cardHeight,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        cardParams = params

        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@PrimeOverlayService, R.drawable.bg_blur_glass_panel)
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
            elevation = 20f
        }

        // Header bar
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (8 * density).toInt())

            val icon = ImageView(this@PrimeOverlayService).apply {
                layoutParams = LinearLayout.LayoutParams((22 * density).toInt(), (22 * density).toInt())
                setImageResource(R.drawable.ic_prime_ai)
            }

            val title = TextView(this@PrimeOverlayService).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins((8 * density).toInt(), 0, (8 * density).toInt(), 0)
                }
                text = "AI Prime (Поверх окон)"
                setTextColor(ContextCompat.getColor(this@PrimeOverlayService, R.color.prime_text_primary))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val btnFull = ImageView(this@PrimeOverlayService).apply {
                layoutParams = LinearLayout.LayoutParams((28 * density).toInt(), (28 * density).toInt())
                setImageResource(R.drawable.ic_fullscreen)
                setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
                setOnClickListener {
                    val intent = Intent(this@PrimeOverlayService, AiChatActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    toggleExpanded()
                }
            }

            val btnClose = ImageView(this@PrimeOverlayService).apply {
                layoutParams = LinearLayout.LayoutParams((28 * density).toInt(), (28 * density).toInt())
                setImageResource(R.drawable.ic_close)
                setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
                setOnClickListener { toggleExpanded() }
            }

            addView(icon)
            addView(title)
            addView(btnFull)
            addView(btnClose)
        }

        // Output Text Area
        val tvOutput = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            text = "AI Помощник поверх всех окон.\nВведите запрос внизу!"
            setTextColor(ContextCompat.getColor(this@PrimeOverlayService, R.color.prime_text_primary))
            textSize = 13f
            background = ContextCompat.getDrawable(this@PrimeOverlayService, R.drawable.bg_date_bubble)
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
        }

        // Input Row
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (8 * density).toInt(), 0, 0)

            val etInput = EditText(this@PrimeOverlayService).apply {
                layoutParams = LinearLayout.LayoutParams(0, (42 * density).toInt(), 1f)
                hint = "Спросите у AI..."
                background = ContextCompat.getDrawable(this@PrimeOverlayService, R.drawable.bg_date_bubble)
                setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@PrimeOverlayService, R.color.prime_text_primary))
                setSingleLine()
            }

            val btnSend = ImageView(this@PrimeOverlayService).apply {
                layoutParams = LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt()).apply {
                    setMargins((6 * density).toInt(), 0, 0, 0)
                }
                setImageResource(R.drawable.ic_send)
                background = ContextCompat.getDrawable(this@PrimeOverlayService, R.drawable.bg_circular_mode_active)
                backgroundTintList = android.content.res.ColorStateList.valueOf(ColorAccentManager.getCurrentAccentColor(this@PrimeOverlayService))
                setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())

                setOnClickListener {
                    val prompt = etInput.text.toString().trim()
                    if (prompt.isNotBlank()) {
                        etInput.setText("")
                        tvOutput.text = "Печатает..."

                        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
                        val apiKey = sp.getString("ai_api_key", "") ?: ""
                        val savedProvider = sp.getString("ai_provider", "") ?: ""
                        val savedModel = sp.getString("ai_model", "") ?: ""
                        val customUrl = sp.getString("ai_custom_url", "") ?: ""

                        val config = AiModelDetector.detect(apiKey, savedProvider, savedModel, customUrl)
                        if (apiKey.isEmpty()) {
                            tvOutput.text = "API ключ не настроен. Нажмите иконку 'Развернуть', чтобы настроить."
                            return@setOnClickListener
                        }

                        var sysPrompt = sp.getString("ai_system_prompt", "Вы полезный AI ассистент мессенджера Prime.") ?: ""
                        if (sp.getBoolean("ai_full_access", true)) {
                            sysPrompt += "\n[Привилегия: Полный доступ включен в оверлейном окне поверх всех приложений.]"
                        }

                        callAiApiOverlay(prompt, apiKey, sysPrompt, config) { res ->
                            RichAiMessageParser.formatAndApply(tvOutput, res)
                        }
                    }
                }
            }

            addView(etInput)
            addView(btnSend)
        }

        cardLayout.addView(header)
        cardLayout.addView(tvOutput)
        cardLayout.addView(inputRow)

        floatingCardView = cardLayout
        cardLayout.visibility = View.GONE
    }

    private fun callAiApiOverlay(prompt: String, apiKey: String, systemPrompt: String, config: AiModelConfig, callback: (String) -> Unit) {
        Thread {
            try {
                val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent?key=$apiKey"
                val url = java.net.URL(urlStr)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.doOutput = true

                val json = org.json.JSONObject().apply {
                    put("contents", org.json.JSONArray().put(org.json.JSONObject().put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", prompt)))))
                    if (systemPrompt.isNotEmpty()) {
                        put("systemInstruction", org.json.JSONObject().put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", systemPrompt))))
                    }
                }
                conn.outputStream.use { os -> os.write(json.toString().toByteArray(Charsets.UTF_8)) }

                val text = if (conn.responseCode in 200..299) {
                    val res = conn.inputStream.bufferedReader().use { it.readText() }
                    org.json.JSONObject(res).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
                } else {
                    "Ошибка API (${conn.responseCode})"
                }
                Handler(Looper.getMainLooper()).post { callback(text) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback("Ошибка: ${e.localizedMessage}") }
            }
        }.start()
    }

    private fun toggleExpanded() {
        val card = floatingCardView ?: return
        isExpanded = !isExpanded

        if (isExpanded) {
            try {
                if (card.parent == null) {
                    windowManager?.addView(card, cardParams)
                }
                card.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                card.visibility = View.GONE
                if (card.parent != null) {
                    windowManager?.removeView(card)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            floatingBubbleView?.let { windowManager?.removeView(it) }
            floatingCardView?.let { if (it.parent != null) windowManager?.removeView(it) }
        } catch (_: Exception) {}
    }

    companion object {
        fun checkOverlayPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        }

        fun requestOverlayPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }

        fun toggleOverlayService(context: Context) {
            if (!checkOverlayPermission(context)) {
                requestOverlayPermission(context)
                val act = context as? android.app.Activity
                if (act != null) {
                    PrimeNotification.show(act, "Разрешите наложение поверх других окон")
                } else {
                    android.widget.Toast.makeText(context, "Разрешите наложение поверх других окон", android.widget.Toast.LENGTH_SHORT).show()
                }
                return
            }
            val intent = Intent(context, PrimeOverlayService::class.java)
            context.startService(intent)
            val act = context as? android.app.Activity
            if (act != null) {
                PrimeNotification.show(act, "AI Помощник запущен поверх окон")
            } else {
                android.widget.Toast.makeText(context, "AI Помощник запущен поверх окон", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

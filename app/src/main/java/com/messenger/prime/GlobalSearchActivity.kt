package com.messenger.prime

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import eightbitlab.com.blurview.BlurView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale

class GlobalSearchActivity : AppCompatActivity() {

    private lateinit var rvResults: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var etQuery: EditText
    private lateinit var btnClear: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var blurHeader: BlurView
    private lateinit var rootLayout: View
    private lateinit var adapter: SearchResultAdapter

    data class SearchResultItem(
        val title: String,
        val snippet: String,
        val time: String,
        val targetUsername: String,
        val deviceAddress: String?,
        val avatarUri: String?,
        val matchedMessageId: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)

        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        setupEdgeToEdge(isDark)

        setContentView(R.layout.activity_global_search)

        rootLayout = findViewById(R.id.globalSearchRoot)
        blurHeader = findViewById(R.id.blurSearchHeader)
        rvResults = findViewById(R.id.rvGlobalSearchResults)
        tvEmptyState = findViewById(R.id.tvSearchEmptyState)
        etQuery = findViewById(R.id.etGlobalSearchQuery)
        btnClear = findViewById(R.id.btnSearchClear)
        btnBack = findViewById(R.id.btnSearchBack)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val density = resources.displayMetrics.density

            rootLayout.setPadding(cutout.left, 0, cutout.right, 0)

            blurHeader.setPadding(
                blurHeader.paddingLeft,
                maxOf(systemBars.top, cutout.top) + (8 * density).toInt(),
                blurHeader.paddingRight,
                (8 * density).toInt()
            )

            val navView = findViewById<View>(R.id.blurBottomNav)
            if (navView != null) {
                val lp = navView.layoutParams as? android.view.ViewGroup.MarginLayoutParams
                if (lp != null) {
                    val targetMargin = maxOf(systemBars.bottom, cutout.bottom) + (12 * density).toInt()
                    if (lp.bottomMargin != targetMargin) {
                        lp.bottomMargin = targetMargin
                        navView.layoutParams = lp
                    }
                }
            }

            val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
            val navStyle = sharedPrefs.getString("navigation_style", "island") ?: "island"
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600
            val isEmbedded = try {
                androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)
            } catch (_: Exception) {
                false
            }

            val bottomPadding = if (navStyle == "bottom_bar" && !isTablet && !isEmbedded) systemBars.bottom + (88 * density).toInt() else maxOf(systemBars.bottom, cutout.bottom) + (24 * density).toInt()

            rvResults.setPadding(
                rvResults.paddingLeft,
                rvResults.paddingTop,
                rvResults.paddingRight,
                bottomPadding
            )

            insets
        }

        setupBottomNav()

        adapter = SearchResultAdapter { item ->
            openChat(item)
        }
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = adapter

        val backAction = {
            val intent = Intent(this, ChatListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
            PrimeTransitions.applyCloseTransition(this)
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backAction()
            }
        })

        btnBack.setOnClickListener {
            backAction()
        }

        btnClear.setOnClickListener {
            etQuery.setText("")
        }

        etQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                btnClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                performSearch(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etQuery.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        ColorAccentManager.tintViewTree(rootLayout, ColorAccentManager.getCurrentAccentColor(this))
        setupBottomNav()
    }

    private fun updateM3TabState(
        isActive: Boolean,
        indicatorView: View?,
        iconView: ImageView?,
        labelView: TextView?,
        accentColor: Int,
        secondaryColor: Int
    ) {
        if (indicatorView == null || labelView == null) return
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val visibleInactiveColor = if (isDark) android.graphics.Color.parseColor("#E6FFFFFF") else android.graphics.Color.parseColor("#E6154B87")

        indicatorView.visibility = View.VISIBLE

        if (isActive) {
            indicatorView.backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
            iconView?.setColorFilter(android.graphics.Color.WHITE)
            labelView.setTextColor(accentColor)
            labelView.setTypeface(null, android.graphics.Typeface.BOLD)
            labelView.alpha = 1.0f
        } else {
            indicatorView.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            iconView?.setColorFilter(visibleInactiveColor)
            labelView.setTextColor(visibleInactiveColor)
            labelView.setTypeface(null, android.graphics.Typeface.NORMAL)
            labelView.alpha = 1.0f
        }
    }

    private fun setupBottomNav() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val navStyle = sharedPrefs.getString("navigation_style", "island") ?: "island"
        val navView = findViewById<View>(R.id.blurBottomNav) ?: return

        val density = resources.displayMetrics.density

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600
        val isEmbedded = try {
            androidx.window.embedding.ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)
        } catch (_: Exception) {
            false
        }

        if (navStyle != "bottom_bar" || isTablet || isEmbedded) {
            navView.visibility = View.GONE
            rvResults.setPadding(
                rvResults.paddingLeft,
                rvResults.paddingTop,
                rvResults.paddingRight,
                (24 * density).toInt()
            )
            return
        }

        navView.visibility = View.VISIBLE
        navView.bringToFront()

        val insets = ViewCompat.getRootWindowInsets(window.decorView)
        val systemBarsBottom = insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0

        val lp = navView.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        if (lp != null) {
            val targetMargin = systemBarsBottom + (12 * density).toInt()
            if (lp.bottomMargin != targetMargin) {
                lp.bottomMargin = targetMargin
                navView.layoutParams = lp
            }
        }

        rvResults.setPadding(
            rvResults.paddingLeft,
            rvResults.paddingTop,
            rvResults.paddingRight,
            systemBarsBottom + (88 * density).toInt()
        )

        val btnNavChats = navView.findViewById<View>(R.id.btnNavChats)
        val btnNavDevices = navView.findViewById<View>(R.id.btnNavDevices)
        val btnNavSearch = navView.findViewById<View>(R.id.btnNavSearch)
        val btnNavProfile = navView.findViewById<View>(R.id.btnNavProfile)

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val secondaryColor = ContextCompat.getColor(this, R.color.prime_text_secondary)

        val vNavChatsIndicator = navView.findViewById<View>(R.id.vNavChatsIndicator)
        val vNavDevicesIndicator = navView.findViewById<View>(R.id.vNavDevicesIndicator)
        val vNavSearchIndicator = navView.findViewById<View>(R.id.vNavSearchIndicator)
        val vNavProfileIndicator = navView.findViewById<View>(R.id.vNavProfileIndicator)

        val ivNavChatsIcon = navView.findViewById<ImageView>(R.id.ivNavChatsIcon)
        val ivNavDevicesIcon = navView.findViewById<ImageView>(R.id.ivNavDevicesIcon)
        val ivNavSearchIcon = navView.findViewById<ImageView>(R.id.ivNavSearchIcon)

        val tvNavChatsLabel = navView.findViewById<TextView>(R.id.tvNavChatsLabel)
        val tvNavDevicesLabel = navView.findViewById<TextView>(R.id.tvNavDevicesLabel)
        val tvNavSearchLabel = navView.findViewById<TextView>(R.id.tvNavSearchLabel)
        val tvNavProfileLabel = navView.findViewById<TextView>(R.id.tvNavProfileLabel)

        updateM3TabState(false, vNavChatsIndicator, ivNavChatsIcon, tvNavChatsLabel, accentColor, secondaryColor)
        updateM3TabState(false, vNavDevicesIndicator, ivNavDevicesIcon, tvNavDevicesLabel, accentColor, secondaryColor)
        updateM3TabState(true, vNavSearchIndicator, ivNavSearchIcon, tvNavSearchLabel, accentColor, secondaryColor)
        updateM3TabState(false, vNavProfileIndicator, null, tvNavProfileLabel, accentColor, secondaryColor)

        val profileAvatarIv = navView.findViewById<ImageView>(R.id.ivNavProfileAvatar)
        if (profileAvatarIv != null) {
            val myAvatar = sharedPrefs.getString("my_avatar", null)
                ?: sharedPrefs.getString("my_local_avatar", null)
            if (!myAvatar.isNullOrEmpty()) {
                val (_, file) = parseAvatarModelAndFile(myAvatar)
                if (file != null && file.exists()) {
                    Glide.with(this).load(file).into(profileAvatarIv)
                } else {
                    Glide.with(this).load(myAvatar).into(profileAvatarIv)
                }
            }
        }

        val blurNavView = navView as? eightbitlab.com.blurview.BlurView
        if (blurNavView != null) {
            val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val overlayColor = if (isDark) android.graphics.Color.parseColor("#700F172A") else android.graphics.Color.parseColor("#70154B87")
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: window.decorView as ViewGroup
            blurNavView.setupBlur(rootView, 22f, overlayColor, window.decorView.background)
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

        btnNavProfile?.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = true)
            finish()
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            adapter.setItems(emptyList(), "")
            tvEmptyState.text = "Введите запрос для поиска..."
            tvEmptyState.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val results = mutableListOf<SearchResultItem>()
            val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
            val json = sharedPrefs.getString("persisted_chats", "[]") ?: "[]"

            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val targetUser = obj.optString("name", "")
                    val devAddr = obj.optString("id", "")
                    val lastMsg = obj.optString("lastMessage", "")
                    val timeStr = obj.optString("time", "")

                    if (targetUser.isEmpty()) continue

                    val contactName = sharedPrefs.getString("contact_name_$devAddr", null)
                        ?: sharedPrefs.getString("${devAddr}_name", null)
                        ?: targetUser
                    val displayContactName = if (BluetoothAdapter.checkBluetoothAddress(contactName)) "Собеседник" else contactName

                    val avatarUri = sharedPrefs.getString("contact_avatar_$devAddr", null)
                        ?: sharedPrefs.getString("${devAddr}_avatar", null)
                        ?: sharedPrefs.getString("contact_avatar_$targetUser", null)

                    val lowerQuery = query.lowercase(Locale.ROOT)

                    if (displayContactName.lowercase(Locale.ROOT).contains(lowerQuery) || targetUser.lowercase(Locale.ROOT).contains(lowerQuery)) {
                        results.add(
                            SearchResultItem(
                                title = displayContactName,
                                snippet = "Чат с пользователем • $lastMsg",
                                time = timeStr,
                                targetUsername = targetUser,
                                deviceAddress = devAddr,
                                avatarUri = avatarUri
                            )
                        )
                    }

                    // Search messages inside history
                    val history = ChatHistoryManager.loadMessages(this@GlobalSearchActivity, targetUser)
                    for (msg in history) {
                        val text = msg.text ?: ""
                        if (text.lowercase(Locale.ROOT).contains(lowerQuery)) {
                            results.add(
                                SearchResultItem(
                                    title = displayContactName,
                                    snippet = text,
                                    time = msg.time ?: timeStr,
                                    targetUsername = targetUser,
                                    deviceAddress = devAddr,
                                    avatarUri = avatarUri,
                                    matchedMessageId = msg.messageId
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    adapter.setItems(results, query)
                    tvEmptyState.text = "Ничего не найдено"
                    tvEmptyState.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun openChat(item: SearchResultItem) {
        val config = resources.configuration
        val isFoldableOrTablet = config.smallestScreenWidthDp >= 600
        if (isFoldableOrTablet) {
            val intent = Intent(this, ChatListActivity::class.java).apply {
                putExtra("EXTRA_CHAT_NAME", item.targetUsername)
                if (!item.deviceAddress.isNullOrEmpty()) {
                    putExtra("EXTRA_DEVICE_ADDRESS", item.deviceAddress)
                }
                if (!item.matchedMessageId.isNullOrEmpty()) {
                    putExtra("EXTRA_TARGET_MESSAGE_ID", item.matchedMessageId)
                }
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, ChatPersonActivity::class.java).apply {
                putExtra("EXTRA_CHAT_NAME", item.targetUsername)
                if (!item.deviceAddress.isNullOrEmpty()) {
                    putExtra("EXTRA_DEVICE_ADDRESS", item.deviceAddress)
                }
                if (!item.avatarUri.isNullOrEmpty()) {
                    putExtra("EXTRA_AVATAR_URI", item.avatarUri)
                }
                if (!item.matchedMessageId.isNullOrEmpty()) {
                    putExtra("EXTRA_TARGET_MESSAGE_ID", item.matchedMessageId)
                }
            }
            startActivity(intent)
            PrimeTransitions.applyOpenTransition(this)
        }
    }

    private class SearchResultAdapter(private val onClick: (SearchResultItem) -> Unit) :
        RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

        private var items = listOf<SearchResultItem>()
        private var currentQuery = ""

        fun setItems(newItems: List<SearchResultItem>, query: String) {
            this.items = newItems
            this.currentQuery = query
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_global_search_result, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvTime.text = item.time

            val accentColor = ColorAccentManager.getCurrentAccentColor(holder.itemView.context)

            // Highlight search query snippet
            if (currentQuery.isNotEmpty() && item.snippet.contains(currentQuery, ignoreCase = true)) {
                val spannable = SpannableString(item.snippet)
                val startIdx = item.snippet.lowercase(Locale.ROOT).indexOf(currentQuery.lowercase(Locale.ROOT))
                if (startIdx >= 0) {
                    val endIdx = startIdx + currentQuery.length
                    spannable.setSpan(
                        ForegroundColorSpan(accentColor),
                        startIdx,
                        endIdx,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                holder.tvSnippet.text = spannable
            } else {
                holder.tvSnippet.text = item.snippet
            }

            if (!item.avatarUri.isNullOrEmpty()) {
                val (_, file) = parseAvatarModelAndFile(item.avatarUri)
                if (file != null && file.exists()) {
                    Glide.with(holder.itemView.context).load(file).into(holder.ivAvatar)
                } else {
                    Glide.with(holder.itemView.context).load(item.avatarUri).into(holder.ivAvatar)
                }
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_person)
            }

            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivAvatar: ImageView = itemView.findViewById(R.id.ivResultAvatar)
            val tvTitle: TextView = itemView.findViewById(R.id.tvResultTitle)
            val tvTime: TextView = itemView.findViewById(R.id.tvResultTime)
            val tvSnippet: TextView = itemView.findViewById(R.id.tvResultSnippet)
        }
    }
}

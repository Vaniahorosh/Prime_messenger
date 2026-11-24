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
import android.view.inputmethod.InputMethodManager
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
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val density = resources.displayMetrics.density

            rootLayout.setPadding(cutout.left, 0, cutout.right, 0)

            blurHeader.setPadding(
                blurHeader.paddingLeft,
                maxOf(systemBars.top, cutout.top) + (8 * density).toInt(),
                blurHeader.paddingRight,
                (8 * density).toInt()
            )

            val navView = findViewById<View>(R.id.blurBottomNav)
            val navEnabled = PrimeBottomNavHelper.isBottomBarEnabled(this, hideOnTabletOrEmbedded = true)
            PrimeBottomNavHelper.updateForIme(navView, isImeVisible, navEnabled)

            val safeBottom = maxOf(systemBars.bottom, cutout.bottom)
            if (navView != null && navEnabled && !isImeVisible) {
                PrimeBottomNavHelper.applyBottomMargin(navView, safeBottom)
            }

            val bottomPadding = if (navEnabled && !isImeVisible) {
                safeBottom + (88 * density).toInt()
            } else {
                maxOf(safeBottom, imeInsets.bottom) + (24 * density).toInt()
            }

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
            if (isTaskRoot) {
                val intent = Intent(this, ChatListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
            finish()
            PrimeTransitions.applyCloseTransition(this)
        }
        // PrimePredictiveBack is now attached centrally in PrimeApplication

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

    private fun setupBottomNav() {
        val blurTarget = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        PrimeBottomNavHelper.setup(
            activity = this,
            activeTab = PrimeBottomNavHelper.NavTab.SEARCH,
            blurTargetRoot = blurTarget,
            hideOnTabletOrEmbedded = true,
            onSameTabReselect = {
                etQuery.text?.clear()
                etQuery.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                @Suppress("DEPRECATION")
                imm?.showSoftInput(etQuery, InputMethodManager.SHOW_IMPLICIT)
            }
        )
        ViewCompat.requestApplyInsets(rootLayout)
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
            val json = ChatHistoryManager.getPersistedChatsJson(this@GlobalSearchActivity)

            try {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val targetUser = obj.optString("name", "")
                    val devAddr = obj.optString("id", "")
                    val lastMsg = obj.optString("lastMessage", "")
                    val timeStr = obj.optString("time", "")

                    val effectiveUser = if (targetUser.isNotEmpty()) targetUser else devAddr
                    if (effectiveUser.isEmpty()) continue

                    val isForgotten = (targetUser.isNotEmpty() && ChatHistoryManager.isDeviceForgotten(this@GlobalSearchActivity, targetUser)) ||
                                      (devAddr.isNotEmpty() && ChatHistoryManager.isDeviceForgotten(this@GlobalSearchActivity, devAddr))
                    if (isForgotten) continue

                    val displayContactName = AvatarManager.getContactDisplayName(this@GlobalSearchActivity, effectiveUser, devAddr, effectiveUser)
                    val avatarUri = AvatarManager.getContactAvatarUriOrFile(this@GlobalSearchActivity, effectiveUser, displayContactName, devAddr)

                    val lowerQuery = query.lowercase(Locale.ROOT)

                    if (displayContactName.lowercase(Locale.ROOT).contains(lowerQuery) || effectiveUser.lowercase(Locale.ROOT).contains(lowerQuery) || devAddr.lowercase(Locale.ROOT).contains(lowerQuery)) {
                        results.add(
                            SearchResultItem(
                                title = displayContactName,
                                snippet = "Чат с пользователем • $lastMsg",
                                time = timeStr,
                                targetUsername = effectiveUser,
                                deviceAddress = devAddr,
                                avatarUri = avatarUri
                            )
                        )
                    }

                    // Search messages inside history
                    val history = ChatHistoryManager.loadMessages(this@GlobalSearchActivity, effectiveUser)
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
            PrimeTransitions.startActivityWithTransition(this, intent)
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
            val context = holder.itemView.context
            val isDesktop = AvatarManager.isDesktopContact(context, item.targetUsername, item.deviceAddress)
            val cleanTitle = item.title.replace("💻", "").trim()
            val accentColor = ColorAccentManager.getCurrentAccentColor(context)

            holder.tvTitle.text = AvatarManager.formatNameWithDesktopIcon(context, cleanTitle, isDesktop, accentColor)
            holder.tvTime.text = item.time

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

            val avatarToLoad = item.avatarUri ?: AvatarManager.getContactAvatarUriOrFile(holder.itemView.context, item.targetUsername, item.title, item.deviceAddress)
            if (!avatarToLoad.isNullOrEmpty()) {
                loadAvatarIntoView(holder.itemView.context, avatarToLoad, holder.ivAvatar, item.title)
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

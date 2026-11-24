package com.messenger.prime

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.messenger.prime.databinding.ActivitySettingsAppearanceBinding
import com.messenger.prime.databinding.DialogColorPickerBinding
import com.messenger.prime.shapes.M3EShapes
import eightbitlab.com.blurview.BlurView
import org.json.JSONArray
import org.json.JSONObject

class SettingsAppearanceActivity : BaseSettingsDetailActivity() {

    private lateinit var b: ActivitySettingsAppearanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsAppearanceBinding.inflate(layoutInflater)
        setContentView(b.root)

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentTheme = sharedPrefs.getString("app_theme", "system") ?: "system"

        setupDetailHeroChrome(
            rootView = b.root,
            heroStage = b.layoutAppearanceHeroStage,
            heroOverlay = b.layoutAppearanceHeroOverlay,
            scrollView = b.scrollAppearanceSettings,
            title = "Оформление",
            subtitle = SettingsSupport.getThemeDisplayName(currentTheme),
            heroHeightFraction = 0.33f,
            heroMinDp = 240,
            heroMaxDp = 296,
            animatedCards = listOf(
                b.cardTheme,
                b.cardNavStyleSection,
                b.cardCategoriesAndNotes,
                b.cardColorAccent,
                b.cardLavaBg,
                b.cardDisableAnimations
            )
        )

        b.vAppearanceHeroWallpaper.setLiveTempWallpaper(
            ChatWallpaper(
                id = "appearance_hero_backdrop",
                name = "Appearance",
                type = "MATERIAL",
                patternType = "material_geo",
                isAnimated = false
            )
        )

        SettingsSupport.applySegmentedGroupShape(b.cardLavaBg, 0, 2)
        SettingsSupport.applySegmentedGroupShape(b.cardDisableAnimations, 1, 2)

        setupThemeSection(currentTheme)
        setupNavStyleSection()
        setupCategoriesAndNotesSection()
        setupAccentSection()
        setupLavaBackgroundSection()
        setupDisableAnimationsSection()
        updateAccentUi()
    }

    private fun setupThemeSection(currentTheme: String) {
        b.tvThemeSummary.text = SettingsSupport.getThemeDisplayName(currentTheme)
        updateThemeSelectionUI(currentTheme)
    }

    private fun updateThemeSelectionUI(themeStr: String) {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isNight = SettingsSupport.isDarkThemeActive(this)

        val isSystem = themeStr == "system"
        val isLight = themeStr == "light"
        val isDark = themeStr == "dark"
        val isTetoLight = themeStr == "teto_light"
        val isTetoDark = themeStr == "teto_dark"

        b.btnResetTheme.visibility = if (!isSystem) View.VISIBLE else View.GONE
        b.btnResetTheme.setOnClickListener {
            getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                .edit().remove("app_theme").apply()
            applyThemeChange("system")
        }

        b.rbThemeSystem.isChecked = isSystem
        b.rbThemeLight.isChecked = isLight
        b.rbThemeDark.isChecked = isDark
        b.rbThemeTetoLight.isChecked = isTetoLight
        b.rbThemeTetoDark.isChecked = isTetoDark

        val radioTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                accentColor,
                if (isNight) Color.parseColor("#66FFFFFF") else Color.parseColor("#66000000")
            )
        )
        b.rbThemeSystem.buttonTintList = radioTintList
        b.rbThemeLight.buttonTintList = radioTintList
        b.rbThemeDark.buttonTintList = radioTintList
        b.rbThemeTetoLight.buttonTintList = radioTintList
        b.rbThemeTetoDark.buttonTintList = radioTintList

        val strokeWidthActive = (2.5f * resources.displayMetrics.density).toInt()
        fun applyCardState(card: com.google.android.material.card.MaterialCardView, selected: Boolean) {
            card.strokeWidth = if (selected) strokeWidthActive else 0
            card.strokeColor = accentColor
            if (!PrimeTransitions.areAnimationsDisabled(this)) {
                card.animate().cancel()
                card.animate()
                    .scaleX(if (selected) 1.03f else 1.0f)
                    .scaleY(if (selected) 1.03f else 1.0f)
                    .translationZ(if (selected) 4f * resources.displayMetrics.density else 0f)
                    .setDuration(180)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }

        applyCardState(b.cardThemeSystem, isSystem)
        applyCardState(b.cardThemeLight, isLight)
        applyCardState(b.cardThemeDark, isDark)
        applyCardState(b.cardThemeTetoLight, isTetoLight)
        applyCardState(b.cardThemeTetoDark, isTetoDark)

        b.itemThemeSystem.setOnClickListener { if (!isSystem) applyThemeChange("system") }
        b.itemThemeLight.setOnClickListener { if (!isLight) applyThemeChange("light") }
        b.itemThemeDark.setOnClickListener { if (!isDark) applyThemeChange("dark") }
        b.itemThemeTetoLight.setOnClickListener { if (!isTetoLight) applyThemeChange("teto_light") }
        b.itemThemeTetoDark.setOnClickListener { if (!isTetoDark) applyThemeChange("teto_dark") }

        if (isTetoLight || isTetoDark) {
            b.cardTetoBanner.visibility = View.VISIBLE
            b.btnTetoSoundEasterEgg.tag = "do_not_tint"

            b.swTetoSound.setOnCheckedChangeListener(null)
            b.swTetoSound.isChecked = ColorAccentManager.isTetoSoundEnabled(this)
            b.swTetoSound.setOnCheckedChangeListener { _, isChecked ->
                ColorAccentManager.setTetoSoundEnabled(this, isChecked)
                if (isChecked) {
                    TetoVoiceSynthesizer.playTetoMicroChime()
                }
            }

            b.swTetoIcons.setOnCheckedChangeListener(null)
            b.swTetoIcons.isChecked = ColorAccentManager.isTetoCustomIconsEnabled(this)
            b.swTetoIcons.setOnCheckedChangeListener { _, isChecked ->
                ColorAccentManager.setTetoCustomIconsEnabled(this, isChecked)
                sendBroadcast(Intent("com.messenger.prime.THEME_CHANGED").setPackage(packageName))
            }

            b.btnTetoSoundEasterEgg.setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                TetoVoiceSynthesizer.playTetoSound(this)
                PrimeNotification.show(this, "Касане Тето: «0401 UTAU Синтезатор активен»")
            }
        } else {
            b.cardTetoBanner.visibility = View.GONE
        }
    }

    private fun applyThemeChange(newTheme: String) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentTheme = sharedPrefs.getString("app_theme", "system")

        if (newTheme != currentTheme) {
            sharedPrefs.edit().putString("app_theme", newTheme).apply()

            val mode = when (newTheme) {
                "light", "teto_light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark", "teto_dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)

            if (newTheme == "teto_light" || newTheme == "teto_dark") {
                val tetoColor = if (newTheme == "teto_light") 0xFFD80032.toInt() else 0xFFFF4D6D.toInt()
                ColorAccentManager.setAccentCustom(this, tetoColor)
            } else {
                ColorAccentManager.setAccentDefault(this)
            }

            val displayName = SettingsSupport.getThemeDisplayName(newTheme)
            b.tvThemeSummary.text = displayName
            updateDetailSubtitle(b.root, displayName)
            updateThemeSelectionUI(newTheme)
            setupSwatches()
            updateAccentUi()

            sendBroadcast(Intent("com.messenger.prime.THEME_CHANGED").setPackage(packageName))
            sendBroadcast(Intent("com.messenger.prime.ACCENT_CHANGED").setPackage(packageName))
        }
    }

    private fun setupNavStyleSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        var currentNavStyle = sharedPrefs.getString("navigation_style", "bottom_bar") ?: "bottom_bar"

        val updateNavStyleUi = { animate: Boolean ->
            val isIsland = currentNavStyle == "island"
            b.rbStyleIsland.isChecked = isIsland
            b.rbStyleBottomBar.isChecked = !isIsland
            b.btnResetNavStyle.visibility = if (!isIsland) View.VISIBLE else View.GONE

            if (animate) {
                TransitionManager.beginDelayedTransition(
                    b.cardNavStyleSection,
                    AutoTransition().apply { duration = 200L }
                )
            }
            b.layoutNavOrder.visibility = if (!isIsland) View.VISIBLE else View.GONE
            b.layoutNavIslandConfig.visibility = if (isIsland) View.VISIBLE else View.GONE

            val accentColor = ColorAccentManager.getCurrentAccentColor(this)
            val isNight = SettingsSupport.isDarkThemeActive(this)

            val radioTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(
                    accentColor,
                    if (isNight) Color.parseColor("#66FFFFFF") else Color.parseColor("#66000000")
                )
            )
            b.rbStyleIsland.buttonTintList = radioTintList
            b.rbStyleBottomBar.buttonTintList = radioTintList

            b.rbStyleIsland.setTextColor(
                if (isIsland) accentColor else ContextCompat.getColor(this, R.color.prime_text_primary)
            )
            b.rbStyleBottomBar.setTextColor(
                if (!isIsland) accentColor else ContextCompat.getColor(this, R.color.prime_text_primary)
            )

            val activeStrokePx = (2 * resources.displayMetrics.density).toInt()
            b.cardStyleIsland.strokeColor = accentColor
            b.cardStyleIsland.strokeWidth = if (isIsland) activeStrokePx else 0

            b.cardStyleBottomBar.strokeColor = accentColor
            b.cardStyleBottomBar.strokeWidth = if (!isIsland) activeStrokePx else 0

            if (!PrimeTransitions.areAnimationsDisabled(this)) {
                val decel = DecelerateInterpolator()
                b.cardStyleIsland.animate().cancel()
                b.cardStyleIsland.animate()
                    .scaleX(if (isIsland) 1.02f else 1.0f)
                    .scaleY(if (isIsland) 1.02f else 1.0f)
                    .setDuration(180)
                    .setInterpolator(decel)
                    .start()

                b.cardStyleBottomBar.animate().cancel()
                b.cardStyleBottomBar.animate()
                    .scaleX(if (!isIsland) 1.02f else 1.0f)
                    .scaleY(if (!isIsland) 1.02f else 1.0f)
                    .setDuration(180)
                    .setInterpolator(decel)
                    .start()
            }
        }

        val setStyle = { style: String ->
            if (currentNavStyle != style) {
                currentNavStyle = style
                sharedPrefs.edit().putString("navigation_style", style).apply()
                updateNavStyleUi(true)
                updateHeroAppearanceShowcase()
                sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            }
        }

        b.btnResetNavStyle.setOnClickListener {
            sharedPrefs.edit().remove("navigation_style").apply()
            setStyle("island")
        }

        b.switchAiContactIsland.setOnCheckedChangeListener(null)
        b.switchAiContactIsland.isChecked = sharedPrefs.getBoolean("ai_chat_contact_enabled", true)
        b.switchAiContactIsland.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("ai_chat_contact_enabled", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        b.switchIslandPullToSettings.setOnCheckedChangeListener(null)
        b.switchIslandPullToSettings.isChecked = sharedPrefs.getBoolean("island_pull_to_settings_enabled", true)
        b.switchIslandPullToSettings.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("island_pull_to_settings_enabled", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        b.switchNavShowLabels.setOnCheckedChangeListener(null)
        b.switchNavShowLabels.isChecked = sharedPrefs.getBoolean("nav_show_labels", false)
        b.switchNavShowLabels.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("nav_show_labels", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        updateNavStyleUi(false)

        b.cardStyleIsland.setOnClickListener { setStyle("island") }
        b.rbStyleIsland.setOnClickListener { setStyle("island") }
        b.cardStyleBottomBar.setOnClickListener { setStyle("bottom_bar") }
        b.rbStyleBottomBar.setOnClickListener { setStyle("bottom_bar") }

        setupNavOrderSection()
    }

    private fun setupNavOrderSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val defaultOrder = "chats,devices,ai,search,downloads,profile"
        val currentOrderStr = sharedPrefs.getString("nav_buttons_order", defaultOrder) ?: defaultOrder
        val currentOrder = currentOrderStr.split(",").filter { it.isNotEmpty() }

        val updateResetNavOrderVisibility = {
            val orderStr = sharedPrefs.getString("nav_buttons_order", defaultOrder) ?: defaultOrder
            val isChanged = orderStr != defaultOrder ||
                !sharedPrefs.getBoolean("nav_button_chats_visible", true) ||
                !sharedPrefs.getBoolean("nav_button_devices_visible", true) ||
                !sharedPrefs.getBoolean("nav_button_ai_visible", true) ||
                !sharedPrefs.getBoolean("nav_button_search_visible", true) ||
                !sharedPrefs.getBoolean("nav_button_downloads_visible", true)
            b.btnResetNavOrder.visibility = if (isChanged) View.VISIBLE else View.GONE
        }

        updateResetNavOrderVisibility()

        b.btnResetNavOrder.setOnClickListener {
            sharedPrefs.edit()
                .remove("nav_buttons_order")
                .remove("nav_button_chats_visible")
                .remove("nav_button_devices_visible")
                .remove("nav_button_ai_visible")
                .remove("nav_button_search_visible")
                .remove("nav_button_downloads_visible")
                .apply()
            setupNavOrderSection()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        val buttonConfigs = mutableListOf<BottomBarButtonConfig>()
        val allIds = listOf("chats", "devices", "ai", "search", "downloads", "profile")
        val orderToUse = if (currentOrder.containsAll(allIds)) currentOrder else defaultOrder.split(",")

        for (id in orderToUse) {
            when (id) {
                "chats" -> buttonConfigs.add(
                    BottomBarButtonConfig("chats", "Чаты", R.drawable.ic_nav_chats, sharedPrefs.getBoolean("nav_button_chats_visible", true))
                )
                "devices" -> buttonConfigs.add(
                    BottomBarButtonConfig("devices", "Устройства", R.drawable.ic_nav_devices, sharedPrefs.getBoolean("nav_button_devices_visible", true))
                )
                "ai" -> buttonConfigs.add(
                    BottomBarButtonConfig("ai", "AI Чат", R.drawable.ic_prime_ai, sharedPrefs.getBoolean("nav_button_ai_visible", true))
                )
                "search" -> buttonConfigs.add(
                    BottomBarButtonConfig("search", "Поиск", R.drawable.ic_search, sharedPrefs.getBoolean("nav_button_search_visible", true))
                )
                "downloads" -> buttonConfigs.add(
                    BottomBarButtonConfig("downloads", "Загрузки", R.drawable.ic_download, sharedPrefs.getBoolean("nav_button_downloads_visible", true))
                )
                "profile" -> buttonConfigs.add(
                    BottomBarButtonConfig("profile", "Профиль", R.drawable.ic_person, true, isMandatory = true)
                )
            }
        }

        val adapter = NavButtonsAdapter(buttonConfigs) {
            val newOrder = buttonConfigs.joinToString(",") { it.id }
            val editor = sharedPrefs.edit()
            editor.putString("nav_buttons_order", newOrder)
            buttonConfigs.forEach {
                if (it.id != "profile") {
                    editor.putBoolean("nav_button_${it.id}_visible", it.isVisible)
                }
            }
            editor.apply()
            updateResetNavOrderVisibility()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        b.rvNavButtons.layoutManager = LinearLayoutManager(this)
        b.rvNavButtons.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }).attachToRecyclerView(b.rvNavButtons)
    }

    private fun setupCategoriesAndNotesSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)

        b.switchCategoryChips.setOnCheckedChangeListener(null)
        b.switchCategoryChips.isChecked = sharedPrefs.getBoolean("category_chips_enabled", true)
        b.switchCategoryChips.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("category_chips_enabled", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        b.switchNotesChat.setOnCheckedChangeListener(null)
        b.switchNotesChat.isChecked = sharedPrefs.getBoolean("notes_chat_enabled", true)
        b.switchNotesChat.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("notes_chat_enabled", isChecked).apply()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
        }

        b.btnAddCustomCategory.setOnClickListener {
            showAddCustomCategoryDialog()
        }

        renderCategoriesList()
    }

    private fun renderCategoriesList() {
        b.containerCategoriesList.removeAllViews()

        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        val customCats = mutableListOf<Triple<String, String, List<String>>>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val name = obj.optString("name", "")
                val chatsArr = obj.optJSONArray("chats")
                val chatsList = mutableListOf<String>()
                if (chatsArr != null) {
                    for (j in 0 until chatsArr.length()) {
                        chatsList.add(chatsArr.optString(j))
                    }
                }
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    customCats.add(Triple(id, name, chatsList))
                }
            }
        } catch (_: Exception) {}

        if (customCats.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Свои категории не созданы"
                setTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_secondary))
                textSize = 12f
                setPadding(0, 4, 0, 4)
            }
            b.containerCategoriesList.addView(tvEmpty)
            return
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val density = resources.displayMetrics.density
        val pad = (8 * density).toInt()

        for (index in customCats.indices) {
            val (catId, catName, catChats) = customCats[index]

            val itemRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, pad, 0, pad)
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTitle = TextView(this).apply {
                text = catName
                setTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_primary))
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }

            val chatsSummary = if (catChats.isEmpty()) "Без чатов" else "Чаты: ${catChats.joinToString(", ")}"
            val tvSubtitle = TextView(this).apply {
                text = chatsSummary
                setTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_secondary))
                textSize = 11f
            }

            textLayout.addView(tvTitle)
            textLayout.addView(tvSubtitle)
            itemRow.addView(textLayout)

            val btnEdit = ImageButton(this).apply {
                setImageResource(R.drawable.ic_edit)
                setBackgroundResource(android.R.color.transparent)
                imageTintList = ColorStateList.valueOf(accentColor)
                setPadding(pad, pad, pad, pad)
                setOnClickListener { showEditCustomCategoryDialog(catId, catName, catChats) }
            }
            itemRow.addView(btnEdit)

            if (index > 0) {
                val btnUp = ImageButton(this).apply {
                    setImageResource(R.drawable.ic_arrow_up)
                    setBackgroundResource(android.R.color.transparent)
                    imageTintList = ColorStateList.valueOf(accentColor)
                    setPadding(pad, pad, pad, pad)
                    setOnClickListener { moveCategory(index, index - 1) }
                }
                itemRow.addView(btnUp)
            }

            if (index < customCats.size - 1) {
                val btnDown = ImageButton(this).apply {
                    setImageResource(R.drawable.ic_expand_more)
                    setBackgroundResource(android.R.color.transparent)
                    imageTintList = ColorStateList.valueOf(accentColor)
                    setPadding(pad, pad, pad, pad)
                    setOnClickListener { moveCategory(index, index + 1) }
                }
                itemRow.addView(btnDown)
            }

            val btnDelete = ImageButton(this).apply {
                setImageResource(R.drawable.ic_trash)
                setBackgroundResource(android.R.color.transparent)
                imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_danger)
                )
                setPadding(pad, pad, pad, pad)
                setOnClickListener {
                    deleteCustomCategory(catId)
                    renderCategoriesList()
                    PrimeNotification.show(this@SettingsAppearanceActivity, "Категория «$catName» удалена")
                    sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
                }
            }
            itemRow.addView(btnDelete)

            b.containerCategoriesList.addView(itemRow)
        }
    }

    private fun moveCategory(fromIdx: Int, toIdx: Int) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        try {
            val array = JSONArray(jsonStr)
            if (fromIdx in 0 until array.length() && toIdx in 0 until array.length()) {
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) list.add(array.getJSONObject(i))
                val moved = list.removeAt(fromIdx)
                list.add(toIdx, moved)
                val newArray = JSONArray()
                list.forEach { newArray.put(it) }
                sharedPrefs.edit().putString("custom_categories_json", newArray.toString()).apply()
                renderCategoriesList()
                sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            }
        } catch (_: Exception) {}
    }

    private fun showEditCustomCategoryDialog(catId: String, currentName: String, currentChats: List<String>) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }

        val inputName = EditText(this).apply {
            hint = "Название категории"
            setText(currentName)
            setTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_primary))
            setHintTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_secondary))
        }
        container.addView(inputName)

        val chatNames = collectAvailableChatNames()
        val selectedChats = currentChats.toMutableSet()
        if (chatNames.isNotEmpty()) {
            val tvLabel = TextView(this).apply {
                text = "Выберите чаты для категории:"
                setTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_secondary))
                textSize = 13f
                setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, (6 * resources.displayMetrics.density).toInt())
            }
            container.addView(tvLabel)

            for (chatName in chatNames) {
                val cb = CheckBox(this).apply {
                    text = chatName
                    setTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_primary))
                    isChecked = selectedChats.contains(chatName)
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) selectedChats.add(chatName) else selectedChats.remove(chatName)
                    }
                }
                container.addView(cb)
            }
        }

        PrimeBlurDialog.show(
            activity = this,
            title = "Редактировать категорию",
            message = null,
            positiveText = "Сохранить",
            negativeText = "Отмена",
            customView = container,
            onPositive = {
                val categoryName = inputName.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    updateCustomCategory(catId, categoryName, selectedChats.toList())
                    renderCategoriesList()
                    PrimeNotification.show(this, "Категория обновлена")
                    sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
                }
            }
        )
    }

    private fun showAddCustomCategoryDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }

        val inputName = EditText(this).apply {
            hint = "Название категории (напр. Работа)"
            setTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_primary))
            setHintTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_secondary))
        }
        container.addView(inputName)

        val chatNames = collectAvailableChatNames()
        val selectedChats = mutableSetOf<String>()
        if (chatNames.isNotEmpty()) {
            val tvLabel = TextView(this).apply {
                text = "Выберите чаты для этой категории:"
                setTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_secondary))
                textSize = 13f
                setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, (6 * resources.displayMetrics.density).toInt())
            }
            container.addView(tvLabel)

            for (chatName in chatNames) {
                val cb = CheckBox(this).apply {
                    text = chatName
                    setTextColor(ContextCompat.getColor(this@SettingsAppearanceActivity, R.color.prime_text_primary))
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) selectedChats.add(chatName) else selectedChats.remove(chatName)
                    }
                }
                container.addView(cb)
            }
        }

        PrimeBlurDialog.show(
            activity = this,
            title = "Создать категорию",
            message = null,
            positiveText = "Сохранить",
            negativeText = "Отмена",
            customView = container,
            onPositive = {
                val categoryName = inputName.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    saveCustomCategory(categoryName, selectedChats.toList())
                    renderCategoriesList()
                    PrimeNotification.show(this, "Категория «$categoryName» создана")
                    sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
                }
            }
        )
    }

    private fun collectAvailableChatNames(): List<String> {
        val jsonChats = ChatHistoryManager.getPersistedChatsJson(this)
        val chatNames = mutableListOf<String>()
        try {
            val array = JSONArray(jsonChats)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "")
                if (name.isNotEmpty() && name != "Заметки" && name != "AI Prime Chat" && !chatNames.contains(name)) {
                    chatNames.add(name)
                }
            }
        } catch (_: Exception) {}
        return chatNames
    }

    private fun updateCustomCategory(id: String, name: String, chats: List<String>) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") == id) {
                    obj.put("name", name)
                    val chatsArray = JSONArray()
                    chats.forEach { chatsArray.put(it) }
                    obj.put("chats", chatsArray)
                    break
                }
            }
            sharedPrefs.edit().putString("custom_categories_json", array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun saveCustomCategory(name: String, chats: List<String>) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        val array = try { JSONArray(jsonStr) } catch (_: Exception) { JSONArray() }

        val newObj = JSONObject().apply {
            put("id", "cat_${System.currentTimeMillis()}")
            put("name", name)
            val chatsArray = JSONArray()
            chats.forEach { chatsArray.put(it) }
            put("chats", chatsArray)
        }
        array.put(newObj)
        sharedPrefs.edit().putString("custom_categories_json", array.toString()).apply()
    }

    private fun deleteCustomCategory(id: String) {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString("custom_categories_json", "[]") ?: "[]"
        try {
            val array = JSONArray(jsonStr)
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") != id) {
                    newArray.put(obj)
                }
            }
            sharedPrefs.edit().putString("custom_categories_json", newArray.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun setupAccentSection() {
        val updateResetAccentVisibility = {
            b.btnResetAccent.visibility =
                if (ColorAccentManager.getAccentType(this) != ColorAccentManager.ACCENT_TYPE_DEFAULT) View.VISIBLE
                else View.GONE
        }
        updateResetAccentVisibility()

        b.btnResetAccent.setOnClickListener {
            ColorAccentManager.setAccentDefault(this)
            onAccentColorChanged()
            updateResetAccentVisibility()
        }

        b.switchMonetAccent.setOnCheckedChangeListener(null)
        b.switchMonetAccent.isChecked = (ColorAccentManager.getAccentType(this) == ColorAccentManager.ACCENT_TYPE_SYSTEM)
        b.switchMonetAccent.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ColorAccentManager.setAccentSystem(this)
            } else if (ColorAccentManager.getAccentType(this) == ColorAccentManager.ACCENT_TYPE_SYSTEM) {
                ColorAccentManager.setAccentDefault(this)
            }
            onAccentColorChanged()
            updateResetAccentVisibility()
        }

        setupSwatches()
        b.btnCustomPicker.setOnClickListener { showCustomColorSpectrumPicker() }
    }

    private fun setupSwatches() {
        val isDark = SettingsSupport.isDarkThemeActive(this)
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val accentType = ColorAccentManager.getAccentType(this)
        val dp36 = (36 * resources.displayMetrics.density).toInt()
        val dp8 = (8 * resources.displayMetrics.density).toInt()

        b.layoutSwatches.tag = "do_not_tint"
        b.layoutSwatches.removeAllViews()

        ColorAccentManager.PRESET_COLORS.forEachIndexed { index, preset ->
            val swatchColor = if (isDark) preset.darkColor else preset.lightColor
            val isSelected = when (accentType) {
                ColorAccentManager.ACCENT_TYPE_SYSTEM -> false
                ColorAccentManager.ACCENT_TYPE_DEFAULT -> index == 0
                ColorAccentManager.ACCENT_TYPE_CUSTOM -> (swatchColor == accentColor) ||
                    (preset.lightColor == accentColor || preset.darkColor == accentColor)
                else -> index == 0
            }

            val btn = MaterialButton(this).apply {
                tag = "do_not_tint"
                layoutParams = LinearLayout.LayoutParams(dp36, dp36).apply { marginEnd = dp8 }
                insetTop = 0
                insetBottom = 0
                cornerRadius = dp36 / 2
                backgroundTintList = ColorStateList.valueOf(swatchColor)
                if (isSelected) {
                    strokeColor = ColorStateList.valueOf(Color.WHITE)
                    strokeWidth = (2.5f * resources.displayMetrics.density).toInt()
                } else {
                    strokeWidth = 0
                }
                setOnClickListener {
                    if (index == 0) {
                        ColorAccentManager.setAccentDefault(this@SettingsAppearanceActivity)
                    } else {
                        ColorAccentManager.setAccentCustom(this@SettingsAppearanceActivity, swatchColor)
                    }
                    onAccentColorChanged()
                }
            }
            b.layoutSwatches.addView(btn)
        }
    }

    private fun onAccentColorChanged() {
        updateAccentUi()
        sendBroadcast(Intent("com.messenger.prime.ACCENT_CHANGED").setPackage(packageName))
        SettingsSupport.restartApp(this)
    }

    private fun showCustomColorSpectrumPicker() {
        val dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
        dialogBinding.root.setViewTreeLifecycleOwner(this)
        dialogBinding.root.setViewTreeSavedStateRegistryOwner(this)
        dialogBinding.btnPipette.visibility = View.GONE

        val blurCard = dialogBinding.root.findViewById<BlurView>(R.id.blurDialogCard)
        if (blurCard != null) {
            val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
                ?: window.decorView as ViewGroup
            val overlayColor = getDefaultBlurOverlayColor(this)
            blurCard.setupBlur(rootView, 16f, overlayColor, window.decorView.background)
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.Theme_Prime_AlertDialog)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    win.attributes = win.attributes.apply { blurBehindRadius = 60 }
                } catch (_: Throwable) {}
            }
        }

        dialog.setOnShowListener {
            blurCard?.apply {
                alpha = 0f
                scaleX = 0.82f
                scaleY = 0.82f
                translationY = 60f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(280L)
                    .setInterpolator(OvershootInterpolator(1.15f))
                    .start()
            }
        }

        var selectedColorInt = ColorAccentManager.getCurrentAccentColor(this)
        var isInternalChange = false

        fun updateDialogColors(color: Int) {
            if (isInternalChange) return
            isInternalChange = true
            try {
                dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                val hexStr = String.format("#%06X", 0xFFFFFF and color)
                if (dialogBinding.etHex.text?.toString() != hexStr) dialogBinding.etHex.setText(hexStr)
                val rStr = Color.red(color).toString()
                if (dialogBinding.etR.text?.toString() != rStr) dialogBinding.etR.setText(rStr)
                val gStr = Color.green(color).toString()
                if (dialogBinding.etG.text?.toString() != gStr) dialogBinding.etG.setText(gStr)
                val bStr = Color.blue(color).toString()
                if (dialogBinding.etB.text?.toString() != bStr) dialogBinding.etB.setText(bStr)
            } catch (_: Exception) {
            } finally {
                isInternalChange = false
            }
        }

        updateDialogColors(selectedColorInt)

        dialogBinding.spectrumView.setOnColorChangedListener { color ->
            selectedColorInt = color
            updateDialogColors(color)
        }

        val rgbWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange) return
                try {
                    val r = dialogBinding.etR.text.toString().toInt().coerceIn(0, 255)
                    val g = dialogBinding.etG.text.toString().toInt().coerceIn(0, 255)
                    val bVal = dialogBinding.etB.text.toString().toInt().coerceIn(0, 255)
                    val color = Color.rgb(r, g, bVal)
                    selectedColorInt = color
                    isInternalChange = true
                    dialogBinding.etHex.setText(String.format("#%06X", 0xFFFFFF and color))
                    dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                } catch (_: Exception) {
                } finally {
                    isInternalChange = false
                }
            }
        }

        val hexWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isInternalChange) return
                try {
                    val color = Color.parseColor(s.toString())
                    selectedColorInt = color
                    isInternalChange = true
                    dialogBinding.etR.setText(Color.red(color).toString())
                    dialogBinding.etG.setText(Color.green(color).toString())
                    dialogBinding.etB.setText(Color.blue(color).toString())
                    dialogBinding.viewColorPreview.backgroundTintList = ColorStateList.valueOf(color)
                } catch (_: Exception) {
                } finally {
                    isInternalChange = false
                }
            }
        }

        dialogBinding.etHex.addTextChangedListener(hexWatcher)
        dialogBinding.etR.addTextChangedListener(rgbWatcher)
        dialogBinding.etG.addTextChangedListener(rgbWatcher)
        dialogBinding.etB.addTextChangedListener(rgbWatcher)

        dialogBinding.btnApplyColor.setOnClickListener {
            try {
                var hex = dialogBinding.etHex.text.toString().trim()
                if (!hex.startsWith("#")) hex = "#$hex"
                selectedColorInt = Color.parseColor(hex)
            } catch (_: Exception) {}

            ColorAccentManager.setAccentCustom(this, selectedColorInt)
            onAccentColorChanged()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupLavaBackgroundSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val isDark = SettingsSupport.isDarkThemeActive(this)

        b.composeLavaPreview.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val renderPreview = {
            b.composeLavaPreview.setContent {
                PrimeTheme(darkTheme = isDark) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedBackground(darkTheme = isDark, ignoreSettingsToggle = false)
                    }
                }
            }
        }

        b.switchLavaBg.setOnCheckedChangeListener(null)
        val isLavaOn = sharedPrefs.getBoolean("settings_lava_bg", true)
        b.switchLavaBg.isChecked = isLavaOn
        b.btnResetLava.visibility = if (!isLavaOn) View.VISIBLE else View.GONE

        if (isLavaOn) {
            renderPreview()
        }

        b.btnResetLava.setOnClickListener {
            b.switchLavaBg.isChecked = true
        }

        b.switchLavaBg.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("settings_lava_bg", isChecked).apply()
            b.btnResetLava.visibility = if (!isChecked) View.VISIBLE else View.GONE
            renderPreview()
            LavaBackgroundState.onActivityResumed()
            sendBroadcast(Intent("com.messenger.prime.NAV_STYLE_CHANGED").setPackage(packageName))
            SettingsSupport.restartApp(this)
        }
    }

    private fun setupDisableAnimationsSection() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        b.switchDisableAnimations.setOnCheckedChangeListener(null)
        b.switchDisableAnimations.isChecked = sharedPrefs.getBoolean("disable_animations", false)
        b.switchDisableAnimations.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("disable_animations", isChecked).apply()
            PrimeTransitions.applyGlobalAnimationScale(this)
            sendBroadcast(Intent("com.messenger.prime.ANIMATIONS_TOGGLED").setPackage(packageName))
        }
    }

    private fun updateAccentUi() {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)
        val colorStateList = ColorStateList.valueOf(accentColor)

        SettingsSupport.applyM3EBadgeIcon(b.ivIconTheme, accentColor, M3EShapes.flower, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivIconNavStyle, accentColor, M3EShapes.cookie4Sided, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivIconCategoriesAndNotes, accentColor, M3EShapes.cookie6Sided, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivIconAccent, accentColor, M3EShapes.verySunny, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivIconLava, accentColor, M3EShapes.clover4Leaf, isDark)
        SettingsSupport.applyM3EBadgeIcon(b.ivIconDisableAnimations, accentColor, M3EShapes.circle, isDark)

        b.tvTitleTheme.setTextColor(accentColor)
        b.tvTitleNavStyle.setTextColor(accentColor)
        b.tvTitleCategoriesAndNotes.setTextColor(accentColor)
        b.tvTitleAccent.setTextColor(accentColor)
        b.tvTitleLava.setTextColor(accentColor)
        b.tvTitleDisableAnimations.setTextColor(accentColor)

        b.tvAccentSummary.text = ColorAccentManager.getAccentSummary(this)
        b.vAccentColorPreview.backgroundTintList = colorStateList

        val tonalBg = ColorStateList.valueOf(ColorUtils.setAlphaComponent(accentColor, if (isDark) 52 else 26))
        listOf(b.btnCustomPicker, b.btnAddCustomCategory).forEach { btn ->
            btn.tag = "do_not_tint"
            btn.backgroundTintList = tonalBg
            btn.setTextColor(accentColor)
            btn.iconTint = colorStateList
            btn.strokeWidth = 0
        }

        updateHeroAppearanceShowcase()
        ColorAccentManager.tintViewTree(b.root, accentColor)
    }

    private fun updateHeroAppearanceShowcase() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        val currentTheme = sharedPrefs.getString("app_theme", "system") ?: "system"
        val currentNavStyle = sharedPrefs.getString("navigation_style", "bottom_bar") ?: "bottom_bar"
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val accentTint = ColorStateList.valueOf(accentColor)

        val isIsland = currentNavStyle == "island"
        b.layoutHeroPreviewIsland.visibility = if (isIsland) View.VISIBLE else View.GONE
        b.layoutHeroPreviewBottomBar.visibility = if (!isIsland) View.VISIBLE else View.GONE

        b.vHeroIslandDot.backgroundTintList = accentTint
        b.vHeroPreviewBubbleOut.backgroundTintList = accentTint
        b.vHeroBottomTabActive.backgroundTintList = accentTint
        b.vHeroAccentDot.backgroundTintList = accentTint

        b.tvHeroThemeBadge.text = SettingsSupport.getThemeDisplayName(currentTheme)
        b.tvHeroAccentBadge.text = ColorAccentManager.getAccentSummary(this)
    }
}

data class BottomBarButtonConfig(
    val id: String,
    val title: String,
    val iconRes: Int,
    var isVisible: Boolean,
    val isMandatory: Boolean = false
)

class NavButtonsAdapter(
    private val items: MutableList<BottomBarButtonConfig>,
    private val onOrderChanged: () -> Unit
) : RecyclerView.Adapter<NavButtonsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(android.R.id.icon)
        val tvTitle: TextView = view.findViewById(android.R.id.title)
        val switchVisibility: MaterialSwitch = view.findViewById(android.R.id.checkbox)
        val ivDragHandle: ImageView = view.findViewById(android.R.id.icon2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val view = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val scale = context.resources.displayMetrics.density
            val paddingV = (12 * scale).toInt()
            setPadding(0, paddingV, 0, paddingV)

            val icon = ImageView(context).apply {
                id = android.R.id.icon
                layoutParams = LinearLayout.LayoutParams((24 * scale).toInt(), (24 * scale).toInt()).apply {
                    marginEnd = (16 * scale).toInt()
                }
            }
            addView(icon)

            val title = TextView(context).apply {
                id = android.R.id.title
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(context, R.color.prime_text_primary))
                textSize = 15f
            }
            addView(title)

            val switch = MaterialSwitch(context).apply {
                id = android.R.id.checkbox
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            addView(switch)

            val dragHandle = ImageView(context).apply {
                id = android.R.id.icon2
                layoutParams = LinearLayout.LayoutParams((24 * scale).toInt(), (24 * scale).toInt()).apply {
                    marginStart = (16 * scale).toInt()
                }
                setImageResource(R.drawable.ic_drag_handle)
                imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.prime_text_secondary)
                )
            }
            addView(dragHandle)
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.ivIcon.setImageResource(item.iconRes)
        val accent = ColorAccentManager.getCurrentAccentColor(holder.itemView.context)
        holder.ivIcon.imageTintList = ColorStateList.valueOf(accent)
        holder.tvTitle.text = item.title

        holder.switchVisibility.setOnCheckedChangeListener(null)
        holder.switchVisibility.isChecked = item.isVisible
        holder.switchVisibility.isEnabled = !item.isMandatory
        holder.switchVisibility.alpha = if (item.isMandatory) 0.5f else 1f

        holder.switchVisibility.setOnCheckedChangeListener { _, isChecked ->
            item.isVisible = isChecked
            onOrderChanged()
        }
    }

    override fun getItemCount() = items.size

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = items.removeAt(fromPosition)
        items.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
        onOrderChanged()
    }
}

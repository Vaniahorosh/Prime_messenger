package com.messenger.prime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.messenger.prime.databinding.ActivitySettingsPluginsBinding
import com.messenger.prime.shapes.M3EDrawable
import com.messenger.prime.shapes.M3EShapes

class SettingsPluginsActivity : BaseSettingsDetailActivity() {

    private lateinit var b: ActivitySettingsPluginsBinding

    private val pluginsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshPluginsList()
        }
    }

    private val pluginFilePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data?.data != null) {
                val uri = result.data?.data ?: return@registerForActivityResult
                var name: String? = null
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) name = cursor.getString(idx)
                        }
                    }
                } catch (_: Exception) {}
                val plugin = PluginManager.importPluginFile(this, uri, name)
                if (plugin != null) {
                    refreshPluginsList()
                    PrimeNotification.show(this, getString(R.string.settings_plugins_imported_format, plugin.name))
                } else {
                    PrimeNotification.show(this, getString(R.string.settings_plugins_import_error))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsPluginsBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupDetailHeroChrome(
            rootView = b.root,
            heroStage = b.layoutPluginsHeroStage,
            heroOverlay = b.layoutPluginsHeroOverlay,
            scrollView = b.scrollPluginsSettings,
            title = getString(R.string.settings_plugins_title),
            subtitle = getString(R.string.settings_hub_plugins_default_sub),
            heroHeightFraction = 0.28f,
            heroMinDp = 208,
            heroMaxDp = 256,
            animatedCards = listOf(b.cardPlugins)
        )

        configureDetailAction(
            rootView = b.root,
            iconRes = R.drawable.ic_add,
            contentDescription = getString(R.string.settings_plugins_add)
        ) {
            showAddPluginDialog()
        }

        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val isDark = SettingsSupport.isDarkThemeActive(this)

        b.flPluginsHeroBadge.background = M3EDrawable(
            shape = M3EShapes.gem,
            color = ColorUtils.setAlphaComponent(accentColor, if (isDark) 68 else 36)
        )
        b.ivPluginsHeroIcon.imageTintList = ColorStateList.valueOf(accentColor)

        SettingsSupport.applyM3EBadgeIcon(b.ivIconPluginsCard, accentColor, M3EShapes.gem, isDark)
        b.tvTitlePluginsCard.setTextColor(accentColor)
        b.btnAddPlugin.backgroundTintList = ColorStateList.valueOf(accentColor)
        b.btnAddPlugin.setOnClickListener { showAddPluginDialog() }

        try {
            ContextCompat.registerReceiver(
                this,
                pluginsReceiver,
                IntentFilter(PluginManager.ACTION_PLUGINS_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {}

        refreshPluginsList()
        ColorAccentManager.tintViewTree(b.root, accentColor)
    }

    override fun onDestroy() {
        try { unregisterReceiver(pluginsReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun refreshPluginsList() {
        val list = PluginManager.getAllPlugins(this)
        val activeCount = list.count { it.isEnabled }
        val summaryText = if (list.isEmpty()) {
            getString(R.string.settings_plugins_summary_empty)
        } else {
            getString(R.string.settings_plugins_summary_active_format, activeCount, list.size)
        }
        updateDetailSubtitle(b.root, summaryText)
        b.tvHeroPluginsCount.text = summaryText

        b.containerPluginsList.removeAllViews()
        if (list.isEmpty()) {
            b.tvEmptyPluginsNotice.visibility = View.VISIBLE
            b.containerPluginsList.visibility = View.GONE
        } else {
            b.tvEmptyPluginsNotice.visibility = View.GONE
            b.containerPluginsList.visibility = View.VISIBLE

            list.forEach { plugin ->
                val row = layoutInflater.inflate(R.layout.item_plugin_row, b.containerPluginsList, false)
                val tvName = row.findViewById<TextView>(R.id.tvPluginName)
                val tvSubtitle = row.findViewById<TextView>(R.id.tvPluginSubtitle)
                val switchEnable = row.findViewById<SwitchCompat>(R.id.switchPluginEnable)
                val btnDelete = row.findViewById<View>(R.id.btnDeletePlugin)
                val btnRun = row.findViewById<View>(R.id.btnRunPlugin)

                tvName.text = plugin.name
                val statusStr = getString(
                    if (plugin.isEnabled) R.string.settings_plugins_status_active
                    else R.string.settings_plugins_status_disabled
                )
                tvSubtitle.text = getString(R.string.settings_plugins_subtitle_format, plugin.fileType, statusStr)

                switchEnable.setOnCheckedChangeListener(null)
                switchEnable.isChecked = plugin.isEnabled
                switchEnable.setOnCheckedChangeListener { _, isChecked ->
                    PluginManager.togglePlugin(this, plugin.id, isChecked)
                    refreshPluginsList()
                }

                btnRun?.setOnClickListener {
                    val execResult = PluginExecutor.executePlugin(this, plugin)
                    PrimeNotification.show(this, execResult.take(120))
                }

                btnDelete.setOnClickListener {
                    PrimeBlurDialog.show(
                        activity = this,
                        title = getString(R.string.settings_plugins_delete_dialog_title),
                        message = getString(R.string.settings_plugins_delete_dialog_msg, plugin.name),
                        positiveText = getString(R.string.action_delete),
                        negativeText = getString(R.string.action_cancel),
                        isPositiveDanger = true,
                        onPositive = {
                            PluginManager.deletePlugin(this, plugin.id)
                            refreshPluginsList()
                            PrimeNotification.show(this, getString(R.string.settings_plugins_deleted))
                        }
                    )
                }

                row.setOnClickListener { showPluginCodeViewerDialog(plugin) }
                b.containerPluginsList.addView(row)
            }
        }
    }

    private fun showAddPluginDialog() {
        PrimeBlurDialog.showList(
            activity = this,
            title = getString(R.string.settings_plugins_add_dialog_title),
            items = arrayOf(
                getString(R.string.settings_plugins_add_source_file),
                getString(R.string.settings_plugins_add_source_manual)
            ),
            iconRes = R.drawable.ic_add,
            onItemSelected = { index, _ ->
                if (index == 0) {
                    try {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        pluginFilePickerLauncher.launch(intent)
                    } catch (_: Exception) {
                        PrimeNotification.show(this, getString(R.string.settings_plugins_picker_error))
                    }
                } else {
                    showManualPluginCreationDialog()
                }
            }
        )
    }

    private fun showManualPluginCreationDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val tvLabelName = TextView(this).apply {
            text = getString(R.string.settings_plugins_manual_name_label)
            setTextColor(ContextCompat.getColor(this@SettingsPluginsActivity, R.color.prime_text_primary))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
        }
        val etName = EditText(this).apply {
            hint = getString(R.string.settings_plugins_manual_name_hint)
            isSingleLine = true
            background = ContextCompat.getDrawable(this@SettingsPluginsActivity, R.drawable.bg_date_bubble)
            setPadding(24, 18, 24, 18)
            setTextColor(ContextCompat.getColor(this@SettingsPluginsActivity, R.color.prime_text_primary))
        }

        val tvLabelLang = TextView(this).apply {
            text = getString(R.string.settings_plugins_manual_lang_label)
            setTextColor(ContextCompat.getColor(this@SettingsPluginsActivity, R.color.prime_text_primary))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 12, 0, 0)
        }
        val languages = arrayOf(
            "Java (.java)", "Kotlin (.kt)", "Python (.py)",
            "JavaScript (.js)", "HTML (.html)", "Shell (.sh)", "JSON (.json)"
        )
        val spinnerLang = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@SettingsPluginsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                languages
            )
        }

        val tvLabelCode = TextView(this).apply {
            text = getString(R.string.settings_plugins_manual_code_label)
            setTextColor(ContextCompat.getColor(this@SettingsPluginsActivity, R.color.prime_text_primary))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 12, 0, 0)
        }
        val etCode = EditText(this).apply {
            hint = getString(R.string.settings_plugins_manual_code_hint)
            background = ContextCompat.getDrawable(this@SettingsPluginsActivity, R.drawable.bg_date_bubble)
            setPadding(24, 18, 24, 18)
            minLines = 6
            maxLines = 10
            setTextColor(ContextCompat.getColor(this@SettingsPluginsActivity, R.color.prime_text_primary))
        }

        layout.addView(tvLabelName)
        layout.addView(etName)
        layout.addView(tvLabelLang)
        layout.addView(spinnerLang)
        layout.addView(tvLabelCode)
        layout.addView(etCode)

        PrimeBlurDialog.show(
            activity = this,
            title = getString(R.string.settings_plugins_manual_title),
            message = getString(R.string.settings_plugins_manual_desc),
            positiveText = getString(R.string.settings_plugins_manual_create_btn),
            negativeText = getString(R.string.action_cancel),
            iconRes = R.drawable.ic_add,
            customView = layout,
            onPositive = {
                val nameInput = etName.text.toString().trim()
                val langIndex = spinnerLang.selectedItemPosition
                val ext = when (langIndex) {
                    1 -> "Kotlin"
                    2 -> "Python"
                    3 -> "JS"
                    4 -> "HTML"
                    5 -> "Shell"
                    6 -> "JSON"
                    else -> "Java"
                }
                val defaultExt = when (ext) {
                    "Kotlin" -> ".kt"
                    "Python" -> ".py"
                    "JS" -> ".js"
                    "HTML" -> ".html"
                    "Shell" -> ".sh"
                    "JSON" -> ".json"
                    else -> ".java"
                }
                val name = when {
                    nameInput.contains(".") -> nameInput
                    nameInput.isNotBlank() -> "$nameInput$defaultExt"
                    else -> "Plugin$defaultExt"
                }
                val code = etCode.text.toString().trim()
                val plugin = PluginManager.createPluginFromCode(this, name, code, ext)
                refreshPluginsList()
                PrimeNotification.show(this, getString(R.string.settings_plugins_created_format, plugin.name))
            }
        )
    }

    private fun showPluginCodeViewerDialog(plugin: PluginItem) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val etCode = EditText(this).apply {
            setText(plugin.codeContent)
            background = ContextCompat.getDrawable(this@SettingsPluginsActivity, R.drawable.bg_date_bubble)
            setPadding(24, 18, 24, 18)
            minLines = 8
            maxLines = 14
            setTextColor(ContextCompat.getColor(this@SettingsPluginsActivity, R.color.prime_text_primary))
        }

        layout.addView(etCode)

        PrimeBlurDialog.show(
            activity = this,
            title = getString(R.string.settings_plugins_viewer_title_format, plugin.name),
            message = getString(R.string.settings_plugins_viewer_msg_format, plugin.fileType),
            positiveText = getString(R.string.settings_plugins_save_run),
            negativeText = getString(R.string.settings_close),
            iconRes = R.drawable.ic_file,
            customView = layout,
            onPositive = {
                val newCode = etCode.text.toString().trim()
                plugin.codeContent = newCode
                PluginManager.savePlugin(this, plugin)
                refreshPluginsList()
                val execResult = PluginExecutor.executePlugin(this, plugin)
                PrimeNotification.show(this, execResult.take(120))
            }
        )
    }
}

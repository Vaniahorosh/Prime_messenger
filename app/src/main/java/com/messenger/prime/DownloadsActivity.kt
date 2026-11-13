package com.messenger.prime

import android.animation.ValueAnimator
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.messenger.prime.databinding.ActivityDownloadsBinding
import eightbitlab.com.blurview.BlurView
import java.io.File
import java.util.Locale

class DownloadsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadsBinding
    private lateinit var adapter: DownloadsAdapter
    private lateinit var activeTransfersAdapter: ActiveTransfersAdapter

    private var allDownloadsList = listOf<DownloadItem>()
    private var activeCategoryFilter: String = "ALL" // ALL, MEDIA, DOCS, MUSIC, UPDATES
    private var currentSortMode: String = "DATE_DESC" // DATE_DESC, DATE_ASC, SIZE_DESC, SIZE_ASC, NAME_ASC, NAME_DESC
    private var searchQuery: String = ""
    private var isGridMode = true

    private val transferListener = object : TransferListener {
        override fun onTransferUpdated(transfers: List<ActiveTransfer>) {
            val currentUser = DownloadsManager.getCurrentUser(this@DownloadsActivity)
            val userTransfers = transfers.filter { it.userLogin == currentUser || currentUser.isEmpty() }
            runOnUiThread {
                activeTransfersAdapter.setItems(userTransfers)
                binding.rvActiveTransfers.visibility = if (userTransfers.isNotEmpty()) View.VISIBLE else View.GONE
                loadDownloadsData()
            }
        }
    }

    companion object {
        @JvmStatic
        fun openWithPermissionCheck(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    val density = activity.resources.displayMetrics.density
                    val isDark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    val container = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
                    }
                    val tvDescription = TextView(activity).apply {
                        text = "Для стабильности работы приложения, сохранения файлов и возможности автоматической установки обновлений Prime необходимо разрешить 'Установку из неизвестных источников'."
                        textSize = 14f
                        setTextColor(if (isDark) Color.parseColor("#E2E8F0") else Color.parseColor("#1E293B"))
                        setLineSpacing(0f, 1.2f)
                    }
                    container.addView(tvDescription)

                    PrimeBlurDialog.show(
                        activity = activity,
                        title = "Разрешение на загрузки",
                        customView = container,
                        positiveText = "Настройки",
                        negativeText = "Отмена",
                        iconRes = R.drawable.ic_download,
                        onPositive = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${activity.packageName}")
                                )
                                activity.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                    return
                }
            }
            val intent = Intent(activity, DownloadsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            PrimeTransitions.startActivityWithTransition(activity, intent)
        }
    }

    private val accentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.messenger.prime.ACCENT_CHANGED") {
                applyIslandHeaderColor()
                setupFilterChips()
                setupBottomNav()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PrimeTransitions.applyOpenTransition(this)
        applyIslandHeaderColor()
        setupSystemBarInsets()
        setupHeaderAndSearch()
        setupFilterChips()
        setupRecyclerView()
        setupStorageSummaryActions()
        setupBottomNav()

        loadDownloadsData()

        try {
            val filter = IntentFilter("com.messenger.prime.ACCENT_CHANGED")
            ContextCompat.registerReceiver(this, accentReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        applyIslandHeaderColor()
        setupBottomNav()
        loadDownloadsData()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(accentReceiver) } catch (_: Exception) {}
    }

    private fun applyIslandHeaderColor() {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)
        val density = resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24 * density
            setColor(accentColor)
        }
        binding.layoutHeaderIsland.background = bg
    }

    private fun setupSystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutDownloadsRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density

            val lpHeader = binding.layoutHeaderIsland.layoutParams as ViewGroup.MarginLayoutParams
            val targetHeaderTop = systemBars.top + (10 * density).toInt()
            if (lpHeader.topMargin != targetHeaderTop) {
                lpHeader.topMargin = targetHeaderTop
                binding.layoutHeaderIsland.layoutParams = lpHeader
            }

            val navView = findViewById<View>(R.id.blurBottomNav)
            if (navView != null && navView.visibility == View.VISIBLE) {
                val lpNav = navView.layoutParams as? ViewGroup.MarginLayoutParams
                if (lpNav != null) {
                    val targetNavBottom = systemBars.bottom + (16 * density).toInt()
                    if (lpNav.bottomMargin != targetNavBottom) {
                        lpNav.bottomMargin = targetNavBottom
                        navView.layoutParams = lpNav
                    }
                }
            }

            val hasBottomNav = navView != null && navView.visibility == View.VISIBLE
            val bottomPadding = systemBars.bottom + ((if (hasBottomNav) 88 else 24) * density).toInt()

            binding.rvDownloads.setPadding(
                binding.rvDownloads.paddingLeft,
                binding.rvDownloads.paddingTop,
                binding.rvDownloads.paddingRight,
                bottomPadding
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.layoutDownloadsRoot)
    }

    private fun setupHeaderAndSearch() {
        val sp = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        isGridMode = sp.getBoolean("downloads_view_mode_grid", true)

        updateViewModeLayout()

        binding.btnToggleViewMode.setOnClickListener {
            it.animate().rotationBy(180f).setDuration(220).start()
            isGridMode = !isGridMode
            sp.edit().putBoolean("downloads_view_mode_grid", isGridMode).apply()
            updateViewModeLayout()
            filterAndRenderDownloads()
        }

        binding.btnBack.setOnClickListener {
            if (isTaskRoot) {
                val intent = Intent(this, ChatListActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
            finish()
            PrimeTransitions.applyCloseTransition(this)
        }

        binding.btnHeaderMore.setOnClickListener { showOptionsMenu(it) }
        binding.btnHeaderSearch.setOnClickListener { openHeaderSearch() }
        binding.btnHeaderSearchClear.setOnClickListener { closeHeaderSearch() }

        binding.etHeaderSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                filterAndRenderDownloads()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateViewModeLayout() {
        if (isGridMode) {
            binding.btnToggleViewMode.setImageResource(R.drawable.ic_format_list_bulleted)
            binding.btnToggleViewMode.contentDescription = "Показать списком"
            binding.rvDownloads.layoutManager = GridLayoutManager(this, 2)
        } else {
            binding.btnToggleViewMode.setImageResource(R.drawable.ic_format_table)
            binding.btnToggleViewMode.contentDescription = "Показать сеткой"
            binding.rvDownloads.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun openHeaderSearch() {
        binding.layoutTitleContainer.animate().cancel()
        binding.etHeaderSearch.animate().cancel()
        binding.btnHeaderSearch.animate().cancel()
        binding.btnToggleViewMode.animate().cancel()
        binding.btnHeaderMore.animate().cancel()

        binding.layoutTitleContainer.visibility = View.GONE
        binding.btnHeaderSearch.visibility = View.GONE
        binding.btnToggleViewMode.visibility = View.GONE
        binding.btnHeaderMore.visibility = View.GONE

        binding.etHeaderSearch.alpha = 0f
        binding.etHeaderSearch.translationX = 30f
        binding.etHeaderSearch.visibility = View.VISIBLE
        binding.etHeaderSearch.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.btnHeaderSearchClear.alpha = 0f
        binding.btnHeaderSearchClear.visibility = View.VISIBLE
        binding.btnHeaderSearchClear.animate().alpha(1f).setDuration(200).start()

        binding.etHeaderSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        @Suppress("DEPRECATION")
        imm?.showSoftInput(binding.etHeaderSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeHeaderSearch() {
        binding.etHeaderSearch.text?.clear()
        searchQuery = ""

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etHeaderSearch.windowToken, 0)
        binding.etHeaderSearch.clearFocus()

        binding.etHeaderSearch.visibility = View.GONE
        binding.btnHeaderSearchClear.visibility = View.GONE

        binding.layoutTitleContainer.alpha = 0f
        binding.layoutTitleContainer.visibility = View.VISIBLE
        binding.layoutTitleContainer.animate().alpha(1f).setDuration(180).start()

        binding.btnHeaderSearch.alpha = 0f
        binding.btnHeaderSearch.visibility = View.VISIBLE
        binding.btnHeaderSearch.animate().alpha(1f).setDuration(180).start()

        binding.btnToggleViewMode.alpha = 0f
        binding.btnToggleViewMode.visibility = View.VISIBLE
        binding.btnToggleViewMode.animate().alpha(1f).setDuration(180).start()

        binding.btnHeaderMore.alpha = 0f
        binding.btnHeaderMore.visibility = View.VISIBLE
        binding.btnHeaderMore.animate().alpha(1f).setDuration(180).start()

        filterAndRenderDownloads()
    }

    private fun setupFilterChips() {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)

        val updateChipStyles = {
            val chips = listOf(
                binding.chipAll to "ALL",
                binding.chipMedia to "MEDIA",
                binding.chipDocs to "DOCS",
                binding.chipMusic to "MUSIC",
                binding.chipUpdates to "UPDATES"
            )

            for ((btn, category) in chips) {
                if (category == activeCategoryFilter) {
                    btn.backgroundTintList = ColorStateList.valueOf(accentColor)
                    btn.setTextColor(Color.WHITE)
                    btn.strokeWidth = 0
                } else {
                    btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    btn.setTextColor(ContextCompat.getColor(this, R.color.prime_text_primary))
                    btn.strokeColor = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
                    btn.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                }
            }
        }

        val onChipClicked = { btn: MaterialButton, category: String ->
            btn.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction {
                btn.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).setInterpolator(OvershootInterpolator(1.2f)).start()
            }.start()
            activeCategoryFilter = category
            updateChipStyles()
            filterAndRenderDownloads()
        }

        binding.chipAll.setOnClickListener { onChipClicked(binding.chipAll, "ALL") }
        binding.chipMedia.setOnClickListener { onChipClicked(binding.chipMedia, "MEDIA") }
        binding.chipDocs.setOnClickListener { onChipClicked(binding.chipDocs, "DOCS") }
        binding.chipMusic.setOnClickListener { onChipClicked(binding.chipMusic, "MUSIC") }
        binding.chipUpdates.setOnClickListener { onChipClicked(binding.chipUpdates, "UPDATES") }

        updateChipStyles()
    }

    private fun setupStorageSummaryActions() {
        binding.cardStorageSummary.setOnClickListener {
            showStorageDetailsDialog()
        }
        binding.btnClearDownloads.setOnClickListener {
            showClearDownloadsDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        TransferManager.registerListener(transferListener)
    }

    override fun onStop() {
        super.onStop()
        TransferManager.unregisterListener(transferListener)
    }

    private fun setupRecyclerView() {
        activeTransfersAdapter = ActiveTransfersAdapter { item ->
            TransferManager.cancelTransfer(item.id)
            PrimeNotification.show(this, "Передача файла отменена")
        }
        binding.rvActiveTransfers.layoutManager = LinearLayoutManager(this)
        binding.rvActiveTransfers.adapter = activeTransfersAdapter

        adapter = DownloadsAdapter(
            onOpenItem = { item -> openFile(item) },
            onShareItem = { item -> shareFile(item) },
            onMoreOptions = { view, item -> showItemOptionsMenu(view, item) }
        )
        binding.rvDownloads.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 200
            removeDuration = 180
        }
        updateViewModeLayout()
        binding.rvDownloads.adapter = adapter
    }

    private fun loadDownloadsData() {
        allDownloadsList = DownloadsManager.getDownloadsForCurrentUser(this)

        val totalSize = allDownloadsList.sumOf { it.fileSize }
        val formattedSize = DownloadsManager.formatFileSize(totalSize)
        binding.tvStorageUsage.text = "$formattedSize • ${allDownloadsList.size} ф."

        binding.tvStorageDetails.text = "Занято: $formattedSize • ${allDownloadsList.size} файлов"

        // Smoothly animate progress bar
        val maxScale = 1000L * 1024L * 1024L // 1 GB scale reference
        val targetProgress = ((totalSize.toDouble() / maxScale.toDouble()) * 100).toInt().coerceIn(4, 100)

        ValueAnimator.ofInt(binding.pbStorageBar.progress, targetProgress).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                binding.pbStorageBar.progress = anim.animatedValue as Int
            }
            start()
        }

        filterAndRenderDownloads()
    }

    private fun filterAndRenderDownloads() {
        val query = searchQuery.lowercase(Locale.ROOT).trim()
        val queryTokens = query.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        var filtered = allDownloadsList.filter { item ->
            val matchesCategory = when (activeCategoryFilter) {
                "MEDIA" -> item.mimeType.startsWith("image") || item.mimeType.startsWith("video") || item.fileName.endsWith(".gif")
                "DOCS" -> !item.mimeType.startsWith("image") && !item.mimeType.startsWith("video") && !item.mimeType.startsWith("audio") && !item.fileName.endsWith(".apk")
                "MUSIC" -> item.mimeType.startsWith("audio") || item.fileName.endsWith(".mp3") || item.fileName.endsWith(".wav")
                "UPDATES" -> item.sourceType == DownloadSourceType.UPDATE || item.fileName.endsWith(".apk")
                else -> true
            }

            val matchesSearch = if (queryTokens.isEmpty()) true else {
                val fileName = item.fileName.lowercase(Locale.ROOT)
                val sourceName = item.sourceName.lowercase(Locale.ROOT)
                val mimeType = item.mimeType.lowercase(Locale.ROOT)
                val formattedSize = DownloadsManager.formatFileSize(item.fileSize).lowercase(Locale.ROOT)
                val formattedDate = DownloadsManager.formatDate(item.timestamp).lowercase(Locale.ROOT)

                val categoryAlias = when {
                    mimeType.startsWith("image") || fileName.endsWith(".gif") -> "фото медиа картинка изображение гиф gif photo image"
                    mimeType.startsWith("video") -> "видео медиа ролик mp4 video"
                    mimeType.startsWith("audio") -> "аудио музыка трек песню mp3 wav music audio"
                    fileName.endsWith(".apk") -> "апк аппка обновление инсталлер приложение apk app update"
                    else -> "документ файл текст doc pdf text file"
                }

                val searchableText = "$fileName $sourceName $mimeType $formattedSize $formattedDate $categoryAlias"

                queryTokens.all { token -> searchableText.contains(token) }
            }

            matchesCategory && matchesSearch
        }

        filtered = when (currentSortMode) {
            "DATE_ASC" -> filtered.sortedBy { it.timestamp }
            "SIZE_DESC" -> filtered.sortedByDescending { it.fileSize }
            "SIZE_ASC" -> filtered.sortedBy { it.fileSize }
            "NAME_ASC" -> filtered.sortedBy { it.fileName.lowercase(Locale.ROOT) }
            "NAME_DESC" -> filtered.sortedByDescending { it.fileName.lowercase(Locale.ROOT) }
            else -> filtered.sortedByDescending { it.timestamp }
        }

        adapter.setItems(filtered, isGridMode)

        if (filtered.isEmpty()) {
            if (binding.layoutEmptyState.visibility != View.VISIBLE) {
                binding.layoutEmptyState.alpha = 0f
                binding.layoutEmptyState.scaleX = 0.9f
                binding.layoutEmptyState.scaleY = 0.9f
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.layoutEmptyState.animate()
                    .alpha(1f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(260)
                    .setInterpolator(OvershootInterpolator(1.1f))
                    .start()
            }
        } else {
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun showSortMenu(view: View) {
        PrimePopupMenu.create(this, view)
            .addItem("По дате (сначала новые)", R.drawable.ic_clock) {
                currentSortMode = "DATE_DESC"
                filterAndRenderDownloads()
            }
            .addItem("По дате (сначала старые)", R.drawable.ic_clock) {
                currentSortMode = "DATE_ASC"
                filterAndRenderDownloads()
            }
            .addItem("По размеру (крупные сначала)", R.drawable.ic_file) {
                currentSortMode = "SIZE_DESC"
                filterAndRenderDownloads()
            }
            .addItem("По размеру (мелкие сначала)", R.drawable.ic_file) {
                currentSortMode = "SIZE_ASC"
                filterAndRenderDownloads()
            }
            .addItem("По имени (А - Я)", R.drawable.ic_format_align_left) {
                currentSortMode = "NAME_ASC"
                filterAndRenderDownloads()
            }
            .addItem("По имени (Я - А)", R.drawable.ic_format_align_left) {
                currentSortMode = "NAME_DESC"
                filterAndRenderDownloads()
            }
            .show()
    }

    private fun showOptionsMenu(view: View) {
        PrimePopupMenu.create(this, view)
            .addItem("Сортировка файлов", R.drawable.ic_sort) { showSortMenu(view) }
            .addItem(if (isGridMode) "Сменить на список" else "Сменить на сетку", if (isGridMode) R.drawable.ic_format_list_bulleted else R.drawable.ic_format_table) {
                binding.btnToggleViewMode.animate().rotationBy(180f).setDuration(220).start()
                isGridMode = !isGridMode
                getSharedPreferences("PrimeLocalDB", MODE_PRIVATE).edit().putBoolean("downloads_view_mode_grid", isGridMode).apply()
                updateViewModeLayout()
                filterAndRenderDownloads()
            }
            .addItem("Сведения о хранилище", R.drawable.ic_download) { showStorageDetailsDialog() }
            .addItem("Очистить список загрузок", R.drawable.ic_trash) { showClearDownloadsDialog() }
            .show()
    }

    private fun showStorageDetailsDialog() {
        val totalSize = allDownloadsList.sumOf { it.fileSize }
        val mediaSize = allDownloadsList.filter { it.mimeType.startsWith("image") || it.mimeType.startsWith("video") || it.fileName.endsWith(".gif") }.sumOf { it.fileSize }
        val docsSize = allDownloadsList.filter { !it.mimeType.startsWith("image") && !it.mimeType.startsWith("video") && !it.mimeType.startsWith("audio") && !it.fileName.endsWith(".apk") }.sumOf { it.fileSize }
        val musicSize = allDownloadsList.filter { it.mimeType.startsWith("audio") || it.fileName.endsWith(".mp3") || it.fileName.endsWith(".wav") }.sumOf { it.fileSize }
        val updatesSize = allDownloadsList.filter { it.sourceType == DownloadSourceType.UPDATE || it.fileName.endsWith(".apk") }.sumOf { it.fileSize }

        val density = resources.displayMetrics.density
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
        }

        val detailsText = """
            • Всего файлов: ${allDownloadsList.size} (${DownloadsManager.formatFileSize(totalSize)})
            
            • Медиа (фото и видео): ${DownloadsManager.formatFileSize(mediaSize)}
            • Документы и файлы: ${DownloadsManager.formatFileSize(docsSize)}
            • Музыка и аудио: ${DownloadsManager.formatFileSize(musicSize)}
            • Обновления ПО: ${DownloadsManager.formatFileSize(updatesSize)}
        """.trimIndent()

        val tvInfo = TextView(this).apply {
            this.text = detailsText
            textSize = 13.5f
            setTextColor(ContextCompat.getColor(this@DownloadsActivity, R.color.prime_text_primary))
            setLineSpacing(0f, 1.3f)
        }
        container.addView(tvInfo)

        PrimeBlurDialog.show(
            activity = this,
            title = "Сведения о хранилище",
            customView = container,
            positiveText = "Очистить список",
            negativeText = "Закрыть",
            iconRes = R.drawable.ic_download,
            onPositive = { showClearDownloadsDialog() }
        )
    }

    private fun showClearDownloadsDialog() {
        val density = resources.displayMetrics.density
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
        }
        val tvText = TextView(this).apply {
            text = "Выберите вариант очистки загруженных файлов:"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@DownloadsActivity, R.color.prime_text_primary))
        }
        container.addView(tvText)

        PrimeBlurDialog.show(
            activity = this,
            title = "Очистка загрузок",
            customView = container,
            positiveText = "Удалить все файлы",
            negativeText = "Только очистить список",
            iconRes = R.drawable.ic_trash,
            onPositive = {
                for (item in allDownloadsList) {
                    DownloadsManager.deleteDownload(this, item)
                }
                loadDownloadsData()
                PrimeNotification.show(this, "Все файлы и история удалены")
            },
            onNegative = {
                val sp = getSharedPreferences("PrimeDownloadsDB", MODE_PRIVATE)
                sp.edit().putString("downloads_list_v2", "[]").apply()
                loadDownloadsData()
                PrimeNotification.show(this, "Список загрузок очищен")
            }
        )
    }

    private fun showItemOptionsMenu(view: View, item: DownloadItem) {
        PrimePopupMenu.create(this, view)
            .addItem("Открыть", R.drawable.ic_file) { openFile(item) }
            .addItem("Поделиться", R.drawable.ic_share) { shareFile(item) }
            .addItem("Скопировать путь", R.drawable.ic_copy) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("FilePath", item.filePath))
                PrimeNotification.show(this, "Путь к файлу скопирован")
            }
            .addItem("Сведения о файле", R.drawable.ic_search) { showFileItemDetailsDialog(item) }
            .addItem("Удалить файл", R.drawable.ic_trash) { deleteFileItemWithUndo(item) }
            .show()
    }

    private fun showFileItemDetailsDialog(item: DownloadItem) {
        val density = resources.displayMetrics.density
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), (8 * density).toInt(), (16 * density).toInt(), (8 * density).toInt())
        }

        val detailsStr = """
            Имя: ${item.fileName}
            Размер: ${DownloadsManager.formatFileSize(item.fileSize)}
            Тип: ${item.mimeType}
            Источник: ${item.sourceName}
            Дата: ${DownloadsManager.formatDate(item.timestamp)}
            
            Путь: ${item.filePath}
        """.trimIndent()

        val tvDetails = TextView(this).apply {
            text = detailsStr
            textSize = 13.5f
            setTextColor(ContextCompat.getColor(this@DownloadsActivity, R.color.prime_text_primary))
            setLineSpacing(0f, 1.25f)
        }
        container.addView(tvDetails)

        PrimeBlurDialog.show(
            activity = this,
            title = "Сведения о файле",
            customView = container,
            positiveText = "Открыть",
            negativeText = "Закрыть",
            iconRes = R.drawable.ic_file,
            onPositive = { openFile(item) }
        )
    }

    private fun deleteFileItemWithUndo(item: DownloadItem) {
        DownloadsManager.deleteDownload(this, item)
        loadDownloadsData()

        PrimeNotification.show(this, "Файл «${item.fileName}» удален", onUndo = {
            DownloadsManager.addDownload(
                context = this,
                fileName = item.fileName,
                filePath = item.filePath,
                fileSize = item.fileSize,
                mimeType = item.mimeType,
                sourceType = item.sourceType,
                sourceName = item.sourceName
            )
            loadDownloadsData()
        })
    }

    private fun openFile(item: DownloadItem) {
        val file = File(item.filePath)
        if (!file.exists()) {
            PrimeNotification.show(this, "Файл не найден на устройстве")
            return
        }

        val lowerName = item.fileName.lowercase(Locale.ROOT)
        val mime = item.mimeType

        if (lowerName.endsWith(".apk")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
                openWithPermissionCheck(this)
                return
            }
            try {
                val apkUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                PrimeNotification.show(this, "Ошибка запуска установки: ${e.localizedMessage}")
            }
        } else if (mime.startsWith("image") || mime.startsWith("video") || lowerName.endsWith(".gif")) {
            val intent = Intent(this, MediaPlayerActivity::class.java).apply {
                putExtra("MEDIA_PATH", file.absolutePath)
                putExtra("MEDIA_TYPE", if (mime.startsWith("video")) "video" else "image")
            }
            PrimeTransitions.startActivityWithTransition(this, intent)
        } else if (mime.startsWith("audio") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav")) {
            val trackItem = PrimeMusicManager.extractMetadata(this, file.absolutePath)
            PrimeMusicManager.playTrack(this, trackItem, listOf(trackItem))
            PrimeMusicPlayerDialog(this).show(trackItem, listOf(trackItem))
        } else {
            try {
                val contentUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Открыть файл"))
            } catch (e: Exception) {
                PrimeNotification.show(this, "Не удалось открыть файл")
            }
        }
    }

    private fun shareFile(item: DownloadItem) {
        val file = File(item.filePath)
        if (!file.exists()) {
            PrimeNotification.show(this, "Файл не найден на устройстве")
            return
        }
        try {
            val contentUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Поделиться файлом"))
        } catch (e: Exception) {
            PrimeNotification.show(this, "Ошибка отправки: ${e.localizedMessage}")
        }
    }

    // =========================================================================
    //                 НИЖНЯЯ ПАНЕЛЬ НАВИГАЦИИ (M3E BOTTOM NAV PANEL)
    // =========================================================================

    private fun setupBottomNav() {
        val sharedPrefs = getSharedPreferences("PrimeLocalDB", MODE_PRIVATE)
        val navStyle = sharedPrefs.getString("navigation_style", "bottom_bar") ?: "bottom_bar"
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
            binding.rvDownloads.setPadding(
                binding.rvDownloads.paddingLeft,
                binding.rvDownloads.paddingTop,
                binding.rvDownloads.paddingRight,
                (24 * density).toInt()
            )
            return
        }

        navView.visibility = View.VISIBLE
        navView.bringToFront()

        val insets = ViewCompat.getRootWindowInsets(binding.layoutDownloadsRoot) ?: ViewCompat.getRootWindowInsets(window.decorView)
        val systemBarsBottom = insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0

        val lp = navView.layoutParams as? ViewGroup.MarginLayoutParams
        if (lp != null) {
            val targetMargin = systemBarsBottom + (16 * density).toInt()
            if (lp.bottomMargin != targetMargin) {
                lp.bottomMargin = targetMargin
                navView.layoutParams = lp
            }
        }

        binding.rvDownloads.setPadding(
            binding.rvDownloads.paddingLeft,
            binding.rvDownloads.paddingTop,
            binding.rvDownloads.paddingRight,
            systemBarsBottom + (88 * density).toInt()
        )

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

        applyBottomNavLabelsVisibility(navView, sharedPrefs.getBoolean("nav_show_labels", false))

        updateM3TabState(false, vNavChatsIndicator, ivNavChatsIcon, accentColor)
        updateM3TabState(false, vNavDevicesIndicator, ivNavDevicesIcon, accentColor)
        updateM3TabState(false, vNavAiIndicator, ivNavAiIcon, accentColor)
        updateM3TabState(false, vNavSearchIndicator, ivNavSearchIcon, accentColor)
        updateM3TabState(true, vNavDownloadsIndicator, ivNavDownloadsIcon, accentColor)
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
            val overlayColor = com.messenger.prime.getDefaultBlurOverlayColor(this)
            val targetRootView = (window.decorView.findViewById<ViewGroup>(android.R.id.content))
                ?: (window.decorView as ViewGroup)
            blurNavView.setupBlur(targetRootView, 18f, overlayColor, window.decorView.background)
        }

        val hapticTap = { view: View ->
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }

        btnNavChats?.setOnClickListener { v ->
            hapticTap(v)
            val intent = Intent(this, ChatListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = false)
            finish()
        }

        btnNavDevices?.setOnClickListener { v ->
            hapticTap(v)
            val intent = Intent(this, ChatListActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("OPEN_DEVICE_SCAN", true)
            }
            startActivity(intent)
            finish()
        }

        btnNavAi?.setOnClickListener { v ->
            hapticTap(v)
            val intent = Intent(this, AiChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyOpenTransition(this)
            finish()
        }

        btnNavSearch?.setOnClickListener { v ->
            hapticTap(v)
            val intent = Intent(this, GlobalSearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyPageFlipTransition(this, isForward = true)
            finish()
        }

        btnNavDownloads?.setOnClickListener { v ->
            hapticTap(v)
            binding.rvDownloads.smoothScrollToPosition(0)
        }

        btnNavProfile?.setOnClickListener { v ->
            hapticTap(v)
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            PrimeTransitions.applyOpenTransition(this)
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
        val activeIconColor = if (isDark) Color.parseColor("#0F172A") else Color.WHITE

        indicatorView.visibility = View.VISIBLE

        if (isActive) {
            indicatorView.backgroundTintList = ColorStateList.valueOf(accentColor)
            indicatorView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(220)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()

            iconView?.setColorFilter(activeIconColor)
            iconView?.animate()?.scaleX(1.1f)?.scaleY(1.1f)?.setDuration(180)?.start()
        } else {
            indicatorView.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            indicatorView.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .alpha(0f)
                .setDuration(180)
                .start()

            iconView?.setColorFilter(if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B"))
            iconView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(180)?.start()
        }
    }

    // Active Transfers RecyclerView Adapter
    private class ActiveTransfersAdapter(
        private val onCancelTransfer: (ActiveTransfer) -> Unit
    ) : RecyclerView.Adapter<ActiveTransfersAdapter.ViewHolder>() {

        private var items = listOf<ActiveTransfer>()

        fun setItems(newItems: List<ActiveTransfer>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_active_transfer, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTransferFileName: TextView = itemView.findViewById(R.id.tvTransferFileName)
            private val tvTransferSource: TextView = itemView.findViewById(R.id.tvTransferSource)
            private val tvTransferPercent: TextView = itemView.findViewById(R.id.tvTransferPercent)
            private val pbTransferProgress: android.widget.ProgressBar = itemView.findViewById(R.id.pbTransferProgress)
            private val btnCancelActiveTransfer: MaterialButton = itemView.findViewById(R.id.btnCancelActiveTransfer)

            fun bind(item: ActiveTransfer) {
                tvTransferFileName.text = item.fileName
                tvTransferSource.text = item.sourceName
                val prefix = if (item.isSending) "Отправка..." else "Загрузка..."
                tvTransferPercent.text = "$prefix ${item.progress}%"
                pbTransferProgress.progress = item.progress

                btnCancelActiveTransfer.setOnClickListener { onCancelTransfer(item) }
            }
        }
    }

    // Downloads RecyclerView Adapter
    private class DownloadsAdapter(
        private val onOpenItem: (DownloadItem) -> Unit,
        private val onShareItem: (DownloadItem) -> Unit,
        private val onMoreOptions: (View, DownloadItem) -> Unit
    ) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

        private var items = listOf<DownloadItem>()
        private var isGrid = true

        fun setItems(newItems: List<DownloadItem>, isGridMode: Boolean) {
            items = newItems
            isGrid = isGridMode
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return if (isGrid) 1 else 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutRes = if (viewType == 1) R.layout.item_download_file_grid else R.layout.item_download_file
            val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)

            holder.itemView.alpha = 0f
            holder.itemView.translationY = 24f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setStartDelay((position * 20L).coerceAtMost(200L))
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val layoutIconHolder: View? = itemView.findViewById(R.id.layoutIconHolder)
            private val ivFileThumbnail: ImageView? = itemView.findViewById(R.id.ivFileThumbnail)
            private val ivFileIcon: ImageView? = itemView.findViewById(R.id.ivFileIcon)
            private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
            private val tvFileMeta: TextView = itemView.findViewById(R.id.tvFileMeta)
            private val tvSourceBadge: TextView = itemView.findViewById(R.id.tvSourceBadge)
            private val btnOpenFile: MaterialButton? = itemView.findViewById(R.id.btnOpenFile)
            private val btnShareFile: ImageButton? = itemView.findViewById(R.id.btnShareFile)
            private val btnMore: ImageButton? = itemView.findViewById(R.id.btnMore)

            fun bind(item: DownloadItem) {
                tvFileName.text = item.fileName

                val formattedSize = DownloadsManager.formatFileSize(item.fileSize)
                val formattedDate = DownloadsManager.formatDate(item.timestamp)
                tvFileMeta.text = "$formattedSize • $formattedDate"

                tvSourceBadge.text = item.sourceName

                val file = File(item.filePath)
                val mime = item.mimeType.lowercase(Locale.ROOT)
                val lowerName = item.fileName.lowercase(Locale.ROOT)

                val context = itemView.context
                val primaryColor = ColorAccentManager.getCurrentAccentColor(context)
                val (iconRes, iconColor) = when {
                    mime.startsWith("image") || lowerName.endsWith(".gif") -> R.drawable.ic_photo to primaryColor
                    mime.startsWith("video") -> R.drawable.ic_video to Color.parseColor("#3B82F6")
                    mime.startsWith("audio") || lowerName.endsWith(".mp3") -> R.drawable.ic_music to Color.parseColor("#AB47BC")
                    lowerName.endsWith(".apk") -> R.drawable.ic_download to Color.parseColor("#10B981")
                    else -> R.drawable.ic_file to Color.parseColor("#64748B")
                }

                ivFileIcon?.setImageResource(iconRes)
                ivFileIcon?.setColorFilter(iconColor)

                if (layoutIconHolder != null) {
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = (14 * context.resources.displayMetrics.density)
                        setColor(ColorUtils.setAlphaComponent(iconColor, 35))
                    }
                    layoutIconHolder.background = bgDrawable
                }

                if (file.exists() && (mime.startsWith("image") || mime.startsWith("video") || lowerName.endsWith(".gif"))) {
                    ivFileThumbnail?.visibility = View.VISIBLE
                    ivFileIcon?.visibility = View.GONE
                    Glide.with(itemView.context)
                        .load(file)
                        .centerCrop()
                        .into(ivFileThumbnail!!)
                } else {
                    ivFileThumbnail?.visibility = View.GONE
                    ivFileIcon?.visibility = View.VISIBLE
                }

                itemView.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).setInterpolator(OvershootInterpolator(1.1f)).start()
                        }
                    }
                    false
                }

                itemView.setOnClickListener { onOpenItem(item) }
                itemView.setOnLongClickListener {
                    onMoreOptions(it, item)
                    true
                }

                btnOpenFile?.setOnClickListener { onOpenItem(item) }
                btnShareFile?.setOnClickListener { onShareItem(item) }
                btnMore?.setOnClickListener { onMoreOptions(it, item) }
            }
        }
    }
}

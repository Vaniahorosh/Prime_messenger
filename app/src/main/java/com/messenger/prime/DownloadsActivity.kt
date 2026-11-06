package com.messenger.prime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
    private var searchQuery: String = ""

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

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorAccentManager.applyAccentToActivity(this)
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PrimeTransitions.applyOpenTransition(this)
        setupSystemBarInsets()
        setupHeaderAndSearch()
        setupFilterChips()
        setupRecyclerView()

        loadDownloadsData()
    }

    override fun onResume() {
        super.onResume()
        loadDownloadsData()
    }

    private fun setupSystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutDownloadsRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density

            val lpHeader = binding.layoutHeaderIsland.layoutParams as ViewGroup.MarginLayoutParams
            lpHeader.topMargin = systemBars.top + (8 * density).toInt()
            binding.layoutHeaderIsland.layoutParams = lpHeader

            binding.rvDownloads.setPadding(
                binding.rvDownloads.paddingLeft,
                binding.rvDownloads.paddingTop,
                binding.rvDownloads.paddingRight,
                systemBars.bottom + (16 * density).toInt()
            )
            insets
        }
    }

    private fun setupHeaderAndSearch() {
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

        binding.btnSearchClear.setOnClickListener {
            binding.etSearchDownloads.text?.clear()
        }

        binding.etSearchDownloads.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                binding.btnSearchClear.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                filterAndRenderDownloads()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        val accentColor = ColorAccentManager.getCurrentAccentColor(this)

        val updateChipStyles = {
            val chips = listOf<Pair<MaterialButton, String>>(
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

        binding.chipAll.setOnClickListener { activeCategoryFilter = "ALL"; updateChipStyles(); filterAndRenderDownloads() }
        binding.chipMedia.setOnClickListener { activeCategoryFilter = "MEDIA"; updateChipStyles(); filterAndRenderDownloads() }
        binding.chipDocs.setOnClickListener { activeCategoryFilter = "DOCS"; updateChipStyles(); filterAndRenderDownloads() }
        binding.chipMusic.setOnClickListener { activeCategoryFilter = "MUSIC"; updateChipStyles(); filterAndRenderDownloads() }
        binding.chipUpdates.setOnClickListener { activeCategoryFilter = "UPDATES"; updateChipStyles(); filterAndRenderDownloads() }

        updateChipStyles()
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
            Toast.makeText(this, "Передача отменена", Toast.LENGTH_SHORT).show()
        }
        binding.rvActiveTransfers.layoutManager = LinearLayoutManager(this)
        binding.rvActiveTransfers.adapter = activeTransfersAdapter

        adapter = DownloadsAdapter(
            onOpenItem = { item -> openFile(item) },
            onShareItem = { item -> shareFile(item) },
            onDeleteItem = { item -> deleteFileItem(item) }
        )
        binding.rvDownloads.layoutManager = LinearLayoutManager(this)
        binding.rvDownloads.adapter = adapter
    }

    private fun loadDownloadsData() {
        val currentUser = DownloadsManager.getCurrentUser(this)
        allDownloadsList = DownloadsManager.getDownloadsForCurrentUser(this)

        val totalSize = allDownloadsList.sumOf { it.fileSize }
        val formattedSize = DownloadsManager.formatFileSize(totalSize)
        val userLabel = if (currentUser.isNotEmpty()) "Пользователь: $currentUser" else "Текущий профиль"
        binding.tvStorageUsage.text = "$userLabel • Занято $formattedSize"

        filterAndRenderDownloads()
    }

    private fun filterAndRenderDownloads() {
        val filtered = allDownloadsList.filter { item ->
            val matchesCategory = when (activeCategoryFilter) {
                "MEDIA" -> item.mimeType.startsWith("image") || item.mimeType.startsWith("video") || item.fileName.endsWith(".gif")
                "DOCS" -> !item.mimeType.startsWith("image") && !item.mimeType.startsWith("video") && !item.mimeType.startsWith("audio") && !item.fileName.endsWith(".apk")
                "MUSIC" -> item.mimeType.startsWith("audio") || item.fileName.endsWith(".mp3") || item.fileName.endsWith(".wav")
                "UPDATES" -> item.sourceType == DownloadSourceType.UPDATE || item.fileName.endsWith(".apk")
                else -> true
            }

            val matchesSearch = if (searchQuery.isEmpty()) true else {
                item.fileName.lowercase(Locale.ROOT).contains(searchQuery.lowercase(Locale.ROOT)) ||
                item.sourceName.lowercase(Locale.ROOT).contains(searchQuery.lowercase(Locale.ROOT))
            }

            matchesCategory && matchesSearch
        }

        adapter.setItems(filtered)
        binding.tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openFile(item: DownloadItem) {
        val file = File(item.filePath)
        if (!file.exists()) {
            Toast.makeText(this, "Файл не найден на устройстве", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Не удалось запустить установщик: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Не удалось открыть файл: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareFile(item: DownloadItem) {
        val file = File(item.filePath)
        if (!file.exists()) {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFileItem(item: DownloadItem) {
        DownloadsManager.deleteDownload(this, item)
        Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show()
        loadDownloadsData()
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
        private val onDeleteItem: (DownloadItem) -> Unit
    ) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

        private var items = listOf<DownloadItem>()

        fun setItems(newItems: List<DownloadItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download_file, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivFileThumbnail: ImageView = itemView.findViewById(R.id.ivFileThumbnail)
            private val ivFileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
            private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
            private val tvSourceBadge: TextView = itemView.findViewById(R.id.tvSourceBadge)
            private val tvFileMeta: TextView = itemView.findViewById(R.id.tvFileMeta)
            private val btnShareFile: ImageButton = itemView.findViewById(R.id.btnShareFile)
            private val btnDeleteFile: ImageButton = itemView.findViewById(R.id.btnDeleteFile)

            fun bind(item: DownloadItem) {
                tvFileName.text = item.fileName
                tvSourceBadge.text = item.sourceName
                val formattedSize = DownloadsManager.formatFileSize(item.fileSize)
                val formattedDate = DownloadsManager.formatDate(item.timestamp)
                tvFileMeta.text = "$formattedSize • $formattedDate"

                val file = File(item.filePath)
                val mime = item.mimeType.lowercase(Locale.ROOT)

                if (file.exists() && (mime.startsWith("image") || item.fileName.endsWith(".gif"))) {
                    ivFileThumbnail.visibility = View.VISIBLE
                    ivFileIcon.visibility = View.GONE
                    Glide.with(itemView.context)
                        .load(file)
                        .centerCrop()
                        .into(ivFileThumbnail)
                } else {
                    ivFileThumbnail.visibility = View.GONE
                    ivFileIcon.visibility = View.VISIBLE

                    val iconRes = when {
                        mime.startsWith("video") -> R.drawable.ic_video
                        mime.startsWith("audio") -> R.drawable.ic_music
                        item.fileName.endsWith(".apk") -> R.drawable.ic_download
                        else -> R.drawable.ic_file
                    }
                    ivFileIcon.setImageResource(iconRes)
                }

                itemView.setOnClickListener { onOpenItem(item) }
                btnShareFile.setOnClickListener { onShareItem(item) }
                btnDeleteFile.setOnClickListener { onDeleteItem(item) }
            }
        }
    }
}

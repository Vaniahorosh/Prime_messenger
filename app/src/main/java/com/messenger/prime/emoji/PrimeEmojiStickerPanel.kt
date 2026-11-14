package com.messenger.prime.emoji

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.messenger.prime.R
import java.io.File

private enum class PanelTab {
    EMOJIS, STICKERS
}

@Composable
fun PrimeEmojiStickerPanel(
    modifier: Modifier = Modifier,
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (path: String, packId: String, packName: String) -> Unit,
    onAddStickerRequested: (packId: String) -> Unit,
    onCloseRequested: () -> Unit,
    onResizeRequested: ((Float) -> Unit)? = null,
    onDeleteBackwardRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()

    val dialogBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val panelBorder = if (isDark) Color(0x3394A3B8) else Color(0x260F172A)
    val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val brandAccent = if (isDark) Color(0xFF4D9FFF) else Color(0xFF154B87)
    val searchBg = if (isDark) Color(0x33000000) else Color(0x1F000000)

    var currentTab by remember { mutableStateOf(PanelTab.EMOJIS) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val defaultCategory = if (EmojiStickerSettings.isStartWithRecents(context)) {
        EmojiCategory.RECENT
    } else {
        EmojiCategory.SMILEYS
    }
    var selectedCategory by remember { mutableStateOf(defaultCategory) }

    // Settings state
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showClearRecentsConfirm by remember { mutableStateOf(false) }
    var stickerSizeDp by remember { mutableIntStateOf(EmojiStickerSettings.getStickerSizeDp(context)) }
    var emojiSizeSp by remember { mutableIntStateOf(EmojiStickerSettings.getEmojiGridSizeSp(context)) }
    var isHapticEnabled by remember { mutableStateOf(EmojiStickerSettings.isHapticEnabled(context)) }
    var isShowPackTag by remember { mutableStateOf(EmojiStickerSettings.isShowPackTag(context)) }
    var isAutoCloseOnSticker by remember { mutableStateOf(EmojiStickerSettings.isAutoCloseOnStickerSend(context)) }
    var isStartWithRecents by remember { mutableStateOf(EmojiStickerSettings.isStartWithRecents(context)) }

    fun performFeedback() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Sticker Packs state
    var stickerPacks by remember { mutableStateOf(emptyList<StickerPack>()) }
    var selectedPackId by remember { mutableStateOf("") }
    var showCreatePackDialog by remember { mutableStateOf(false) }
    var packToEdit by remember { mutableStateOf<StickerPack?>(null) }
    var packToDelete by remember { mutableStateOf<StickerPack?>(null) }

    // Fullscreen Blur Sticker Preview state
    var previewStickerPath by remember { mutableStateOf<String?>(null) }

    fun refreshPacks() {
        val packs = StickerManager.getStickerPacks(context)
        stickerPacks = packs
        if (selectedPackId.isEmpty() && packs.isNotEmpty()) {
            selectedPackId = packs[0].id
        } else if (packs.none { it.id == selectedPackId } && packs.isNotEmpty()) {
            selectedPackId = packs[0].id
        }
    }

    LaunchedEffect(Unit) {
        refreshPacks()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
        ) {
            // 0. Drag Handle Bar: fluid resizing both upwards and downwards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 6.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            onResizeRequested?.invoke(-dragAmount)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(textSecondary.copy(alpha = 0.45f))
                )
            }

            // 1. Top Bar: Segmented Switcher & Settings Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Tabs Pill
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(searchBg)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabButton(
                        text = "Эмодзи",
                        selected = currentTab == PanelTab.EMOJIS,
                        accentColor = brandAccent,
                        textColor = textPrimary,
                        onClick = {
                            performFeedback()
                            currentTab = PanelTab.EMOJIS
                        }
                    )
                    TabButton(
                        text = "Стикеры",
                        selected = currentTab == PanelTab.STICKERS,
                        accentColor = brandAccent,
                        textColor = textPrimary,
                        onClick = {
                            performFeedback()
                            currentTab = PanelTab.STICKERS
                            refreshPacks()
                        }
                    )
                }

                // Settings button only
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(searchBg)
                        .clickable {
                            performFeedback()
                            showSettingsDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Настройки",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. Tab Content with Crossfade transition
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(durationMillis = 180),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "panel_tab_crossfade"
            ) { tab ->
                when (tab) {
                    PanelTab.EMOJIS -> {
                        EmojiSectionContent(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onFocusChanged = { isSearchFocused = it },
                            selectedCategory = selectedCategory,
                            onCategorySelected = {
                                performFeedback()
                                selectedCategory = it
                                searchQuery = ""
                            },
                            onEmojiClick = { emoji ->
                                performFeedback()
                                EmojiData.addRecentEmoji(context, emoji)
                                onEmojiSelected(emoji)
                            },
                            searchBg = searchBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            brandAccent = brandAccent,
                            emojiSizeSp = emojiSizeSp
                        )
                    }
                    PanelTab.STICKERS -> {
                        StickersSectionContent(
                            packs = stickerPacks,
                            selectedPackId = selectedPackId,
                            onPackSelected = { packId ->
                                performFeedback()
                                selectedPackId = packId
                            },
                            onCreatePackClick = {
                                performFeedback()
                                showCreatePackDialog = true
                            },
                            onAddStickerClick = { packId ->
                                performFeedback()
                                onAddStickerRequested(packId)
                            },
                            onStickerClick = { path, packId, packName ->
                                performFeedback()
                                onStickerSelected(path, packId, packName)
                            },
                            onEditPackRequested = { pack ->
                                performFeedback()
                                packToEdit = pack
                            },
                            onPreviewStickerChanged = { path ->
                                if (path != previewStickerPath) {
                                    previewStickerPath = path
                                    if (path != null) performFeedback()
                                }
                            },
                            brandAccent = brandAccent,
                            panelBorder = panelBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            searchBg = searchBg
                        )
                    }
                }
            }
        }

        // 3. Floating M3E Backspace button on bottom right
        AnimatedVisibility(
            visible = currentTab == PanelTab.EMOJIS,
            enter = slideInVertically(animationSpec = tween(320)) { -it * 3 } + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(animationSpec = tween(240)) { it * 2 } + fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF233247) else Color(0xFFFFFFFF))
                    .border(1.dp, brandAccent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable {
                        performFeedback()
                        onDeleteBackwardRequested()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_backspace),
                    contentDescription = "Удалить символ",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 4. TRUE FULLSCREEN BLUR STICKER PREVIEW DIALOG (covers whole screen, no text labels)
        if (previewStickerPath != null) {
            Dialog(
                onDismissRequest = { previewStickerPath = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                val dialogView = LocalView.current
                DisposableEffect(dialogView) {
                    val window = (dialogView.parent as? DialogWindowProvider)?.window
                    if (window != null) {
                        window.setLayout(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT
                        )
                        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                        window.addFlags(
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                                val p = window.attributes
                                p.blurBehindRadius = 60
                                window.attributes = p
                            } catch (_: Throwable) {}
                        }
                    }
                    onDispose {}
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        previewStickerPath?.let { path ->
                            StickerThumbnail(
                                path = path,
                                modifier = Modifier.fillMaxSize(),
                                previewResolution = 512
                            )
                        }
                    }
                }
            }
        }
    }

    // Detailed Pack Editor Dialog
    packToEdit?.let { pack ->
        PackDetailsDialog(
            pack = pack,
            allPacks = stickerPacks,
            dialogBg = dialogBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            brandAccent = brandAccent,
            panelBorder = panelBorder,
            searchBg = searchBg,
            onAddStickerClick = {
                onAddStickerRequested(pack.id)
            },
            onDeleteSticker = { path ->
                performFeedback()
                StickerManager.deleteSticker(context, pack.id, path)
                val updated = StickerManager.getPackById(context, pack.id)
                packToEdit = updated
                refreshPacks()
            },
            onSetCover = { path ->
                performFeedback()
                StickerManager.setPackCover(context, pack.id, path)
                val updated = StickerManager.getPackById(context, pack.id)
                packToEdit = updated
                refreshPacks()
            },
            onReorderStickers = { fromIndex, toIndex ->
                performFeedback()
                StickerManager.reorderStickersInPack(context, pack.id, fromIndex, toIndex)
                val updated = StickerManager.getPackById(context, pack.id)
                packToEdit = updated
                refreshPacks()
            },
            onReorderPack = { fromIndex, toIndex ->
                performFeedback()
                StickerManager.reorderPacks(context, fromIndex, toIndex)
                refreshPacks()
            },
            onDeletePack = {
                performFeedback()
                packToDelete = pack
                packToEdit = null
            },
            onSave = { newName, newAuthor ->
                performFeedback()
                StickerManager.updatePackDetails(context, pack.id, newName, newAuthor)
                refreshPacks()
                packToEdit = null
            },
            onDismiss = { packToEdit = null }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        EmojiStickerSettingsDialog(
            dialogBg = dialogBg,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            brandAccent = brandAccent,
            searchBg = searchBg,
            stickerSizeDp = stickerSizeDp,
            onStickerSizeChanged = { newSizeDp ->
                performFeedback()
                stickerSizeDp = newSizeDp
                EmojiStickerSettings.setStickerSizeDp(context, newSizeDp)
            },
            emojiSizeSp = emojiSizeSp,
            onEmojiSizeChanged = { newSizeSp ->
                performFeedback()
                emojiSizeSp = newSizeSp
                EmojiStickerSettings.setEmojiGridSizeSp(context, newSizeSp)
            },
            isHapticEnabled = isHapticEnabled,
            onHapticChanged = { enabled ->
                isHapticEnabled = enabled
                EmojiStickerSettings.setHapticEnabled(context, enabled)
                if (enabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            },
            isShowPackTag = isShowPackTag,
            onShowPackTagChanged = { show ->
                performFeedback()
                isShowPackTag = show
                EmojiStickerSettings.setShowPackTag(context, show)
            },
            isAutoCloseOnSticker = isAutoCloseOnSticker,
            onAutoCloseOnStickerChanged = { autoClose ->
                performFeedback()
                isAutoCloseOnSticker = autoClose
                EmojiStickerSettings.setAutoCloseOnStickerSend(context, autoClose)
            },
            isStartWithRecents = isStartWithRecents,
            onStartWithRecentsChanged = { startRecents ->
                performFeedback()
                isStartWithRecents = startRecents
                EmojiStickerSettings.setStartWithRecents(context, startRecents)
            },
            onClearRecentsClick = {
                performFeedback()
                showClearRecentsConfirm = true
            },
            totalPacks = stickerPacks.size,
            totalStickers = stickerPacks.sumOf { it.stickerPaths.size },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Confirm Clear Recents Dialog
    if (showClearRecentsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearRecentsConfirm = false },
            containerColor = dialogBg,
            title = {
                Text(
                    text = "Очистить недавние эмодзи?",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                )
            },
            text = {
                Text(
                    text = "Список часто используемых смайликов будет сброшен к начальному набору.",
                    style = TextStyle(fontSize = 13.sp, color = textSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performFeedback()
                        EmojiStickerSettings.clearRecents(context)
                        showClearRecentsConfirm = false
                    }
                ) {
                    Text("Очистить", color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearRecentsConfirm = false }) {
                    Text("Отмена", color = textSecondary)
                }
            }
        )
    }

    // Create Pack Dialog
    if (showCreatePackDialog) {
        var packTitle by remember { mutableStateOf("") }
        var packAuthor by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePackDialog = false },
            containerColor = dialogBg,
            title = {
                Text(
                    text = "Новый стикерпак",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Введите название и автора для авторского набора стикеров",
                        style = TextStyle(fontSize = 13.sp, color = textSecondary)
                    )
                    OutlinedTextField(
                        value = packTitle,
                        onValueChange = { packTitle = it },
                        placeholder = { Text("Название набора...", style = TextStyle(fontSize = 14.sp, color = textSecondary)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandAccent,
                            unfocusedBorderColor = panelBorder,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = packAuthor,
                        onValueChange = { packAuthor = it },
                        placeholder = { Text("Автор (по умолчанию: Вы)", style = TextStyle(fontSize = 14.sp, color = textSecondary)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandAccent,
                            unfocusedBorderColor = panelBorder,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (packTitle.isNotBlank()) {
                            val created = StickerManager.createPack(context, packTitle, packAuthor)
                            refreshPacks()
                            selectedPackId = created.id
                        }
                        showCreatePackDialog = false
                    }
                ) {
                    Text("Создать", color = brandAccent, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePackDialog = false }) {
                    Text("Отмена", color = textSecondary)
                }
            }
        )
    }

    // Delete Pack Confirmation Dialog
    packToDelete?.let { pack ->
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            containerColor = dialogBg,
            title = {
                Text(
                    text = "Удалить стикерпак?",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                )
            },
            text = {
                Text(
                    text = "Набор «${pack.name}» и все его стикеры будут удалены без возможности восстановления.",
                    style = TextStyle(fontSize = 13.sp, color = textSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        StickerManager.deletePack(context, pack.id)
                        refreshPacks()
                        packToDelete = null
                    }
                ) {
                    Text("Удалить", color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { packToDelete = null }) {
                    Text("Отмена", color = textSecondary)
                }
            }
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (selected) accentColor else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "tab_btn_bg"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (selected) Color.White else textColor,
        animationSpec = tween(durationMillis = 180),
        label = "tab_btn_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = animatedTextColor
            )
        )
    }
}

@Composable
private fun EmojiSectionContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    selectedCategory: EmojiCategory,
    onCategorySelected: (EmojiCategory) -> Unit,
    onEmojiClick: (String) -> Unit,
    searchBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    brandAccent: Color,
    emojiSizeSp: Int
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(searchBg)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "Поиск",
                tint = textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Поиск эмодзи (огонь, кот, сердце...)",
                        style = TextStyle(fontSize = 13.sp, color = textSecondary)
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(fontSize = 13.sp, color = textPrimary),
                    singleLine = true,
                    cursorBrush = SolidColor(brandAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                )
            }
            if (searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onSearchQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Очистить",
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 2. Categories row
        AnimatedVisibility(visible = searchQuery.isBlank()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(EmojiCategory.entries) { category ->
                    val isSelected = category == selectedCategory
                    val animatedPillBg by animateColorAsState(
                        targetValue = if (isSelected) brandAccent.copy(alpha = 0.15f) else Color.Transparent,
                        animationSpec = tween(160),
                        label = "cat_pill_bg"
                    )
                    val animatedPillText by animateColorAsState(
                        targetValue = if (isSelected) brandAccent else textSecondary,
                        animationSpec = tween(160),
                        label = "cat_pill_text"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(animatedPillBg)
                            .clickable { onCategorySelected(category) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${category.icon} ${category.title}",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = animatedPillText
                            )
                        )
                    }
                }
            }
        }

        // 3. Grid of emojis
        val emojisToDisplay = remember(searchQuery, selectedCategory) {
            if (searchQuery.isNotBlank()) {
                EmojiData.search(searchQuery)
            } else {
                EmojiData.getByCategory(selectedCategory, context)
            }
        }

        if (emojisToDisplay.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Ничего не найдено",
                    style = TextStyle(fontSize = 13.sp, color = textSecondary)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = emojisToDisplay,
                    key = { index, item -> "$item-$index" }
                ) { _, emoji ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEmojiClick(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            style = TextStyle(fontSize = emojiSizeSp.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickersSectionContent(
    packs: List<StickerPack>,
    selectedPackId: String,
    onPackSelected: (String) -> Unit,
    onCreatePackClick: () -> Unit,
    onAddStickerClick: (packId: String) -> Unit,
    onStickerClick: (path: String, packId: String, packName: String) -> Unit,
    onEditPackRequested: (StickerPack) -> Unit,
    onPreviewStickerChanged: (path: String?) -> Unit,
    brandAccent: Color,
    panelBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    searchBg: Color
) {
    val context = LocalContext.current

    if (packs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stickers),
                    contentDescription = null,
                    tint = brandAccent,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "У вас пока нет стикерпаков",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                )
                Text(
                    text = "Создавайте авторские наборы, добавляйте PNG стикеры и видеостикеры",
                    style = TextStyle(fontSize = 12.sp, color = textSecondary),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(brandAccent)
                        .clickable(onClick = onCreatePackClick)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Создать стикерпак",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    )
                }
            }
        }
        return
    }

    val activePack = packs.find { it.id == selectedPackId } ?: packs.firstOrNull()
    val recentStickers = remember(packs) { StickerManager.getRecentStickers(context) }
    // Plain HashMap prevents recomposition storms on 120Hz scroll
    val stickerBounds = remember { mutableMapOf<String, Rect>() }
    var gridOffsetInWindow by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Packs Selector Row: Focus purely on cover icons (no text labels)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // New pack icon button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brandAccent.copy(alpha = 0.14f))
                    .clickable(onClick = onCreatePackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Новый стикерпак",
                    tint = brandAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Packs list: only cover icons
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(packs, key = { it.id }) { pack ->
                    val isSelected = pack.id == selectedPackId
                    val coverPath = pack.coverPath?.takeIf { File(it).exists() }
                        ?: pack.stickerPaths.firstOrNull { File(it).exists() }

                    val animatedBorderColor by animateColorAsState(
                        targetValue = if (isSelected) brandAccent else panelBorder.copy(alpha = 0.4f),
                        animationSpec = tween(160),
                        label = "pack_border_color"
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) brandAccent.copy(alpha = 0.15f) else searchBg)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = animatedBorderColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .combinedClickable(
                                onClick = { onPackSelected(pack.id) },
                                onLongClick = { onEditPackRequested(pack) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!coverPath.isNullOrEmpty()) {
                            StickerThumbnail(
                                path = coverPath,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_stickers),
                                contentDescription = pack.name,
                                tint = if (isSelected) brandAccent else textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Recent stickers row (if available)
        if (recentStickers.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Text(
                    text = "Недавние стикеры",
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textSecondary),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentStickers, key = { it }) { rPath ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(searchBg)
                                .combinedClickable(
                                    onClick = {
                                        val p = packs.find { it.stickerPaths.contains(rPath) }
                                        onStickerClick(rPath, p?.id ?: "", p?.name ?: "Стикеры")
                                    },
                                    onLongClick = {
                                        onPreviewStickerChanged(rPath)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            StickerThumbnail(path = rPath, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        // 3. Sticker Grid for Active Pack (120Hz optimized, zero recomposition storms)
        if (activePack != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activePack.name} • ${activePack.stickerPaths.size} стикеров",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(searchBg)
                            .clickable { onEditPackRequested(activePack) }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Настройки пака",
                            tint = brandAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Настройки",
                            style = TextStyle(fontSize = 11.sp, color = brandAccent, fontWeight = FontWeight.Medium)
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            gridOffsetInWindow = coordinates.positionInWindow()
                        }
                        .pointerInput(activePack.stickerPaths) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val winPos = offset + gridOffsetInWindow
                                    val hit = stickerBounds.entries.firstOrNull { it.value.contains(winPos) }?.key
                                    if (hit != null) {
                                        onPreviewStickerChanged(hit)
                                    }
                                },
                                onDrag = { change, _ ->
                                    val winPos = change.position + gridOffsetInWindow
                                    val hit = stickerBounds.entries.firstOrNull { it.value.contains(winPos) }?.key
                                    if (hit != null) {
                                        onPreviewStickerChanged(hit)
                                    }
                                },
                                onDragEnd = {
                                    onPreviewStickerChanged(null)
                                },
                                onDragCancel = {
                                    onPreviewStickerChanged(null)
                                }
                            )
                        },
                    contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "add_sticker_action") {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(searchBg)
                                .border(1.dp, brandAccent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .clickable { onAddStickerClick(activePack.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add),
                                    contentDescription = "Добавить",
                                    tint = brandAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Добавить",
                                    style = TextStyle(fontSize = 10.sp, color = brandAccent, fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }

                    items(
                        items = activePack.stickerPaths,
                        key = { it }
                    ) { path ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .onGloballyPositioned { coordinates ->
                                    stickerBounds[path] = coordinates.boundsInWindow()
                                }
                                .combinedClickable(
                                    onClick = { onStickerClick(path, activePack.id, activePack.name) },
                                    onLongClick = { onPreviewStickerChanged(path) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            StickerThumbnail(path = path, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerThumbnail(
    path: String,
    modifier: Modifier = Modifier,
    previewResolution: Int = 160
) {
    val isVideo = remember(path) { path.endsWith(".mp4", ignoreCase = true) || path.endsWith(".webm", ignoreCase = true) }
    val cached = remember(path) { StickerBitmapCache.get(path) }
    var bitmap by remember(path) { mutableStateOf(cached) }

    LaunchedEffect(path, previewResolution) {
        if (bitmap == null || (previewResolution > 200 && bitmap!!.width < 300)) {
            val loaded = StickerBitmapCache.loadOptimized(path, previewResolution)
            if (loaded != null) {
                bitmap = loaded
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Стикер",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VID",
                        style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }
        } else {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PackDetailsDialog(
    pack: StickerPack,
    allPacks: List<StickerPack>,
    dialogBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    brandAccent: Color,
    panelBorder: Color,
    searchBg: Color,
    onAddStickerClick: () -> Unit,
    onDeleteSticker: (String) -> Unit,
    onSetCover: (String?) -> Unit,
    onReorderStickers: (fromIndex: Int, toIndex: Int) -> Unit,
    onReorderPack: (fromIndex: Int, toIndex: Int) -> Unit,
    onDeletePack: () -> Unit,
    onSave: (name: String, author: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(pack.name) }
    var author by remember { mutableStateOf(pack.author) }

    val packIndex = allPacks.indexOfFirst { it.id == pack.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Text(
                text = "Настройки стикерпака",
                style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = textPrimary)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название набора", style = TextStyle(fontSize = 12.sp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandAccent,
                        unfocusedBorderColor = panelBorder,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Author field
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Автор", style = TextStyle(fontSize = 12.sp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandAccent,
                        unfocusedBorderColor = panelBorder,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Cover section
                Text(
                    text = "Обложка стикерпака:",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(searchBg)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cover = pack.coverPath
                    if (!cover.isNullOrEmpty() && File(cover).exists()) {
                        StickerThumbnail(
                            path = cover,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Установлена обложка",
                                style = TextStyle(fontSize = 12.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "Отображается в панели стикеров",
                                style = TextStyle(fontSize = 10.sp, color = textSecondary)
                            )
                        }
                        TextButton(onClick = { onSetCover(null) }) {
                            Text("Удалить", color = Color(0xFFEF4444), fontSize = 11.sp)
                        }
                    } else {
                        Text(
                            text = "Нажмите ★ на любом стикере ниже, чтобы сделать его иконкой набора",
                            style = TextStyle(fontSize = 11.sp, color = textSecondary),
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                // Add stickers action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(brandAccent.copy(alpha = 0.12f))
                        .clickable(onClick = onAddStickerClick)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            tint = brandAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Добавить стикеры (PNG, видео)",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = brandAccent)
                        )
                    }
                }

                // Pack Position reordering
                if (allPacks.size > 1 && packIndex != -1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { onReorderPack(packIndex, packIndex - 1) },
                            enabled = packIndex > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("← Сдвинуть левее", fontSize = 11.sp, color = if (packIndex > 0) brandAccent else textSecondary.copy(alpha = 0.4f))
                        }
                        TextButton(
                            onClick = { onReorderPack(packIndex, packIndex + 1) },
                            enabled = packIndex < allPacks.size - 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Сдвинуть правее →", fontSize = 11.sp, color = if (packIndex < allPacks.size - 1) brandAccent else textSecondary.copy(alpha = 0.4f))
                        }
                    }
                }

                // Grid of stickers with Cover selection, Reorder, and Delete
                Text(
                    text = "Стикеры (${pack.stickerPaths.size}):",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                )

                if (pack.stickerPaths.isEmpty()) {
                    Text(
                        text = "В наборе пока нет стикеров",
                        style = TextStyle(fontSize = 12.sp, color = textSecondary)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(pack.stickerPaths) { sIndex, path ->
                            val isCurrentCover = pack.coverPath == path
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(searchBg),
                                contentAlignment = Alignment.Center
                            ) {
                                StickerThumbnail(path = path, modifier = Modifier.fillMaxSize())

                                // Set Cover Button (Top Left)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(2.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentCover) brandAccent else Color.Black.copy(alpha = 0.6f))
                                        .clickable { onSetCover(path) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "★",
                                        color = if (isCurrentCover) Color.Yellow else Color.White,
                                        fontSize = 12.sp
                                    )
                                }

                                // Delete sticker button (Top Right)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = 0.8f))
                                        .clickable { onDeleteSticker(path) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = "Удалить стикер",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }

                                // Reorder buttons (Bottom)
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (sIndex > 0) {
                                        Text(
                                            text = "◀",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            modifier = Modifier.clickable { onReorderStickers(sIndex, sIndex - 1) }
                                        )
                                    }
                                    if (sIndex < pack.stickerPaths.size - 1) {
                                        Text(
                                            text = "▶",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            modifier = Modifier.clickable { onReorderStickers(sIndex, sIndex + 1) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Delete entire pack button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDeletePack)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash),
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Удалить весь стикерпак",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title, author)
                }
            ) {
                Text("Сохранить", color = brandAccent, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = textSecondary)
            }
        }
    )
}

@Composable
private fun EmojiStickerSettingsDialog(
    dialogBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    brandAccent: Color,
    searchBg: Color,
    stickerSizeDp: Int,
    onStickerSizeChanged: (Int) -> Unit,
    emojiSizeSp: Int,
    onEmojiSizeChanged: (Int) -> Unit,
    isHapticEnabled: Boolean,
    onHapticChanged: (Boolean) -> Unit,
    isShowPackTag: Boolean,
    onShowPackTagChanged: (Boolean) -> Unit,
    isAutoCloseOnSticker: Boolean,
    onAutoCloseOnStickerChanged: (Boolean) -> Unit,
    isStartWithRecents: Boolean,
    onStartWithRecentsChanged: (Boolean) -> Unit,
    onClearRecentsClick: () -> Unit,
    totalPacks: Int,
    totalStickers: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = null,
                    tint = brandAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Настройки панели",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Haptic feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Виброотклик",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                        )
                        Text(
                            text = "Тактильный отклик при нажатии на смайлы и стикеры",
                            style = TextStyle(fontSize = 11.sp, color = textSecondary)
                        )
                    }
                    Switch(
                        checked = isHapticEnabled,
                        onCheckedChange = onHapticChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = brandAccent
                        )
                    )
                }

                // Section 2: Auto close on sticker send
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сворачивать после отправки",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                        )
                        Text(
                            text = "Закрывать панель сразу после выбора стикера",
                            style = TextStyle(fontSize = 11.sp, color = textSecondary)
                        )
                    }
                    Switch(
                        checked = isAutoCloseOnSticker,
                        onCheckedChange = onAutoCloseOnStickerChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = brandAccent
                        )
                    )
                }

                // Section 3: Start with recents
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Начинать с недавних",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                        )
                        Text(
                            text = "Открывать раздел часто используемых эмодзи",
                            style = TextStyle(fontSize = 11.sp, color = textSecondary)
                        )
                    }
                    Switch(
                        checked = isStartWithRecents,
                        onCheckedChange = onStartWithRecentsChanged,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = brandAccent
                        )
                    )
                }

                // Section 4: Clear recents
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClearRecentsClick)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Очистить историю эмодзи",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
                        )
                        Text(
                            text = "Сбросить сохранённые недавние смайлики",
                            style = TextStyle(fontSize = 11.sp, color = textSecondary)
                        )
                    }
                }

                // Section 5: Statistics summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(searchBg)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Коллекция стикеров",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textSecondary)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalPacks авторских наборов • $totalStickers стикеров",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово", color = brandAccent, fontWeight = FontWeight.Medium)
            }
        }
    )
}

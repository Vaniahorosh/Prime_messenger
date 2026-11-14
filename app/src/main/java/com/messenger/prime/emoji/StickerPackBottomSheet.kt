package com.messenger.prime.emoji

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.messenger.prime.R
import java.io.File

class StickerPackBottomSheet : BottomSheetDialogFragment() {

    private var packId: String? = null
    private var packName: String? = null
    private var onStickerSelected: ((path: String, packId: String, packName: String) -> Unit)? = null

    companion object {
        fun newInstance(
            packId: String?,
            packName: String?,
            onStickerSelected: (path: String, packId: String, packName: String) -> Unit
        ): StickerPackBottomSheet {
            return StickerPackBottomSheet().apply {
                this.packId = packId
                this.packName = packName
                this.onStickerSelected = onStickerSelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val context = LocalContext.current
                val view = LocalView.current
                val isDark = isSystemInDarkTheme()

                val panelBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                val textPrimary = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                val searchBg = if (isDark) Color(0x33000000) else Color(0x14000000)

                val pack = remember(packId, packName) {
                    val packs = StickerManager.getStickerPacks(context)
                    packs.find { it.id == packId }
                        ?: packs.find { it.name == packName }
                        ?: packs.firstOrNull()
                }

                var previewStickerPath by remember { mutableStateOf<String?>(null) }
                val stickerBounds = remember { mutableMapOf<String, Rect>() }
                var gridOffsetInWindow by remember { mutableStateOf(Offset.Zero) }

                fun performFeedback() {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(panelBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(textSecondary.copy(alpha = 0.4f))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Header with pack cover / info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                val coverPath = pack?.coverPath ?: pack?.stickerPaths?.firstOrNull()
                                if (!coverPath.isNullOrEmpty() && File(coverPath).exists()) {
                                    StickerSheetThumbnail(
                                        path = coverPath,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(searchBg)
                                            .padding(4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column {
                                    Text(
                                        text = pack?.name ?: (packName ?: "Стикерпак"),
                                        style = TextStyle(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Автор: ${pack?.author ?: "Prime"} • ${pack?.stickerPaths?.size ?: 0} стикеров",
                                        style = TextStyle(fontSize = 12.sp, color = textSecondary)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(searchBg)
                                    .clickable { dismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Закрыть",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sticker Grid with Long-Press & Drag preview
                        if (pack != null && pack.stickerPaths.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 340.dp)
                                    .onGloballyPositioned { coordinates ->
                                        gridOffsetInWindow = coordinates.positionInWindow()
                                    }
                                    .pointerInput(pack.stickerPaths) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                val winPos = offset + gridOffsetInWindow
                                                val hit = stickerBounds.entries.firstOrNull { it.value.contains(winPos) }?.key
                                                if (hit != null) {
                                                    previewStickerPath = hit
                                                    performFeedback()
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                val winPos = change.position + gridOffsetInWindow
                                                val hit = stickerBounds.entries.firstOrNull { it.value.contains(winPos) }?.key
                                                if (hit != null && hit != previewStickerPath) {
                                                    previewStickerPath = hit
                                                    performFeedback()
                                                }
                                            },
                                            onDragEnd = {
                                                previewStickerPath = null
                                            },
                                            onDragCancel = {
                                                previewStickerPath = null
                                            }
                                        )
                                    },
                                contentPadding = PaddingValues(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pack.stickerPaths, key = { it }) { path ->
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(searchBg)
                                            .onGloballyPositioned { coordinates ->
                                                stickerBounds[path] = coordinates.boundsInWindow()
                                            }
                                            .clickable {
                                                onStickerSelected?.invoke(path, pack.id, pack.name)
                                                dismiss()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StickerSheetThumbnail(
                                            path = path,
                                            modifier = Modifier.fillMaxSize().padding(4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "В этом стикерпаке пока нет стикеров",
                                    style = TextStyle(fontSize = 13.sp, color = textSecondary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // True Fullscreen Blur Sticker Preview Dialog without text
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
                                        StickerSheetThumbnail(
                                            path = path,
                                            modifier = Modifier.fillMaxSize(),
                                            targetSize = 512
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerSheetThumbnail(
    path: String,
    modifier: Modifier = Modifier,
    targetSize: Int = 180
) {
    val cached = remember(path) { StickerBitmapCache.get(path) }
    var bitmap by remember(path) { mutableStateOf(cached) }

    LaunchedEffect(path, targetSize) {
        if (bitmap == null || targetSize > 200) {
            bitmap = StickerBitmapCache.loadOptimized(path, targetSize)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Стикер",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        }
    }
}

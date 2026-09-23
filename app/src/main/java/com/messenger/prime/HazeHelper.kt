package com.messenger.prime

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp

object HazeHelper {
    @JvmStatic
    @JvmOverloads
    fun setupHaze(composeView: ComposeView?, tintColor: Color = Color(0x33154B87)) {
        composeView?.setContent {
            BlurView(
                modifier = Modifier.fillMaxSize(),
                blurRadius = 24.dp,
                tint = tintColor
            )
        }
    }
}

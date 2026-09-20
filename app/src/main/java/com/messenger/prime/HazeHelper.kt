package com.messenger.prime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

object HazeHelper {
    @JvmField
    val defaultHazeState = HazeState()

    @JvmStatic
    @JvmOverloads
    fun setupHaze(composeView: ComposeView?, hazeState: HazeState = defaultHazeState) {
        composeView?.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = 24.dp,
                            noiseFactor = 0.05f,
                            tint = HazeTint(Color(0x33154B87))
                        )
                    )
            )
        }
    }
}

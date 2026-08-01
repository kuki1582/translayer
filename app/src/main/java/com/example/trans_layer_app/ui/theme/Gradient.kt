package com.example.trans_layer_app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppGradients(
    val mainActionButton: List<Color> = MainActionButtonGradient,
    val bubbleButton: List<Color> = BubbleButtonGradient,
    val mainBackground: List<Color> = MainBackgroundGradient,
    val radialHighlight: List<Color> = RadialHighlightGradient,
    val glossyHighlight: List<Color> = GlossyHighlightGradient,
    val glassPanel: List<Color> = GlassPanelGradient,
    val spinner: List<Color> = SpinnerGradient,
    val dialogBackground: List<Color> = DialogBackgroundGradient
)

val LocalAppGradients = staticCompositionLocalOf { AppGradients() }

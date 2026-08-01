package com.example.trans_layer_app.ui.theme

import androidx.compose.ui.graphics.Color

const val PreviewBackgroundHex = 0xFF121212

// App specific colors
val DeepLavender = Color(0xFF5B4A84)
val SoftPink = Color(0xFFFFF2FB)
val SoftViolet = Color(0xFFE8DFFF)
val GlassContainerColor = Color(0x18FFF7FE)
val GlassBorderColor = Color.White.copy(alpha = 0.28f)

// Spinner specific colors
val SpinnerBorderColor = Color.White.copy(alpha = 0.40f)
val SpinnerLabelColor = Color(0xFFE5DCF7)
val SpinnerValueColor = Color(0xFFFFFBFF)
val SpinnerIconColor = Color(0xFFF4EDFF)
val DropdownItemUnselected = Color(0xFF2D3654)

// Recent Section specific colors
val RecentLabelColor = Color(0x99E4DBF6)
val RecentChipSelectedText = Color(0xFFF1EBFF)
val RecentChipUnselectedText = Color(0xB8ECE2FF)
val RecentChipSelectedBorder = Color.White.copy(alpha = 0.24f)
val RecentChipUnselectedBorder = Color.White.copy(alpha = 0.10f)

// Dialog specific colors
val DialogContainerColor = Color(0x160E1327)
val DialogMessageColor = Color(0xFFE6DDF6)

// Overlay specific colors
val OverlayDimColor = Color.Black.copy(alpha = 0.25f)
val OverlayBorderColor = Color.White.copy(alpha = 0.58f)
val OverlayShadowColor = Color.White.copy(alpha = 0.65f)
val ProgressIndicatorColor = Color(0xFF8C7AC0)
val ProgressTrackColor = Color.White.copy(alpha = 0.35f)

// Gradient colors
val GlassLightPink = Color(0xECFFF7FE)
val GlassLightPurple = Color(0xE3F1E8FF)
val GlassLightBlue = Color(0xD7DDEDFF)

val BubbleLightPink = Color(0xEEFFF5FE)
val BubbleLightPurple = Color(0xD9F2E6FF)
val BubbleLightBlue = Color(0xC4D8ECFF)

// Glass colors
val GlassDeepBlue = Color(0xFF313E64)

// Background colors
val DarkBlue900 = Color(0xFF171C31)
val DarkBlue800 = Color(0xFF22253D)
val DarkBlue700 = Color(0xFF2A3150)
val DarkBlue950 = Color(0xFF171D30)

// Text colors
val TextWhite = Color(0xFFFDF7FF)
val TextLightPurple = Color(0xFFDCD4F1)

// Predefined Gradient Lists
val MainActionButtonGradient = listOf(GlassLightPink, GlassLightPurple, GlassLightBlue)
val BubbleButtonGradient = listOf(BubbleLightPink, BubbleLightPurple, BubbleLightBlue)
val MainBackgroundGradient = listOf(DarkBlue900, DarkBlue800, DarkBlue700, DarkBlue950)
val RadialHighlightGradient = listOf(
    SoftPink.copy(alpha = 0.33f),
    SoftViolet.copy(alpha = 0.26f),
    Color.Transparent
)
val GlossyHighlightGradient = listOf(
    Color.White.copy(alpha = 0.78f),
    Color.White.copy(alpha = 0.12f),
    Color.Transparent
)
val GlassPanelGradient = listOf(
    SoftPink.copy(alpha = 0.13f),
    SoftViolet.copy(alpha = 0.10f),
    GlassDeepBlue.copy(alpha = 0.07f)
)
val SpinnerGradient = listOf(
    Color(0x30FFF7FE),
    Color(0x2AEFE4FF),
    Color(0x206E7FB4)
)
val DialogBackgroundGradient = listOf(
    Color(0xEE232A44),
    Color(0xE22A3150),
    Color(0xD61B2137)
)

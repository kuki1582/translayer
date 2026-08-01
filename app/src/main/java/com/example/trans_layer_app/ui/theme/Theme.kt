package com.example.trans_layer_app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = darkColorScheme(
    primary = DeepLavender,
    onPrimary = TextWhite,
    secondary = SpinnerLabelColor,
    onSecondary = SpinnerValueColor,
    tertiary = SpinnerIconColor,
    background = DarkBlue900,
    onBackground = TextWhite,
    surface = GlassContainerColor,
    onSurface = TextLightPurple,
    surfaceVariant = ProgressTrackColor,
    onSurfaceVariant = DialogMessageColor,
    surfaceContainer = DialogContainerColor,
    outline = GlassBorderColor,
    outlineVariant = SpinnerBorderColor,
    scrim = OverlayDimColor,
    inverseSurface = OverlayBorderColor,
    inverseOnSurface = OverlayShadowColor,
    inversePrimary = ProgressIndicatorColor,
    primaryContainer = RecentChipSelectedBorder,
    onPrimaryContainer = RecentChipSelectedText,
    secondaryContainer = RecentChipUnselectedBorder,
    onSecondaryContainer = RecentChipUnselectedText,
    tertiaryContainer = RecentLabelColor,
    onTertiaryContainer = DropdownItemUnselected
)

// We use the same color scheme for both light and dark to maintain the app's universe branding
private val DarkColorScheme = AppColorScheme
private val LightColorScheme = AppColorScheme

object TranslayerTheme {
    val gradients: AppGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalAppGradients.current
}

@Composable
fun TranslayerappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalAppGradients provides AppGradients()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

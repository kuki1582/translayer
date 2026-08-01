package com.example.trans_layer_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.trans_layer_app.R
import com.example.trans_layer_app.data.LanguageCatalog
import com.example.trans_layer_app.ui.MainViewModel
import com.example.trans_layer_app.ui.components.GlassPanel
import com.example.trans_layer_app.ui.components.LanguageSpinner
import com.example.trans_layer_app.ui.components.MainActionButton
import com.example.trans_layer_app.ui.components.RecentLanguageSection
import com.example.trans_layer_app.ui.theme.TranslayerTheme
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    isAccessibilityEnabled: () -> Boolean
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()
    val languageOptions = remember { LanguageCatalog.allLanguages }

    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()

    var startButtonScale by remember { mutableFloatStateOf(1f) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isAccessibilityEnabled()) {
                viewModel.triggerStartButtonAnimation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel.startButtonAnimationTick) {
        if (viewModel.startButtonAnimationTick == 0) return@LaunchedEffect

        repeat(2) {
            startButtonScale = 1.06f
            delay(150)
            startButtonScale = 0.97f
            delay(130)
            startButtonScale = 1f
            delay(110)
        }
        startButtonScale = 1f
    }

    val backgroundBrush = Brush.linearGradient(colors = TranslayerTheme.gradients.mainBackground)

    val handleButtonClick = {
        if (isServiceRunning) {
            onStopClick()
        } else {
            onStartClick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 20.dp)
            .background(
                Brush.radialGradient(colors = TranslayerTheme.gradients.radialHighlight)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.main_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.main_subtitle),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Source Language Section
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.source_language_title),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(18.dp))
                if (viewModel.recentSourceLanguages.isNotEmpty()) {
                    RecentLanguageSection(
                        text = stringResource(R.string.quick_picks),
                        options = viewModel.recentSourceLanguages.take(4),
                        selected = viewModel.selectedSourceLanguage,
                        onSelect = { viewModel.onSelectSourceLanguage(it) }
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                }
                LanguageSpinner(
                    options = languageOptions,
                    selected = viewModel.selectedSourceLanguage,
                    onSelect = { viewModel.onSelectSourceLanguage(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target Language Section
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.target_language_title),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(18.dp))
                if (viewModel.recentTargetLanguages.isNotEmpty()) {
                    RecentLanguageSection(
                        text = stringResource(R.string.quick_picks),
                        options = viewModel.recentTargetLanguages.take(4),
                        selected = viewModel.selectedTargetLanguage,
                        onSelect = { viewModel.onSelectTargetLanguage(it) }
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                }
                LanguageSpinner(
                    options = languageOptions,
                    selected = viewModel.selectedTargetLanguage,
                    onSelect = { viewModel.onSelectTargetLanguage(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TODO: Implement Ad Banner here
            /*
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(94.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = stringResource(R.string.ad_placeholder),
                    color = Color(0xFFF3E9FF),
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.ad_placeholder_description),
                    color = Color(0xFFD3C7EA),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            */

            // Padding for fixed button
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Fixed Button at bottom
        MainActionButton(
            isServiceRunning = isServiceRunning,
            onClick = handleButtonClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            scale = startButtonScale
        )
    }
}

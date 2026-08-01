package com.example.trans_layer_app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.trans_layer_app.data.models.OcrItem
import com.example.trans_layer_app.ui.components.FLOATING_BUTTON_CONTENT_SIZE_DP
import com.example.trans_layer_app.ui.components.FLOATING_BUTTON_WINDOW_PADDING_DP
import com.example.trans_layer_app.ui.components.FloatingButton
import com.example.trans_layer_app.ui.components.TranslationOverlay
import com.example.trans_layer_app.ui.theme.TranslayerappTheme
import com.example.trans_layer_app.ui.state.FloatingButtonState
import com.example.trans_layer_app.utils.WindowUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingUiManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val floatingButtonState: MutableState<FloatingButtonState>,
    private val isTranslating: MutableState<Boolean>,
    private val onTranslateRequest: () -> Unit
) {
    private var floatingView: ComposeView? = null
    private var floatingButtonParams: WindowManager.LayoutParams? = null
    private var overlayView: ComposeView? = null

    private var floatingButtonMotionJob: Job? = null
    private var floatingButtonIntroJob: Job? = null

    companion object {
        private const val FLOATING_BUTTON_MARGIN_DP = 16
        private const val FLOATING_BUTTON_TOP_OFFSET_DP = 72
        private const val FLOATING_BUTTON_COLLAPSED_VISIBLE_DP = 32
        private const val FLOATING_BUTTON_INTRO_DURATION_MS = 1800L
    }

    fun showFloatingButton() {
        if (floatingView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val density = context.resources.displayMetrics.density
        val windowSizePx =
            ((FLOATING_BUTTON_CONTENT_SIZE_DP + FLOATING_BUTTON_WINDOW_PADDING_DP * 2) * density).toInt()

        val params = WindowManager.LayoutParams(
            windowSizePx,
            windowSizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = getExpandedButtonX()
            y = getFloatingButtonTopY()
        }
        floatingButtonParams = params
        floatingButtonState.value = FloatingButtonState.INTRO

        floatingView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

            setContent {
                TranslayerappTheme {
                    FloatingButton(
                        state = floatingButtonState.value,
                        onClick = { handleFloatingButtonTap() },
                        onDrag = { dx, dy -> updateFloatingButtonPosition(dx, dy) }
                    )
                }
            }
        }

        windowManager.addView(floatingView, params)
        playFloatingButtonIntro()
    }

    private fun handleFloatingButtonTap() {
        when (floatingButtonState.value) {
            FloatingButtonState.INTRO -> {
                stopAnimations()
                floatingButtonState.value = FloatingButtonState.READY
                onTranslateRequest()
            }

            FloatingButtonState.LEFT_COLLAPSED,
            FloatingButtonState.RIGHT_COLLAPSED -> expandFloatingButton()

            FloatingButtonState.READY -> onTranslateRequest()
        }
    }

    private fun playFloatingButtonIntro() {
        floatingButtonIntroJob?.cancel()
        floatingButtonIntroJob = lifecycleOwner.lifecycleScope.launch {
            floatingButtonState.value = FloatingButtonState.INTRO
            animateFloatingButtonTo(getExpandedButtonX(), getFloatingButtonTopY())
            delay(FLOATING_BUTTON_INTRO_DURATION_MS)
            collapseFloatingButton()
        }
    }

    private fun collapseFloatingButton() {
        floatingButtonState.value = FloatingButtonState.RIGHT_COLLAPSED
        animateFloatingButtonTo(getRightCollapsedButtonX(), getFloatingButtonTopY())
    }

    private fun expandFloatingButton() {
        floatingButtonIntroJob?.cancel()
        floatingButtonState.value = FloatingButtonState.READY
        animateFloatingButtonTo(getExpandedButtonX(), getFloatingButtonTopY())
    }

    private fun updateFloatingButtonPosition(dx: Float, dy: Float) {
        val view = floatingView ?: return
        val params = floatingButtonParams ?: return
        val (screenWidth, screenHeight) = WindowUtils.getScreenSize(context)
        val minX = -((view.width / 2)).coerceAtLeast(0)
        val maxX = (screenWidth - (view.width / 2)).coerceAtLeast(0)
        val minY = WindowUtils.getStatusBarHeight(context)
        val maxY =
            (screenHeight - view.height - WindowUtils.getNavigationBarHeight(context)).coerceAtLeast(
                minY
            )

        params.x = (params.x + dx.toInt()).coerceIn(minX, maxX)
        params.y = (params.y + dy.toInt()).coerceIn(minY, maxY)

        if (params.x <= minX + (view.width / 6)) {
            params.x = minX
            floatingButtonState.value = FloatingButtonState.LEFT_COLLAPSED
        } else if (params.x >= getRightCollapsedButtonX() - (view.width / 6)) {
            params.x = getRightCollapsedButtonX()
            floatingButtonState.value = FloatingButtonState.RIGHT_COLLAPSED
        } else {
            floatingButtonState.value = FloatingButtonState.READY
        }
        windowManager.updateViewLayout(view, params)
    }

    private fun animateFloatingButtonTo(targetX: Int, targetY: Int, durationMs: Long = 220L) {
        val view = floatingView ?: return
        val params = floatingButtonParams ?: return
        floatingButtonMotionJob?.cancel()
        floatingButtonMotionJob = lifecycleOwner.lifecycleScope.launch {
            val startX = params.x
            val startY = params.y
            val steps = 12
            repeat(steps) { index ->
                val progress = (index + 1) / steps.toFloat()
                params.x = WindowUtils.lerp(startX, targetX, progress)
                params.y = WindowUtils.lerp(startY, targetY, progress)
                runCatching { windowManager.updateViewLayout(view, params) }
                delay(durationMs / steps)
            }
        }
    }

    fun stopAnimations() {
        floatingButtonIntroJob?.cancel()
        floatingButtonIntroJob = null
        floatingButtonMotionJob?.cancel()
        floatingButtonMotionJob = null
    }

    fun resetPosition() {
        val view = floatingView ?: return
        val params = floatingButtonParams ?: return
        val density = context.resources.displayMetrics.density
        val offsetPx = (FLOATING_BUTTON_MARGIN_DP * density).toInt()

        floatingButtonState.value = FloatingButtonState.READY
        params.x = offsetPx
        params.y = WindowUtils.getStatusBarHeight(context) + offsetPx

        runCatching {
            windowManager.updateViewLayout(view, params)
        }.onFailure { e ->
            Log.e("FloatingUiManager", "Layout update failed: ${e.message}")
        }
    }

    fun showOverlayTexts(
        results: List<OcrItem>,
        capturedBitmap: Bitmap?
    ) {
        removeOverlayView()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        overlayView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

            setContent {
                TranslayerappTheme {
                    TranslationOverlay(
                        results = results,
                        isLoading = isTranslating.value,
                        backgroundBitmap = capturedBitmap,
                        onClick = {
                            isTranslating.value = false
                            removeOverlayView()
                        }
                    )
                }
            }
        }

        windowManager.addView(overlayView, params)
    }

    fun removeOverlayView() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    fun cleanup() {
        stopAnimations()
        removeOverlayView()
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
    }

    private fun getExpandedButtonX(): Int {
        val density = context.resources.displayMetrics.density
        val (screenWidth, _) = WindowUtils.getScreenSize(context)
        val contentSizePx = (FLOATING_BUTTON_CONTENT_SIZE_DP * density).toInt()
        val paddingPx = (FLOATING_BUTTON_WINDOW_PADDING_DP * density).toInt()
        val marginPx = (FLOATING_BUTTON_MARGIN_DP * density).toInt()
        // (vx - P) where vx is screenWidth - contentSize - margin
        return screenWidth - contentSizePx - marginPx - paddingPx
    }

    private fun getRightCollapsedButtonX(): Int {
        val density = context.resources.displayMetrics.density
        val (screenWidth, _) = WindowUtils.getScreenSize(context)
        val visiblePx = (FLOATING_BUTTON_COLLAPSED_VISIBLE_DP * density).toInt()
        val paddingPx = (FLOATING_BUTTON_WINDOW_PADDING_DP * density).toInt()
        // (vx - P) where vx is screenWidth - visibleAmount
        return screenWidth - visiblePx - paddingPx
    }

    private fun getFloatingButtonTopY(): Int {
        val density = context.resources.displayMetrics.density
        val topOffsetPx = (FLOATING_BUTTON_TOP_OFFSET_DP * density).toInt()
        val paddingPx = (FLOATING_BUTTON_WINDOW_PADDING_DP * density).toInt()
        // (vy - P) where vy is statusBarHeight + topOffset
        return WindowUtils.getStatusBarHeight(context) + topOffsetPx - paddingPx
    }
}

package com.example.trans_layer_app.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.window.layout.WindowMetricsCalculator
import kotlin.math.roundToInt

object WindowUtils {
    fun getScreenSize(context: Context): Pair<Int, Int> {
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeMaximumWindowMetrics(context)
        val width = metrics.bounds.width()
        val height = metrics.bounds.height()
        return width to height
    }

    fun getStatusBarHeight(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.statusBars()
            )
            return insets.top
        }

        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    fun getNavigationBarHeight(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.navigationBars()
            )
            return insets.bottom
        }

        @SuppressLint("InternalInsetResource", "DiscouragedApi")
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    fun lerp(start: Int, end: Int, fraction: Float): Int {
        return (start + (end - start) * fraction).roundToInt()
    }
}

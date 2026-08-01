package com.example.trans_layer_app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.trans_layer_app.data.ForegroundTrackingStore
import dagger.hilt.android.AndroidEntryPoint

// アプリの切り替えを監視し、現在のパッケージ名を共有ストアに報告する
@AndroidEntryPoint
class ForegroundAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // ウィンドウの状態が変わった時（アプリ起動・切り替え）にのみ実行
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val foregroundPackage = event.packageName?.toString() ?: return
        // 現在のパッケージを更新（ScreenCaptureService側でこれを監視して自動終了を判断する）
        ForegroundTrackingStore.setCurrentPackage(foregroundPackage)
    }

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }

    companion object {
        private const val TAG = "ForegroundA11yService"
    }
}

package com.example.trans_layer_app.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.trans_layer_app.R
import com.example.trans_layer_app.data.ForegroundTrackingStore
import com.example.trans_layer_app.data.LanguagePreferences
import com.example.trans_layer_app.domain.models.TranslationResult
import com.example.trans_layer_app.domain.repository.TranslationRepository
import com.example.trans_layer_app.ui.FloatingUiManager
import com.example.trans_layer_app.ui.state.FloatingButtonState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class ScreenCaptureService : LifecycleService(), SavedStateRegistryOwner {
    private lateinit var windowManager: WindowManager
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var floatingUiManager: FloatingUiManager

    private var translationJob: Job? = null
    private var trackingArmJob: Job? = null

    private var launcherPackages = listOf<String>()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    @Inject
    lateinit var translationRepository: TranslationRepository

    private val isTranslating = mutableStateOf(false)
    private val floatingButtonState = mutableStateOf(FloatingButtonState.INTRO)

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        screenCaptureManager = ScreenCaptureManager(this)
        floatingUiManager = FloatingUiManager(
            context = this,
            windowManager = windowManager,
            lifecycleOwner = this,
            savedStateRegistryOwner = this,
            floatingButtonState = floatingButtonState,
            isTranslating = isTranslating,
            onTranslateRequest = { startTranslation() },
            onOverlayDismiss = { cancelTranslation() }
        )

        updateLauncherPackages()
        observeForegroundPackageChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val notification = createNotification()
        ServiceCompat.startForeground(
            this,
            1,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            }
        )

        val resultCode = intent?.getIntExtra(RESULT_CODE, 0) ?: 0
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(DATA)
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            if (mediaProjection != null) {
                mediaProjection.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        screenCaptureManager.stop()
                    }

                    override fun onCapturedContentResize(width: Int, height: Int) {
                        super.onCapturedContentResize(width, height)
                        screenCaptureManager.setupImageReader(width, height)
                    }

                    override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                        super.onCapturedContentVisibilityChanged(isVisible)
                        if (!isVisible) {
                            floatingUiManager.removeOverlayView()
                        }
                    }
                }, null)
                screenCaptureManager.setMediaProjection(mediaProjection)
            }
        }

        floatingUiManager.showFloatingButton()
        armSharedAppTracking()

        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        floatingUiManager.removeOverlayView()
        floatingUiManager.stopAnimations()
        floatingUiManager.resetPosition()
    }

    private fun createNotification(): Notification {
        val channelId = "screen_capture_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_translation_service),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title_active))
            .setContentText(getString(R.string.notification_text_ready))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun armSharedAppTracking() {
        trackingArmJob?.cancel()
        trackingArmJob = lifecycleScope.launch {
            ForegroundTrackingStore.clearTrackedPackage()
            try {
                val detectedApp = withTimeout(2000.milliseconds) {
                    ForegroundTrackingStore.currentPackage.first { pkg ->
                        !pkg.isNullOrBlank() &&
                                pkg != packageName &&
                                pkg != PACKAGE_SYSTEM_UI &&
                                pkg != PACKAGE_GOOGLE_SYSTEM_UI
                    }
                }
                ForegroundTrackingStore.setTrackedPackage(detectedApp)
            } catch (e: TimeoutCancellationException) {
                ForegroundTrackingStore.setTrackedPackage(ForegroundTrackingStore.MODE_ENTIRE_SCREEN)
            }
        }
    }

    /** 翻訳を開始する。翻訳中の場合は新しいリクエストを無視する。 */
    private fun startTranslation() {
        if (translationJob?.isActive == true) return
        translationJob = lifecycleScope.launch { processTranslation() }
    }

    /**
     * ユーザーがオーバーレイを閉じたときに呼ばれる。
     * 実行中の翻訳もキャンセルし、閉じた後に結果が再表示されないようにする。
     */
    private fun cancelTranslation() {
        translationJob?.cancel()
        translationJob = null
        isTranslating.value = false
        floatingUiManager.removeOverlayView()
    }

    private suspend fun processTranslation() {
        isTranslating.value = true
        try {
            val bitmap = screenCaptureManager.captureScreen()
            if (bitmap == null) {
                // 以前は何も表示せずに終わっていたため、ボタンを押しても反応がないように見えた
                showError(
                    if (screenCaptureManager.isCapturing) {
                        R.string.error_capture_failed
                    } else {
                        R.string.error_screen_sharing_stopped
                    }
                )
                return
            }

            floatingUiManager.showOverlayTexts(emptyList(), bitmap)

            val targetLanguage = LanguagePreferences.getTargetPromptLabel(this)
            val sourceLanguageCode = LanguagePreferences.getSourceLanguageCode(this)
            val result =
                translationRepository.translateScreen(bitmap, sourceLanguageCode, targetLanguage)

            // 結果のオーバーレイをローディング表示なしで描画するため、先にフラグを戻す
            isTranslating.value = false
            when (result) {
                is TranslationResult.Success ->
                    floatingUiManager.showOverlayTexts(result.items, bitmap)

                TranslationResult.NoTextFound -> showError(R.string.error_no_text_found)
                TranslationResult.ResponseTruncated -> showError(R.string.error_response_truncated)
                TranslationResult.InvalidResponse -> showError(R.string.error_invalid_response)
                TranslationResult.ServiceUnavailable -> showError(R.string.error_service_unavailable)
                is TranslationResult.Failed -> showError(R.string.error_translation_failed)
            }
        } catch (e: CancellationException) {
            // キャンセルはエラーではないので、そのまま上に伝える
            // (握りつぶすとコルーチンが正しく終了しない)
            throw e
        } catch (e: Exception) {
            // キャプチャや設定の読み込みなど、Repository の外で起きた想定外のエラー
            Log.e(TAG, "Translation failed", e)
            showError(R.string.error_translation_failed)
        } finally {
            // 成功・失敗・キャンセルのどの場合でも、翻訳中フラグを必ず戻す
            isTranslating.value = false
        }
    }

    private fun showError(@StringRes messageRes: Int) {
        floatingUiManager.removeOverlayView()
        // Toast は通知が許可されていないと表示されないため、オーバーレイで表示する
        floatingUiManager.showMessage(getString(messageRes))
    }

    private fun dismissFloatingUi() {
        floatingUiManager.cleanup()
        trackingArmJob?.cancel()
        trackingArmJob = null
        ForegroundTrackingStore.clearTrackedPackage()
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        screenCaptureManager.stop()
        floatingUiManager.cleanup()
        trackingArmJob?.cancel()
        _isRunning.value = false
        super.onDestroy()
    }

    private fun updateLauncherPackages() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        launcherPackages = resolveInfos.map { it.activityInfo.packageName }
    }

    private fun observeForegroundPackageChanges() {
        lifecycleScope.launch {
            ForegroundTrackingStore.currentPackage.collect { foregroundPackage ->
                if (foregroundPackage == null) return@collect
                val tracked = ForegroundTrackingStore.trackedPackage.value ?: return@collect
                val isLauncher = launcherPackages.contains(foregroundPackage)
                val isMyApp = foregroundPackage == packageName
                val isSystemUI = ForegroundTrackingStore.isSystemUI(foregroundPackage)

                if (isMyApp || isSystemUI) return@collect

                if (tracked == ForegroundTrackingStore.MODE_ENTIRE_SCREEN) {
                    if (isLauncher) floatingUiManager.removeOverlayView()
                } else {
                    if (foregroundPackage != tracked) dismissFloatingUi()
                }
            }
        }
    }

    companion object {
        const val RESULT_CODE = "RESULT_CODE"
        const val DATA = "DATA"
        private const val TAG = "ScreenCaptureService"
        private const val PACKAGE_SYSTEM_UI = "com.android.systemui"
        private const val PACKAGE_GOOGLE_SYSTEM_UI = "com.google.android.systemui"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }
}

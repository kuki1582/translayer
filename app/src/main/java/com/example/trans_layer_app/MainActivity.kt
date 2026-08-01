package com.example.trans_layer_app

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.trans_layer_app.service.ForegroundAccessibilityService
import com.example.trans_layer_app.service.ScreenCaptureService
import com.example.trans_layer_app.ui.MainViewModel
import com.example.trans_layer_app.ui.components.PermissionNoticeDialog
import com.example.trans_layer_app.ui.screens.MainScreen
import com.example.trans_layer_app.ui.theme.TranslayerappTheme
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.AndroidEntryPoint

private lateinit var firebaseAnalytics: FirebaseAnalytics

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Firebaseの基本初期化
        Firebase.initialize(context = this)

        // App Checkの初期化 (他のサービスより先に設定するのが推奨)
        val factory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(factory)

        // Remote Config の初期化
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        // Fetch and activate Remote Config values
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d(TAG, "Remote Config values fetched and activated: $updated")
                } else {
                    Log.e(TAG, "Error fetching Remote Config", task.exception)
                }
            }
        // Add a real-time Remote Config listener
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d(TAG, "Updated keys: ${configUpdate.updatedKeys}")
                remoteConfig.activate().addOnCompleteListener {
                    // Optionally, add an action to perform on update here.
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w(TAG, "Config update error with code: ${error.code}", error)
            }
        })

        // Analyticsの初期化
        firebaseAnalytics = Firebase.analytics

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            // 画面キャプチャの許可リクエスト用ランチャー
            val captureLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val intent = Intent(this, ScreenCaptureService::class.java).apply {
                        putExtra(ScreenCaptureService.RESULT_CODE, result.resultCode)
                        putExtra(ScreenCaptureService.DATA, result.data)
                    }
                    ContextCompat.startForegroundService(this, intent)
                }
            }

            TranslayerappTheme {
                MainScreen(
                    viewModel = viewModel,
                    onStartClick = {
                        firebaseAnalytics.logEvent("start_button_clicked") {
                            param("action", "screen_capture_start")
                        }

                        when {
                            !hasOverlayPermission() -> openOverlayPermissionSettings()
                            !isAccessibilityServiceEnabled() -> viewModel.showAccessibilityDialog =
                                true

                            else -> viewModel.showMediaProjectionDialog = true
                        }
                    },
                    onStopClick = {
                        val intent = Intent(this, ScreenCaptureService::class.java)
                        stopService(intent)

                        firebaseAnalytics.logEvent("stop_button_clicked") {
                            param("action", "screen_capture_stop")
                        }
                    },
                    isAccessibilityEnabled = { isAccessibilityServiceEnabled() }
                )

                if (viewModel.showAccessibilityDialog) {
                    PermissionNoticeDialog(
                        title = stringResource(R.string.permission_accessibility_title),
                        message = buildAnnotatedString {
                            append(stringResource(R.string.permission_accessibility_message_prefix))
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.permission_accessibility_message_highlight))
                            }
                            append(stringResource(R.string.permission_accessibility_message_suffix))
                        },
                        confirmLabel = stringResource(R.string.permission_open_settings),
                        dismissLabel = stringResource(R.string.permission_later),
                        onConfirm = {
                            viewModel.showAccessibilityDialog = false
                            openAccessibilitySettings()
                        },
                        onDismiss = { viewModel.showAccessibilityDialog = false }
                    )
                }

                if (viewModel.showMediaProjectionDialog) {
                    PermissionNoticeDialog(
                        title = stringResource(R.string.permission_media_projection_title),
                        message = buildAnnotatedString {
                            append(stringResource(R.string.permission_media_projection_message))
                        },
                        confirmLabel = stringResource(R.string.permission_continue),
                        dismissLabel = stringResource(R.string.permission_cancel),
                        onConfirm = {
                            viewModel.showMediaProjectionDialog = false
                            requestMediaProjection(captureLauncher)
                        },
                        onDismiss = { viewModel.showMediaProjectionDialog = false }
                    )
                }
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )
        startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val expectedService = "$packageName/${ForegroundAccessibilityService::class.java.name}"
        return enabledServices.split(':').any { it.equals(expectedService, ignoreCase = true) }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestMediaProjection(
        launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        launcher.launch(projectionManager.createScreenCaptureIntent())
    }
}

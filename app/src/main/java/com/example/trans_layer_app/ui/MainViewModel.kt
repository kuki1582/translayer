package com.example.trans_layer_app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trans_layer_app.data.LanguageCatalog
import com.example.trans_layer_app.data.LanguagePreferences
import com.example.trans_layer_app.domain.models.LanguageOption
import com.example.trans_layer_app.service.ScreenCaptureService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    val isServiceRunning: StateFlow<Boolean> = ScreenCaptureService.isRunning

    var selectedTargetLanguage by mutableStateOf(LanguageCatalog.defaultTargetLanguage)
        private set
    var selectedSourceLanguage by mutableStateOf(LanguageCatalog.defaultSourceLanguage)
        private set
    var recentTargetLanguages by mutableStateOf(emptyList<LanguageOption>())
        private set
    var recentSourceLanguages by mutableStateOf(emptyList<LanguageOption>())
        private set

    var showAccessibilityDialog by mutableStateOf(false)
    var showMediaProjectionDialog by mutableStateOf(false)

    // アニメーション用トリガー
    var startButtonAnimationTick by mutableIntStateOf(0)
        private set

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            selectedTargetLanguage = LanguagePreferences.getSelectedTargetLanguage(app)
            selectedSourceLanguage = LanguagePreferences.getSelectedSourceLanguage(app)
            recentTargetLanguages = LanguagePreferences.getRecentTargetLanguages(app)
            recentSourceLanguages = LanguagePreferences.getRecentSourceLanguages(app)
        }
    }

    fun onSelectTargetLanguage(option: LanguageOption) {
        selectedTargetLanguage = option
        viewModelScope.launch {
            val app = getApplication<Application>()
            LanguagePreferences.saveTargetLanguage(app, option)
            recentTargetLanguages = LanguagePreferences.getRecentTargetLanguages(app)
        }
    }

    fun onSelectSourceLanguage(option: LanguageOption) {
        selectedSourceLanguage = option
        viewModelScope.launch {
            val app = getApplication<Application>()
            LanguagePreferences.saveSourceLanguage(app, option)
            recentSourceLanguages = LanguagePreferences.getRecentSourceLanguages(app)
        }
    }

    fun triggerStartButtonAnimation() {
        startButtonAnimationTick++
    }
}

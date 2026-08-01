package com.example.trans_layer_app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ForegroundTrackingStore {
    const val MODE_ENTIRE_SCREEN = "MODE_ENTIRE_SCREEN"
    const val PACKAGE_SYSTEM_UI = "com.android.systemui"
    const val PACKAGE_GOOGLE_SYSTEM_UI = "com.google.android.systemui"

    private val _currentPackage = MutableStateFlow<String?>(null)
    val currentPackage = _currentPackage.asStateFlow()

    private val _trackedPackage = MutableStateFlow<String?>(null)
    val trackedPackage = _trackedPackage.asStateFlow()

    fun setCurrentPackage(pkg: String?) {
        _currentPackage.value = pkg
    }

    fun setTrackedPackage(pkg: String?) {
        _trackedPackage.value = pkg
    }

    fun clearTrackedPackage() {
        _trackedPackage.value = null
    }

    fun isSystemUI(pkg: String?): Boolean =
        pkg == PACKAGE_SYSTEM_UI || pkg == PACKAGE_GOOGLE_SYSTEM_UI
}

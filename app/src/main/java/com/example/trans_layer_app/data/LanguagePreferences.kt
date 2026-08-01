package com.example.trans_layer_app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.trans_layer_app.domain.models.LanguageOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.languageDataStore by preferencesDataStore(name = "language_preferences")

object LanguagePreferences {
    private val targetLanguageKey = stringPreferencesKey("target_language")
    private val sourceLanguageKey = stringPreferencesKey("source_language")
    private val recentTargetLanguageCodesKey = stringPreferencesKey("recent_language_codes")
    private val recentSourceLanguageCodesKey = stringPreferencesKey("recent_source_language_codes")
    private const val MAX_RECENT_LANGUAGES = 4

    // Initial languages for new users
    private val initialLanguageCodes = listOf("ja", "en", "ko", "zh")

    fun targetLanguageCodeFlow(context: Context): Flow<String> {
        return context.languageDataStore.data.map { preferences ->
            preferences[targetLanguageKey] ?: LanguageCatalog.defaultTargetLanguage.code
        }
    }

    fun sourceLanguageCodeFlow(context: Context): Flow<String> {
        return context.languageDataStore.data.map { preferences ->
            preferences[sourceLanguageKey] ?: LanguageCatalog.defaultSourceLanguage.code
        }
    }

    fun recentTargetLanguageCodesFlow(context: Context): Flow<List<String>> {
        return context.languageDataStore.data.map { preferences ->
            val stored = preferences[recentTargetLanguageCodesKey]
            stored?.split(',')?.map { it.trim() }?.filter { LanguageCatalog.findByCode(it) != null }
                ?.distinct()?.take(MAX_RECENT_LANGUAGES)
                ?: initialLanguageCodes
        }
    }

    fun recentSourceLanguageCodesFlow(context: Context): Flow<List<String>> {
        return context.languageDataStore.data.map { preferences ->
            val stored = preferences[recentSourceLanguageCodesKey]
            stored?.split(',')?.map { it.trim() }?.filter { LanguageCatalog.findByCode(it) != null }
                ?.distinct()?.take(MAX_RECENT_LANGUAGES)
                ?: initialLanguageCodes
        }
    }

    suspend fun saveTargetLanguage(context: Context, languageOption: LanguageOption) {
        context.languageDataStore.edit { preferences ->
            preferences[targetLanguageKey] = languageOption.code
            val currentRecent = (preferences[recentTargetLanguageCodesKey]
                ?: initialLanguageCodes.joinToString(","))
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() && it != languageOption.code }

            preferences[recentTargetLanguageCodesKey] =
                (listOf(languageOption.code) + currentRecent)
                    .take(MAX_RECENT_LANGUAGES)
                    .joinToString(separator = ",")
        }
    }

    suspend fun saveSourceLanguage(context: Context, languageOption: LanguageOption) {
        context.languageDataStore.edit { preferences ->
            preferences[sourceLanguageKey] = languageOption.code
            val currentRecent = (preferences[recentSourceLanguageCodesKey]
                ?: initialLanguageCodes.joinToString(","))
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() && it != languageOption.code }

            preferences[recentSourceLanguageCodesKey] =
                (listOf(languageOption.code) + currentRecent)
                    .take(MAX_RECENT_LANGUAGES)
                    .joinToString(separator = ",")
        }
    }

    suspend fun getTargetPromptLabel(context: Context): String {
        val code = targetLanguageCodeFlow(context).first()
        return LanguageCatalog.findByCode(code)?.promptLabel
            ?: LanguageCatalog.defaultTargetLanguage.promptLabel
    }

    suspend fun getSourceLanguageCode(context: Context): String {
        return sourceLanguageCodeFlow(context).first()
    }

    suspend fun getSelectedTargetLanguage(context: Context): LanguageOption {
        val code = targetLanguageCodeFlow(context).first()
        return LanguageCatalog.findByCode(code) ?: LanguageCatalog.defaultTargetLanguage
    }

    suspend fun getSelectedSourceLanguage(context: Context): LanguageOption {
        val code = sourceLanguageCodeFlow(context).first()
        return LanguageCatalog.findByCode(code) ?: LanguageCatalog.defaultSourceLanguage
    }

    suspend fun getRecentTargetLanguages(context: Context): List<LanguageOption> {
        return recentTargetLanguageCodesFlow(context).first()
            .mapNotNull(LanguageCatalog::findByCode)
    }

    suspend fun getRecentSourceLanguages(context: Context): List<LanguageOption> {
        return recentSourceLanguageCodesFlow(context).first()
            .mapNotNull(LanguageCatalog::findByCode)
    }
}

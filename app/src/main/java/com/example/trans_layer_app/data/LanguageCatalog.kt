package com.example.trans_layer_app.data

import com.example.trans_layer_app.domain.models.LanguageOption
import java.util.Locale

object LanguageCatalog {
    val allLanguages: List<LanguageOption> by lazy {
        Locale.getAvailableLocales()
            .mapNotNull { locale ->
                val code = locale.language.trim()
                if (code.isBlank() || code == "und") {
                    null
                } else {
                    code
                }
            }
            .distinct()
            .map { code ->
                val locale = Locale.forLanguageTag(code)
                LanguageOption(
                    label = locale.getDisplayLanguage(locale)
                        .ifBlank { locale.getDisplayLanguage(Locale.ENGLISH) }
                        .trim()
                        .replaceFirstChar { firstChar ->
                            if (firstChar.isLowerCase()) {
                                firstChar.titlecase(locale)
                            } else {
                                firstChar.toString()
                            }
                        }
                        .ifBlank { code.uppercase(Locale.ENGLISH) },
                    code = code,
                    promptLabel = locale.getDisplayLanguage(Locale.ENGLISH)
                        .ifBlank { code.uppercase(Locale.ENGLISH) }
                )
            }
            .sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    val defaultTargetLanguage: LanguageOption by lazy {
        findByCode("ja") ?: allLanguages.first()
    }

    val defaultSourceLanguage: LanguageOption by lazy {
        findByCode("en") ?: allLanguages.first()
    }

    fun findByCode(code: String): LanguageOption? {
        return allLanguages.firstOrNull { it.code == code }
    }
}

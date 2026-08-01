package com.example.trans_layer_app.data.config

import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

/**
 * ML Kit TextRecognizer の種類を管理する列挙型。
 * 各言語コードに対応する認識器の設定とマッピングを担当します。
 */
enum class TextRecognitionType(val supportedCodes: Set<String>) {
    JAPANESE(setOf("ja")),
    KOREAN(setOf("ko")),
    CHINESE(setOf("zh")),
    DEVANAGARI(setOf("hi", "mr", "ne")),
    LATIN(emptySet());

    /**
     * 対応する ML Kit の設定オプションを生成します。
     */
    fun createOptions(): TextRecognizerOptionsInterface {
        return when (this) {
            JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
            KOREAN -> KoreanTextRecognizerOptions.Builder().build()
            CHINESE -> ChineseTextRecognizerOptions.Builder().build()
            DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
            LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
        }
    }

    companion object {
        /**
         * 言語コードから最適な認識器タイプを判定します。
         */
        fun fromLanguageCode(code: String): TextRecognitionType {
            val normalized = code.lowercase(Locale.ROOT)
            return entries.find { normalized in it.supportedCodes } ?: LATIN
        }
    }
}

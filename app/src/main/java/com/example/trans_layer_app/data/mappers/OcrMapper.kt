package com.example.trans_layer_app.data.mappers

import com.example.trans_layer_app.data.models.OcrItem
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCR データを、AI に送るプロンプト用の JSON に変換するマッパー。
 *
 * AI からの応答の変換は、generateObject() が AiTranslationResponse として直接行う。
 */
@Singleton
class OcrMapper @Inject constructor(
    private val json: Json
) {
    /**
     * OCRアイテムのリストを、AIプロンプト送信用にJSON文字列に変換します。
     */
    fun toJsonForPrompt(items: List<OcrItem>): String {
        return json.encodeToString(items)
    }
}

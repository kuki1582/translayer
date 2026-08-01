package com.example.trans_layer_app.data.mappers

import android.util.Log
import com.example.trans_layer_app.data.models.OcrItem
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCRデータとJSONの相互変換を管理するマッパー。
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

    /**
     * AI（Gemini）からのレスポンス文字列をパースして、[OcrItem] のリストに変換します。
     */
    fun toOcrItemList(raw: String?): List<OcrItem> {
        val clean = raw
            ?.replace("```json", "")
            ?.replace("```", "")
            ?.trim()
            .orEmpty()

        if (clean.isBlank()) return emptyList()

        return try {
            json.decodeFromString<List<OcrItem>>(clean)
        } catch (e: Exception) {
            Log.e("OcrMapper", "JSONパースエラー: ${e.message}")
            emptyList()
        }
    }
}

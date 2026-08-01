package com.example.trans_layer_app.domain.repository

import android.graphics.Bitmap
import com.example.trans_layer_app.data.models.OcrItem

/**
 * 画面上のテキスト認識と翻訳を処理するリポジトリ。
 */
interface TranslationRepository {
    /**
     * 与えられた [bitmap] からテキストを抽出し、指定された [targetLanguage] に翻訳します。
     * @param sourceLanguageCode 認識対象の言語コード (例: "ja", "ko", "zh", "en")
     * @return 翻訳された [OcrItem] のリスト。失敗時やテキストがない場合は空リスト。
     */
    suspend fun translateScreen(bitmap: Bitmap, sourceLanguageCode: String, targetLanguage: String): List<OcrItem>
}

package com.example.trans_layer_app.domain.models

import com.example.trans_layer_app.data.models.OcrItem

/**
 * 画面翻訳の結果。
 *
 * 失敗の種類を区別することで、ユーザーに原因に合ったメッセージを出せるようにする。
 * （以前は失敗がすべて空リストになり、「文字が見つからない」と表示されていた）
 */
sealed interface TranslationResult {
    /** 翻訳に成功した。 */
    data class Success(val items: List<OcrItem>) : TranslationResult

    /** OCR で文字が見つからなかった。 */
    data object NoTextFound : TranslationResult

    /** AI の出力がトークン上限に達し、途中で切れた。 */
    data object ResponseTruncated : TranslationResult

    /** AI の応答が空、または想定した JSON として読み取れなかった。 */
    data object InvalidResponse : TranslationResult

    /**
     * サーバー側の問題で AI を利用できない（API の残高切れ、サーバーエラーなど）。
     * 時間をおいても直らない場合があるため、一時的な失敗とは分けて扱う。
     */
    data object ServiceUnavailable : TranslationResult

    /** 通信エラーなど、上記以外の失敗。 */
    data class Failed(val cause: Throwable) : TranslationResult
}

package com.example.trans_layer_app.domain.models

/**
 * アプリ内で使用される言語選択肢の情報を保持するデータクラス。
 *
 * UI表示、設定の保存、およびAIへの指示（プロンプト）に使用されます。
 *
 * @property label ユーザーインターフェースに表示される言語名。
 * 可能な限りその言語自体の表記（例: "日本語"、"English"、"Français"）が使用され、
 * 読みやすさのために先頭が大文字に整形されています。
 *
 * @property code 言語を識別するためのISO 639-1コード（例: "ja"、"en"、"fr"）。
 * DataStoreへの保存や、システムのロケール判定に使用されます。
 *
 * @property promptLabel AI（Firebase AIなど）への指示に使用するための英語表記の言語名（例: "Japanese"、"English"）。
 * AIモデルが理解しやすいように、常に一貫して英語の名称が保持されます。
 */
data class LanguageOption(
    val label: String,
    val code: String,
    val promptLabel: String
) {
    companion object {
        val PREVIEWS = listOf(
            LanguageOption("日本語", "ja", "Japanese"),
            LanguageOption("English", "en", "English"),
            LanguageOption("Français", "fr", "French"),
            LanguageOption("Deutsch", "de", "German")
        )
    }
}

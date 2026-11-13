package com.example.trans_layer_app.data.models

import com.google.firebase.ai.annotations.Generable
import com.google.firebase.ai.annotations.Guide
import kotlinx.serialization.Serializable

/**
 * AI (Gemini) から返される翻訳結果。
 *
 * firebase-ai-ksp-processor がこのクラスから JSON スキーマを生成し
 * (`AiTranslationResponse.firebaseAISchema()`)、応答もこのクラスとして直接受け取る。
 * スキーマと型を手書きで二重管理しないため、両者が食い違うことがない。
 *
 * 注意: スキーマのキーには Kotlin のプロパティ名がそのまま使われ、@SerialName は反映されない。
 * JSON のキー名を変えたい場合は、プロパティ名を変えること。
 */
@Serializable
@Generable(description = "Translation results for the input texts")
data class AiTranslationResponse(
    @Guide(description = "One result for each input item, in the same order")
    val items: List<AiTranslatedText>
) {
    // KSP が firebaseAISchema() 拡張関数を生成するために必要
    companion object
}

/** 1 件分の翻訳結果。 */
@Serializable
@Generable(description = "A single translated text")
data class AiTranslatedText(
    @Guide(description = "The same ID as the \"i\" value of the input item")
    val id: Int,
    @Guide(description = "The translated text in the target language")
    val text: String
) {
    companion object
}

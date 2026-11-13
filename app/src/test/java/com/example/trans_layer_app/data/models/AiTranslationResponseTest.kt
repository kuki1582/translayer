package com.example.trans_layer_app.data.models

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GenerateObjectResponse.getObject() は、設定なしの Json (デフォルト設定) で応答を変換する。
 * それと同じ条件で、想定した形の JSON を読めることを確認する。
 */
class AiTranslationResponseTest {

    @Test
    fun `スキーマどおりのJSONを変換できる`() {
        val raw = """{"items":[{"id":1,"text":"Hello"},{"id":2,"text":"World"}]}"""

        val result = Json.decodeFromString<AiTranslationResponse>(raw)

        assertEquals(
            AiTranslationResponse(
                items = listOf(
                    AiTranslatedText(id = 1, text = "Hello"),
                    AiTranslatedText(id = 2, text = "World")
                )
            ),
            result
        )
    }
}

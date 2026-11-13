package com.example.trans_layer_app.data.mappers

import com.example.trans_layer_app.data.models.OcrItem
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrMapperTest {

    // NetworkModule と同じ設定
    private val mapper = OcrMapper(
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    )

    @Test
    fun `プロンプト用のJSONでは短いキーを使い、位置情報は含めない`() {
        val items = listOf(
            OcrItem(id = 1, text = "こんにちは", box2d = listOf(10, 20, 30, 40)),
            OcrItem(id = 2, text = "世界", box2d = listOf(50, 60, 70, 80))
        )

        assertEquals(
            """[{"i":1,"t":"こんにちは"},{"i":2,"t":"世界"}]""",
            mapper.toJsonForPrompt(items)
        )
    }
}

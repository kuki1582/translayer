package com.example.trans_layer_app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OcrItem(
    @SerialName("i")
    val id: Int,
    @SerialName("t")
    val text: String,
    @kotlinx.serialization.Transient // JSONシリアライズ（Geminiへの送信）からは除外する
    val box2d: List<Int> = emptyList()
)

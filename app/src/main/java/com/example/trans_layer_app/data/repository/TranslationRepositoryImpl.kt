package com.example.trans_layer_app.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.trans_layer_app.data.config.TextRecognitionType
import com.example.trans_layer_app.data.mappers.OcrMapper
import com.example.trans_layer_app.data.models.OcrItem
import com.example.trans_layer_app.domain.repository.TranslationRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.remoteconfig.remoteConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

class TranslationRepositoryImpl @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val ocrMapper: OcrMapper
) : TranslationRepository {

    private val recognizers = mutableMapOf<TextRecognitionType, TextRecognizer>()

    override suspend fun translateScreen(
        bitmap: Bitmap,
        sourceLanguageCode: String,
        targetLanguage: String
    ): List<OcrItem> = withContext(Dispatchers.IO) {
        try {
            val ocrItems = extractOcrItems(bitmap, sourceLanguageCode)
            if (ocrItems.isEmpty()) return@withContext emptyList()

            val remoteConfig = Firebase.remoteConfig
            val userPrompt = remoteConfig.getString("prompt")
                .replace("{{target_language}}", targetLanguage)
                .replace("{{json_data}}", ocrMapper.toJsonForPrompt(ocrItems))

            val response = generativeModel.generateContent(userPrompt)
            val responseText = response.text ?: throw Exception("AIからの応答が空でした")
            val translatedItems = ocrMapper.toOcrItemList(responseText)

            ocrItems.mapNotNull { item ->
                val translatedText = translatedItems.firstOrNull { it.id == item.id }?.text?.trim()
                if (translatedText.isNullOrEmpty()) null else item.copy(text = translatedText)
            }
        } catch (e: Exception) {
            Log.e("TranslationRepo", "Error during translation: ${e.message}")
            throw e
        }
    }

    private suspend fun extractOcrItems(bitmap: Bitmap, sourceLanguageCode: String): List<OcrItem> {
        val visionText = recognizeText(bitmap, sourceLanguageCode)
        val width = bitmap.width.toFloat().coerceAtLeast(1f)
        val height = bitmap.height.toFloat().coerceAtLeast(1f)
        val items = mutableListOf<OcrItem>()
        var id = 1

        visionText.textBlocks.forEach { block ->
            val rawText = block.text.trim()
            if (rawText.isBlank()) return@forEach
            val box = block.boundingBox ?: return@forEach
            val box2d = listOf(
                ((box.top / height) * 1000f).roundToInt(),
                ((box.left / width) * 1000f).roundToInt(),
                ((box.bottom / height) * 1000f).roundToInt(),
                ((box.right / width) * 1000f).roundToInt()
            )
            items.add(OcrItem(id = id, text = rawText, box2d = box2d))
            id++
        }
        return items
    }

    private suspend fun recognizeText(
        bitmap: Bitmap,
        sourceLanguageCode: String
    ): Text = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = getRecognizer(sourceLanguageCode)
        recognizer.process(image)
            .addOnSuccessListener { result -> cont.resume(result) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    private fun getRecognizer(sourceLanguageCode: String): TextRecognizer {
        val type = TextRecognitionType.fromLanguageCode(sourceLanguageCode)
        return recognizers.getOrPut(type) {
            TextRecognition.getClient(type.createOptions())
        }
    }
}

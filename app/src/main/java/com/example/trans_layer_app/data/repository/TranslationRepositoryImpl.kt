package com.example.trans_layer_app.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.trans_layer_app.data.config.TextRecognitionType
import com.example.trans_layer_app.data.mappers.OcrMapper
import com.example.trans_layer_app.data.models.AiTranslatedText
import com.example.trans_layer_app.data.models.AiTranslationResponse
import com.example.trans_layer_app.data.models.OcrItem
import com.example.trans_layer_app.data.models.firebaseAISchema
import com.example.trans_layer_app.domain.models.TranslationResult
import com.example.trans_layer_app.domain.repository.TranslationRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.FinishReason
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.CancellationException
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

    private companion object {
        const val TAG = "TranslationRepo"
    }

    override suspend fun translateScreen(
        bitmap: Bitmap,
        sourceLanguageCode: String,
        targetLanguage: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        try {
            val ocrItems = extractOcrItems(bitmap, sourceLanguageCode)
            if (ocrItems.isEmpty()) return@withContext TranslationResult.NoTextFound

            val userPrompt = Firebase.remoteConfig.getString("prompt")
                .replace("{{target_language}}", targetLanguage)
                .replace("{{json_data}}", ocrMapper.toJsonForPrompt(ocrItems))

            val response = generativeModel.generateObject(
                AiTranslationResponse.firebaseAISchema(),
                userPrompt
            )
            if (response.response.candidates.firstOrNull()?.finishReason == FinishReason.MAX_TOKENS) {
                return@withContext TranslationResult.ResponseTruncated
            }

            // getObject() は、応答が空のときや JSON として読めないときに null を返す
            val translated = response.getObject()
                ?: return@withContext TranslationResult.InvalidResponse

            mergeTranslations(ocrItems, translated.items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseStoppedException) {
            // 生成が途中で止まった場合。トークン上限なら専用の結果にする
            val finishReason = e.response.candidates.firstOrNull()?.finishReason
            if (finishReason == FinishReason.MAX_TOKENS) {
                TranslationResult.ResponseTruncated
            } else {
                Log.e(TAG, "Generation stopped: $finishReason", e)
                TranslationResult.Failed(e)
            }
        } catch (e: ServerException) {
            // 残高切れ (429 prepayment credits depleted) などもこの例外で届く。
            // メッセージ文字列での判定は文言の変更で壊れるため、まとめてサービス側の問題として扱う
            Log.e(TAG, "Server error during translation", e)
            TranslationResult.ServiceUnavailable
        } catch (e: Exception) {
            Log.e(TAG, "Error during translation", e)
            TranslationResult.Failed(e)
        }
    }

    /**
     * OCR の結果に、AI が返した翻訳文を ID で対応付けて差し込む。
     * 位置情報 (box2d) は AI に送っていないため、元の OCR 結果のものを使う。
     */
    private fun mergeTranslations(
        ocrItems: List<OcrItem>,
        translatedItems: List<AiTranslatedText>
    ): TranslationResult {
        // ID ごとに検索するとループの中でループになる (O(n²)) ため、先に Map にする
        val translatedTextById = translatedItems.associate { it.id to it.text.trim() }
        val merged = ocrItems.mapNotNull { item ->
            val translatedText = translatedTextById[item.id]
            if (translatedText.isNullOrEmpty()) null else item.copy(text = translatedText)
        }
        // JSON としては読めたが、ID が 1 つも一致しなかった場合も不正な応答として扱う
        return if (merged.isEmpty()) {
            TranslationResult.InvalidResponse
        } else {
            TranslationResult.Success(merged)
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

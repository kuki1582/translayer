package com.example.trans_layer_app.di

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.remoteconfig.remoteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AIモデルの生成と設定を管理する Hilt モジュール。
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    /**
     * Firebase Remote Config の設定に基づいた [GenerativeModel] を提供します。
     */
    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        val remoteConfig = Firebase.remoteConfig
        
        val jsonSchema = Schema.array(
            Schema.obj(
                mapOf(
                    "i" to Schema.string(),
                    "t" to Schema.string()
                )
            )
        )

        val config = generationConfig {
            maxOutputTokens = remoteConfig.getLong("max_output_tokens").toInt()
            temperature = remoteConfig.getDouble("temperature").toFloat()
            responseMimeType = "application/json"
            responseSchema = jsonSchema
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.MINIMAL
            }
        }

        return Firebase.ai(
            useLimitedUseAppCheckTokens = remoteConfig.getBoolean("use_limited_use_appcheck_tokens"),
            backend = GenerativeBackend.googleAI()
        ).generativeModel(
            modelName = remoteConfig.getString("model_name"),
            systemInstruction = content { text(remoteConfig.getString("system_instructions")) },
            generationConfig = config,
            safetySettings = listOf(
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.ONLY_HIGH)
            )
        )
    }
}

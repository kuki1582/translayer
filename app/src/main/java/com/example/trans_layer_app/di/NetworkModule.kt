package com.example.trans_layer_app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * アプリ全体で使用するシリアライズ（JSON）設定を管理するモジュール。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            // 未知のキーを無視する（APIの追加フィールドでクラッシュしないようにする）
            ignoreUnknownKeys = true
            // 緩いパースを許可する
            isLenient = true
            // デフォルト値を持つフィールドがJSONにない場合もデフォルト値を使用する
            encodeDefaults = true
            // 整形されたJSONを出力する場合は true (デバッグ時などに便利)
            prettyPrint = false
        }
    }
}

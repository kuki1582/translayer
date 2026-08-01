package com.example.trans_layer_app.di

import com.example.trans_layer_app.data.repository.TranslationRepositoryImpl
import com.example.trans_layer_app.domain.repository.TranslationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * アプリケーション全体のインターフェースと実装の紐付けを管理する Hilt モジュール。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTranslationRepository(
        impl: TranslationRepositoryImpl
    ): TranslationRepository
}

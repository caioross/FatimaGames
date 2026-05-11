package com.fatimagames.app.core.di

import com.fatimagames.app.core.feedback.HapticController
import com.fatimagames.app.core.feedback.SoundController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeedbackModule {
    // HapticController e SoundController já são @Singleton com @Inject constructor,
    // então o Hilt os fornece automaticamente. Este módulo está aqui para futuras
    // configurações específicas (e.g., volume padrão, paths de assets).
}

package com.fatimagames.app.core.di

import com.fatimagames.app.data.repository.GameStateRepositoryImpl
import com.fatimagames.app.data.repository.RecordRepositoryImpl
import com.fatimagames.app.domain.repository.GameStateRepository
import com.fatimagames.app.domain.repository.RecordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindRecordRepo(impl: RecordRepositoryImpl): RecordRepository

    @Binds @Singleton
    abstract fun bindGameStateRepo(impl: GameStateRepositoryImpl): GameStateRepository
}

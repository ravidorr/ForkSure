package com.ravidor.forksure.di

import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.AIRepositoryImpl
import com.ravidor.forksure.repository.SecurityRepository
import com.ravidor.forksure.repository.SecurityRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds AIRepository interface to AIRepositoryImpl
     */
    @Binds
    @Singleton
    abstract fun bindAIRepository(
        aiRepositoryImpl: AIRepositoryImpl
    ): AIRepository

    /**
     * Binds SecurityRepository interface to SecurityRepositoryImpl
     */
    @Binds
    @Singleton
    abstract fun bindSecurityRepository(
        securityRepositoryImpl: SecurityRepositoryImpl
    ): SecurityRepository
} 
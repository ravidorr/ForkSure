package com.ravidor.forksure.di

import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.AIRepositoryImpl
import com.ravidor.forksure.repository.SecurityRepository
import com.ravidor.forksure.repository.SecurityRepositoryImpl
import com.ravidor.forksure.repository.RecipeRepository
import com.ravidor.forksure.repository.RecipeRepositoryImpl
import com.ravidor.forksure.repository.UserPreferencesRepository
import com.ravidor.forksure.repository.UserPreferencesRepositoryImpl
import com.ravidor.forksure.repository.PreferencesCacheManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.Provides

/**
 * Hilt module for binding repository interfaces to their implementations
 * This module follows the Repository pattern for clean architecture
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds AIRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindAIRepository(
        aiRepositoryImpl: AIRepositoryImpl
    ): AIRepository

    /**
     * Binds SecurityRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindSecurityRepository(
        securityRepositoryImpl: SecurityRepositoryImpl
    ): SecurityRepository

    /**
     * Binds RecipeRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindRecipeRepository(
        recipeRepositoryImpl: RecipeRepositoryImpl
    ): RecipeRepository

    /**
     * Binds UserPreferencesRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesCacheManagerModule {
    @Provides
    @Singleton
    fun providePreferencesCacheManager(
        userPreferencesRepository: UserPreferencesRepository,
        recipeRepository: RecipeRepository
    ): PreferencesCacheManager {
        return PreferencesCacheManager(userPreferencesRepository, recipeRepository)
    }
} 
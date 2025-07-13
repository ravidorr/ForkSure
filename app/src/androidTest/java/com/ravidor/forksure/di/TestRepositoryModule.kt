package com.ravidor.forksure.di

import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.SecurityRepository
import com.ravidor.forksure.repository.FakeAIRepository
import com.ravidor.forksure.repository.FakeSecurityRepository
import com.ravidor.forksure.repository.UserPreferencesRepository
import com.ravidor.forksure.repository.RecipeRepository
import com.ravidor.forksure.repository.PreferencesCacheManager
import io.mockk.mockk
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import dagger.Provides
import android.content.Context
import com.ravidor.forksure.di.PreferencesCacheManagerModule

/**
 * Test module that replaces RepositoryModule for Android integration testing
 * Provides fake implementations of repositories for controlled testing
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class, PreferencesCacheManagerModule::class]
)
abstract class TestRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTestAIRepository(
        fakeAIRepository: FakeAIRepository
    ): AIRepository

    @Binds
    @Singleton
    abstract fun bindTestSecurityRepository(
        fakeSecurityRepository: FakeSecurityRepository
    ): SecurityRepository
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object TestRepositoryProvidesModule {
    @Provides
    @Singleton
    fun provideTestUserPreferencesRepository(): UserPreferencesRepository = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideTestRecipeRepository(): RecipeRepository = mockk(relaxed = true)

    // @Provides
    // @Singleton
    // fun provideTestPreferencesCacheManager(
    //     userPreferencesRepository: UserPreferencesRepository,
    //     recipeRepository: RecipeRepository
    // ): PreferencesCacheManager = PreferencesCacheManager(userPreferencesRepository, recipeRepository)
} 
package com.ravidor.forksure.di

import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.SecurityRepository
import com.ravidor.forksure.repository.FakeAIRepository
import com.ravidor.forksure.repository.FakeSecurityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test module that replaces RepositoryModule for unit testing
 * Provides fake implementations of repositories for controlled testing
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
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
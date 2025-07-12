package com.ravidor.forksure.di

import android.content.Context
import android.content.SharedPreferences
import com.google.ai.client.generativeai.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test module that replaces AppModule for Android integration testing
 * Provides mock implementations of dependencies
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {

    @Provides
    @Singleton
    fun provideTestGenerativeModel(): GenerativeModel {
        return mockk<GenerativeModel>(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideTestSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return mockk<SharedPreferences>(relaxed = true)
    }
} 
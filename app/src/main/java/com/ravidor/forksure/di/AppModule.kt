package com.ravidor.forksure.di

import android.content.Context
import android.content.SharedPreferences
import com.google.ai.client.generativeai.GenerativeModel
import com.ravidor.forksure.BuildConfig
import com.ravidor.forksure.SecurityConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the GenerativeModel for AI interactions
     */
    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.apiKey
        )
    }

    /**
     * Provides SharedPreferences for security-related storage
     */
    @Provides
    @Singleton
    fun provideSecuritySharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(
            SecurityConstants.PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }
} 
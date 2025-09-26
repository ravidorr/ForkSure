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
        android.util.Log.d("AppModule", "Creating GenerativeModel with API key: ${BuildConfig.apiKey.take(10)}...")
        return GenerativeModel(
            modelName = "gemini-2.0-flash",
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

    /**
     * Provides application context for injection
     */
    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context = context
} 
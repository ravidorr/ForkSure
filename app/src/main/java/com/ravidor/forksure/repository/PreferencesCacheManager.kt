package com.ravidor.forksure.repository

import com.ravidor.forksure.data.model.UserPreferences
import com.ravidor.forksure.data.model.RecipeAnalysisRequest
import com.ravidor.forksure.data.model.RecipeAnalysisResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager to coordinate atomic updates to preferences and cache.
 */
@Singleton
class PreferencesCacheManager @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val recipeRepository: RecipeRepository
) {
    private val mutex = Mutex()

    /**
     * Example atomic update: update theme and clear cache atomically.
     */
    suspend fun updateThemeAndClearCacheAtomically(theme: com.ravidor.forksure.data.model.AppTheme) = mutex.withLock {
        userPreferencesRepository.updateTheme(theme)
        recipeRepository.clearCache()
    }

    // Add more atomic methods as needed using only the public API
} 
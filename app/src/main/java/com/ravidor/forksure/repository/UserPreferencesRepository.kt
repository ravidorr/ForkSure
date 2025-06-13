package com.ravidor.forksure.repository

import com.ravidor.forksure.data.model.AppTheme
import com.ravidor.forksure.data.model.AppUsageStats
import com.ravidor.forksure.data.model.FontSize
import com.ravidor.forksure.data.model.ImageQuality
import com.ravidor.forksure.data.model.UserPreferences
import com.ravidor.forksure.data.source.local.PreferencesDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for user preferences and settings
 */
interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    val usageStats: Flow<AppUsageStats>
    
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateHapticFeedback(enabled: Boolean)
    suspend fun updateSoundEffects(enabled: Boolean)
    suspend fun updateCacheSettings(enabled: Boolean, maxSize: Int, retentionDays: Int)
    suspend fun updateAccessibilitySettings(
        enableAccessibilityFeatures: Boolean,
        fontSize: FontSize,
        enableHighContrast: Boolean,
        enableReducedMotion: Boolean
    )
    suspend fun updateAnalyticsSettings(enableAnalytics: Boolean, enableCrashReporting: Boolean)
    suspend fun updateImageQuality(quality: ImageQuality)
    suspend fun updateLanguage(language: String)
    suspend fun updateAppVersion(version: String)
    
    suspend fun addFavoriteRecipe(recipeId: String)
    suspend fun removeFavoriteRecipe(recipeId: String)
    suspend fun getFavoriteRecipeIds(): Set<String>
    
    suspend fun incrementAnalysisCount()
    suspend fun recordAppLaunch()
    suspend fun recordPhotoAnalysis()
    suspend fun recordSampleImageUsage()
    suspend fun updateSessionDuration(durationMs: Long)
    
    suspend fun resetAllSettings()
    suspend fun exportSettings(): String
    suspend fun importSettings(settingsJson: String): Boolean
}

/**
 * Implementation of UserPreferencesRepository using local data source
 */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : UserPreferencesRepository {

    override val userPreferences: Flow<UserPreferences> = preferencesDataSource.userPreferences
    override val usageStats: Flow<AppUsageStats> = preferencesDataSource.usageStats

    override suspend fun updateTheme(theme: AppTheme) {
        preferencesDataSource.updateTheme(theme)
    }

    override suspend fun updateHapticFeedback(enabled: Boolean) {
        preferencesDataSource.updateHapticFeedback(enabled)
    }

    override suspend fun updateSoundEffects(enabled: Boolean) {
        val current = preferencesDataSource.getCurrentUserPreferences()
        preferencesDataSource.saveUserPreferences(current.copy(enableSoundEffects = enabled))
    }

    override suspend fun updateCacheSettings(enabled: Boolean, maxSize: Int, retentionDays: Int) {
        preferencesDataSource.updateCacheSettings(enabled, maxSize, retentionDays)
    }

    override suspend fun updateAccessibilitySettings(
        enableAccessibilityFeatures: Boolean,
        fontSize: FontSize,
        enableHighContrast: Boolean,
        enableReducedMotion: Boolean
    ) {
        val current = preferencesDataSource.getCurrentUserPreferences()
        preferencesDataSource.saveUserPreferences(
            current.copy(
                enableAccessibilityFeatures = enableAccessibilityFeatures,
                fontSize = fontSize,
                enableHighContrast = enableHighContrast,
                enableReducedMotion = enableReducedMotion
            )
        )
    }

    override suspend fun updateAnalyticsSettings(enableAnalytics: Boolean, enableCrashReporting: Boolean) {
        val current = preferencesDataSource.getCurrentUserPreferences()
        preferencesDataSource.saveUserPreferences(
            current.copy(
                enableAnalytics = enableAnalytics,
                enableCrashReporting = enableCrashReporting
            )
        )
    }

    override suspend fun updateImageQuality(quality: ImageQuality) {
        val current = preferencesDataSource.getCurrentUserPreferences()
        preferencesDataSource.saveUserPreferences(current.copy(preferredImageQuality = quality))
    }

    override suspend fun updateLanguage(language: String) {
        val current = preferencesDataSource.getCurrentUserPreferences()
        preferencesDataSource.saveUserPreferences(current.copy(language = language))
    }

    override suspend fun updateAppVersion(version: String) {
        val current = preferencesDataSource.getCurrentUserPreferences()
        preferencesDataSource.saveUserPreferences(current.copy(lastAppVersion = version))
    }

    override suspend fun addFavoriteRecipe(recipeId: String) {
        preferencesDataSource.addFavoriteRecipe(recipeId)
    }

    override suspend fun removeFavoriteRecipe(recipeId: String) {
        preferencesDataSource.removeFavoriteRecipe(recipeId)
    }

    override suspend fun getFavoriteRecipeIds(): Set<String> {
        return preferencesDataSource.getCurrentUserPreferences().favoriteRecipeIds
    }

    override suspend fun incrementAnalysisCount() {
        preferencesDataSource.incrementAnalysisCount()
    }

    override suspend fun recordAppLaunch() {
        preferencesDataSource.recordAppLaunch()
    }

    override suspend fun recordPhotoAnalysis() {
        val current = preferencesDataSource.getCurrentUsageStats()
        preferencesDataSource.saveUsageStats(
            current.copy(totalPhotosAnalyzed = current.totalPhotosAnalyzed + 1)
        )
    }

    override suspend fun recordSampleImageUsage() {
        val current = preferencesDataSource.getCurrentUsageStats()
        preferencesDataSource.saveUsageStats(
            current.copy(totalSampleImagesUsed = current.totalSampleImagesUsed + 1)
        )
    }

    override suspend fun updateSessionDuration(durationMs: Long) {
        val current = preferencesDataSource.getCurrentUsageStats()
        val totalSessions = current.totalLaunches
        val currentAverage = current.averageSessionDuration
        
        // Calculate new average session duration
        val newAverage = if (totalSessions > 0) {
            ((currentAverage * (totalSessions - 1)) + durationMs) / totalSessions
        } else {
            durationMs
        }
        
        preferencesDataSource.saveUsageStats(
            current.copy(
                averageSessionDuration = newAverage,
                lastUsedDate = System.currentTimeMillis()
            )
        )
    }

    override suspend fun resetAllSettings() {
        preferencesDataSource.clearAllPreferences()
    }

    override suspend fun exportSettings(): String {
        val preferences = preferencesDataSource.getCurrentUserPreferences()
        val stats = preferencesDataSource.getCurrentUsageStats()
        
        return buildString {
            appendLine("{")
            appendLine("  \"theme\": \"${preferences.theme.name}\",")
            appendLine("  \"language\": \"${preferences.language}\",")
            appendLine("  \"enableHapticFeedback\": ${preferences.enableHapticFeedback},")
            appendLine("  \"enableSoundEffects\": ${preferences.enableSoundEffects},")
            appendLine("  \"cacheRecipes\": ${preferences.cacheRecipes},")
            appendLine("  \"maxCacheSize\": ${preferences.maxCacheSize},")
            appendLine("  \"cacheRetentionDays\": ${preferences.cacheRetentionDays},")
            appendLine("  \"enableAnalytics\": ${preferences.enableAnalytics},")
            appendLine("  \"enableCrashReporting\": ${preferences.enableCrashReporting},")
            appendLine("  \"preferredImageQuality\": \"${preferences.preferredImageQuality.name}\",")
            appendLine("  \"enableAccessibilityFeatures\": ${preferences.enableAccessibilityFeatures},")
            appendLine("  \"fontSize\": \"${preferences.fontSize.name}\",")
            appendLine("  \"enableHighContrast\": ${preferences.enableHighContrast},")
            appendLine("  \"enableReducedMotion\": ${preferences.enableReducedMotion},")
            appendLine("  \"totalAnalysisCount\": ${preferences.totalAnalysisCount},")
            appendLine("  \"favoriteRecipeIds\": [${preferences.favoriteRecipeIds.joinToString(",") { "\"$it\"" }}],")
            appendLine("  \"usageStats\": {")
            appendLine("    \"totalLaunches\": ${stats.totalLaunches},")
            appendLine("    \"totalAnalyses\": ${stats.totalAnalyses},")
            appendLine("    \"totalPhotosAnalyzed\": ${stats.totalPhotosAnalyzed},")
            appendLine("    \"totalSampleImagesUsed\": ${stats.totalSampleImagesUsed},")
            appendLine("    \"averageSessionDuration\": ${stats.averageSessionDuration}")
            appendLine("  }")
            append("}")
        }
    }

    override suspend fun importSettings(settingsJson: String): Boolean {
        return try {
            // Simplified implementation - in a real app, use proper JSON parsing
            false
        } catch (e: Exception) {
            false
        }
    }
} 
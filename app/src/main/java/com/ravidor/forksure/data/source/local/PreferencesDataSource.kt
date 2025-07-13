package com.ravidor.forksure.data.source.local

import android.content.SharedPreferences
import androidx.core.content.edit
import com.ravidor.forksure.data.model.AppTheme
import com.ravidor.forksure.data.model.AppUsageStats
import com.ravidor.forksure.data.model.FontSize
import com.ravidor.forksure.data.model.ImageQuality
import com.ravidor.forksure.data.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest

/**
 * Local data source for user preferences using SharedPreferences
 */
@Singleton
class PreferencesDataSource @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    private val _userPreferences = MutableStateFlow(loadUserPreferences())
    val userPreferences: Flow<UserPreferences> = _userPreferences.asStateFlow()

    private val _usageStats = MutableStateFlow(loadUsageStats())
    val usageStats: Flow<AppUsageStats> = _usageStats.asStateFlow()

    companion object {
        // Preference keys
        private const val KEY_THEME = "theme"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_SOUND_EFFECTS = "sound_effects"
        private const val KEY_CACHE_RECIPES = "cache_recipes"
        private const val KEY_MAX_CACHE_SIZE = "max_cache_size"
        private const val KEY_AUTO_DELETE_OLD_RECIPES = "auto_delete_old_recipes"
        private const val KEY_CACHE_RETENTION_DAYS = "cache_retention_days"
        private const val KEY_ENABLE_ANALYTICS = "enable_analytics"
        private const val KEY_ENABLE_CRASH_REPORTING = "enable_crash_reporting"
        private const val KEY_PREFERRED_IMAGE_QUALITY = "preferred_image_quality"
        private const val KEY_ENABLE_ACCESSIBILITY_FEATURES = "enable_accessibility_features"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_ENABLE_HIGH_CONTRAST = "enable_high_contrast"
        private const val KEY_ENABLE_REDUCED_MOTION = "enable_reduced_motion"
        private const val KEY_LAST_APP_VERSION = "last_app_version"
        private const val KEY_FIRST_LAUNCH_DATE = "first_launch_date"
        private const val KEY_TOTAL_ANALYSIS_COUNT = "total_analysis_count"
        private const val KEY_FAVORITE_RECIPE_IDS = "favorite_recipe_ids"

        // Usage stats keys
        private const val KEY_TOTAL_LAUNCHES = "total_launches"
        private const val KEY_TOTAL_ANALYSES = "total_analyses"
        private const val KEY_TOTAL_PHOTOS_ANALYZED = "total_photos_analyzed"
        private const val KEY_TOTAL_SAMPLE_IMAGES_USED = "total_sample_images_used"
        private const val KEY_AVERAGE_SESSION_DURATION = "average_session_duration"
        private const val KEY_LAST_USED_DATE = "last_used_date"
        private const val KEY_PREFS_VERSION = "prefs_version"
        private const val KEY_PREFS_CHECKSUM = "prefs_checksum"
        private const val PREFS_VERSION = 1
    }

    private fun calculateChecksum(preferences: UserPreferences): String {
        val data = listOf(
            preferences.theme.name,
            preferences.language,
            preferences.enableHapticFeedback.toString(),
            preferences.enableSoundEffects.toString(),
            preferences.cacheRecipes.toString(),
            preferences.maxCacheSize.toString(),
            preferences.autoDeleteOldRecipes.toString(),
            preferences.cacheRetentionDays.toString(),
            preferences.enableAnalytics.toString(),
            preferences.enableCrashReporting.toString(),
            preferences.preferredImageQuality.name,
            preferences.enableAccessibilityFeatures.toString(),
            preferences.fontSize.name,
            preferences.enableHighContrast.toString(),
            preferences.enableReducedMotion.toString(),
            preferences.lastAppVersion,
            preferences.firstLaunchDate.toString(),
            preferences.totalAnalysisCount.toString(),
            preferences.favoriteRecipeIds.sorted().joinToString(",")
        ).joinToString("|")
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun isPrefsValid(preferences: UserPreferences): Boolean {
        val storedVersion = sharedPreferences.getInt(KEY_PREFS_VERSION, PREFS_VERSION)
        val storedChecksum = sharedPreferences.getString(KEY_PREFS_CHECKSUM, null)
        val calculatedChecksum = calculateChecksum(preferences)
        return storedVersion == PREFS_VERSION && storedChecksum == calculatedChecksum
    }

    /**
     * Load user preferences from SharedPreferences
     */
    private fun loadUserPreferences(): UserPreferences {
        val prefs = try {
            UserPreferences(
                theme = AppTheme.valueOf(
                    sharedPreferences.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
                ),
                language = sharedPreferences.getString(KEY_LANGUAGE, "en") ?: "en",
                enableHapticFeedback = sharedPreferences.getBoolean(KEY_HAPTIC_FEEDBACK, true),
                enableSoundEffects = sharedPreferences.getBoolean(KEY_SOUND_EFFECTS, true),
                cacheRecipes = sharedPreferences.getBoolean(KEY_CACHE_RECIPES, true),
                maxCacheSize = sharedPreferences.getInt(KEY_MAX_CACHE_SIZE, 50),
                autoDeleteOldRecipes = sharedPreferences.getBoolean(KEY_AUTO_DELETE_OLD_RECIPES, true),
                cacheRetentionDays = sharedPreferences.getInt(KEY_CACHE_RETENTION_DAYS, 30),
                enableAnalytics = sharedPreferences.getBoolean(KEY_ENABLE_ANALYTICS, false),
                enableCrashReporting = sharedPreferences.getBoolean(KEY_ENABLE_CRASH_REPORTING, true),
                preferredImageQuality = ImageQuality.valueOf(
                    sharedPreferences.getString(KEY_PREFERRED_IMAGE_QUALITY, ImageQuality.HIGH.name)
                        ?: ImageQuality.HIGH.name
                ),
                enableAccessibilityFeatures = sharedPreferences.getBoolean(KEY_ENABLE_ACCESSIBILITY_FEATURES, false),
                fontSize = FontSize.valueOf(
                    sharedPreferences.getString(KEY_FONT_SIZE, FontSize.MEDIUM.name) ?: FontSize.MEDIUM.name
                ),
                enableHighContrast = sharedPreferences.getBoolean(KEY_ENABLE_HIGH_CONTRAST, false),
                enableReducedMotion = sharedPreferences.getBoolean(KEY_ENABLE_REDUCED_MOTION, false),
                lastAppVersion = sharedPreferences.getString(KEY_LAST_APP_VERSION, "") ?: "",
                firstLaunchDate = sharedPreferences.getLong(KEY_FIRST_LAUNCH_DATE, System.currentTimeMillis()),
                totalAnalysisCount = sharedPreferences.getInt(KEY_TOTAL_ANALYSIS_COUNT, 0),
                favoriteRecipeIds = sharedPreferences.getStringSet(KEY_FAVORITE_RECIPE_IDS, emptySet()) ?: emptySet()
            )
        } catch (e: Exception) {
            sharedPreferences.edit().clear().apply()
            UserPreferences()
        }
        return if (isPrefsValid(prefs)) prefs else UserPreferences()
    }

    /**
     * Load usage statistics from SharedPreferences
     */
    private fun loadUsageStats(): AppUsageStats {
        return try {
            AppUsageStats(
                totalLaunches = sharedPreferences.getInt(KEY_TOTAL_LAUNCHES, 0),
                totalAnalyses = sharedPreferences.getInt(KEY_TOTAL_ANALYSES, 0),
                totalPhotosAnalyzed = sharedPreferences.getInt(KEY_TOTAL_PHOTOS_ANALYZED, 0),
                totalSampleImagesUsed = sharedPreferences.getInt(KEY_TOTAL_SAMPLE_IMAGES_USED, 0),
                averageSessionDuration = sharedPreferences.getLong(KEY_AVERAGE_SESSION_DURATION, 0),
                lastUsedDate = sharedPreferences.getLong(KEY_LAST_USED_DATE, System.currentTimeMillis())
            )
        } catch (e: Exception) {
            // Handle corruption, reset to defaults
            sharedPreferences.edit().clear().apply()
            AppUsageStats()
        }
    }

    /**
     * Save user preferences to SharedPreferences
     */
    suspend fun saveUserPreferences(preferences: UserPreferences) = withContext(Dispatchers.IO) {
        val checksum = calculateChecksum(preferences)
        sharedPreferences.edit {
            putString(KEY_THEME, preferences.theme.name)
            putString(KEY_LANGUAGE, preferences.language)
            putBoolean(KEY_HAPTIC_FEEDBACK, preferences.enableHapticFeedback)
            putBoolean(KEY_SOUND_EFFECTS, preferences.enableSoundEffects)
            putBoolean(KEY_CACHE_RECIPES, preferences.cacheRecipes)
            putInt(KEY_MAX_CACHE_SIZE, preferences.maxCacheSize)
            putBoolean(KEY_AUTO_DELETE_OLD_RECIPES, preferences.autoDeleteOldRecipes)
            putInt(KEY_CACHE_RETENTION_DAYS, preferences.cacheRetentionDays)
            putBoolean(KEY_ENABLE_ANALYTICS, preferences.enableAnalytics)
            putBoolean(KEY_ENABLE_CRASH_REPORTING, preferences.enableCrashReporting)
            putString(KEY_PREFERRED_IMAGE_QUALITY, preferences.preferredImageQuality.name)
            putBoolean(KEY_ENABLE_ACCESSIBILITY_FEATURES, preferences.enableAccessibilityFeatures)
            putString(KEY_FONT_SIZE, preferences.fontSize.name)
            putBoolean(KEY_ENABLE_HIGH_CONTRAST, preferences.enableHighContrast)
            putBoolean(KEY_ENABLE_REDUCED_MOTION, preferences.enableReducedMotion)
            putString(KEY_LAST_APP_VERSION, preferences.lastAppVersion)
            putLong(KEY_FIRST_LAUNCH_DATE, preferences.firstLaunchDate)
            putInt(KEY_TOTAL_ANALYSIS_COUNT, preferences.totalAnalysisCount)
            putStringSet(KEY_FAVORITE_RECIPE_IDS, preferences.favoriteRecipeIds)
            putInt(KEY_PREFS_VERSION, PREFS_VERSION)
            putString(KEY_PREFS_CHECKSUM, checksum)
        }
        _userPreferences.value = preferences
    }

    /**
     * Save usage statistics to SharedPreferences
     */
    suspend fun saveUsageStats(stats: AppUsageStats) = withContext(Dispatchers.IO) {
        sharedPreferences.edit {
            putInt(KEY_TOTAL_LAUNCHES, stats.totalLaunches)
            putInt(KEY_TOTAL_ANALYSES, stats.totalAnalyses)
            putInt(KEY_TOTAL_PHOTOS_ANALYZED, stats.totalPhotosAnalyzed)
            putInt(KEY_TOTAL_SAMPLE_IMAGES_USED, stats.totalSampleImagesUsed)
            putLong(KEY_AVERAGE_SESSION_DURATION, stats.averageSessionDuration)
            putLong(KEY_LAST_USED_DATE, stats.lastUsedDate)
        }
        _usageStats.value = stats
    }

    /**
     * Update specific preference
     */
    suspend fun updateTheme(theme: AppTheme) {
        val current = _userPreferences.value
        saveUserPreferences(current.copy(theme = theme))
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        val current = _userPreferences.value
        saveUserPreferences(current.copy(enableHapticFeedback = enabled))
    }

    suspend fun updateCacheSettings(enabled: Boolean, maxSize: Int, retentionDays: Int) {
        val current = _userPreferences.value
        saveUserPreferences(
            current.copy(
                cacheRecipes = enabled,
                maxCacheSize = maxSize,
                cacheRetentionDays = retentionDays
            )
        )
    }

    suspend fun addFavoriteRecipe(recipeId: String) {
        val current = _userPreferences.value
        val updatedFavorites = current.favoriteRecipeIds + recipeId
        saveUserPreferences(current.copy(favoriteRecipeIds = updatedFavorites))
    }

    suspend fun removeFavoriteRecipe(recipeId: String) {
        val current = _userPreferences.value
        val updatedFavorites = current.favoriteRecipeIds - recipeId
        saveUserPreferences(current.copy(favoriteRecipeIds = updatedFavorites))
    }

    /**
     * Increment analysis count
     */
    suspend fun incrementAnalysisCount() {
        val currentPrefs = _userPreferences.value
        val currentStats = _usageStats.value
        
        saveUserPreferences(currentPrefs.copy(totalAnalysisCount = currentPrefs.totalAnalysisCount + 1))
        saveUsageStats(currentStats.copy(totalAnalyses = currentStats.totalAnalyses + 1))
    }

    /**
     * Record app launch
     */
    suspend fun recordAppLaunch() {
        val current = _usageStats.value
        saveUsageStats(current.copy(totalLaunches = current.totalLaunches + 1))
    }

    /**
     * Get current user preferences value
     */
    suspend fun getCurrentUserPreferences(): UserPreferences = withContext(Dispatchers.IO) {
        return@withContext _userPreferences.value
    }

    /**
     * Get current usage stats value
     */
    suspend fun getCurrentUsageStats(): AppUsageStats = withContext(Dispatchers.IO) {
        return@withContext _usageStats.value
    }

    /**
     * Clear all preferences (for testing or reset)
     */
    suspend fun clearAllPreferences() = withContext(Dispatchers.IO) {
        sharedPreferences.edit { clear() }
        _userPreferences.value = UserPreferences()
        _usageStats.value = AppUsageStats()
    }
} 
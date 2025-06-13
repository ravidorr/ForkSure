package com.ravidor.forksure.data.model

import androidx.compose.runtime.Immutable

/**
 * User preferences data model
 */
@Immutable
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "en",
    val enableHapticFeedback: Boolean = true,
    val enableSoundEffects: Boolean = true,
    val cacheRecipes: Boolean = true,
    val maxCacheSize: Int = 50,
    val autoDeleteOldRecipes: Boolean = true,
    val cacheRetentionDays: Int = 30,
    val enableAnalytics: Boolean = false,
    val enableCrashReporting: Boolean = true,
    val preferredImageQuality: ImageQuality = ImageQuality.HIGH,
    val enableAccessibilityFeatures: Boolean = false,
    val fontSize: FontSize = FontSize.MEDIUM,
    val enableHighContrast: Boolean = false,
    val enableReducedMotion: Boolean = false,
    val lastAppVersion: String = "",
    val firstLaunchDate: Long = System.currentTimeMillis(),
    val totalAnalysisCount: Int = 0,
    val favoriteRecipeIds: Set<String> = emptySet()
)

/**
 * App theme options
 */
@Immutable
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Image quality preferences
 */
@Immutable
enum class ImageQuality {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Font size preferences
 */
@Immutable
enum class FontSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}

/**
 * Analytics event data
 */
@Immutable
data class AnalyticsEvent(
    val eventName: String,
    val parameters: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String? = null
)

/**
 * App usage statistics
 */
@Immutable
data class AppUsageStats(
    val totalLaunches: Int = 0,
    val totalAnalyses: Int = 0,
    val totalPhotosAnalyzed: Int = 0,
    val totalSampleImagesUsed: Int = 0,
    val averageSessionDuration: Long = 0,
    val lastUsedDate: Long = System.currentTimeMillis(),
    val favoriteFeatures: Map<String, Int> = emptyMap(),
    val errorCounts: Map<String, Int> = emptyMap()
) 
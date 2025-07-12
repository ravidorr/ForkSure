package com.ravidor.forksure.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Comprehensive unit tests for UserPreferences data model
 * Tests preferences creation, validation, and transformations
 */
class UserPreferencesTest {

    @Test
    fun `UserPreferences should have proper default values`() {
        // Given - localThis pattern
        val localThis = object {
            // No specific values needed for default test
        }

        // When
        val userPreferences = UserPreferences()

        // Then
        assertThat(userPreferences.theme).isEqualTo(AppTheme.SYSTEM)
        assertThat(userPreferences.language).isEqualTo("en")
        assertThat(userPreferences.enableHapticFeedback).isTrue()
        assertThat(userPreferences.enableSoundEffects).isTrue()
        assertThat(userPreferences.cacheRecipes).isTrue()
        assertThat(userPreferences.maxCacheSize).isEqualTo(50)
        assertThat(userPreferences.autoDeleteOldRecipes).isTrue()
        assertThat(userPreferences.cacheRetentionDays).isEqualTo(30)
        assertThat(userPreferences.enableAnalytics).isFalse()
        assertThat(userPreferences.enableCrashReporting).isTrue()
        assertThat(userPreferences.preferredImageQuality).isEqualTo(ImageQuality.HIGH)
        assertThat(userPreferences.enableAccessibilityFeatures).isFalse()
        assertThat(userPreferences.fontSize).isEqualTo(FontSize.MEDIUM)
        assertThat(userPreferences.enableHighContrast).isFalse()
        assertThat(userPreferences.enableReducedMotion).isFalse()
        assertThat(userPreferences.lastAppVersion).isEmpty()
        assertThat(userPreferences.firstLaunchDate).isGreaterThan(0L)
        assertThat(userPreferences.totalAnalysisCount).isEqualTo(0)
        assertThat(userPreferences.favoriteRecipeIds).isEmpty()
    }

    @Test
    fun `UserPreferences should create with custom values`() {
        // Given - localThis pattern
        val localThis = object {
            val theme = AppTheme.DARK
            val language = "es"
            val enableHapticFeedback = false
            val enableSoundEffects = false
            val cacheRecipes = false
            val maxCacheSize = 100
            val autoDeleteOldRecipes = false
            val cacheRetentionDays = 60
            val enableAnalytics = true
            val enableCrashReporting = false
            val preferredImageQuality = ImageQuality.LOW
            val enableAccessibilityFeatures = true
            val fontSize = FontSize.LARGE
            val enableHighContrast = true
            val enableReducedMotion = true
            val lastAppVersion = "1.2.3"
            val firstLaunchDate = 1234567890L
            val totalAnalysisCount = 50
            val favoriteRecipeIds = setOf("recipe1", "recipe2", "recipe3")
        }

        // When
        val userPreferences = UserPreferences(
            theme = localThis.theme,
            language = localThis.language,
            enableHapticFeedback = localThis.enableHapticFeedback,
            enableSoundEffects = localThis.enableSoundEffects,
            cacheRecipes = localThis.cacheRecipes,
            maxCacheSize = localThis.maxCacheSize,
            autoDeleteOldRecipes = localThis.autoDeleteOldRecipes,
            cacheRetentionDays = localThis.cacheRetentionDays,
            enableAnalytics = localThis.enableAnalytics,
            enableCrashReporting = localThis.enableCrashReporting,
            preferredImageQuality = localThis.preferredImageQuality,
            enableAccessibilityFeatures = localThis.enableAccessibilityFeatures,
            fontSize = localThis.fontSize,
            enableHighContrast = localThis.enableHighContrast,
            enableReducedMotion = localThis.enableReducedMotion,
            lastAppVersion = localThis.lastAppVersion,
            firstLaunchDate = localThis.firstLaunchDate,
            totalAnalysisCount = localThis.totalAnalysisCount,
            favoriteRecipeIds = localThis.favoriteRecipeIds
        )

        // Then
        assertThat(userPreferences.theme).isEqualTo(localThis.theme)
        assertThat(userPreferences.language).isEqualTo(localThis.language)
        assertThat(userPreferences.enableHapticFeedback).isEqualTo(localThis.enableHapticFeedback)
        assertThat(userPreferences.enableSoundEffects).isEqualTo(localThis.enableSoundEffects)
        assertThat(userPreferences.cacheRecipes).isEqualTo(localThis.cacheRecipes)
        assertThat(userPreferences.maxCacheSize).isEqualTo(localThis.maxCacheSize)
        assertThat(userPreferences.autoDeleteOldRecipes).isEqualTo(localThis.autoDeleteOldRecipes)
        assertThat(userPreferences.cacheRetentionDays).isEqualTo(localThis.cacheRetentionDays)
        assertThat(userPreferences.enableAnalytics).isEqualTo(localThis.enableAnalytics)
        assertThat(userPreferences.enableCrashReporting).isEqualTo(localThis.enableCrashReporting)
        assertThat(userPreferences.preferredImageQuality).isEqualTo(localThis.preferredImageQuality)
        assertThat(userPreferences.enableAccessibilityFeatures).isEqualTo(localThis.enableAccessibilityFeatures)
        assertThat(userPreferences.fontSize).isEqualTo(localThis.fontSize)
        assertThat(userPreferences.enableHighContrast).isEqualTo(localThis.enableHighContrast)
        assertThat(userPreferences.enableReducedMotion).isEqualTo(localThis.enableReducedMotion)
        assertThat(userPreferences.lastAppVersion).isEqualTo(localThis.lastAppVersion)
        assertThat(userPreferences.firstLaunchDate).isEqualTo(localThis.firstLaunchDate)
        assertThat(userPreferences.totalAnalysisCount).isEqualTo(localThis.totalAnalysisCount)
        assertThat(userPreferences.favoriteRecipeIds).isEqualTo(localThis.favoriteRecipeIds)
    }

    @Test
    fun `UserPreferences should handle cache settings validation`() {
        // Given - localThis pattern
        val localThis = object {
            val validCacheSizes = listOf(10, 50, 100, 200, 500)
            val validRetentionDays = listOf(1, 7, 30, 60, 90, 365)
        }

        // When/Then - Test valid cache sizes
        localThis.validCacheSizes.forEach { cacheSize ->
            val preferences = UserPreferences(maxCacheSize = cacheSize)
            assertThat(preferences.maxCacheSize).isEqualTo(cacheSize)
            assertThat(preferences.maxCacheSize).isGreaterThan(0)
        }

        // When/Then - Test valid retention days
        localThis.validRetentionDays.forEach { retentionDays ->
            val preferences = UserPreferences(cacheRetentionDays = retentionDays)
            assertThat(preferences.cacheRetentionDays).isEqualTo(retentionDays)
            assertThat(preferences.cacheRetentionDays).isGreaterThan(0)
        }
    }

    @Test
    fun `UserPreferences should handle language codes correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val supportedLanguages = listOf("en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh")
        }

        // When/Then
        localThis.supportedLanguages.forEach { language ->
            val preferences = UserPreferences(language = language)
            assertThat(preferences.language).isEqualTo(language)
            assertThat(preferences.language).hasLength(2)
        }
    }

    @Test
    fun `UserPreferences should handle analysis count correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val analysisCounts = listOf(0, 1, 10, 50, 100, 1000)
        }

        // When/Then
        localThis.analysisCounts.forEach { count ->
            val preferences = UserPreferences(totalAnalysisCount = count)
            assertThat(preferences.totalAnalysisCount).isEqualTo(count)
            assertThat(preferences.totalAnalysisCount).isAtLeast(0)
        }
    }

    @Test
    fun `UserPreferences should handle favorite recipe IDs`() {
        // Given - localThis pattern
        val localThis = object {
            val emptyFavorites = emptySet<String>()
            val singleFavorite = setOf("recipe1")
            val multipleFavorites = setOf("recipe1", "recipe2", "recipe3", "recipe4", "recipe5")
            val largeFavorites = (1..100).map { "recipe$it" }.toSet()
        }

        // When/Then - Empty favorites
        val emptyPreferences = UserPreferences(favoriteRecipeIds = localThis.emptyFavorites)
        assertThat(emptyPreferences.favoriteRecipeIds).isEmpty()

        // When/Then - Single favorite
        val singlePreferences = UserPreferences(favoriteRecipeIds = localThis.singleFavorite)
        assertThat(singlePreferences.favoriteRecipeIds).hasSize(1)
        assertThat(singlePreferences.favoriteRecipeIds).contains("recipe1")

        // When/Then - Multiple favorites
        val multiplePreferences = UserPreferences(favoriteRecipeIds = localThis.multipleFavorites)
        assertThat(multiplePreferences.favoriteRecipeIds).hasSize(5)
        assertThat(multiplePreferences.favoriteRecipeIds).containsExactlyElementsIn(localThis.multipleFavorites)

        // When/Then - Large favorites list
        val largePreferences = UserPreferences(favoriteRecipeIds = localThis.largeFavorites)
        assertThat(largePreferences.favoriteRecipeIds).hasSize(100)
        assertThat(largePreferences.favoriteRecipeIds).containsExactlyElementsIn(localThis.largeFavorites)
    }

    @Test
    fun `AppTheme should have all expected values`() {
        // Given - localThis pattern
        val localThis = object {
            val expectedThemes = listOf(
                AppTheme.LIGHT,
                AppTheme.DARK,
                AppTheme.SYSTEM
            )
        }

        // When/Then
        localThis.expectedThemes.forEach { theme ->
            assertThat(theme).isNotNull()
            assertThat(theme.name).isNotEmpty()
        }
    }

    @Test
    fun `ImageQuality should have all expected values`() {
        // Given - localThis pattern
        val localThis = object {
            val expectedQualities = listOf(
                ImageQuality.LOW,
                ImageQuality.MEDIUM,
                ImageQuality.HIGH
            )
        }

        // When/Then
        localThis.expectedQualities.forEach { quality ->
            assertThat(quality).isNotNull()
            assertThat(quality.name).isNotEmpty()
        }
    }

    @Test
    fun `FontSize should have all expected values`() {
        // Given - localThis pattern
        val localThis = object {
            val expectedSizes = listOf(
                FontSize.SMALL,
                FontSize.MEDIUM,
                FontSize.LARGE,
                FontSize.EXTRA_LARGE
            )
        }

        // When/Then
        localThis.expectedSizes.forEach { size ->
            assertThat(size).isNotNull()
            assertThat(size.name).isNotEmpty()
        }
    }

    @Test
    fun `AnalyticsEvent should create correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val eventName = "recipe_analyzed"
            val parameters = mapOf(
                "recipe_id" to "recipe123",
                "analysis_time" to 1500L,
                "success" to true
            )
            val timestamp = 1234567890L
            val sessionId = "session123"
        }

        // When
        val event = AnalyticsEvent(
            eventName = localThis.eventName,
            parameters = localThis.parameters,
            timestamp = localThis.timestamp,
            sessionId = localThis.sessionId
        )

        // Then
        assertThat(event.eventName).isEqualTo(localThis.eventName)
        assertThat(event.parameters).isEqualTo(localThis.parameters)
        assertThat(event.timestamp).isEqualTo(localThis.timestamp)
        assertThat(event.sessionId).isEqualTo(localThis.sessionId)
    }

    @Test
    fun `AnalyticsEvent should handle default values`() {
        // Given - localThis pattern
        val localThis = object {
            val eventName = "app_opened"
        }

        // When
        val event = AnalyticsEvent(eventName = localThis.eventName)

        // Then
        assertThat(event.eventName).isEqualTo(localThis.eventName)
        assertThat(event.parameters).isEmpty()
        assertThat(event.timestamp).isGreaterThan(0L)
        assertThat(event.sessionId).isNull()
    }

    @Test
    fun `AppUsageStats should create correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val totalLaunches = 100
            val totalAnalyses = 50
            val totalPhotosAnalyzed = 30
            val totalSampleImagesUsed = 20
            val averageSessionDuration = 120000L
            val lastUsedDate = 1234567890L
            val favoriteFeatures = mapOf(
                "camera" to 25,
                "sample_images" to 15,
                "sharing" to 10
            )
            val errorCounts = mapOf(
                "network_error" to 5,
                "api_error" to 3,
                "unknown_error" to 2
            )
        }

        // When
        val stats = AppUsageStats(
            totalLaunches = localThis.totalLaunches,
            totalAnalyses = localThis.totalAnalyses,
            totalPhotosAnalyzed = localThis.totalPhotosAnalyzed,
            totalSampleImagesUsed = localThis.totalSampleImagesUsed,
            averageSessionDuration = localThis.averageSessionDuration,
            lastUsedDate = localThis.lastUsedDate,
            favoriteFeatures = localThis.favoriteFeatures,
            errorCounts = localThis.errorCounts
        )

        // Then
        assertThat(stats.totalLaunches).isEqualTo(localThis.totalLaunches)
        assertThat(stats.totalAnalyses).isEqualTo(localThis.totalAnalyses)
        assertThat(stats.totalPhotosAnalyzed).isEqualTo(localThis.totalPhotosAnalyzed)
        assertThat(stats.totalSampleImagesUsed).isEqualTo(localThis.totalSampleImagesUsed)
        assertThat(stats.averageSessionDuration).isEqualTo(localThis.averageSessionDuration)
        assertThat(stats.lastUsedDate).isEqualTo(localThis.lastUsedDate)
        assertThat(stats.favoriteFeatures).isEqualTo(localThis.favoriteFeatures)
        assertThat(stats.errorCounts).isEqualTo(localThis.errorCounts)
    }

    @Test
    fun `AppUsageStats should have proper default values`() {
        // Given - localThis pattern
        val localThis = object {
            // No specific values needed for default test
        }

        // When
        val stats = AppUsageStats()

        // Then
        assertThat(stats.totalLaunches).isEqualTo(0)
        assertThat(stats.totalAnalyses).isEqualTo(0)
        assertThat(stats.totalPhotosAnalyzed).isEqualTo(0)
        assertThat(stats.totalSampleImagesUsed).isEqualTo(0)
        assertThat(stats.averageSessionDuration).isEqualTo(0L)
        assertThat(stats.lastUsedDate).isGreaterThan(0L)
        assertThat(stats.favoriteFeatures).isEmpty()
        assertThat(stats.errorCounts).isEmpty()
    }

    @Test
    fun `UserPreferences should be immutable`() {
        // Given - localThis pattern
        val localThis = object {
            val originalPreferences = UserPreferences(
                theme = AppTheme.LIGHT,
                language = "en",
                enableHapticFeedback = true
            )
        }

        // When
        val modifiedPreferences = localThis.originalPreferences.copy(
            theme = AppTheme.DARK,
            language = "es"
        )

        // Then
        assertThat(localThis.originalPreferences.theme).isEqualTo(AppTheme.LIGHT)
        assertThat(localThis.originalPreferences.language).isEqualTo("en")
        assertThat(modifiedPreferences.theme).isEqualTo(AppTheme.DARK)
        assertThat(modifiedPreferences.language).isEqualTo("es")
        assertThat(modifiedPreferences.enableHapticFeedback).isEqualTo(localThis.originalPreferences.enableHapticFeedback)
    }

    @Test
    fun `UserPreferences should handle accessibility settings correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val accessibilitySettings = listOf(
                Triple(true, true, true),   // All accessibility enabled
                Triple(false, false, false), // All accessibility disabled
                Triple(true, false, true),  // Mixed settings
                Triple(false, true, false)  // Mixed settings
            )
        }

        // When/Then
        localThis.accessibilitySettings.forEach { (accessibility, contrast, motion) ->
            val preferences = UserPreferences(
                enableAccessibilityFeatures = accessibility,
                enableHighContrast = contrast,
                enableReducedMotion = motion
            )
            
            assertThat(preferences.enableAccessibilityFeatures).isEqualTo(accessibility)
            assertThat(preferences.enableHighContrast).isEqualTo(contrast)
            assertThat(preferences.enableReducedMotion).isEqualTo(motion)
        }
    }
} 
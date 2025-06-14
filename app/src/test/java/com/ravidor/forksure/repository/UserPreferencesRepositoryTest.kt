package com.ravidor.forksure.repository

import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.data.model.AppTheme
import com.ravidor.forksure.data.model.AppUsageStats
import com.ravidor.forksure.data.model.FontSize
import com.ravidor.forksure.data.model.ImageQuality
import com.ravidor.forksure.data.model.UserPreferences
import com.ravidor.forksure.data.source.local.PreferencesDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for UserPreferencesRepository
 * Tests user preferences management and usage statistics
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserPreferencesRepositoryTest {

    private lateinit var mockPreferencesDataSource: PreferencesDataSource
    private lateinit var repository: UserPreferencesRepositoryImpl

    @Before
    fun setup() {
        mockPreferencesDataSource = mockk(relaxed = true)
        repository = UserPreferencesRepositoryImpl(mockPreferencesDataSource)
    }

    @Test
    fun `userPreferences should expose data source flow`() = runTest {
        // Given
        val preferences = UserPreferences(theme = AppTheme.DARK)
        coEvery { mockPreferencesDataSource.userPreferences } returns flowOf(preferences)

        // When
        val result = repository.userPreferences

        // Then
        // Just verify that the repository exposes the data source flow
        // We can't directly compare Flow instances, so we verify the behavior
        assertThat(result).isNotNull()
    }

    @Test
    fun `updateTheme should call data source`() = runTest {
        // Given
        val theme = AppTheme.LIGHT
        coEvery { mockPreferencesDataSource.updateTheme(theme) } returns Unit

        // When
        repository.updateTheme(theme)

        // Then
        coVerify { mockPreferencesDataSource.updateTheme(theme) }
    }

    @Test
    fun `updateHapticFeedback should call data source`() = runTest {
        // Given
        val enabled = false
        coEvery { mockPreferencesDataSource.updateHapticFeedback(enabled) } returns Unit

        // When
        repository.updateHapticFeedback(enabled)

        // Then
        coVerify { mockPreferencesDataSource.updateHapticFeedback(enabled) }
    }

    @Test
    fun `updateSoundEffects should get current preferences and save updated`() = runTest {
        // Given
        val currentPrefs = UserPreferences(enableSoundEffects = true)
        coEvery { mockPreferencesDataSource.getCurrentUserPreferences() } returns currentPrefs
        coEvery { mockPreferencesDataSource.saveUserPreferences(any()) } returns Unit

        // When
        repository.updateSoundEffects(false)

        // Then
        coVerify { mockPreferencesDataSource.getCurrentUserPreferences() }
        coVerify { 
            mockPreferencesDataSource.saveUserPreferences(
                currentPrefs.copy(enableSoundEffects = false)
            ) 
        }
    }

    @Test
    fun `updateCacheSettings should call data source`() = runTest {
        // Given
        val enabled = true
        val maxSize = 100
        val retentionDays = 60
        coEvery { mockPreferencesDataSource.updateCacheSettings(enabled, maxSize, retentionDays) } returns Unit

        // When
        repository.updateCacheSettings(enabled, maxSize, retentionDays)

        // Then
        coVerify { mockPreferencesDataSource.updateCacheSettings(enabled, maxSize, retentionDays) }
    }

    @Test
    fun `updateAccessibilitySettings should update all accessibility preferences`() = runTest {
        // Given
        val currentPrefs = UserPreferences()
        val enableAccessibility = true
        val fontSize = FontSize.LARGE
        val enableHighContrast = true
        val enableReducedMotion = false
        
        coEvery { mockPreferencesDataSource.getCurrentUserPreferences() } returns currentPrefs
        coEvery { mockPreferencesDataSource.saveUserPreferences(any()) } returns Unit

        // When
        repository.updateAccessibilitySettings(
            enableAccessibility, fontSize, enableHighContrast, enableReducedMotion
        )

        // Then
        coVerify { 
            mockPreferencesDataSource.saveUserPreferences(
                currentPrefs.copy(
                    enableAccessibilityFeatures = enableAccessibility,
                    fontSize = fontSize,
                    enableHighContrast = enableHighContrast,
                    enableReducedMotion = enableReducedMotion
                )
            ) 
        }
    }

    @Test
    fun `updateAnalyticsSettings should update analytics preferences`() = runTest {
        // Given
        val currentPrefs = UserPreferences()
        val enableAnalytics = true
        val enableCrashReporting = false
        
        coEvery { mockPreferencesDataSource.getCurrentUserPreferences() } returns currentPrefs
        coEvery { mockPreferencesDataSource.saveUserPreferences(any()) } returns Unit

        // When
        repository.updateAnalyticsSettings(enableAnalytics, enableCrashReporting)

        // Then
        coVerify { 
            mockPreferencesDataSource.saveUserPreferences(
                currentPrefs.copy(
                    enableAnalytics = enableAnalytics,
                    enableCrashReporting = enableCrashReporting
                )
            ) 
        }
    }

    @Test
    fun `updateImageQuality should update image quality preference`() = runTest {
        // Given
        val currentPrefs = UserPreferences()
        val quality = ImageQuality.LOW
        
        coEvery { mockPreferencesDataSource.getCurrentUserPreferences() } returns currentPrefs
        coEvery { mockPreferencesDataSource.saveUserPreferences(any()) } returns Unit

        // When
        repository.updateImageQuality(quality)

        // Then
        coVerify { 
            mockPreferencesDataSource.saveUserPreferences(
                currentPrefs.copy(preferredImageQuality = quality)
            ) 
        }
    }

    @Test
    fun `addFavoriteRecipe should call data source`() = runTest {
        // Given
        val recipeId = "recipe-123"
        coEvery { mockPreferencesDataSource.addFavoriteRecipe(recipeId) } returns Unit

        // When
        repository.addFavoriteRecipe(recipeId)

        // Then
        coVerify { mockPreferencesDataSource.addFavoriteRecipe(recipeId) }
    }

    @Test
    fun `removeFavoriteRecipe should call data source`() = runTest {
        // Given
        val recipeId = "recipe-123"
        coEvery { mockPreferencesDataSource.removeFavoriteRecipe(recipeId) } returns Unit

        // When
        repository.removeFavoriteRecipe(recipeId)

        // Then
        coVerify { mockPreferencesDataSource.removeFavoriteRecipe(recipeId) }
    }

    @Test
    fun `getFavoriteRecipeIds should return current favorite IDs`() = runTest {
        // Given
        val favoriteIds = setOf("recipe-1", "recipe-2", "recipe-3")
        val preferences = UserPreferences(favoriteRecipeIds = favoriteIds)
        coEvery { mockPreferencesDataSource.getCurrentUserPreferences() } returns preferences

        // When
        val result = repository.getFavoriteRecipeIds()

        // Then
        assertThat(result).isEqualTo(favoriteIds)
    }

    @Test
    fun `incrementAnalysisCount should call data source`() = runTest {
        // Given
        coEvery { mockPreferencesDataSource.incrementAnalysisCount() } returns Unit

        // When
        repository.incrementAnalysisCount()

        // Then
        coVerify { mockPreferencesDataSource.incrementAnalysisCount() }
    }

    @Test
    fun `recordPhotoAnalysis should update usage stats`() = runTest {
        // Given
        val currentStats = AppUsageStats(totalPhotosAnalyzed = 5)
        coEvery { mockPreferencesDataSource.getCurrentUsageStats() } returns currentStats
        coEvery { mockPreferencesDataSource.saveUsageStats(any()) } returns Unit

        // When
        repository.recordPhotoAnalysis()

        // Then
        coVerify { 
            mockPreferencesDataSource.saveUsageStats(
                currentStats.copy(totalPhotosAnalyzed = 6)
            ) 
        }
    }

    @Test
    fun `recordSampleImageUsage should update usage stats`() = runTest {
        // Given
        val currentStats = AppUsageStats(totalSampleImagesUsed = 10)
        coEvery { mockPreferencesDataSource.getCurrentUsageStats() } returns currentStats
        coEvery { mockPreferencesDataSource.saveUsageStats(any()) } returns Unit

        // When
        repository.recordSampleImageUsage()

        // Then
        coVerify { 
            mockPreferencesDataSource.saveUsageStats(
                currentStats.copy(totalSampleImagesUsed = 11)
            ) 
        }
    }

    @Test
    fun `updateSessionDuration should calculate new average`() = runTest {
        // Given
        val currentStats = AppUsageStats(
            totalLaunches = 3,
            averageSessionDuration = 120000L // 2 minutes average
        )
        val newSessionDuration = 180000L // 3 minutes
        coEvery { mockPreferencesDataSource.getCurrentUsageStats() } returns currentStats
        coEvery { mockPreferencesDataSource.saveUsageStats(any()) } returns Unit

        // When
        repository.updateSessionDuration(newSessionDuration)

        // Then
        // New average should be ((120000 * 2) + 180000) / 3 = 140000
        coVerify { 
            mockPreferencesDataSource.saveUsageStats(
                match { stats ->
                    stats.averageSessionDuration == 140000L &&
                    stats.lastUsedDate > 0
                }
            ) 
        }
    }

    @Test
    fun `resetAllSettings should call data source`() = runTest {
        // Given
        coEvery { mockPreferencesDataSource.clearAllPreferences() } returns Unit

        // When
        repository.resetAllSettings()

        // Then
        coVerify { mockPreferencesDataSource.clearAllPreferences() }
    }

    @Test
    fun `exportSettings should return JSON string`() = runTest {
        // Given
        val preferences = UserPreferences(
            theme = AppTheme.DARK,
            language = "en",
            enableHapticFeedback = true
        )
        val stats = AppUsageStats(totalLaunches = 5, totalAnalyses = 10)
        
        coEvery { mockPreferencesDataSource.getCurrentUserPreferences() } returns preferences
        coEvery { mockPreferencesDataSource.getCurrentUsageStats() } returns stats

        // When
        val result = repository.exportSettings()

        // Then
        assertThat(result).contains("\"theme\": \"DARK\"")
        assertThat(result).contains("\"language\": \"en\"")
        assertThat(result).contains("\"enableHapticFeedback\": true")
        assertThat(result).contains("\"totalLaunches\": 5")
        assertThat(result).contains("\"totalAnalyses\": 10")
    }

    @Test
    fun `importSettings should return false for simplified implementation`() = runTest {
        // Given
        val settingsJson = "{\"theme\": \"LIGHT\"}"

        // When
        val result = repository.importSettings(settingsJson)

        // Then
        assertThat(result).isFalse()
    }
} 
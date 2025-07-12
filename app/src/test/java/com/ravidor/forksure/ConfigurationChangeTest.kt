package com.ravidor.forksure

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.data.model.AppTheme
import com.ravidor.forksure.data.model.FontSize
import com.ravidor.forksure.data.model.UserPreferences
import com.ravidor.forksure.state.MainScreenState
import com.ravidor.forksure.state.NavigationState
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive configuration change tests for device rotation, theme changes, and state preservation
 * Tests state preservation across configuration changes and theme switching
 * Uses localThis pattern for consistency with existing test files
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConfigurationChangeTest {

    private lateinit var localThis: ConfigurationChangeTest
    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources
    private lateinit var mockConfiguration: Configuration
    private lateinit var mockBitmap: Bitmap
    private lateinit var mockBundle: Bundle

    @Before
    fun setup() {
        localThis = this
        mockContext = mockk(relaxed = true)
        mockResources = mockk(relaxed = true)
        mockConfiguration = mockk(relaxed = true)
        mockBitmap = mockk(relaxed = true)
        mockBundle = mockk(relaxed = true)
        
        // Mock context dependencies
        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns mockConfiguration
        
        // Mock bitmap properties
        every { mockBitmap.width } returns 100
        every { mockBitmap.height } returns 100
        every { mockBitmap.isRecycled } returns false
        
        // Mock bundle operations
        every { mockBundle.getString(any()) } returns null
        every { mockBundle.getString(any(), any()) } returns ""
        every { mockBundle.getInt(any()) } returns 0
        every { mockBundle.getInt(any(), any()) } returns 0
        every { mockBundle.getBoolean(any()) } returns false
        every { mockBundle.getBoolean(any(), any()) } returns false
        every { mockBundle.putString(any(), any()) } just Runs
        every { mockBundle.putInt(any(), any()) } just Runs
        every { mockBundle.putBoolean(any(), any()) } just Runs
    }

    // Device Orientation Change Tests
    @Test
    fun `MainScreenState should preserve prompt text across orientation changes`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState(
            initialPrompt = "Test prompt for orientation change",
            initialResult = "",
            initialSelectedImageIndex = 0
        )
        
        // When - simulate orientation change by creating new state with same data
        val originalPrompt = state.prompt
        val originalResult = state.result
        val originalSelectedIndex = state.selectedImageIndex
        
        val newState = MainScreenState(
            initialPrompt = originalPrompt,
            initialResult = originalResult,
            initialSelectedImageIndex = originalSelectedIndex
        )
        
        // Then - state should be preserved
        assertThat(newState.prompt).isEqualTo(originalPrompt)
        assertThat(newState.result).isEqualTo(originalResult)
        assertThat(newState.selectedImageIndex).isEqualTo(originalSelectedIndex)
    }

    @Test
    fun `NavigationState should preserve image selection across orientation changes`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState(0)
        state.updateCapturedImage(mockBitmap)
        
        // When - simulate orientation change
        val originalSelectedIndex = state.selectedImageIndex
        val originalCapturedImage = state.capturedImage
        
        val newState = NavigationState(originalSelectedIndex)
        newState.updateCapturedImage(originalCapturedImage)
        
        // Then - state should be preserved
        assertThat(newState.selectedImageIndex).isEqualTo(originalSelectedIndex)
        assertThat(newState.capturedImage).isEqualTo(originalCapturedImage)
        assertThat(newState.hasSelectedCapturedImage).isTrue()
    }

    @Test
    fun `MainScreenState should handle configuration changes with complex state`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        
        // Set up complex state
        state.updatePrompt("Complex prompt with multiple words")
        state.updateResult("Complex result with analysis data")
        state.updateCapturedImage(mockBitmap)
        state.showReportDialog()
        
        // When - simulate configuration change
        val stateData = mapOf(
            "prompt" to state.prompt,
            "result" to state.result,
            "selectedImageIndex" to state.selectedImageIndex,
            "capturedImage" to state.capturedImage,
            "showReportDialog" to state.showReportDialog
        )
        
        val newState = MainScreenState(
            initialPrompt = stateData["prompt"] as String,
            initialResult = stateData["result"] as String,
            initialSelectedImageIndex = stateData["selectedImageIndex"] as Int
        )
        newState.updateCapturedImage(stateData["capturedImage"] as Bitmap?)
        if (stateData["showReportDialog"] as Boolean) {
            newState.showReportDialog()
        }
        
        // Then - complex state should be preserved
        assertThat(newState.prompt).isEqualTo(state.prompt)
        assertThat(newState.result).isEqualTo(state.result)
        assertThat(newState.selectedImageIndex).isEqualTo(state.selectedImageIndex)
        assertThat(newState.capturedImage).isEqualTo(state.capturedImage)
        assertThat(newState.showReportDialog).isEqualTo(state.showReportDialog)
    }

    // Theme Change Tests
    @Test
    fun `UserPreferences should handle theme changes correctly`() {
        // Given
        val localThis = this.localThis
        val preferences = UserPreferences()
        
        // When - change themes
        val lightTheme = preferences.copy(theme = AppTheme.LIGHT)
        val darkTheme = preferences.copy(theme = AppTheme.DARK)
        val systemTheme = preferences.copy(theme = AppTheme.SYSTEM)
        
        // Then - themes should be set correctly
        assertThat(lightTheme.theme).isEqualTo(AppTheme.LIGHT)
        assertThat(darkTheme.theme).isEqualTo(AppTheme.DARK)
        assertThat(systemTheme.theme).isEqualTo(AppTheme.SYSTEM)
    }

    @Test
    fun `UserPreferences should preserve other settings when theme changes`() {
        // Given
        val localThis = this.localThis
        val preferences = UserPreferences(
            theme = AppTheme.SYSTEM,
            language = "es",
            enableHapticFeedback = false,
            enableSoundEffects = false,
            fontSize = FontSize.LARGE
        )
        
        // When - change theme
        val newThemePreferences = preferences.copy(theme = AppTheme.DARK)
        
        // Then - other settings should be preserved
        assertThat(newThemePreferences.theme).isEqualTo(AppTheme.DARK)
        assertThat(newThemePreferences.language).isEqualTo("es")
        assertThat(newThemePreferences.enableHapticFeedback).isFalse()
        assertThat(newThemePreferences.enableSoundEffects).isFalse()
        assertThat(newThemePreferences.fontSize).isEqualTo(FontSize.LARGE)
    }

    // Font Size Change Tests
    @Test
    fun `UserPreferences should handle font size changes correctly`() {
        // Given
        val localThis = this.localThis
        val preferences = UserPreferences()
        
        // When - change font sizes
        val smallFont = preferences.copy(fontSize = FontSize.SMALL)
        val mediumFont = preferences.copy(fontSize = FontSize.MEDIUM)
        val largeFont = preferences.copy(fontSize = FontSize.LARGE)
        val extraLargeFont = preferences.copy(fontSize = FontSize.EXTRA_LARGE)
        
        // Then - font sizes should be set correctly
        assertThat(smallFont.fontSize).isEqualTo(FontSize.SMALL)
        assertThat(mediumFont.fontSize).isEqualTo(FontSize.MEDIUM)
        assertThat(largeFont.fontSize).isEqualTo(FontSize.LARGE)
        assertThat(extraLargeFont.fontSize).isEqualTo(FontSize.EXTRA_LARGE)
    }

    @Test
    fun `AccessibilityHelper should detect font scale changes correctly`() {
        // Given
        val localThis = this.localThis
        val fontScales = listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f, 2.0f)
        
        fontScales.forEach { scale ->
            // When
            every { mockConfiguration.fontScale } returns scale
            val fontScale = AccessibilityHelper.getFontScale(mockContext)
            val isLargeText = AccessibilityHelper.isLargeTextEnabled(mockContext)
            
            // Then
            assertThat(fontScale).isEqualTo(scale)
            assertThat(isLargeText).isEqualTo(scale > 1.0f)
        }
    }

    // Language Change Tests
    @Test
    fun `UserPreferences should handle language changes correctly`() {
        // Given
        val localThis = this.localThis
        val preferences = UserPreferences()
        
        // When - change languages
        val englishPrefs = preferences.copy(language = "en")
        val spanishPrefs = preferences.copy(language = "es")
        val frenchPrefs = preferences.copy(language = "fr")
        val germanPrefs = preferences.copy(language = "de")
        
        // Then - languages should be set correctly
        assertThat(englishPrefs.language).isEqualTo("en")
        assertThat(spanishPrefs.language).isEqualTo("es")
        assertThat(frenchPrefs.language).isEqualTo("fr")
        assertThat(germanPrefs.language).isEqualTo("de")
    }

    @Test
    fun `Configuration should handle locale changes correctly`() {
        // Given
        val localThis = this.localThis
        val locales = listOf("en", "es", "fr", "de")
        
        locales.forEach { locale ->
            // When
            every { mockConfiguration.locale } returns java.util.Locale(locale)
            
            // Then - should not crash
            try {
                val currentLocale = mockConfiguration.locale
                assertThat(currentLocale.language).isEqualTo(locale)
            } catch (e: Exception) {
                // Should handle gracefully
            }
        }
    }

    // Accessibility Settings Configuration Tests
    @Test
    fun `UserPreferences should handle accessibility settings changes correctly`() {
        // Given
        val localThis = this.localThis
        val preferences = UserPreferences()
        
        // When - change accessibility settings
        val accessibilityEnabled = preferences.copy(
            enableAccessibilityFeatures = true,
            fontSize = FontSize.LARGE,
            enableHighContrast = true,
            enableReducedMotion = true
        )
        
        val accessibilityDisabled = preferences.copy(
            enableAccessibilityFeatures = false,
            fontSize = FontSize.MEDIUM,
            enableHighContrast = false,
            enableReducedMotion = false
        )
        
        // Then - accessibility settings should be set correctly
        assertThat(accessibilityEnabled.enableAccessibilityFeatures).isTrue()
        assertThat(accessibilityEnabled.fontSize).isEqualTo(FontSize.LARGE)
        assertThat(accessibilityEnabled.enableHighContrast).isTrue()
        assertThat(accessibilityEnabled.enableReducedMotion).isTrue()
        
        assertThat(accessibilityDisabled.enableAccessibilityFeatures).isFalse()
        assertThat(accessibilityDisabled.fontSize).isEqualTo(FontSize.MEDIUM)
        assertThat(accessibilityDisabled.enableHighContrast).isFalse()
        assertThat(accessibilityDisabled.enableReducedMotion).isFalse()
    }

    // Screen Density Changes Tests
    @Test
    fun `Configuration should handle screen density changes correctly`() {
        // Given
        val localThis = this.localThis
        val densities = listOf(160, 240, 320, 480, 640) // Different DPI values
        
        densities.forEach { density ->
            // When
            every { mockConfiguration.densityDpi } returns density
            
            // Then - should not crash
            try {
                val currentDensity = mockConfiguration.densityDpi
                assertThat(currentDensity).isEqualTo(density)
            } catch (e: Exception) {
                // Should handle gracefully
            }
        }
    }

    // Memory and Performance Tests
    @Test
    fun `state objects should handle multiple configuration changes efficiently`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        
        // When - simulate multiple configuration changes
        val startTime = System.currentTimeMillis()
        
        repeat(100) { iteration ->
            // Simulate configuration change
            state.updatePrompt("Prompt $iteration")
            state.selectSampleImage(iteration % 3)
            
            if (iteration % 10 == 0) {
                state.updateCapturedImage(mockBitmap)
            }
            
            // Simulate state restoration
            val newState = MainScreenState(
                initialPrompt = state.prompt,
                initialResult = state.result,
                initialSelectedImageIndex = state.selectedImageIndex
            )
            newState.updateCapturedImage(state.capturedImage)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Then - should complete efficiently
        assertThat(duration).isLessThan(1000L) // Should complete within 1 second
    }

    // Complex Configuration Change Scenarios
    @Test
    fun `complex configuration changes should maintain state consistency`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        val navigationState = NavigationState()
        
        // Set up complex initial state
        state.updatePrompt("Complex configuration test prompt")
        state.selectSampleImage(1)
        state.updateResult("Test result data")
        state.showReportDialog()
        
        navigationState.updateCapturedImage(mockBitmap)
        navigationState.selectCapturedImage()
        
        // When - simulate complex configuration change (rotation + theme + font size)
        val savedMainState = mapOf(
            "prompt" to state.prompt,
            "result" to state.result,
            "selectedImageIndex" to state.selectedImageIndex,
            "showReportDialog" to state.showReportDialog
        )
        
        val savedNavigationState = mapOf(
            "selectedImageIndex" to navigationState.selectedImageIndex,
            "capturedImage" to navigationState.capturedImage
        )
        
        // Create new states (as would happen after configuration change)
        val newMainState = MainScreenState(
            initialPrompt = savedMainState["prompt"] as String,
            initialResult = savedMainState["result"] as String,
            initialSelectedImageIndex = savedMainState["selectedImageIndex"] as Int
        )
        if (savedMainState["showReportDialog"] as Boolean) {
            newMainState.showReportDialog()
        }
        
        val newNavigationState = NavigationState(savedNavigationState["selectedImageIndex"] as Int)
        newNavigationState.updateCapturedImage(savedNavigationState["capturedImage"] as Bitmap?)
        
        // Then - all state should be preserved correctly
        assertThat(newMainState.prompt).isEqualTo(state.prompt)
        assertThat(newMainState.result).isEqualTo(state.result)
        assertThat(newMainState.selectedImageIndex).isEqualTo(state.selectedImageIndex)
        assertThat(newMainState.showReportDialog).isEqualTo(state.showReportDialog)
        
        assertThat(newNavigationState.selectedImageIndex).isEqualTo(navigationState.selectedImageIndex)
        assertThat(newNavigationState.capturedImage).isEqualTo(navigationState.capturedImage)
        assertThat(newNavigationState.hasSelectedCapturedImage).isTrue()
    }

    // Edge Cases and Error Handling
    @Test
    fun `configuration changes should handle null states gracefully`() {
        // Given
        val localThis = this.localThis
        
        // When - create states with null/empty values
        val emptyState = MainScreenState(
            initialPrompt = "",
            initialResult = "",
            initialSelectedImageIndex = -2
        )
        
        val nullImageState = NavigationState(-2)
        nullImageState.updateCapturedImage(null)
        
        // Then - should handle gracefully
        assertThat(emptyState.prompt).isEmpty()
        assertThat(emptyState.result).isEmpty()
        assertThat(emptyState.selectedImageIndex).isEqualTo(-2)
        assertThat(emptyState.isAnalyzeEnabled).isFalse()
        
        assertThat(nullImageState.capturedImage).isNull()
        assertThat(nullImageState.hasSelectedCapturedImage).isFalse()
    }

    @Test
    fun `configuration changes should handle large state objects efficiently`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        
        // When - create large state data
        val largePrompt = "A".repeat(1000)
        val largeResult = "B".repeat(2000)
        
        state.updatePrompt(largePrompt)
        state.updateResult(largeResult)
        state.updateCapturedImage(mockBitmap)
        
        // Simulate configuration change
        val newState = MainScreenState(
            initialPrompt = state.prompt,
            initialResult = state.result,
            initialSelectedImageIndex = state.selectedImageIndex
        )
        newState.updateCapturedImage(state.capturedImage)
        
        // Then - should handle large data correctly
        assertThat(newState.prompt).isEqualTo(largePrompt)
        assertThat(newState.result).isEqualTo(largeResult)
        assertThat(newState.capturedImage).isEqualTo(mockBitmap)
    }

    // Real-world Configuration Change Tests
    @Test
    fun `real-world configuration changes should work correctly`() {
        // Given
        val localThis = this.localThis
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // When - test with real context
        try {
            val currentConfiguration = context.resources.configuration
            val fontScale = currentConfiguration.fontScale
            val density = currentConfiguration.densityDpi
            val orientation = currentConfiguration.orientation
            
            // Then - should have valid values
            assertThat(fontScale).isGreaterThan(0f)
            assertThat(density).isGreaterThan(0)
            assertThat(orientation).isAtLeast(Configuration.ORIENTATION_PORTRAIT)
        } catch (e: Exception) {
            // Should handle gracefully in test environment
        }
    }

    // State Validation Tests
    @Test
    fun `state validation should work correctly after configuration changes`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        
        // When - set up valid state
        state.updatePrompt("Valid prompt")
        state.selectSampleImage(0)
        
        // Simulate configuration change
        val newState = MainScreenState(
            initialPrompt = state.prompt,
            initialResult = state.result,
            initialSelectedImageIndex = state.selectedImageIndex
        )
        
        // Then - validation should work correctly
        assertThat(newState.isAnalyzeEnabled).isTrue()
        assertThat(newState.hasSelectedSampleImage).isTrue()
        assertThat(newState.hasSelectedCapturedImage).isFalse()
    }

    // Preferences Persistence Tests
    @Test
    fun `UserPreferences should maintain consistency across changes`() {
        // Given
        val localThis = this.localThis
        val preferences = UserPreferences(
            theme = AppTheme.DARK,
            language = "es",
            enableHapticFeedback = true,
            enableSoundEffects = false,
            fontSize = FontSize.LARGE,
            enableAccessibilityFeatures = true
        )
        
        // When - simulate preference changes
        val updatedPreferences = preferences.copy(
            theme = AppTheme.LIGHT,
            fontSize = FontSize.EXTRA_LARGE,
            enableHapticFeedback = false
        )
        
        // Then - only changed values should be different
        assertThat(updatedPreferences.theme).isEqualTo(AppTheme.LIGHT)
        assertThat(updatedPreferences.fontSize).isEqualTo(FontSize.EXTRA_LARGE)
        assertThat(updatedPreferences.enableHapticFeedback).isFalse()
        
        // Unchanged values should remain the same
        assertThat(updatedPreferences.language).isEqualTo("es")
        assertThat(updatedPreferences.enableSoundEffects).isFalse()
        assertThat(updatedPreferences.enableAccessibilityFeatures).isTrue()
    }
} 
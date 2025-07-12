package com.ravidor.forksure

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.navigation.NavHostController
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.data.model.AppTheme
import com.ravidor.forksure.data.model.FontSize
import com.ravidor.forksure.data.model.UserPreferences
import com.ravidor.forksure.navigation.navigateToCamera
import com.ravidor.forksure.navigation.navigateToMain
import com.ravidor.forksure.state.MainScreenState
import com.ravidor.forksure.state.NavigationState
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive branch coverage tests for conditional logic paths
 * Tests decision points and conditional branches to achieve 70%+ coverage
 * Uses localThis pattern for consistency with existing test files
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BranchCoverageAdvancedTest {

    private lateinit var localThis: BranchCoverageAdvancedTest
    private lateinit var mockContext: Context
    private lateinit var mockNavController: NavHostController
    private lateinit var mockBitmap: Bitmap
    private lateinit var selectedImageState: MutableIntState

    @Before
    fun setup() {
        localThis = this
        mockContext = mockk(relaxed = true)
        mockNavController = mockk(relaxed = true)
        mockBitmap = mockk(relaxed = true)
        selectedImageState = mutableIntStateOf(-2)
        
        // Mock navigation controller methods
        every { mockNavController.navigate(any<String>()) } just Runs
        every { mockNavController.navigate(any<String>(), any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) } just Runs
        every { mockNavController.popBackStack() } returns true
        
        // Mock bitmap methods
        every { mockBitmap.width } returns 100
        every { mockBitmap.height } returns 100
        every { mockBitmap.isRecycled } returns false
    }

    // MainScreenState Branch Coverage Tests
    @Test
    fun `MainScreenState isAnalyzeEnabled should return true when prompt and image available`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState(
            initialPrompt = "Test prompt",
            initialResult = "",
            initialSelectedImageIndex = 0
        )
        
        // When
        val result = state.isAnalyzeEnabled
        
        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `MainScreenState isAnalyzeEnabled should return false when prompt empty`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState(
            initialPrompt = "",
            initialResult = "",
            initialSelectedImageIndex = 0
        )
        
        // When
        val result = state.isAnalyzeEnabled
        
        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `MainScreenState isAnalyzeEnabled should return false when no image selected`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState(
            initialPrompt = "Test prompt",
            initialResult = "",
            initialSelectedImageIndex = -2
        )
        
        // When
        val result = state.isAnalyzeEnabled
        
        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `MainScreenState isAnalyzeEnabled should return true when captured image available`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState(
            initialPrompt = "Test prompt",
            initialResult = "",
            initialSelectedImageIndex = -2
        )
        state.updateCapturedImage(mockBitmap)
        
        // When
        val result = state.isAnalyzeEnabled
        
        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `MainScreenState hasSelectedCapturedImage should return true when captured image selected`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        state.updateCapturedImage(mockBitmap)
        
        // When
        val result = state.hasSelectedCapturedImage
        
        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `MainScreenState hasSelectedCapturedImage should return false when no captured image`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        
        // When
        val result = state.hasSelectedCapturedImage
        
        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `MainScreenState hasSelectedSampleImage should return true when sample image selected`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState(initialSelectedImageIndex = 0)
        
        // When
        val result = state.hasSelectedSampleImage
        
        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `MainScreenState hasSelectedSampleImage should return false when no sample image selected`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState(initialSelectedImageIndex = -2)
        
        // When
        val result = state.hasSelectedSampleImage
        
        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `MainScreenState selectSampleImage should clear captured image`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        state.updateCapturedImage(mockBitmap)
        
        // When
        state.selectSampleImage(0)
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(0)
    }

    @Test
    fun `MainScreenState updateCapturedImage should auto-select captured image when non-null`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        
        // When
        state.updateCapturedImage(mockBitmap)
        
        // Then
        assertThat(state.selectedImageIndex).isEqualTo(-1)
        assertThat(state.capturedImage).isEqualTo(mockBitmap)
    }

    @Test
    fun `MainScreenState updateCapturedImage should not change selection when null`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState(initialSelectedImageIndex = 0)
        
        // When
        state.updateCapturedImage(null)
        
        // Then
        assertThat(state.selectedImageIndex).isEqualTo(0)
        assertThat(state.capturedImage).isNull()
    }

    @Test
    fun `MainScreenState clearCapturedImage should reset selection when captured image was selected`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        state.updateCapturedImage(mockBitmap)
        
        // When
        state.clearCapturedImage()
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(-2)
    }

    @Test
    fun `MainScreenState clearCapturedImage should not change selection when sample image selected`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState(initialSelectedImageIndex = 0)
        state.updateCapturedImage(mockBitmap)
        state.selectSampleImage(1)
        
        // When
        state.clearCapturedImage()
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(1)
    }

    // NavigationState Branch Coverage Tests
    @Test
    fun `NavigationState hasSelectedCapturedImage should return true when captured image selected`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        state.updateCapturedImage(mockBitmap)
        
        // When
        val result = state.hasSelectedCapturedImage
        
        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `NavigationState hasSelectedCapturedImage should return false when no captured image`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        
        // When
        val result = state.hasSelectedCapturedImage
        
        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `NavigationState hasSelectedSampleImage should return true when sample image selected`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState(0)
        
        // When
        val result = state.hasSelectedSampleImage
        
        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `NavigationState hasSelectedSampleImage should return false when negative index`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState(-2)
        
        // When
        val result = state.hasSelectedSampleImage
        
        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `NavigationState clearCapturedImage should reset selection when captured image was selected`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        state.updateCapturedImage(mockBitmap)
        state.selectCapturedImage()
        
        // When
        state.clearCapturedImage()
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(-2)
    }

    @Test
    fun `NavigationState clearCapturedImage should not change selection when sample image selected`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState(0)
        state.updateCapturedImage(mockBitmap)
        
        // When
        state.clearCapturedImage()
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(0)
    }

    // Navigation Extension Function Branch Coverage Tests
    @Test
    fun `navigateToCamera should call navigate with correct parameters`() {
        // Given
        val localThis = this.localThis
        
        // When
        mockNavController.navigateToCamera()
        
        // Then
        verify { mockNavController.navigate(NavigationConstants.ROUTE_CAMERA, any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) }
    }

    @Test
    fun `navigateToMain should call navigate with correct parameters`() {
        // Given
        val localThis = this.localThis
        
        // When
        mockNavController.navigateToMain()
        
        // Then
        verify { mockNavController.navigate(NavigationConstants.ROUTE_MAIN, any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) }
    }

    // AccessibilityHelper Branch Coverage Tests
    @Test
    fun `AccessibilityHelper isScreenReaderEnabled should return true when touch exploration enabled`() {
        // Given
        val localThis = this.localThis
        val mockAccessibilityManager = mockk<android.view.accessibility.AccessibilityManager>()
        every { mockContext.getSystemService(android.view.accessibility.AccessibilityManager::class.java) } returns mockAccessibilityManager
        every { mockAccessibilityManager.isTouchExplorationEnabled } returns true
        
        // When
        val result = AccessibilityHelper.isScreenReaderEnabled(mockContext)
        
        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `AccessibilityHelper isScreenReaderEnabled should return false when touch exploration disabled`() {
        // Given
        val localThis = this.localThis
        val mockAccessibilityManager = mockk<android.view.accessibility.AccessibilityManager>()
        every { mockContext.getSystemService(android.view.accessibility.AccessibilityManager::class.java) } returns mockAccessibilityManager
        every { mockAccessibilityManager.isTouchExplorationEnabled } returns false
        
        // When
        val result = AccessibilityHelper.isScreenReaderEnabled(mockContext)
        
        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `AccessibilityHelper isLargeTextEnabled should return true when font scale greater than 1`() {
        // Given
        val localThis = this.localThis
        val mockResources = mockk<android.content.res.Resources>()
        val mockConfiguration = mockk<android.content.res.Configuration>()
        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns mockConfiguration
        every { mockConfiguration.fontScale } returns 1.5f
        
        // When
        val result = AccessibilityHelper.isLargeTextEnabled(mockContext)
        
        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `AccessibilityHelper isLargeTextEnabled should return false when font scale equals 1`() {
        // Given
        val localThis = this.localThis
        val mockResources = mockk<android.content.res.Resources>()
        val mockConfiguration = mockk<android.content.res.Configuration>()
        every { mockContext.resources } returns mockResources
        every { mockResources.configuration } returns mockConfiguration
        every { mockConfiguration.fontScale } returns 1.0f
        
        // When
        val result = AccessibilityHelper.isLargeTextEnabled(mockContext)
        
        // Then
        assertThat(result).isFalse()
    }

    // UserPreferences Branch Coverage Tests
    @Test
    fun `UserPreferences should have correct defaults`() {
        // Given
        val localThis = this.localThis
        
        // When
        val preferences = UserPreferences()
        
        // Then
        assertThat(preferences.theme).isEqualTo(AppTheme.SYSTEM)
        assertThat(preferences.language).isEqualTo("en")
        assertThat(preferences.enableHapticFeedback).isTrue()
        assertThat(preferences.enableSoundEffects).isTrue()
        assertThat(preferences.cacheRecipes).isTrue()
        assertThat(preferences.fontSize).isEqualTo(FontSize.MEDIUM)
    }

    @Test
    fun `UserPreferences theme switching should work correctly`() {
        // Given
        val localThis = this.localThis
        val preferences = UserPreferences()
        
        // When
        val lightTheme = preferences.copy(theme = AppTheme.LIGHT)
        val darkTheme = preferences.copy(theme = AppTheme.DARK)
        
        // Then
        assertThat(lightTheme.theme).isEqualTo(AppTheme.LIGHT)
        assertThat(darkTheme.theme).isEqualTo(AppTheme.DARK)
    }

    @Test
    fun `UserPreferences accessibility settings should work correctly`() {
        // Given
        val localThis = this.localThis
        val preferences = UserPreferences()
        
        // When
        val accessibilityEnabled = preferences.copy(
            enableAccessibilityFeatures = true,
            fontSize = FontSize.LARGE,
            enableHighContrast = true,
            enableReducedMotion = true
        )
        
        // Then
        assertThat(accessibilityEnabled.enableAccessibilityFeatures).isTrue()
        assertThat(accessibilityEnabled.fontSize).isEqualTo(FontSize.LARGE)
        assertThat(accessibilityEnabled.enableHighContrast).isTrue()
        assertThat(accessibilityEnabled.enableReducedMotion).isTrue()
    }

    // Complex Conditional Logic Branch Coverage Tests
    @Test
    fun `complex conditional logic should handle multiple conditions correctly`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        
        // Test all combinations of conditions
        val conditions = listOf(
            Triple("", null, -2), // Empty prompt, no image, no selection
            Triple("Test", null, -2), // Has prompt, no image, no selection  
            Triple("", mockBitmap, -1), // Empty prompt, has image, selected
            Triple("Test", mockBitmap, -1), // Has prompt, has image, selected
            Triple("Test", null, 0), // Has prompt, no captured image, sample selected
            Triple("", null, 0), // Empty prompt, no captured image, sample selected
        )
        
        conditions.forEach { (prompt, image, selection) ->
            // When
            state.updatePrompt(prompt)
            state.updateCapturedImage(image)
            if (selection == -1) {
                state.selectCapturedImage()
            } else if (selection >= 0) {
                state.selectSampleImage(selection)
            }
            
            // Then
            val expectedEnabled = prompt.isNotEmpty() && (image != null || selection >= 0)
            assertThat(state.isAnalyzeEnabled).isEqualTo(expectedEnabled)
        }
    }

    @Test
    fun `edge case conditions should be handled correctly`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        
        // Test edge cases
        val edgeCases = listOf(
            " ", // Whitespace only prompt
            "a", // Single character prompt
            "A".repeat(1000), // Very long prompt
        )
        
        edgeCases.forEach { prompt ->
            // When
            state.updatePrompt(prompt)
            state.selectSampleImage(0)
            
            // Then
            val expectedEnabled = prompt.isNotEmpty()
            assertThat(state.isAnalyzeEnabled).isEqualTo(expectedEnabled)
        }
    }

    @Test
    fun `state transitions should maintain consistency`() {
        // Given
        val localThis = this.localThis
        val state = MainScreenState()
        
        // When - complex state transitions
        state.updatePrompt("Test")
        state.selectSampleImage(0)
        assertThat(state.isAnalyzeEnabled).isTrue()
        
        state.updateCapturedImage(mockBitmap) // Should auto-select captured image
        assertThat(state.hasSelectedCapturedImage).isTrue()
        assertThat(state.hasSelectedSampleImage).isFalse()
        
        state.selectSampleImage(1) // Should clear captured image
        assertThat(state.hasSelectedCapturedImage).isFalse()
        assertThat(state.hasSelectedSampleImage).isTrue()
        assertThat(state.capturedImage).isNull()
        
        state.clearCapturedImage() // Should not affect sample selection
        assertThat(state.selectedImageIndex).isEqualTo(1)
        
        // Then - final state consistency check
        assertThat(state.isAnalyzeEnabled).isTrue()
        assertThat(state.hasSelectedSampleImage).isTrue()
        assertThat(state.hasSelectedCapturedImage).isFalse()
    }
} 
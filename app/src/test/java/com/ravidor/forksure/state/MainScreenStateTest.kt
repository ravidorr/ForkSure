package com.ravidor.forksure.state

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MainScreenState
 * Tests state management and derived state calculations
 */
class MainScreenStateTest {

    private lateinit var state: MainScreenState
    private val mockBitmap = mockk<Bitmap>(relaxed = true)

    @Before
    fun setup() {
        state = MainScreenState()
    }

    @Test
    fun `initial state should have correct default values`() {
        assertThat(state.prompt).isEmpty()
        assertThat(state.result).isEmpty()
        assertThat(state.selectedImageIndex).isEqualTo(0)
        assertThat(state.capturedImage).isNull()
        assertThat(state.showReportDialog).isFalse()
    }

    @Test
    fun `updatePrompt should update prompt value`() {
        // When
        state.updatePrompt("What is this recipe?")
        
        // Then
        assertThat(state.prompt).isEqualTo("What is this recipe?")
    }

    @Test
    fun `updateResult should update result value`() {
        // When
        state.updateResult("This is a chocolate cake recipe")
        
        // Then
        assertThat(state.result).isEqualTo("This is a chocolate cake recipe")
    }

    @Test
    fun `selectSampleImage should update selected index and clear captured image`() {
        // Given
        state.updateCapturedImage(mockBitmap)
        
        // When
        state.selectSampleImage(2)
        
        // Then
        assertThat(state.selectedImageIndex).isEqualTo(2)
        assertThat(state.capturedImage).isNull()
    }

    @Test
    fun `selectCapturedImage should set index to -1`() {
        // When
        state.selectCapturedImage()
        
        // Then
        assertThat(state.selectedImageIndex).isEqualTo(-1)
    }

    @Test
    fun `updateCapturedImage should set image and auto-select it`() {
        // When
        state.updateCapturedImage(mockBitmap)
        
        // Then
        assertThat(state.capturedImage).isEqualTo(mockBitmap)
        assertThat(state.selectedImageIndex).isEqualTo(-1)
    }

    @Test
    fun `clearCapturedImage should remove image and reset selection if captured was selected`() {
        // Given
        state.updateCapturedImage(mockBitmap)
        assertThat(state.selectedImageIndex).isEqualTo(-1)
        
        // When
        state.clearCapturedImage()
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(0)
    }

    @Test
    fun `clearCapturedImage should not change selection if sample image was selected`() {
        // Given
        state.selectSampleImage(2)
        state.updateCapturedImage(mockBitmap)
        state.selectSampleImage(1) // Select sample image again
        
        // When
        state.clearCapturedImage()
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(1) // Should remain unchanged
    }

    @Test
    fun `showReportDialog should set dialog visibility to true`() {
        // When
        state.showReportDialog()
        
        // Then
        assertThat(state.showReportDialog).isTrue()
    }

    @Test
    fun `hideReportDialog should set dialog visibility to false`() {
        // Given
        state.showReportDialog()
        
        // When
        state.hideReportDialog()
        
        // Then
        assertThat(state.showReportDialog).isFalse()
    }

    @Test
    fun `isAnalyzeEnabled should be true when prompt and image are available`() {
        // Given
        state.updatePrompt("What is this?")
        state.selectSampleImage(0)
        
        // Then
        assertThat(state.isAnalyzeEnabled).isTrue()
    }

    @Test
    fun `isAnalyzeEnabled should be true with captured image`() {
        // Given
        state.updatePrompt("What is this?")
        state.updateCapturedImage(mockBitmap)
        
        // Then
        assertThat(state.isAnalyzeEnabled).isTrue()
    }

    @Test
    fun `isAnalyzeEnabled should be false when prompt is empty`() {
        // Given
        state.selectSampleImage(0)
        // prompt is empty by default
        
        // Then
        assertThat(state.isAnalyzeEnabled).isFalse()
    }

    @Test
    fun `isAnalyzeEnabled should be false when no image is selected`() {
        // Given
        state.updatePrompt("What is this?")
        // Default selectedImageIndex is 0, which means first sample image is selected
        // To have no image selected, we need to clear captured image and set invalid index
        state.selectCapturedImage() // This sets index to -1
        // But we need to ensure no captured image exists
        
        // Then
        // With selectedImageIndex = -1 and no captured image, analyze should be disabled
        assertThat(state.isAnalyzeEnabled).isFalse()
    }

    @Test
    fun `hasSelectedCapturedImage should be true when captured image is selected`() {
        // Given
        state.updateCapturedImage(mockBitmap)
        
        // Then
        assertThat(state.hasSelectedCapturedImage).isTrue()
    }

    @Test
    fun `hasSelectedCapturedImage should be false when sample image is selected`() {
        // Given
        state.updateCapturedImage(mockBitmap)
        state.selectSampleImage(0)
        
        // Then
        assertThat(state.hasSelectedCapturedImage).isFalse()
    }

    @Test
    fun `hasSelectedCapturedImage should be false when no captured image exists`() {
        // Given
        state.selectCapturedImage()
        // no captured image
        
        // Then
        assertThat(state.hasSelectedCapturedImage).isFalse()
    }

    @Test
    fun `hasSelectedSampleImage should be true when sample image is selected`() {
        // Given
        state.selectSampleImage(1)
        
        // Then
        assertThat(state.hasSelectedSampleImage).isTrue()
    }

    @Test
    fun `hasSelectedSampleImage should be false when captured image is selected`() {
        // Given
        state.updateCapturedImage(mockBitmap)
        
        // Then
        assertThat(state.hasSelectedSampleImage).isFalse()
    }

    @Test
    fun `resetToInitialState should reset all values to defaults`() {
        // Given - modify all state
        state.updatePrompt("Test prompt")
        state.updateResult("Test result")
        state.selectSampleImage(2)
        state.updateCapturedImage(mockBitmap)
        state.showReportDialog()
        
        // When
        state.resetToInitialState()
        
        // Then
        assertThat(state.prompt).isEmpty()
        assertThat(state.result).isEmpty()
        assertThat(state.selectedImageIndex).isEqualTo(0)
        assertThat(state.capturedImage).isNull()
        assertThat(state.showReportDialog).isFalse()
    }

    @Test
    fun `state with custom initial values should use provided values`() {
        // When
        val customState = MainScreenState(
            initialPrompt = "Custom prompt",
            initialResult = "Custom result",
            initialSelectedImageIndex = 2
        )
        
        // Then
        assertThat(customState.prompt).isEqualTo("Custom prompt")
        assertThat(customState.result).isEqualTo("Custom result")
        assertThat(customState.selectedImageIndex).isEqualTo(2)
    }
} 
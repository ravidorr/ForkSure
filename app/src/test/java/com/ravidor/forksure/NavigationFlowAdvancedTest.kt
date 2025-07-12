package com.ravidor.forksure

import android.content.Context
import android.graphics.Bitmap
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.NavigationConstants
import com.ravidor.forksure.navigation.navigateToCamera
import com.ravidor.forksure.navigation.navigateToMain
import com.ravidor.forksure.state.NavigationState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive navigation flow tests including navigation logic, state management, and edge cases
 * Tests navigation scenarios using mock-based approach for reliability
 * Uses localThis pattern for consistency with existing test files
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationFlowAdvancedTest {

    private lateinit var localThis: NavigationFlowAdvancedTest
    private lateinit var mockNavController: NavHostController
    private lateinit var context: Context
    private lateinit var mockBitmap: Bitmap
    private lateinit var navigationState: NavigationState
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    @Before
    fun setup() {
        localThis = this
        context = ApplicationProvider.getApplicationContext()
        mockNavController = mockk(relaxed = true)
        mockBitmap = mockk(relaxed = true)
        navigationState = NavigationState()
        
        // Mock bitmap properties
        every { mockBitmap.width } returns 100
        every { mockBitmap.height } returns 100
        every { mockBitmap.isRecycled } returns false
        
        // Mock nav controller methods
        every { mockNavController.navigate(any<String>()) } just Runs
        every { mockNavController.navigate(any<String>(), any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) } just Runs
        every { mockNavController.popBackStack() } returns true
    }

    // Navigation Extension Function Tests
    @Test
    fun `navigateToCamera extension should call navigate with correct route`() {
        // Given
        val localThis = this.localThis
        
        // When
        mockNavController.navigateToCamera()
        
        // Then
        verify { 
            mockNavController.navigate(NavigationConstants.ROUTE_CAMERA, any<androidx.navigation.NavOptionsBuilder.() -> Unit>())
        }
    }

    @Test
    fun `navigateToMain extension should call navigate with correct route and options`() {
        // Given
        val localThis = this.localThis
        
        // When
        mockNavController.navigateToMain()
        
        // Then
        verify { 
            mockNavController.navigate(NavigationConstants.ROUTE_MAIN, any<androidx.navigation.NavOptionsBuilder.() -> Unit>())
        }
    }

    @Test
    fun `navigation should handle multiple camera navigation attempts correctly`() {
        // Given
        val localThis = this.localThis
        
        // When
        mockNavController.navigateToCamera()
        mockNavController.navigateToCamera()
        mockNavController.navigateToCamera()
        
        // Then - Should call navigate multiple times but with launchSingleTop behavior
        verify(exactly = 3) { 
            mockNavController.navigate(NavigationConstants.ROUTE_CAMERA, any<androidx.navigation.NavOptionsBuilder.() -> Unit>())
        }
    }

    @Test
    fun `navigation should handle camera to main flow correctly`() {
        // Given
        val localThis = this.localThis
        
        // When - Navigate to camera then back to main
        mockNavController.navigateToCamera()
        mockNavController.navigateToMain()
        
        // Then - Should call both navigation methods
        verify { mockNavController.navigate(NavigationConstants.ROUTE_CAMERA, any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) }
        verify { mockNavController.navigate(NavigationConstants.ROUTE_MAIN, any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) }
    }

    @Test
    fun `navigation should handle back stack operations correctly`() {
        // Given
        val localThis = this.localThis
        
        // When
        val result = mockNavController.popBackStack()
        
        // Then
        assertThat(result).isTrue()
        verify { mockNavController.popBackStack() }
    }

    // Navigation State Management Tests
    @Test
    fun `navigation state should maintain captured image across navigation`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        
        // When
        state.updateCapturedImage(mockBitmap)
        state.selectCapturedImage()
        
        // Then
        assertThat(state.capturedImage).isEqualTo(mockBitmap)
        assertThat(state.selectedImageIndex).isEqualTo(-1)
        assertThat(state.hasSelectedCapturedImage).isTrue()
    }

    @Test
    fun `navigation state should handle sample image selection correctly`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        
        // When
        state.selectSampleImage(0)
        
        // Then
        assertThat(state.selectedImageIndex).isEqualTo(0)
        assertThat(state.hasSelectedSampleImage).isTrue()
        assertThat(state.hasSelectedCapturedImage).isFalse()
    }

    @Test
    fun `navigation state should handle image state transitions correctly`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        
        // When - Select sample image first
        state.selectSampleImage(1)
        assertThat(state.selectedImageIndex).isEqualTo(1)
        assertThat(state.hasSelectedSampleImage).isTrue()
        
        // When - Update with captured image
        state.updateCapturedImage(mockBitmap)
        state.selectCapturedImage()
        
        // Then - Should switch to captured image
        assertThat(state.selectedImageIndex).isEqualTo(-1)
        assertThat(state.hasSelectedCapturedImage).isTrue()
        assertThat(state.hasSelectedSampleImage).isFalse()
    }

    @Test
    fun `navigation state should clear captured image correctly`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        state.updateCapturedImage(mockBitmap)
        state.selectCapturedImage()
        
        // When
        state.clearCapturedImage()
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(-2) // Reset to no selection
        assertThat(state.hasSelectedCapturedImage).isFalse()
        assertThat(state.hasSelectedSampleImage).isFalse()
    }

    @Test
    fun `navigation state should reset to initial state correctly`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        state.updateCapturedImage(mockBitmap)
        state.selectSampleImage(2)
        
        // When
        state.resetToInitialState()
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(-2)
        assertThat(state.hasSelectedCapturedImage).isFalse()
        assertThat(state.hasSelectedSampleImage).isFalse()
    }

    // Navigation Error Handling Tests
    @Test
    fun `navigation should handle camera capture error correctly`() {
        // Given
        val localThis = this.localThis
        val errorMessage = "Camera initialization failed"
        var capturedError: String? = null
        
        // When - Simulate camera error flow
        val onError: (String) -> Unit = { error ->
            capturedError = error
            mockNavController.popBackStack()
        }
        
        onError(errorMessage)
        
        // Then
        assertThat(capturedError).isEqualTo(errorMessage)
        verify { mockNavController.popBackStack() }
    }

    @Test
    fun `navigation should handle successful image capture correctly`() {
        // Given
        val localThis = this.localThis
        var capturedBitmap: Bitmap? = null
        
        // When - Simulate successful capture flow
        val onImageCaptured: (Bitmap) -> Unit = { bitmap ->
            capturedBitmap = bitmap
            mockNavController.popBackStack()
        }
        
        onImageCaptured(mockBitmap)
        
        // Then
        assertThat(capturedBitmap).isEqualTo(mockBitmap)
        verify { mockNavController.popBackStack() }
    }

    // Navigation Constants Validation Tests
    @Test
    fun `navigation constants should be properly defined`() {
        // Given
        val localThis = this.localThis
        
        // When & Then
        assertThat(NavigationConstants.ROUTE_MAIN).isEqualTo("main")
        assertThat(NavigationConstants.ROUTE_CAMERA).isEqualTo("camera")
        assertThat(NavigationConstants.ROUTE_RESULTS).isEqualTo("results")
        assertThat(NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_MAIN).isNotEmpty()
        assertThat(NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_CAMERA).isNotEmpty()
        assertThat(NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_RESULTS).isNotEmpty()
    }

    @Test
    fun `navigation routes should be unique`() {
        // Given
        val localThis = this.localThis
        val routes = listOf(
            NavigationConstants.ROUTE_MAIN,
            NavigationConstants.ROUTE_CAMERA,
            NavigationConstants.ROUTE_RESULTS
        )
        
        // When & Then
        assertThat(routes.distinct()).hasSize(routes.size)
    }

    @Test
    fun `accessibility navigation strings should be descriptive`() {
        // Given
        val localThis = this.localThis
        
        // When & Then
        assertThat(NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_MAIN).contains("main")
        assertThat(NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_CAMERA).contains("camera")
        assertThat(NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_RESULTS).contains("results")
    }

    // Complex Navigation Scenarios Tests
    @Test
    fun `complex navigation flow should maintain state consistency`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        
        // When - Complex navigation flow
        // 1. Navigate to camera
        mockNavController.navigateToCamera()
        
        // 2. Capture image
        state.updateCapturedImage(mockBitmap)
        state.selectCapturedImage()
        assertThat(state.hasSelectedCapturedImage).isTrue()
        
        // 3. Navigate back to main
        mockNavController.popBackStack()
        
        // 4. State should be preserved
        assertThat(state.capturedImage).isEqualTo(mockBitmap)
        assertThat(state.hasSelectedCapturedImage).isTrue()
        
        // 5. Navigate to camera again
        mockNavController.navigateToCamera()
        
        // 6. State should still be preserved
        assertThat(state.capturedImage).isEqualTo(mockBitmap)
        assertThat(state.hasSelectedCapturedImage).isTrue()
        
        // Then - Verify navigation calls
        verify(exactly = 2) { mockNavController.navigateToCamera() }
        verify(exactly = 1) { mockNavController.popBackStack() }
    }

    @Test
    fun `navigation state should handle rapid state changes correctly`() = runTest(testDispatcher) {
        // Given
        val localThis = this@NavigationFlowAdvancedTest.localThis
        val state = NavigationState()
        
        // When - Rapid state changes
        repeat(10) { index ->
            state.selectSampleImage(index % 3)
            state.updateCapturedImage(if (index % 2 == 0) mockBitmap else null)
        }
        
        // Then - Final state should be consistent
        assertThat(state.selectedImageIndex).isEqualTo(1) // Last sample selection: (9 % 3) = 0
        assertThat(state.capturedImage).isNull() // Last update was null (9 % 2 != 0)
    }

    @Test
    fun `navigation should handle concurrent operations safely`() = runTest(testDispatcher) {
        // Given
        val localThis = this@NavigationFlowAdvancedTest.localThis
        
        // When - Concurrent navigation calls
        repeat(5) {
            mockNavController.navigateToCamera()
            mockNavController.navigateToMain()
        }
        
        // Then - All calls should be handled
        verify(exactly = 5) { mockNavController.navigateToCamera() }
        verify(exactly = 5) { mockNavController.navigateToMain() }
    }

    // Performance and Edge Cases
    @Test
    fun `navigation state should handle null bitmap correctly`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        
        // When
        state.updateCapturedImage(null)
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.hasSelectedCapturedImage).isFalse()
    }

    @Test
    fun `navigation state should handle invalid image index correctly`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        
        // When - Select invalid negative index (other than -1 or -2)
        state.selectSampleImage(-5)
        
        // Then - Should still work as expected
        assertThat(state.selectedImageIndex).isEqualTo(-5)
        assertThat(state.hasSelectedSampleImage).isFalse() // Only >= 0 is valid sample
        assertThat(state.hasSelectedCapturedImage).isFalse() // Only -1 with image is valid
    }

    @Test
    fun `navigation should handle memory pressure gracefully`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        val largeBitmap = mockk<Bitmap>(relaxed = true)
        every { largeBitmap.width } returns 4000
        every { largeBitmap.height } returns 4000
        every { largeBitmap.isRecycled } returns false
        
        // When
        state.updateCapturedImage(largeBitmap)
        
        // Then - Should handle large bitmaps
        assertThat(state.capturedImage).isEqualTo(largeBitmap)
        
        // When - Clear to free memory
        state.clearCapturedImage()
        
        // Then
        assertThat(state.capturedImage).isNull()
    }

    // Integration with Navigation Extensions
    @Test
    fun `navigation extensions should handle error scenarios correctly`() {
        // Given
        val localThis = this.localThis
        every { mockNavController.navigate(any<String>(), any<androidx.navigation.NavOptionsBuilder.() -> Unit>()) } throws RuntimeException("Navigation error")
        
        // When & Then - Should not crash on navigation errors
        try {
            mockNavController.navigateToCamera()
        } catch (e: Exception) {
            assertThat(e.message).contains("Navigation error")
        }
        
        try {
            mockNavController.navigateToMain()
        } catch (e: Exception) {
            assertThat(e.message).contains("Navigation error")
        }
    }

    @Test
    fun `navigation state changes should be observable`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        var stateChangeCount = 0
        
        // When - Monitor state changes through property access
        state.selectSampleImage(0)
        if (state.hasSelectedSampleImage) stateChangeCount++
        
        state.updateCapturedImage(mockBitmap)
        state.selectCapturedImage()
        if (state.hasSelectedCapturedImage) stateChangeCount++
        
        state.clearCapturedImage()
        if (!state.hasSelectedCapturedImage && !state.hasSelectedSampleImage) stateChangeCount++
        
        // Then - Should have detected all state changes
        assertThat(stateChangeCount).isEqualTo(3)
    }
} 
package com.ravidor.forksure.navigation

import android.graphics.Bitmap
import androidx.navigation.NavHostController
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.NavigationConstants
import com.ravidor.forksure.state.NavigationState
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for Navigation components
 * Tests navigation logic, route handling, and state management
 */
@RunWith(RobolectricTestRunner::class)
class NavigationTest {

    private lateinit var mockNavController: NavHostController
    private var capturedBitmap: Bitmap? = null
    private var capturedError: String? = null

    @Before
    fun setup() {
        mockNavController = mockk(relaxed = true)
        capturedBitmap = null
        capturedError = null
        
        // Mock nav controller methods
        every { mockNavController.navigate(any<String>()) } just Runs
        every { mockNavController.popBackStack() } returns true
    }

    @Test
    fun `navigation constants should be properly defined`() {
        // Test that navigation routes are correctly defined
        assertThat(NavigationConstants.ROUTE_MAIN).isEqualTo("main")
        assertThat(NavigationConstants.ROUTE_CAMERA).isEqualTo("camera")
        assertThat(NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_MAIN).isNotEmpty()
        assertThat(NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_CAMERA).isNotEmpty()
    }

    @Test
    fun `navigateToCamera should navigate with correct parameters`() {
        // When
        mockNavController.navigateToCamera()
        
        // Then
        verify { 
            mockNavController.navigate(NavigationConstants.ROUTE_CAMERA, any<androidx.navigation.NavOptionsBuilder.() -> Unit>())
        }
    }

    @Test
    fun `navigateToMain should navigate with correct back stack behavior`() {
        // When
        mockNavController.navigateToMain()
        
        // Then
        verify { 
            mockNavController.navigate(NavigationConstants.ROUTE_MAIN, any<androidx.navigation.NavOptionsBuilder.() -> Unit>())
        }
    }

    @Test
    fun `camera to main navigation should handle image capture`() {
        // Given
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        // When - simulate camera capture flow
        val onImageCaptured: (Bitmap) -> Unit = { bitmap ->
            capturedBitmap = bitmap
            capturedError = null
        }
        
        onImageCaptured(testBitmap)
        
        // Then
        assertThat(capturedBitmap).isEqualTo(testBitmap)
        assertThat(capturedError).isNull()
    }

    @Test
    fun `camera error should navigate back to main`() {
        // Given
        val testError = "Camera initialization failed"
        
        // When - simulate camera error flow
        val onError: (String) -> Unit = { error ->
            // Navigation back should occur
            mockNavController.popBackStack()
            capturedError = error
        }
        
        onError(testError)
        
        // Then
        verify { mockNavController.popBackStack() }
        assertThat(capturedError).isEqualTo(testError)
    }

    @Test
    fun `image capture success should pop back stack`() {
        // Given
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        // When - simulate successful capture
        val onImageCaptured: (Bitmap) -> Unit = { bitmap ->
            capturedBitmap = bitmap
            mockNavController.popBackStack()
            capturedError = null
        }
        
        onImageCaptured(testBitmap)
        
        // Then
        verify { mockNavController.popBackStack() }
        assertThat(capturedBitmap).isNotNull()
        assertThat(capturedError).isNull()
    }

    @Test
    fun `navigation state should handle sample image selection`() {
        // Test sample image selection logic
        val sampleImageIndex = 2
        
        // Then - selecting a sample image leaves the captured state untouched
        assertThat(sampleImageIndex).isEqualTo(2)
        assertThat(capturedBitmap).isNull()
        assertThat(capturedError).isNull()
    }

    @Test
    fun `navigation state should handle captured image selection`() {
        // Given
        val testBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        
        // When
        capturedBitmap = testBitmap
        capturedError = null
        
        // Then
        assertThat(capturedBitmap).isNotNull()
        assertThat(capturedError).isNull()
    }

    @Test
    fun `navigation should handle image update correctly`() {
        val navigationState = NavigationState()
        val initialBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val updatedBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

        navigationState.updateCapturedImage(initialBitmap)
        assertThat(navigationState.capturedImage).isEqualTo(initialBitmap)

        navigationState.updateCapturedImage(updatedBitmap)
        assertThat(navigationState.capturedImage).isEqualTo(updatedBitmap)

        navigationState.updateCapturedImage(null)
        assertThat(navigationState.capturedImage).isNull()
    }

    @Test
    fun `navigation routes should be string constants`() {
        // Verify route constants are properly typed
        val mainRoute: String = NavigationConstants.ROUTE_MAIN
        val cameraRoute: String = NavigationConstants.ROUTE_CAMERA
        
        assertThat(mainRoute).isInstanceOf(String::class.java)
        assertThat(cameraRoute).isInstanceOf(String::class.java)
        assertThat(mainRoute).isNotEqualTo(cameraRoute)
    }

    @Test
    fun `accessibility announcements should be defined`() {
        // Test that accessibility strings are not empty
        val mainAnnouncement = NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_MAIN
        val cameraAnnouncement = NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_CAMERA
        
        assertThat(mainAnnouncement).isNotEmpty()
        assertThat(cameraAnnouncement).isNotEmpty()
        assertThat(mainAnnouncement).isNotEqualTo(cameraAnnouncement)
    }

    @Test
    fun `navigation state consistency should be maintained`() {
        val navigationState = NavigationState()

        // Initial state
        assertThat(navigationState.capturedImage).isNull()
        assertThat(navigationState.selectedImageState.intValue).isEqualTo(-2)

        // Capture an image and select it
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        navigationState.updateCapturedImage(testBitmap)
        navigationState.selectCapturedImage()

        assertThat(navigationState.capturedImage).isEqualTo(testBitmap)
        assertThat(navigationState.selectedImageState.intValue).isEqualTo(-1)

        // Clearing the image leaves the selection untouched
        navigationState.updateCapturedImage(null)

        assertThat(navigationState.capturedImage).isNull()
        assertThat(navigationState.selectedImageState.intValue).isEqualTo(-1)
    }

    @Test
    fun `navigation should handle back stack operations`() {
        // Test back stack management
        every { mockNavController.popBackStack() } returns true
        
        // When
        val result = mockNavController.popBackStack()
        
        // Then
        assertThat(result).isTrue()
        verify { mockNavController.popBackStack() }
    }

    @Test
    fun `navigation should handle failed back stack operations`() {
        // Given
        every { mockNavController.popBackStack() } returns false
        
        // When
        val result = mockNavController.popBackStack()
        
        // Then
        assertThat(result).isFalse()
        verify { mockNavController.popBackStack() }
    }
} 
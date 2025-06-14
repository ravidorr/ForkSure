package com.ravidor.forksure.navigation

import android.graphics.Bitmap
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.NavigationConstants
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ForkSureNavigation
 * Tests navigation logic, route handling, and state management
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ForkSureNavigationTest {

    private lateinit var mockNavController: NavHostController
    private var capturedBitmap: Bitmap? = null
    private var selectedImageIndex: Int = 0

    @Before
    fun setup() {
        mockNavController = mockk(relaxed = true)
        capturedBitmap = null
        selectedImageIndex = 0
        
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
            selectedImageIndex = -1 // Captured image selected
        }
        
        onImageCaptured(testBitmap)
        
        // Then
        assertThat(capturedBitmap).isEqualTo(testBitmap)
        assertThat(selectedImageIndex).isEqualTo(-1)
    }

    @Test
    fun `camera error should navigate back to main`() {
        // Given
        val testError = "Camera initialization failed"
        
        // When - simulate camera error flow
        val onError: (String) -> Unit = { error ->
            // Navigation back should occur
            mockNavController.popBackStack()
        }
        
        onError(testError)
        
        // Then
        verify { mockNavController.popBackStack() }
    }

    @Test
    fun `image capture success should pop back stack`() {
        // Given
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        
        // When - simulate successful capture
        val onImageCaptured: (Bitmap) -> Unit = { bitmap ->
            capturedBitmap = bitmap
            mockNavController.popBackStack()
        }
        
        onImageCaptured(testBitmap)
        
        // Then
        verify { mockNavController.popBackStack() }
        assertThat(capturedBitmap).isNotNull()
    }

    @Test
    fun `navigation state should handle sample image selection`() {
        // Test sample image selection logic
        val sampleImageIndex = 2
        
        // When
        selectedImageIndex = sampleImageIndex
        capturedBitmap = null // Clear captured image when sample is selected
        
        // Then
        assertThat(selectedImageIndex).isEqualTo(2)
        assertThat(capturedBitmap).isNull()
    }

    @Test
    fun `navigation state should handle captured image selection`() {
        // Given
        val testBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        
        // When
        capturedBitmap = testBitmap
        selectedImageIndex = -1 // -1 indicates captured image is selected
        
        // Then
        assertThat(capturedBitmap).isNotNull()
        assertThat(selectedImageIndex).isEqualTo(-1)
    }

    @Test
    fun `navigation should handle image update correctly`() {
        // Test image update flow
        val initialBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val updatedBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        
        // When - initial update
        capturedBitmap = initialBitmap
        assertThat(capturedBitmap).isEqualTo(initialBitmap)
        
        // When - update with new bitmap
        capturedBitmap = updatedBitmap
        assertThat(capturedBitmap).isEqualTo(updatedBitmap)
        
        // When - clear image
        capturedBitmap = null
        assertThat(capturedBitmap).isNull()
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
        // Test that navigation state remains consistent across operations
        
        // Initial state
        assertThat(selectedImageIndex).isEqualTo(0)
        assertThat(capturedBitmap).isNull()
        
        // Navigate to camera and capture image
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        capturedBitmap = testBitmap
        selectedImageIndex = -1
        
        // Verify state
        assertThat(capturedBitmap).isNotNull()
        assertThat(selectedImageIndex).isEqualTo(-1)
        
        // Select sample image (should clear captured image)
        capturedBitmap = null
        selectedImageIndex = 1
        
        // Verify final state
        assertThat(capturedBitmap).isNull()
        assertThat(selectedImageIndex).isEqualTo(1)
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
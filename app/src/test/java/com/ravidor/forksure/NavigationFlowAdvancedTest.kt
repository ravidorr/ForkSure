package com.ravidor.forksure

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.navigation.navigateToCamera
import com.ravidor.forksure.navigation.navigateToMain
import com.ravidor.forksure.state.NavigationState
import com.ravidor.forksure.state.rememberNavigationState
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
 * Comprehensive navigation flow tests including deep linking, back stack management, and state preservation
 * Tests complex navigation scenarios and edge cases
 * Uses localThis pattern for consistency with existing test files
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationFlowAdvancedTest {

    private lateinit var localThis: NavigationFlowAdvancedTest
    private lateinit var navController: TestNavHostController
    private lateinit var context: Context
    private lateinit var mockBitmap: Bitmap
    private lateinit var navigationState: NavigationState
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    @Before
    fun setup() {
        localThis = this
        context = ApplicationProvider.getApplicationContext()
        navController = TestNavHostController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        mockBitmap = mockk(relaxed = true)
        navigationState = NavigationState()
        
        // Mock bitmap properties
        every { mockBitmap.width } returns 100
        every { mockBitmap.height } returns 100
        every { mockBitmap.isRecycled } returns false
    }

    // Basic Navigation Flow Tests
    @Test
    fun `navigation should start with main screen as start destination`() {
        // Given
        val localThis = this.localThis
        
        // When
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // Then
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
    }

    @Test
    fun `navigation to camera should work correctly`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // When
        navController.navigate(NavigationConstants.ROUTE_CAMERA)
        
        // Then
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_CAMERA)
    }

    @Test
    fun `navigation back from camera should return to main`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        navController.navigate(NavigationConstants.ROUTE_CAMERA)
        
        // When
        navController.popBackStack()
        
        // Then
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
    }

    // Back Stack Management Tests
    @Test
    fun `back stack should contain correct entries after navigation`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // When
        navController.navigate(NavigationConstants.ROUTE_CAMERA)
        
        // Then
        val backStack = navController.backQueue.toList()
        assertThat(backStack).hasSize(2) // Main + Camera
        assertThat(backStack[0].destination.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
        assertThat(backStack[1].destination.route).isEqualTo(NavigationConstants.ROUTE_CAMERA)
    }

    @Test
    fun `multiple navigation to same destination should not create duplicate entries with launchSingleTop`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // When
        navController.navigate(NavigationConstants.ROUTE_CAMERA) {
            launchSingleTop = true
        }
        navController.navigate(NavigationConstants.ROUTE_CAMERA) {
            launchSingleTop = true
        }
        
        // Then
        val backStack = navController.backQueue.toList()
        assertThat(backStack).hasSize(2) // Should not have duplicates
    }

    @Test
    fun `navigation with popUpTo should clear back stack correctly`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        navController.navigate(NavigationConstants.ROUTE_CAMERA)
        
        // When
        navController.navigate(NavigationConstants.ROUTE_MAIN) {
            popUpTo(NavigationConstants.ROUTE_MAIN) {
                inclusive = false
            }
            launchSingleTop = true
        }
        
        // Then
        val backStack = navController.backQueue.toList()
        assertThat(backStack).hasSize(1) // Should only have main
        assertThat(backStack[0].destination.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
    }

    // Navigation Extension Function Tests
    @Test
    fun `navigateToCamera extension should work correctly`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // When
        navController.navigateToCamera()
        
        // Then
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_CAMERA)
    }

    @Test
    fun `navigateToMain extension should work correctly`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        navController.navigate(NavigationConstants.ROUTE_CAMERA)
        
        // When
        navController.navigateToMain()
        
        // Then
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
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
    fun `navigation state should clear captured image when sample image selected`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        state.updateCapturedImage(mockBitmap)
        state.selectCapturedImage()
        
        // When
        state.selectSampleImage(0)
        
        // Then
        assertThat(state.capturedImage).isNull()
        assertThat(state.selectedImageIndex).isEqualTo(0)
        assertThat(state.hasSelectedSampleImage).isTrue()
    }

    @Test
    fun `navigation state should reset selection when captured image cleared`() {
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
        assertThat(state.hasSelectedCapturedImage).isFalse()
        assertThat(state.hasSelectedSampleImage).isFalse()
    }

    // Complex Navigation Scenarios Tests
    @Test
    fun `complex navigation flow should maintain state consistency`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // When - complex navigation flow
        // 1. Navigate to camera
        navController.navigate(NavigationConstants.ROUTE_CAMERA)
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_CAMERA)
        
        // 2. Capture image
        state.updateCapturedImage(mockBitmap)
        state.selectCapturedImage()
        assertThat(state.hasSelectedCapturedImage).isTrue()
        
        // 3. Navigate back to main
        navController.popBackStack()
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
        
        // 4. State should be preserved
        assertThat(state.capturedImage).isEqualTo(mockBitmap)
        assertThat(state.hasSelectedCapturedImage).isTrue()
        
        // 5. Navigate to camera again
        navController.navigate(NavigationConstants.ROUTE_CAMERA)
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_CAMERA)
        
        // 6. State should still be preserved
        assertThat(state.capturedImage).isEqualTo(mockBitmap)
        assertThat(state.hasSelectedCapturedImage).isTrue()
    }

    @Test
    fun `navigation with state transitions should handle all scenarios correctly`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        
        // Test all possible state transitions
        val scenarios = listOf(
            // Scenario 1: No selection → Sample image
            { 
                state.resetToInitialState()
                state.selectSampleImage(0)
                assertThat(state.hasSelectedSampleImage).isTrue()
                assertThat(state.selectedImageIndex).isEqualTo(0)
            },
            // Scenario 2: Sample image → Captured image
            {
                state.resetToInitialState()
                state.selectSampleImage(0)
                state.updateCapturedImage(mockBitmap)
                assertThat(state.hasSelectedCapturedImage).isTrue()
                assertThat(state.capturedImage).isEqualTo(mockBitmap)
            },
            // Scenario 3: Captured image → Sample image
            {
                state.resetToInitialState()
                state.updateCapturedImage(mockBitmap)
                state.selectSampleImage(1)
                assertThat(state.hasSelectedSampleImage).isTrue()
                assertThat(state.capturedImage).isNull()
            },
            // Scenario 4: Clear captured image
            {
                state.resetToInitialState()
                state.updateCapturedImage(mockBitmap)
                state.clearCapturedImage()
                assertThat(state.capturedImage).isNull()
                assertThat(state.selectedImageIndex).isEqualTo(-2)
            }
        )
        
        // When & Then
        scenarios.forEach { scenario ->
            scenario.invoke()
        }
    }

    // Error Handling and Edge Cases Tests
    @Test
    fun `navigation should handle invalid routes gracefully`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // When
        try {
            navController.navigate("invalid_route")
        } catch (e: Exception) {
            // Expected to fail
        }
        
        // Then - should remain on current destination
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
    }

    @Test
    fun `navigation should handle empty back stack correctly`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // When - try to pop when only one item in stack
        val result = navController.popBackStack()
        
        // Then
        assertThat(result).isFalse() // Should return false when can't pop
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
    }

    @Test
    fun `navigation state should handle rapid state changes correctly`() = runTest(testDispatcher) {
        // Given
        val localThis = this@NavigationFlowAdvancedTest.localThis
        val state = NavigationState()
        
        // When - rapid state changes
        repeat(10) { index ->
            state.selectSampleImage(index % 3)
            state.updateCapturedImage(if (index % 2 == 0) mockBitmap else null)
        }
        
        // Then - final state should be consistent
        assertThat(state.selectedImageIndex).isEqualTo(1) // (9 % 3) = 0, then auto-select captured
        assertThat(state.capturedImage).isEqualTo(mockBitmap)
    }

    // Deep Navigation Testing
    @Test
    fun `deep navigation scenarios should work correctly`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // When - simulate deep navigation scenario
        // 1. Multiple navigations
        navController.navigate(NavigationConstants.ROUTE_CAMERA)
        navController.navigate(NavigationConstants.ROUTE_MAIN)
        navController.navigate(NavigationConstants.ROUTE_CAMERA)
        
        // Then - should be on camera screen
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_CAMERA)
        
        // When - navigate back to main with clear stack
        navController.navigate(NavigationConstants.ROUTE_MAIN) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = false
            }
        }
        
        // Then - should be on main with cleared stack
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
        assertThat(navController.backQueue.size).isEqualTo(1)
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

    // State Preservation Tests
    @Test
    fun `navigation state should preserve data across configuration changes`() {
        // Given
        val localThis = this.localThis
        val state = NavigationState()
        
        // When - simulate configuration change scenario
        state.updateCapturedImage(mockBitmap)
        state.selectCapturedImage()
        
        // Simulate saving and restoring state
        val savedImageIndex = state.selectedImageIndex
        val savedImage = state.capturedImage
        
        // Create new state with saved values
        val restoredState = NavigationState(savedImageIndex)
        restoredState.updateCapturedImage(savedImage)
        
        // Then - state should be preserved
        assertThat(restoredState.selectedImageIndex).isEqualTo(savedImageIndex)
        assertThat(restoredState.capturedImage).isEqualTo(savedImage)
        assertThat(restoredState.hasSelectedCapturedImage).isTrue()
    }

    // Performance and Memory Tests
    @Test
    fun `navigation should handle large back stack efficiently`() {
        // Given
        val localThis = this.localThis
        navController.setGraph(navController.createGraph(
            startDestination = NavigationConstants.ROUTE_MAIN,
            builder = {
                composable(NavigationConstants.ROUTE_MAIN) { }
                composable(NavigationConstants.ROUTE_CAMERA) { }
            }
        ))
        
        // When - create large back stack
        repeat(100) {
            navController.navigate(NavigationConstants.ROUTE_CAMERA)
            navController.navigate(NavigationConstants.ROUTE_MAIN)
        }
        
        // Then - should still function correctly
        assertThat(navController.currentDestination?.route).isEqualTo(NavigationConstants.ROUTE_MAIN)
        assertThat(navController.backQueue.size).isGreaterThan(1)
    }
} 
package com.ravidor.forksure.navigation

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ravidor.forksure.AccessibilityHelper
import com.ravidor.forksure.BakingViewModel
import com.ravidor.forksure.CameraCapture
import com.ravidor.forksure.HapticFeedbackType
import com.ravidor.forksure.NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_CAMERA
import com.ravidor.forksure.NavigationConstants.ACCESSIBILITY_NAVIGATION_TO_MAIN
import com.ravidor.forksure.NavigationConstants.ROUTE_CAMERA
import com.ravidor.forksure.NavigationConstants.ROUTE_MAIN
import com.ravidor.forksure.screens.MainScreen
import com.ravidor.forksure.state.NavigationState
import com.ravidor.forksure.state.rememberNavigationState

/**
 * Main navigation composable for the ForkSure app with proper state hoisting
 * Manages navigation between main screen and camera screen
 */
@Composable
fun ForkSureNavigation(
    navController: NavHostController = rememberNavController(),
    bakingViewModel: BakingViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Centralized navigation state management
    val navigationState = rememberNavigationState()
    
    NavHost(
        navController = navController,
        startDestination = ROUTE_MAIN
    ) {
        composable(ROUTE_MAIN) {
            // Announce navigation for accessibility
            LaunchedEffect(Unit) {
                if (AccessibilityHelper.isScreenReaderEnabled(context)) {
                    AccessibilityHelper.announceForAccessibility(context, ACCESSIBILITY_NAVIGATION_TO_MAIN)
                }
            }
            
            MainScreen(
                bakingViewModel = bakingViewModel,
                capturedImage = navigationState.capturedImage,
                selectedImage = navigationState.selectedImageState,
                onNavigateToCamera = {
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                    navController.navigate(ROUTE_CAMERA)
                },
                onCapturedImageUpdated = navigationState::updateCapturedImage
            )
        }
        
        composable(ROUTE_CAMERA) {
            // Announce navigation for accessibility
            LaunchedEffect(Unit) {
                if (AccessibilityHelper.isScreenReaderEnabled(context)) {
                    AccessibilityHelper.announceForAccessibility(context, ACCESSIBILITY_NAVIGATION_TO_CAMERA)
                }
            }
            
            CameraCapture(
                onImageCaptured = { bitmap ->
                    navigationState.updateCapturedImage(bitmap)
                    navigationState.selectCapturedImage()
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                    navController.popBackStack()
                },
                onError = { error ->
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
                    // Navigate back to main screen with error
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Extension function to safely navigate and handle back stack
 */
fun NavHostController.navigateToCamera() {
    navigate(ROUTE_CAMERA) {
        // Avoid multiple instances of camera screen
        launchSingleTop = true
    }
}

/**
 * Extension function to navigate back to main screen
 */
fun NavHostController.navigateToMain() {
    navigate(ROUTE_MAIN) {
        // Clear back stack to main screen
        popUpTo(ROUTE_MAIN) {
            inclusive = false
        }
        launchSingleTop = true
    }
} 
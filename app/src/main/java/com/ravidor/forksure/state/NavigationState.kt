package com.ravidor.forksure.state

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * State holder for navigation-level state
 * Manages state that needs to be shared between navigation destinations
 */
@Stable
class NavigationState(
    initialSelectedImageIndex: Int = -2
) {
    // Image selection state - exposed as MutableIntState for compatibility
    private val _selectedImageState = mutableIntStateOf(initialSelectedImageIndex)
    val selectedImageState: MutableIntState = _selectedImageState
    
    // Captured image state
    var capturedImage by mutableStateOf<Bitmap?>(null)
        private set
    
    // Derived state
    val selectedImageIndex: Int
        get() = selectedImageState.intValue
    
    // State update functions
    fun updateCapturedImage(bitmap: Bitmap?) {
        capturedImage = bitmap
    }
    
    fun selectCapturedImage() {
        selectedImageState.intValue = -1
    }
}

/**
 * Remember a NavigationState instance
 */
@Composable
fun rememberNavigationState(
    initialSelectedImageIndex: Int = -2
): NavigationState {
    return remember {
        NavigationState(initialSelectedImageIndex)
    }
} 
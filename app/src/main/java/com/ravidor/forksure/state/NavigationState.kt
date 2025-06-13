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
    initialSelectedImageIndex: Int = 0
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
    
    val hasSelectedCapturedImage: Boolean
        get() = selectedImageIndex == -1 && capturedImage != null
    
    val hasSelectedSampleImage: Boolean
        get() = selectedImageIndex >= 0
    
    // State update functions
    fun updateCapturedImage(bitmap: Bitmap?) {
        capturedImage = bitmap
    }
    
    fun selectCapturedImage() {
        selectedImageState.intValue = -1
    }
    
    fun selectSampleImage(index: Int) {
        selectedImageState.intValue = index
    }
    
    fun clearCapturedImage() {
        capturedImage = null
        if (selectedImageIndex == -1) {
            // Reset to first sample image if captured image was selected
            selectedImageState.intValue = 0
        }
    }
    
    fun resetToInitialState() {
        capturedImage = null
        selectedImageState.intValue = 0
    }
}

/**
 * Remember a NavigationState instance
 */
@Composable
fun rememberNavigationState(
    initialSelectedImageIndex: Int = 0
): NavigationState {
    return remember {
        NavigationState(initialSelectedImageIndex)
    }
} 
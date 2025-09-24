package com.ravidor.forksure.state

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ravidor.forksure.ContentReportingHelper

/**
 * State holder for the main screen
 * Implements proper state hoisting by centralizing all UI state
 */
@Stable
class MainScreenState(
    initialPrompt: String = "",
    initialResult: String = "",
    initialSelectedImageIndex: Int = -2
) {
    // Prompt input state
    var prompt by mutableStateOf(initialPrompt)
        private set
    
    // Results display state
    var result by mutableStateOf(initialResult)
        private set
    
    // Image selection state
    var selectedImageIndex by mutableIntStateOf(initialSelectedImageIndex)
        private set
    
    // Captured image state
    var capturedImage by mutableStateOf<Bitmap?>(null)
        private set
    
    // Dialog states
    var showReportDialog by mutableStateOf(false)
        private set
    
    // Derived state for UI logic
    val isAnalyzeEnabled: Boolean
        get() = prompt.isNotEmpty() && (capturedImage != null || selectedImageIndex >= 0)
    
    val hasSelectedCapturedImage: Boolean
        get() = selectedImageIndex == -1 && capturedImage != null
    
    val hasSelectedSampleImage: Boolean
        get() = selectedImageIndex >= 0
    
    // State update functions
    fun updatePrompt(newPrompt: String) {
        prompt = newPrompt
    }
    
    fun updateResult(newResult: String) {
        result = newResult
    }
    
    fun selectSampleImage(index: Int) {
        selectedImageIndex = index
        // Clear captured image when sample is selected
        capturedImage = null
    }
    
    fun selectCapturedImage() {
        selectedImageIndex = -1
    }
    
    fun updateCapturedImage(bitmap: Bitmap?) {
        capturedImage = bitmap
        if (bitmap != null) {
            // Auto-select captured image when it's updated
            selectedImageIndex = -1
        }
    }
    
    fun clearCapturedImage() {
        capturedImage = null
        if (selectedImageIndex == -1) {
            // Reset to no selection if captured image was selected
            selectedImageIndex = -2
        }
    }
    
    fun showReportDialog() {
        showReportDialog = true
    }
    
    fun hideReportDialog() {
        showReportDialog = false
    }
    
    fun resetToInitialState() {
        prompt = ""
        result = ""
        selectedImageIndex = -2
        capturedImage = null
        showReportDialog = false
    }
}

/**
 * Remember a MainScreenState instance with proper state preservation
 */
@Composable
fun rememberMainScreenState(
    initialPrompt: String = "",
    initialResult: String = "",
    initialSelectedImageIndex: Int = -2
): MainScreenState {
    return remember {
        MainScreenState(
            initialPrompt = initialPrompt,
            initialResult = initialResult,
            initialSelectedImageIndex = initialSelectedImageIndex
        )
    }
}



/**
 * Actions interface for main screen
 * Defines all possible actions that can be performed on the main screen
 */
@Stable
interface MainScreenActions {
    fun onPromptChange(prompt: String)
    fun onSampleImageSelected(index: Int)
    fun onCapturedImageSelected()
    fun onCapturedImageUpdated(bitmap: Bitmap?)
    fun onAnalyzeClick()
    fun onNavigateToCamera()
    fun onShowReportDialog()
    fun onHideReportDialog()
    fun onReportSubmitted(report: ContentReportingHelper.ContentReport)
    fun onRetryAnalysis()
    fun onDismissError()
    fun onBackToMainScreen()
}

/**
 * Default implementation of MainScreenActions
 * Can be customized for different use cases
 */
@Stable
class DefaultMainScreenActions(
    private val state: MainScreenState,
    private val navigateToCamera: () -> Unit,
    private val onAnalyze: (Bitmap, String) -> Unit,
    private val onSubmitReport: (ContentReportingHelper.ContentReport) -> Unit,
    private val onRetry: () -> Unit,
    private val onDismissError: () -> Unit,
    private val onClearState: () -> Unit
) : MainScreenActions {
    
    override fun onPromptChange(prompt: String) {
        state.updatePrompt(prompt)
    }
    
    override fun onSampleImageSelected(index: Int) {
        state.selectSampleImage(index)
    }
    
    override fun onCapturedImageSelected() {
        state.selectCapturedImage()
    }
    
    override fun onCapturedImageUpdated(bitmap: Bitmap?) {
        state.updateCapturedImage(bitmap)
    }
    
    override fun onAnalyzeClick() {
        val bitmap = if (state.hasSelectedCapturedImage) {
            state.capturedImage!!
        } else {
            // Will be handled by the caller to get sample image bitmap
            null
        }
        bitmap?.let { onAnalyze(it, state.prompt) }
    }
    
    override fun onNavigateToCamera() {
        navigateToCamera()
    }
    
    override fun onShowReportDialog() {
        state.showReportDialog()
    }
    
    override fun onHideReportDialog() {
        state.hideReportDialog()
    }
    
    override fun onReportSubmitted(report: ContentReportingHelper.ContentReport) {
        state.hideReportDialog()
        onSubmitReport(report)
    }
    
    override fun onRetryAnalysis() {
        onRetry()
    }
    
    override fun onDismissError() {
        onDismissError()
    }
    
    override fun onBackToMainScreen() {
        state.resetToInitialState()
        onClearState()
    }
}

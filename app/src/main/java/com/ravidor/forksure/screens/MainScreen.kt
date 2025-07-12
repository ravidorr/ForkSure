package com.ravidor.forksure.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch

// Centralized constants imports
import com.ravidor.forksure.AccessibilityHelper
import com.ravidor.forksure.BakingViewModel
import com.ravidor.forksure.ContentReportingHelper
import com.ravidor.forksure.Dimensions
import com.ravidor.forksure.HapticFeedbackType
import com.ravidor.forksure.R
import com.ravidor.forksure.SampleDataConstants
import com.ravidor.forksure.SecurityStatusIndicator
import com.ravidor.forksure.UiState
import com.ravidor.forksure.state.MainScreenState
import com.ravidor.forksure.state.MainScreenActions
import com.ravidor.forksure.state.DefaultMainScreenActions
import com.ravidor.forksure.state.rememberMainScreenState
import com.ravidor.forksure.MessageContainer
import com.ravidor.forksure.UserMessage
import com.ravidor.forksure.MessageType

/**
 * Main screen of the ForkSure app with proper state hoisting
 * All state is managed through MainScreenState and actions through MainScreenActions
 */
@Composable
fun MainScreen(
    bakingViewModel: BakingViewModel,
    capturedImage: Bitmap?,
    selectedImage: MutableIntState,
    onNavigateToCamera: () -> Unit,
    onCapturedImageUpdated: (Bitmap?) -> Unit
) {
    val placeholderPrompt = stringResource(R.string.prompt_placeholder)
    val placeholderResult = stringResource(R.string.results_placeholder)
    val uiState by bakingViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Centralized state management
    val mainScreenState = rememberMainScreenState(
        initialPrompt = placeholderPrompt,
        initialResult = placeholderResult,
        initialSelectedImageIndex = -2 // -2 means no selection initially (-1 is for captured image)
    )
    
    // Sync external state with internal state - prevent circular updates
    LaunchedEffect(capturedImage) {
        if (capturedImage != mainScreenState.capturedImage) {
            mainScreenState.updateCapturedImage(capturedImage)
        }
    }
    
    LaunchedEffect(selectedImage.intValue) {
        if (selectedImage.intValue != mainScreenState.selectedImageIndex) {
            if (selectedImage.intValue == -1) {
                mainScreenState.selectCapturedImage()
            } else if (selectedImage.intValue >= 0) {
                mainScreenState.selectSampleImage(selectedImage.intValue)
            }
        }
    }
    
    // Update external state when internal state changes - prevent circular updates
    LaunchedEffect(mainScreenState.selectedImageIndex) {
        if (selectedImage.intValue != mainScreenState.selectedImageIndex) {
            selectedImage.intValue = mainScreenState.selectedImageIndex
        }
    }
    
    LaunchedEffect(mainScreenState.capturedImage) {
        if (capturedImage != mainScreenState.capturedImage) {
            onCapturedImageUpdated(mainScreenState.capturedImage)
        }
    }

    // Wrap the entire screen in MessageContainer for user feedback
    MessageContainer { snackbarHostState, showMessage ->
        
        // Create actions with message display support
        val actions = remember(mainScreenState, bakingViewModel, showMessage) {
            DefaultMainScreenActions(
                state = mainScreenState,
                navigateToCamera = onNavigateToCamera,
                onAnalyze = { bitmap, prompt ->
                    bakingViewModel.sendPrompt(bitmap, prompt)
                },
                onSubmitReport = { report ->
                    coroutineScope.launch {
                        try {
                            ContentReportingHelper.submitReport(context, report)
                            showMessage(
                                UserMessage(
                                    text = context.getString(R.string.success_report_submitted),
                                    type = MessageType.SUCCESS
                                )
                            )
                        } catch (e: Exception) {
                            showMessage(
                                UserMessage(
                                    text = context.getString(R.string.error_report_submission_failed),
                                    type = MessageType.ERROR
                                )
                            )
                        }
                    }
                },
                onRetry = { bakingViewModel.retryLastRequest() },
                onDismissError = { bakingViewModel.clearError() }
            )
        }
        
        // Show success message when analysis completes
        LaunchedEffect(uiState) {
            if (uiState is UiState.Success) {
                showMessage(
                    UserMessage(
                        text = context.getString(R.string.success_recipe_generated),
                        type = MessageType.SUCCESS
                    )
                )
            }
        }
        
        MainScreenContent(
            state = mainScreenState,
            actions = actions,
            uiState = uiState,
            bakingViewModel = bakingViewModel,
            showMessage = showMessage
        )
    }
}

/**
 * Stateless main screen content
 * Receives all state and actions as parameters
 */
@Composable
private fun MainScreenContent(
    state: MainScreenState,
    actions: MainScreenActions,
    uiState: UiState,
    bakingViewModel: BakingViewModel,
    showMessage: suspend (UserMessage) -> Unit
) {
    val context = LocalContext.current
    val mainScreenDescription = stringResource(R.string.accessibility_main_screen)
    
    // Announce image selection changes for accessibility and show user feedback
    LaunchedEffect(state.selectedImageIndex) {
        when {
            state.hasSelectedCapturedImage -> {
                showMessage(
                    UserMessage(
                        text = context.getString(R.string.success_image_selected),
                        type = MessageType.INFO
                    )
                )
                if (AccessibilityHelper.isScreenReaderEnabled(context)) {
                    AccessibilityHelper.announceForAccessibility(
                        context, 
                        "Captured image selected for analysis"
                    )
                }
            }
            state.hasSelectedSampleImage -> {
                showMessage(
                    UserMessage(
                        text = context.getString(R.string.success_image_selected),
                        type = MessageType.INFO
                    )
                )
                if (AccessibilityHelper.isScreenReaderEnabled(context)) {
                    val imageDescriptions = SampleDataConstants.IMAGE_DESCRIPTIONS
                    if (state.selectedImageIndex < imageDescriptions.size) {
                        val description = context.getString(imageDescriptions[state.selectedImageIndex])
                        AccessibilityHelper.announceForAccessibility(
                            context, 
                            "$description sample image selected for analysis"
                        )
                    }
                }
            }
            else -> {
                // No selection - don't announce or show message
            }
        }
    }
    
    // Use the hardcoded prompt internally - users cannot see or edit it
    val internalPrompt = stringResource(R.string.prompt_placeholder)
    
    // Create analyze action that handles bitmap creation safely
    val handleAnalyzeClick: () -> Unit = {
        try {
            if (state.hasSelectedCapturedImage) {
                state.capturedImage?.let { bitmap ->
                    if (!bitmap.isRecycled) {
                        bakingViewModel.sendPrompt(bitmap, internalPrompt)
                    }
                }
            } else if (state.hasSelectedSampleImage && state.selectedImageIndex >= 0 && state.selectedImageIndex < SampleDataConstants.SAMPLE_IMAGES.size) {
                val bitmap = BitmapFactory.decodeResource(
                    context.resources,
                    SampleDataConstants.SAMPLE_IMAGES[state.selectedImageIndex]
                )
                bitmap?.let {
                    bakingViewModel.sendPrompt(it, internalPrompt)
                }
            }
        } catch (e: Exception) {
            // Handle bitmap loading errors gracefully
            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { 
                contentDescription = mainScreenDescription
            }
    ) {
        // Main heading
        MainScreenHeader()

        // Security status indicator
        SecurityStatusIndicator(
            viewModel = bakingViewModel,
            modifier = Modifier.padding(bottom = Dimensions.PADDING_SMALL)
        )

        // Only show input controls when not displaying results
        if (uiState !is UiState.Success) {
            // Instructional text - only show when not loading/processing and no image selected
            if (uiState !is UiState.Loading) {
                if (!state.hasSelectedCapturedImage && !state.hasSelectedSampleImage) {
                    InstructionalTextSection()
                }
            }

            // Camera section
            CameraSection(
                onTakePhoto = actions::onNavigateToCamera
            )

            // Show captured image if available
            state.capturedImage?.let { bitmap ->
                CapturedImageCard(
                    bitmap = bitmap,
                    isSelected = state.hasSelectedCapturedImage,
                    onImageClick = actions::onCapturedImageSelected
                )
            }

            // Sample images section
            SampleImagesSection(
                images = SampleDataConstants.SAMPLE_IMAGES,
                imageDescriptions = SampleDataConstants.IMAGE_DESCRIPTIONS,
                selectedImageIndex = if (state.hasSelectedSampleImage) state.selectedImageIndex else -1,
                onImageSelected = actions::onSampleImageSelected
            )

            // Analyze button section (only show when image is selected)
            val shouldShowAnalyzeButton = state.hasSelectedCapturedImage || state.hasSelectedSampleImage
            if (shouldShowAnalyzeButton) {
                AnalyzeButtonSection(
                    isAnalyzeEnabled = true, // Always enabled when visible since we check the condition above
                    onAnalyzeClick = handleAnalyzeClick
                )
            }
        }

        // Results section
        MainResultsSection(
            uiState = uiState,
            result = state.result,
            showReportDialog = state.showReportDialog,
            onShowReportDialog = actions::onShowReportDialog,
            onHideReportDialog = actions::onHideReportDialog,
            onReportSubmitted = actions::onReportSubmitted,
            onRetry = actions::onRetryAnalysis,
            onDismiss = actions::onDismissError,
            onTakeAnotherPhoto = actions::onNavigateToCamera,
            showMessage = showMessage
        )
    }
} 
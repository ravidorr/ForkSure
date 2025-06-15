package com.ravidor.forksure.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import dev.jeziellago.compose.markdowntext.MarkdownText

// Centralized constants imports
import com.ravidor.forksure.AccessibilityHelper
import com.ravidor.forksure.BakingViewModel

import com.ravidor.forksure.ContentReportingHelper
import com.ravidor.forksure.Dimensions
import com.ravidor.forksure.ErrorHandler
import com.ravidor.forksure.ErrorType
import com.ravidor.forksure.HapticFeedbackType
import com.ravidor.forksure.PrintHelper
import com.ravidor.forksure.R
import com.ravidor.forksure.SampleDataConstants
import com.ravidor.forksure.SecurityStatusIndicator
import com.ravidor.forksure.StatelessContentReportDialog
import com.ravidor.forksure.ShareButton
import com.ravidor.forksure.UiState
import com.ravidor.forksure.state.MainScreenState
import com.ravidor.forksure.state.MainScreenActions
import com.ravidor.forksure.state.DefaultMainScreenActions
import com.ravidor.forksure.state.rememberMainScreenState


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
    
    LaunchedEffect(selectedImage.value) {
        if (selectedImage.value != mainScreenState.selectedImageIndex) {
            if (selectedImage.value == -1) {
                mainScreenState.selectCapturedImage()
            } else if (selectedImage.value >= 0) {
                mainScreenState.selectSampleImage(selectedImage.value)
            }
        }
    }
    
    // Actions implementation
    val actions = remember(mainScreenState, bakingViewModel) {
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
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                    } catch (e: Exception) {
                        // Handle error gracefully
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
                    }
                }
            },
            onRetry = { bakingViewModel.retryLastRequest() },
            onDismissError = { bakingViewModel.clearError() }
        )
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

    MainScreenContent(
        state = mainScreenState,
        actions = actions,
        uiState = uiState,
        bakingViewModel = bakingViewModel
    )
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
    bakingViewModel: BakingViewModel
) {
    val context = LocalContext.current
    
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
                contentDescription = "ForkSure baking assistant main screen"
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
            onTakeAnotherPhoto = actions::onNavigateToCamera
        )
    }
}

@Composable
private fun MainScreenHeader() {
    Text(
        text = stringResource(R.string.baking_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .padding(Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = "ForkSure - Baking with AI, main heading"
            }
    )
}

@Composable
private fun InstructionalTextSection() {
    Text(
        text = stringResource(R.string.results_placeholder),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.PADDING_STANDARD, vertical = Dimensions.PADDING_MEDIUM)
            .semantics {
                contentDescription = "Instructions for using the app"
            }
    )
}

@Composable
private fun MainResultsSection(
    uiState: UiState,
    result: String,
    showReportDialog: Boolean,
    onShowReportDialog: () -> Unit,
    onHideReportDialog: () -> Unit,
    onReportSubmitted: (ContentReportingHelper.ContentReport) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onTakeAnotherPhoto: () -> Unit
) {
    when (uiState) {
        is UiState.Loading -> {
            LoadingSection(
                modifier = Modifier.fillMaxSize()
            )
        }
        is UiState.Error -> {
            ErrorSection(
                errorState = uiState,
                onRetry = onRetry,
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxSize()
            )
        }
        is UiState.Success -> {
            val context = LocalContext.current
            RecipeResultsSection(
                outputText = uiState.outputText,
                onReportContent = onShowReportDialog,
                onPrintRecipe = { 
                    // Extract recipe title and print
                    val recipeTitle = PrintHelper.extractRecipeTitle(uiState.outputText)
                    PrintHelper.printRecipe(
                        context = context,
                        recipeContent = uiState.outputText,
                        recipeName = recipeTitle
                    )
                },
                onTakeAnotherPhoto = onTakeAnotherPhoto,
                modifier = Modifier.fillMaxSize()
            )
            
            // Report dialog
            if (showReportDialog) {
                StatelessContentReportDialog(
                    content = uiState.outputText,
                    onDismiss = onHideReportDialog,
                    onReportSubmitted = onReportSubmitted
                )
            }
        }
        else -> {
            // Initial state - no content to show, instructions are above
            Spacer(modifier = Modifier.fillMaxSize())
        }
    }
}



@Composable
private fun CameraSection(
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.PADDING_STANDARD),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { 
                onTakePhoto()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(64.dp)
                .padding(bottom = Dimensions.PADDING_SMALL)
                .semantics {
                    contentDescription = "Take photo button. Opens camera to capture baked goods"
                }
        ) {
            Text(
                text = stringResource(R.string.take_photo),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun CapturedImageCard(
    bitmap: Bitmap,
    isSelected: Boolean,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .padding(Dimensions.PADDING_STANDARD)
            .fillMaxWidth()
            .height(Dimensions.CAPTURED_IMAGE_HEIGHT)
            .clickable { 
                onImageClick()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            }
            .then(
                if (isSelected) {
                    Modifier.border(BorderStroke(Dimensions.BORDER_WIDTH_STANDARD, MaterialTheme.colorScheme.primary))
                } else Modifier
            )
            .semantics {
                contentDescription = if (isSelected) {
                    "Captured image, currently selected for analysis"
                } else {
                    "Captured image, tap to select for analysis"
                }
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SampleImagesSection(
    images: Array<Int>,
    imageDescriptions: Array<Int>,
    selectedImageIndex: Int,
    onImageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = "Horizontal list of sample baking images"
            }
    ) {
        itemsIndexed(images) { index, image ->
            SampleImageItem(
                imageRes = image,
                imageDescription = stringResource(imageDescriptions[index]),
                isSelected = index == selectedImageIndex,
                onImageClick = { onImageSelected(index) }
            )
        }
    }
}

@Composable
private fun SampleImageItem(
    imageRes: Int,
    imageDescription: String,
    isSelected: Boolean,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    
    var imageModifier = Modifier
        .padding(start = Dimensions.PADDING_SMALL, end = Dimensions.PADDING_SMALL)
        .requiredSize(Dimensions.SAMPLE_IMAGE_SIZE)
        .clickable { 
            onImageClick()
            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
        }
        .semantics {
            contentDescription = if (isSelected) {
                "$imageDescription sample image, currently selected for analysis"
            } else {
                "$imageDescription sample image, tap to select for analysis"
            }
        }
        
    if (isSelected) {
        imageModifier = imageModifier.border(BorderStroke(Dimensions.BORDER_WIDTH_STANDARD, MaterialTheme.colorScheme.primary))
    }
    
    Image(
        painter = painterResource(imageRes),
        contentDescription = "",
        modifier = imageModifier
    )
}

@Composable
private fun AnalyzeButtonSection(
    isAnalyzeEnabled: Boolean,
    onAnalyzeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = Dimensions.PADDING_STANDARD),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                onAnalyzeClick()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            enabled = isAnalyzeEnabled,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(64.dp)
                .semantics {
                    contentDescription = if (isAnalyzeEnabled) {
                        "Analyze button. Start AI analysis of selected image"
                    } else {
                        "Analyze button. Disabled. Select an image to enable"
                    }
                }
        ) {
            Text(
                text = stringResource(R.string.action_go),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun LoadingSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .semantics {
                contentDescription = "Loading AI analysis results"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.analyzing_baked_goods),
            modifier = Modifier.padding(top = Dimensions.PADDING_SMALL),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorSection(
    errorState: UiState.Error,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .padding(Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = "Error occurred during analysis"
            }
    ) {
        val errorIcon = when (errorState.errorType) {
            ErrorType.NETWORK -> "📶"
            ErrorType.API_KEY -> "🔑"
            ErrorType.QUOTA_EXCEEDED -> "⏰"
            ErrorType.CONTENT_POLICY -> "🚫"
            ErrorType.IMAGE_SIZE -> "📷"
            ErrorType.SERVER_ERROR -> "🔧"
            ErrorType.UNKNOWN -> "⚠️"
        }
        
        // Error icon and message in a row for better alignment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimensions.PADDING_STANDARD),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = errorIcon,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(end = Dimensions.PADDING_MEDIUM)
                    .semantics {
                        contentDescription = "Error icon: ${errorState.errorType.name.lowercase().replace('_', ' ')}"
                    }
            )
            
            Text(
                text = ErrorHandler.getErrorMessageWithSuggestion(errorState),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .semantics {
                        contentDescription = "Error message and suggestions"
                    }
            )
        }
        
        // Center the action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            ErrorActionButtons(
                canRetry = errorState.canRetry,
                onRetry = onRetry,
                onDismiss = onDismiss
            )
        }
    }
    
    LaunchedEffect(errorState) {
        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
    }
}

@Composable
private fun ErrorActionButtons(
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)) {
        if (canRetry) {
            Button(
                onClick = {
                    onRetry()
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                },
                modifier = Modifier.semantics {
                    contentDescription = "Retry analysis button. Try the AI analysis again"
                }
            ) {
                Text(stringResource(R.string.action_retry))
            }
        }
        
        Button(
            onClick = {
                onDismiss()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            modifier = Modifier.semantics {
                contentDescription = "Dismiss error button. Clear the error message"
            }
        ) {
            Text(stringResource(R.string.action_dismiss))
        }
    }
}

@Composable
private fun RecipeResultsSection(
    outputText: String,
    onReportContent: () -> Unit,
    onPrintRecipe: () -> Unit,
    onTakeAnotherPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .padding(Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = "AI analysis results"
            }
    ) {

        
        MarkdownText(
            markdown = outputText,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .semantics {
                    contentDescription = "AI-generated recipe and analysis: $outputText"
                }
        )
        
        // Action buttons at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimensions.PADDING_STANDARD),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL, Alignment.CenterHorizontally)
        ) {
            // Print button
            Button(
                onClick = {
                    onPrintRecipe()
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                },
                modifier = Modifier.semantics {
                    contentDescription = "Print recipe button. Print the AI-generated recipe"
                }
            ) {
                Text(stringResource(R.string.action_print))
            }
            
            // Share button
            ShareButton(
                outputText = outputText,
                onShareComplete = { success ->
                    if (success) {
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                    }
                }
            )
            
            // Report button
            Button(
                onClick = {
                    onReportContent()
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                },
                modifier = Modifier.semantics {
                    contentDescription = "Report content button. Report inappropriate AI-generated content"
                }
            ) {
                Text(stringResource(R.string.action_report))
            }
        }
        
        // Take Another Photo button - prominent and separate
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimensions.PADDING_MEDIUM),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    onTakeAnotherPhoto()
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .semantics {
                        contentDescription = "Take another photo button. Start analyzing a new baked good"
                    }
            ) {
                Text(
                    text = stringResource(R.string.take_another_photo),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
} 
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.ravidor.forksure.ContentReportDialog
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
import com.ravidor.forksure.UiState
import com.ravidor.forksure.state.MainScreenState
import com.ravidor.forksure.state.MainScreenActions
import com.ravidor.forksure.state.DefaultMainScreenActions
import com.ravidor.forksure.state.rememberMainScreenState
import com.ravidor.forksure.state.rememberContentReportDialogState

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
        initialSelectedImageIndex = selectedImage.value
    )
    
    // Sync external state with internal state
    LaunchedEffect(capturedImage) {
        mainScreenState.updateCapturedImage(capturedImage)
    }
    
    LaunchedEffect(selectedImage.value) {
        if (selectedImage.value != mainScreenState.selectedImageIndex) {
            if (selectedImage.value == -1) {
                mainScreenState.selectCapturedImage()
            } else {
                mainScreenState.selectSampleImage(selectedImage.value)
            }
        }
    }
    
    // Actions implementation
    val actions = remember(mainScreenState, bakingViewModel) {
        DefaultMainScreenActions(
            state = mainScreenState,
            onNavigateToCamera = onNavigateToCamera,
            onAnalyze = { bitmap, prompt ->
                bakingViewModel.sendPrompt(bitmap, prompt)
            },
            onSubmitReport = { report ->
                coroutineScope.launch {
                    ContentReportingHelper.submitReport(context, report)
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                }
            },
            onRetry = { bakingViewModel.retryLastRequest() },
            onDismissError = { bakingViewModel.clearError() }
        )
    }
    
    // Update external state when internal state changes
    LaunchedEffect(mainScreenState.selectedImageIndex) {
        selectedImage.intValue = mainScreenState.selectedImageIndex
    }
    
    LaunchedEffect(mainScreenState.capturedImage) {
        onCapturedImageUpdated(mainScreenState.capturedImage)
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
    
    // Create analyze action that handles bitmap creation
    val handleAnalyzeClick: () -> Unit = {
        if (state.hasSelectedCapturedImage) {
            state.capturedImage?.let { bitmap ->
                bakingViewModel.sendPrompt(bitmap, state.prompt)
            }
        } else if (state.hasSelectedSampleImage) {
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                SampleDataConstants.SAMPLE_IMAGES[state.selectedImageIndex]
            )
            bakingViewModel.sendPrompt(bitmap, state.prompt)
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

        // Input section
        PromptInputSection(
            prompt = state.prompt,
            onPromptChange = actions::onPromptChange,
            isAnalyzeEnabled = state.isAnalyzeEnabled,
            onAnalyzeClick = handleAnalyzeClick
        )

        // Results section
        MainResultsSection(
            uiState = uiState,
            result = state.result,
            showReportDialog = state.showReportDialog,
            onShowReportDialog = actions::onShowReportDialog,
            onHideReportDialog = actions::onHideReportDialog,
            onReportSubmitted = actions::onReportSubmitted,
            onRetry = actions::onRetryAnalysis,
            onDismiss = actions::onDismissError
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
private fun MainResultsSection(
    uiState: UiState,
    result: String,
    showReportDialog: Boolean,
    onShowReportDialog: () -> Unit,
    onHideReportDialog: () -> Unit,
    onReportSubmitted: (ContentReportingHelper.ContentReport) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
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
            // Initial state - show placeholder
            InitialStateSection(result = result)
        }
    }
}

@Composable
private fun InitialStateSection(
    result: String
) {
    Text(
        text = result,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(Dimensions.PADDING_STANDARD)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .semantics {
                contentDescription = "Welcome message"
            }
    )
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
                .padding(bottom = Dimensions.PADDING_SMALL)
                .semantics {
                    contentDescription = "Take photo button. Opens camera to capture baked goods"
                }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "",
                modifier = Modifier.padding(end = Dimensions.PADDING_SMALL)
            )
            Text(stringResource(R.string.take_photo))
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
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.or_choose_sample),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(horizontal = Dimensions.PADDING_STANDARD, vertical = Dimensions.PADDING_SMALL)
                .semantics {
                    contentDescription = "Sample images section heading"
                }
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
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
private fun PromptInputSection(
    prompt: String,
    onPromptChange: (String) -> Unit,
    isAnalyzeEnabled: Boolean,
    onAnalyzeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier
            .padding(all = Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = "Analysis input section"
            }
    ) {
        TextField(
            value = prompt,
            label = { Text(stringResource(R.string.label_prompt)) },
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(0.8f)
                .padding(end = Dimensions.PADDING_STANDARD)
                .align(Alignment.CenterVertically)
                .semantics {
                    contentDescription = "Prompt input field. Enter your question about the baked goods"
                }
        )

        Button(
            onClick = {
                onAnalyzeClick()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            enabled = isAnalyzeEnabled,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .semantics {
                    contentDescription = if (isAnalyzeEnabled) {
                        "Analyze button. Start AI analysis of selected image with your prompt"
                    } else {
                        "Analyze button. Disabled. Select an image and enter a prompt to enable"
                    }
                }
        ) {
            Text(text = stringResource(R.string.action_go))
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
            text = "Analyzing your baked goods...",
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
        Text(
            text = stringResource(R.string.results_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(bottom = Dimensions.PADDING_STANDARD)
                .semantics {
                    contentDescription = "AI analysis results section"
                }
        )
        
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
    }
} 
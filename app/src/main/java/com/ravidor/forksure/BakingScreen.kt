package com.ravidor.forksure

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import dev.jeziellago.compose.markdowntext.MarkdownText

// Centralized constants imports
import com.ravidor.forksure.SampleDataConstants
import com.ravidor.forksure.Dimensions

// Constants moved to centralized Constants.kt file

@Composable
fun BakingScreen(
    bakingViewModel: BakingViewModel = viewModel()
) {
    val selectedImage = remember { mutableIntStateOf(0) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    val placeholderPrompt = stringResource(R.string.prompt_placeholder)
    val placeholderResult = stringResource(R.string.results_placeholder)
    var prompt by rememberSaveable { mutableStateOf(placeholderPrompt) }
    var result by rememberSaveable { mutableStateOf(placeholderResult) }
    val uiState by bakingViewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (showCamera) {
        CameraCapture(
            onImageCaptured = { bitmap ->
                capturedImage = bitmap
                showCamera = false
                selectedImage.intValue = -1 // Indicate that a captured image is selected
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
            },
            onError = { error ->
                // Handle camera error
                showCamera = false
                result = "Camera error: $error"
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
            }
        )
    } else {
        BakingMainContent(
            selectedImage = selectedImage,
            capturedImage = capturedImage,
            prompt = prompt,
            result = result,
            uiState = uiState,
            bakingViewModel = bakingViewModel,
            onTakePhoto = { showCamera = true },
            onCapturedImageClick = { selectedImage.intValue = -1 },
            onSampleImageSelected = { index ->
                selectedImage.intValue = index
                capturedImage = null
            },
            onPromptChange = { prompt = it },
            onAnalyzeClick = {
                val bitmap = if (capturedImage != null && selectedImage.intValue == -1) {
                    capturedImage!!
                } else {
                    BitmapFactory.decodeResource(
                        context.resources,
                        SampleDataConstants.SAMPLE_IMAGES[selectedImage.intValue]
                    )
                }
                bakingViewModel.sendPrompt(bitmap, prompt, context)
            }
        )
    }
}

@Composable
private fun BakingMainContent(
    selectedImage: androidx.compose.runtime.MutableIntState,
    capturedImage: Bitmap?,
    prompt: String,
    result: String,
    uiState: UiState,
    bakingViewModel: BakingViewModel,
    onTakePhoto: () -> Unit,
    onCapturedImageClick: () -> Unit,
    onSampleImageSelected: (Int) -> Unit,
    onPromptChange: (String) -> Unit,
    onAnalyzeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { 
                contentDescription = "ForkSure baking assistant main screen"
            }
    ) {
        // Main heading
        BakingScreenHeader()

        // Security status indicator
        SecurityStatusIndicator(
            viewModel = bakingViewModel,
            modifier = Modifier.padding(bottom = Dimensions.PADDING_SMALL)
        )

        // Camera section
        CameraSection(
            onTakePhoto = onTakePhoto
        )

        // Show captured image if available
        capturedImage?.let { bitmap ->
            CapturedImageCard(
                bitmap = bitmap,
                isSelected = selectedImage.value == -1,
                onImageClick = onCapturedImageClick
            )
        }

        // Sample images section
        SampleImagesSection(
            images = SampleDataConstants.SAMPLE_IMAGES,
            imageDescriptions = SampleDataConstants.IMAGE_DESCRIPTIONS,
            selectedImageIndex = selectedImage.value,
            onImageSelected = onSampleImageSelected
        )

        // Input section
        val isAnalyzeEnabled = prompt.isNotEmpty() && (capturedImage != null || selectedImage.value >= 0)
        PromptInputSection(
            prompt = prompt,
            onPromptChange = onPromptChange,
            isAnalyzeEnabled = isAnalyzeEnabled,
            onAnalyzeClick = onAnalyzeClick
        )

        // Results section
        BakingResultsSection(
            uiState = uiState,
            result = result,
            bakingViewModel = bakingViewModel
        )
    }
}

@Composable
private fun BakingScreenHeader() {
    Text(
        text = stringResource(R.string.baking_title),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .padding(16.dp)
            .semantics {
                contentDescription = "ForkSure - Baking with AI, main heading"
            }
    )
}

@Composable
private fun BakingResultsSection(
    uiState: UiState,
    result: String,
    bakingViewModel: BakingViewModel
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
                onRetry = { bakingViewModel.retryLastRequest() },
                onDismiss = { bakingViewModel.clearError() },
                modifier = Modifier.fillMaxSize()
            )
        }
        is UiState.Success -> {
            var showReportDialog by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            
            RecipeResultsSection(
                outputText = uiState.outputText,
                onReportContent = { showReportDialog = true },
                modifier = Modifier.fillMaxSize()
            )
            
            // Report dialog
            if (showReportDialog) {
                ContentReportDialog(
                    content = uiState.outputText,
                    onDismiss = { showReportDialog = false },
                    onReportSubmitted = { report ->
                        showReportDialog = false
                        coroutineScope.launch {
                            ContentReportingHelper.submitReport(context, report)
                            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                        }
                    }
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
            .padding(16.dp)
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
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { 
                onTakePhoto()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            modifier = Modifier
                .padding(bottom = 8.dp)
                .semantics {
                    contentDescription = "Take photo button. Opens camera to capture baked goods"
                }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "",
                modifier = Modifier.padding(end = 8.dp)
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
            .padding(16.dp)
            .fillMaxWidth()
            .height(200.dp)
            .clickable { 
                onImageClick()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            }
            .then(
                if (isSelected) {
                    Modifier.border(BorderStroke(4.dp, MaterialTheme.colorScheme.primary))
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
        .padding(start = 8.dp, end = 8.dp)
        .requiredSize(120.dp)
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
        imageModifier = imageModifier.border(BorderStroke(4.dp, MaterialTheme.colorScheme.primary))
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
            .padding(all = 16.dp)
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
                .padding(end = 16.dp)
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
            modifier = Modifier.padding(top = 8.dp),
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
            .padding(16.dp)
            .semantics {
                contentDescription = "Error occurred during analysis"
            },
        horizontalAlignment = Alignment.CenterHorizontally
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
        
        Text(
            text = errorIcon,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .semantics {
                    contentDescription = "Error icon: ${errorState.errorType.name.lowercase().replace('_', ' ')}"
                }
        )
        
        Text(
            text = ErrorHandler.getErrorMessageWithSuggestion(errorState),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
                .semantics {
                    contentDescription = "Error message and suggestions"
                }
        )
        
        ErrorActionButtons(
            canRetry = errorState.canRetry,
            onRetry = onRetry,
            onDismiss = onDismiss
        )
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
    
    if (canRetry) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            
            Button(
                onClick = { 
                    onDismiss()
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Dismiss error button. Clear the error message"
                }
            ) {
                Text(stringResource(R.string.action_dismiss))
            }
        }
    } else {
        Button(
            onClick = { 
                onDismiss()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .padding(16.dp)
            .semantics {
                contentDescription = "AI analysis results"
            }
    ) {
        Text(
            text = "Your Recipe",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .semantics {
                    contentDescription = "Recipe results section heading"
                }
        )
        
        val scrollState = rememberScrollState()
        MarkdownText(
            markdown = outputText,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            ),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .semantics {
                    contentDescription = "AI-generated recipe and analysis"
                }
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { 
                    onReportContent()
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Report content button. Report inappropriate AI-generated content"
                }
            ) {
                Text(stringResource(R.string.action_report_content))
            }
        }
    }
    
    LaunchedEffect(outputText) {
        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
    }
}

@Preview(showSystemUi = true)
@Composable
fun BakingScreenPreview() {
    BakingScreen()
}
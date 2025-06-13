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

val images = arrayOf(
    // Image generated using Gemini from the prompt "cupcake image"
    R.drawable.baked_goods_1,
    // Image generated using Gemini from the prompt "cookies images"
    R.drawable.baked_goods_2,
    // Image generated using Gemini from the prompt "cake images"
    R.drawable.baked_goods_3,
)
val imageDescriptions = arrayOf(
    R.string.image1_description,
    R.string.image2_description,
    R.string.image3_description,
)

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics { 
                    contentDescription = "ForkSure baking assistant main screen"
                }
        ) {
            // Main heading
            Text(
                text = stringResource(R.string.baking_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(16.dp)
                    .semantics {
                        contentDescription = "ForkSure - Baking with AI, main heading"
                    }
            )

            // Security status indicator
            SecurityStatusIndicator(
                viewModel = bakingViewModel,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Camera section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { 
                        showCamera = true
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
                        contentDescription = "", // Handled by button description
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.take_photo))
                }
            }

            // Show captured image if available
            capturedImage?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { 
                            selectedImage.intValue = -1
                            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                        }
                        .then(
                            if (selectedImage.intValue == -1) {
                                Modifier.border(BorderStroke(4.dp, MaterialTheme.colorScheme.primary))
                            } else Modifier
                        )
                        .semantics {
                            contentDescription = if (selectedImage.intValue == -1) {
                                "Captured image, currently selected for analysis"
                            } else {
                                "Captured image, tap to select for analysis"
                            }
                        }
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "", // Handled by Card
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Sample images section
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
                    val isSelected = index == selectedImage.intValue
                    val imageDescription = stringResource(imageDescriptions[index])
                    
                    var imageModifier = Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .requiredSize(200.dp)
                        .clickable {
                            selectedImage.intValue = index
                            capturedImage = null // Clear captured image when selecting preset
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
                        painter = painterResource(image),
                        contentDescription = "", // Handled by modifier
                        modifier = imageModifier
                    )
                }
            }

            // Input section
            Row(
                modifier = Modifier
                    .padding(all = 16.dp)
                    .semantics {
                        contentDescription = "Analysis input section"
                    }
            ) {
                TextField(
                    value = prompt,
                    label = { Text(stringResource(R.string.label_prompt)) },
                    onValueChange = { prompt = it },
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(end = 16.dp)
                        .align(Alignment.CenterVertically)
                        .semantics {
                            contentDescription = "Prompt input field. Enter your question about the baked goods"
                        }
                )

                val isAnalyzeEnabled = prompt.isNotEmpty() && (capturedImage != null || selectedImage.intValue >= 0)
                Button(
                    onClick = {
                        val bitmap = if (capturedImage != null && selectedImage.intValue == -1) {
                            capturedImage!!
                        } else {
                            BitmapFactory.decodeResource(
                                context.resources,
                                images[selectedImage.intValue]
                            )
                        }
                        bakingViewModel.sendPrompt(bitmap, prompt, context)
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

            // Results section
            if (uiState is UiState.Loading) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .semantics {
                            contentDescription = "Loading AI analysis results"
                        }
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Analyzing your baked goods...",
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                when (val currentState = uiState) {
                    is UiState.Error -> {
                        // Enhanced error display
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                                .fillMaxSize()
                                .semantics {
                                    contentDescription = "Error occurred during analysis"
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Error icon based on error type
                            val errorIcon = when (currentState.errorType) {
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
                                        contentDescription = "Error icon: ${currentState.errorType.name.lowercase().replace('_', ' ')}"
                                    }
                            )
                            
                            Text(
                                text = ErrorHandler.getErrorMessageWithSuggestion(currentState),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .verticalScroll(rememberScrollState())
                                    .semantics {
                                        contentDescription = "Error message and suggestions"
                                    }
                            )
                            
                            // Retry button (only show if retry is possible)
                            if (currentState.canRetry) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { 
                                            bakingViewModel.retryLastRequest()
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
                                            bakingViewModel.clearError()
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
                                        bakingViewModel.clearError()
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
                        
                        // Announce error with haptic feedback
                        LaunchedEffect(currentState) {
                            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
                        }
                    }
                    is UiState.Success -> {
                        var showReportDialog by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()
                        
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                                .fillMaxSize()
                                .semantics {
                                    contentDescription = "AI analysis results"
                                }
                        ) {
                            // Results heading
                            Text(
                                text = "AI Analysis Results",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .semantics {
                                        contentDescription = "AI analysis results section heading"
                                    }
                            )
                            
                            // Report button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { 
                                        showReportDialog = true
                                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .semantics {
                                            contentDescription = "Report content button. Report inappropriate AI-generated content"
                                        }
                                ) {
                                    Text(stringResource(R.string.action_report_content))
                                }
                            }
                            
                            // AI-generated content
                            val scrollState = rememberScrollState()
                            Text(
                                text = currentState.outputText,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .semantics {
                                        contentDescription = "AI-generated recipe and analysis"
                                    }
                            )
                        }
                        
                        // Report dialog
                        if (showReportDialog) {
                            ContentReportDialog(
                                content = currentState.outputText,
                                onDismiss = { showReportDialog = false },
                                onReportSubmitted = { report ->
                                    showReportDialog = false
                                    coroutineScope.launch {
                                        val result = ContentReportingHelper.submitReport(context, report)
                                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                                    }
                                }
                            )
                        }
                        
                        // Announce success with haptic feedback
                        LaunchedEffect(currentState) {
                            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                        }
                    }
                    else -> {
                        // Initial state - show placeholder
                        Text(
                            text = result,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .semantics {
                                    contentDescription = "Welcome message"
                                }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun BakingScreenPreview() {
    BakingScreen()
}
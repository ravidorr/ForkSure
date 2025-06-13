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

/**
 * Constants for the baking screen
 */
object BakingConstants {
    val SAMPLE_IMAGES = arrayOf(
        // Image generated using Gemini from the prompt "cupcake image"
        R.drawable.baked_goods_1,
        // Image generated using Gemini from the prompt "cookies images"
        R.drawable.baked_goods_2,
        // Image generated using Gemini from the prompt "cake images"
        R.drawable.baked_goods_3,
    )
    
    val IMAGE_DESCRIPTIONS = arrayOf(
        R.string.image1_description,
        R.string.image2_description,
        R.string.image3_description,
    )
}

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
                        BakingConstants.SAMPLE_IMAGES[selectedImage.intValue]
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
            modifier = Modifier.padding(bottom = 8.dp)
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
            images = BakingConstants.SAMPLE_IMAGES,
            imageDescriptions = BakingConstants.IMAGE_DESCRIPTIONS,
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

@Preview(showSystemUi = true)
@Composable
fun BakingScreenPreview() {
    BakingScreen()
}
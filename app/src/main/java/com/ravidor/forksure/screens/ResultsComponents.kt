package com.ravidor.forksure.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ravidor.forksure.AccessibilityHelper
import com.ravidor.forksure.ContentReportingHelper
import com.ravidor.forksure.Dimensions
import com.ravidor.forksure.HapticFeedbackType
import com.ravidor.forksure.MessageType
import com.ravidor.forksure.PrintHelper
import com.ravidor.forksure.R
import com.ravidor.forksure.ShareButton
import com.ravidor.forksure.StatelessContentReportDialog
import com.ravidor.forksure.UiState
import com.ravidor.forksure.UserMessage

/**
 * Results-related UI components for the main screen
 * Contains results display and recipe components
 */

@Composable
fun MainResultsSection(
    uiState: UiState,
    result: String,
    showReportDialog: Boolean,
    onShowReportDialog: () -> Unit,
    onHideReportDialog: () -> Unit,
    onReportSubmitted: (ContentReportingHelper.ContentReport) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onBackToMainScreen: () -> Unit,
    showMessage: suspend (UserMessage) -> Unit
) {
    val context = LocalContext.current
    
    // Announce state changes for accessibility
    LaunchedEffect(uiState) {
        if (AccessibilityHelper.isScreenReaderEnabled(context)) {
            when (uiState) {
                is UiState.Loading -> {
                    AccessibilityHelper.announceForAccessibility(
                        context, 
                        context.getString(R.string.analyzing_baked_goods)
                    )
                }
                is UiState.Success -> {
                    AccessibilityHelper.announceForAccessibility(
                        context, 
                        "Analysis complete. Recipe results are ready."
                    )
                }
                is UiState.Error -> {
                    AccessibilityHelper.announceForAccessibility(
                        context, 
                        "Analysis failed. Error: ${uiState.errorMessage}"
                    )
                }
                else -> { /* No announcement needed for initial state */ }
            }
        }
    }
    
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
            RecipeResultsSection(
                outputText = uiState.outputText,
                onReportContent = onShowReportDialog,
                onPrintRecipe = { 
                    // Extract recipe title and print
                    val recipeTitle = PrintHelper.extractRecipeTitle(uiState.outputText)
                    val printJob = PrintHelper.printRecipe(
                        context = context,
                        recipeContent = uiState.outputText,
                        recipeName = recipeTitle
                    )
                    
                    // Show feedback based on print result
                    CoroutineScope(Dispatchers.Main).launch {
                        if (printJob != null) {
                            showMessage(
                                UserMessage(
                                    text = context.getString(R.string.success_print_started),
                                    type = MessageType.SUCCESS
                                )
                            )
                        } else {
                            showMessage(
                                UserMessage(
                                    text = if (PrintHelper.isPrintingAvailable(context)) {
                                        context.getString(R.string.error_print_failed)
                                    } else {
                                        context.getString(R.string.error_print_not_available)
                                    },
                                    type = MessageType.ERROR
                                )
                            )
                        }
                    }
                },
                onBackToMainScreen = onBackToMainScreen,
                showMessage = showMessage,
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
fun RecipeResultsSection(
    outputText: String,
    onReportContent: () -> Unit,
    onPrintRecipe: () -> Unit,
    onBackToMainScreen: () -> Unit,
    showMessage: suspend (UserMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resultsSectionDescription = stringResource(R.string.accessibility_results_section)
    val aiContentDescription = stringResource(R.string.accessibility_ai_content, outputText)
    val actionPrintText = stringResource(R.string.action_print)
    val actionReportText = stringResource(R.string.action_report)
    val backToMainText = stringResource(R.string.back_to_main_screen)
    
    Column(
        modifier = modifier
            .padding(Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = resultsSectionDescription
            }
    ) {
        MarkdownText(
            markdown = outputText,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .semantics {
                    contentDescription = aiContentDescription
                }
        )
        
        // Action buttons at the bottom
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimensions.PADDING_STANDARD),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL),
            contentPadding = PaddingValues(horizontal = Dimensions.PADDING_SMALL)
        ) {
            // Print button
            item {
                Button(
                    onClick = {
                        onPrintRecipe()
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Print recipe button. Print the AI-generated recipe"
                    }
                ) {
                    Text(
                        text = actionPrintText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Share button
            item {
                ShareButton(
                    outputText = outputText,
                    onShareComplete = { success ->
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                showMessage(
                                    UserMessage(
                                        text = context.getString(R.string.success_share_completed),
                                        type = MessageType.SUCCESS
                                    )
                                )
                            } else {
                                showMessage(
                                    UserMessage(
                                        text = context.getString(R.string.error_share_failed),
                                        type = MessageType.ERROR
                                    )
                                )
                            }
                        }
                    }
                )
            }
            
            // Report button
            item {
                Button(
                    onClick = {
                        onReportContent()
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Report content button. Report inappropriate AI-generated content"
                    }
                ) {
                    Text(
                        text = actionReportText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Back to Main Screen button
            item {
                Button(
                    onClick = {
                        onBackToMainScreen()
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Home button. Return to the main screen to select a new image"
                    }
                ) {
                    Text(
                        text = backToMainText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
} 
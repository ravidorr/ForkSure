package com.ravidor.forksure.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ravidor.forksure.AccessibilityHelper
import com.ravidor.forksure.Dimensions
import com.ravidor.forksure.ErrorHandler
import com.ravidor.forksure.ErrorType
import com.ravidor.forksure.HapticFeedbackType
import com.ravidor.forksure.R
import com.ravidor.forksure.UiState

/**
 * State-specific UI components for the main screen
 * Contains loading, error, and action button components
 */

@Composable
fun LoadingSection(
    modifier: Modifier = Modifier
) {
    val loadingDescription = stringResource(R.string.accessibility_loading_analysis)
    val analyzingText = stringResource(R.string.analyzing_baked_goods)
    
    Column(
        modifier = modifier
            .semantics {
                contentDescription = loadingDescription
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            text = analyzingText,
            modifier = Modifier.padding(top = Dimensions.PADDING_SMALL),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ErrorSection(
    errorState: UiState.Error,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val errorDescription = stringResource(R.string.accessibility_error_occurred)
    
    Column(
        modifier = modifier
            .padding(Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = errorDescription
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
                style = MaterialTheme.typography.bodyMedium,
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
fun ErrorActionButtons(
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val retryButtonDescription = stringResource(R.string.accessibility_retry_button)
    val dismissButtonDescription = stringResource(R.string.accessibility_dismiss_error_button)
    val retryText = stringResource(R.string.action_retry)
    val dismissText = stringResource(R.string.action_dismiss)
    
    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)) {
        if (canRetry) {
            Button(
                onClick = {
                    onRetry()
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                },
                modifier = Modifier.semantics {
                    contentDescription = retryButtonDescription
                }
            ) {
                Text(
                    text = retryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Button(
            onClick = {
                onDismiss()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            modifier = Modifier.semantics {
                contentDescription = dismissButtonDescription
            }
        ) {
            Text(
                text = dismissText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
} 
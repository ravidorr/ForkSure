package com.ravidor.forksure

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Helper for displaying user feedback messages using Snackbar
 * Integrates with the app's accessibility and haptic feedback systems
 */
@Stable
object MessageDisplayHelper {
    
    /**
     * Show a success message with haptic feedback
     */
    suspend fun showSuccessMessage(
        snackbarHostState: SnackbarHostState,
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult {
        return snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration
        )
    }
    
    /**
     * Show an error message with haptic feedback
     */
    suspend fun showErrorMessage(
        snackbarHostState: SnackbarHostState,
        message: String,
        actionLabel: String? = "Retry",
        duration: SnackbarDuration = SnackbarDuration.Long
    ): SnackbarResult {
        return snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration
        )
    }
    
    /**
     * Show a simple info message
     */
    suspend fun showInfoMessage(
        snackbarHostState: SnackbarHostState,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult {
        return snackbarHostState.showSnackbar(
            message = message,
            duration = duration
        )
    }
}

/**
 * Message types for consistent styling and behavior
 */
@Stable
enum class MessageType {
    SUCCESS,
    ERROR,
    INFO,
    WARNING
}

/**
 * Data class for message display
 */
@Immutable
data class UserMessage(
    val text: String,
    val type: MessageType,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = when (type) {
        MessageType.SUCCESS -> SnackbarDuration.Short
        MessageType.ERROR -> SnackbarDuration.Long
        MessageType.INFO -> SnackbarDuration.Short
        MessageType.WARNING -> SnackbarDuration.Long
    }
)

/**
 * Composable for displaying themed Snackbar messages
 */
@Composable
fun ThemedSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    messageType: MessageType = MessageType.INFO
) {
    val backgroundColor = when (messageType) {
        MessageType.SUCCESS -> ThemeColors.successColor()
        MessageType.ERROR -> ThemeColors.errorColor()
        MessageType.WARNING -> ThemeColors.warningColor()
        MessageType.INFO -> MaterialTheme.colorScheme.inverseSurface
    }
    
    val contentColor = when (messageType) {
        MessageType.SUCCESS -> Color.White
        MessageType.ERROR -> Color.White
        MessageType.WARNING -> Color.Black
        MessageType.INFO -> MaterialTheme.colorScheme.inverseOnSurface
    }
    
    Snackbar(
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = contentColor,
        action = {
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                TextButton(
                    onClick = { snackbarData.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = contentColor
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    ) {
        Text(snackbarData.visuals.message)
    }
}

/**
 * Enhanced SnackbarHost that handles different message types
 */
@Composable
fun ThemedSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    currentMessageType: MessageType = MessageType.INFO
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { snackbarData ->
        ThemedSnackbar(
            snackbarData = snackbarData,
            messageType = currentMessageType
        )
    }
}

/**
 * Composable wrapper that manages message display with proper feedback
 */
@Composable
fun MessageContainer(
    modifier: Modifier = Modifier,
    content: @Composable (
        snackbarHostState: SnackbarHostState,
        showMessage: suspend (UserMessage) -> Unit
    ) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var currentMessageType by remember { mutableStateOf(MessageType.INFO) }
    val context = LocalContext.current
    
    val showMessage: suspend (UserMessage) -> Unit = { message ->
        currentMessageType = message.type
        
        // Provide haptic feedback based on message type
        when (message.type) {
            MessageType.SUCCESS -> AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
            MessageType.ERROR -> AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
            else -> AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
        }
        
        // Show the message
        snackbarHostState.showSnackbar(
            message = message.text,
            actionLabel = message.actionLabel,
            duration = message.duration
        )
        
        // Announce for accessibility if screen reader is enabled
        if (AccessibilityHelper.isScreenReaderEnabled(context)) {
            AccessibilityHelper.announceForAccessibility(context, message.text)
        }
    }
    
    Box(modifier = modifier) {
        content(snackbarHostState, showMessage)
        
        ThemedSnackbarHost(
            hostState = snackbarHostState,
            currentMessageType = currentMessageType,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
} 
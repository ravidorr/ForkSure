package com.ravidor.forksure

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * A sealed hierarchy describing the state of the text generation.
 */
@Immutable
sealed interface UiState {

    /**
     * Empty state when the screen is first shown
     */
    @Immutable
    object Initial : UiState

    /**
     * Still loading
     */
    @Immutable
    object Loading : UiState

    /**
     * Text has been generated
     */
    @Immutable
    data class Success(val outputText: String) : UiState

    /**
     * There was an error generating text
     */
    @Immutable
    data class Error(
        val errorMessage: String,
        val errorType: ErrorType = ErrorType.UNKNOWN,
        val canRetry: Boolean = true
    ) : UiState
}

/**
 * Different types of errors that can occur
 */
@Stable
enum class ErrorType {
    NETWORK,           // Network connectivity issues
    API_KEY,           // API key problems
    QUOTA_EXCEEDED,    // Rate limiting or quota issues
    CONTENT_POLICY,    // Content blocked by policy
    IMAGE_SIZE,        // Image too large or invalid format
    SERVER_ERROR,      // Server-side issues
    UNKNOWN           // Unknown or generic errors
}
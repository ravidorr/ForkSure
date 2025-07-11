package com.ravidor.forksure

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

// Centralized constants imports
import com.ravidor.forksure.AppConstants

@Stable
object ErrorHandler {
    private const val TAG = AppConstants.TAG_ERROR_HANDLER

    /**
     * Check if device has network connectivity
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)!!
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Parse exception and return appropriate UiState.Error
     */
    fun handleError(exception: Exception, context: Context): UiState.Error {
        Log.e(TAG, "Error occurred: ${exception.message}", exception)
        
        return when (exception) {
            is UnknownHostException, is SocketTimeoutException -> {
                if (!isNetworkAvailable(context)) {
                    UiState.Error(
                        errorMessage = "No internet connection. Please check your network and try again.",
                        errorType = ErrorType.NETWORK,
                        canRetry = true
                    )
                } else {
                    UiState.Error(
                        errorMessage = "Network timeout. Please check your connection and try again.",
                        errorType = ErrorType.NETWORK,
                        canRetry = true
                    )
                }
            }
            
            else -> {
                // Check for common error messages in the exception
                val message = exception.message?.lowercase() ?: ""
                when {
                    message.contains("api key") || message.contains("invalid key") || message.contains("unauthorized") -> UiState.Error(
                        errorMessage = "API key issue. Please check your configuration.",
                        errorType = ErrorType.API_KEY,
                        canRetry = false
                    )
                    
                    message.contains("quota") || message.contains("rate limit") || message.contains("too many requests") -> UiState.Error(
                        errorMessage = "Rate limit exceeded. Please wait a moment and try again.",
                        errorType = ErrorType.QUOTA_EXCEEDED,
                        canRetry = true
                    )
                    
                    message.contains("blocked") || message.contains("safety") || message.contains("policy") -> UiState.Error(
                        errorMessage = "Content was blocked by safety filters. Please try a different image or prompt.",
                        errorType = ErrorType.CONTENT_POLICY,
                        canRetry = false
                    )
                    
                    message.contains("image") && (message.contains("size") || message.contains("format") || message.contains("large")) -> UiState.Error(
                        errorMessage = "Image issue. Please try a smaller image or different format.",
                        errorType = ErrorType.IMAGE_SIZE,
                        canRetry = false
                    )
                    
                    message.contains("server") || message.contains("internal") || message.contains("503") || message.contains("500") -> UiState.Error(
                        errorMessage = "Server error occurred. Please try again in a few moments.",
                        errorType = ErrorType.SERVER_ERROR,
                        canRetry = true
                    )
                    
                    !isNetworkAvailable(context) -> UiState.Error(
                        errorMessage = "No internet connection. Please check your network and try again.",
                        errorType = ErrorType.NETWORK,
                        canRetry = true
                    )
                    
                    else -> UiState.Error(
                        errorMessage = "An unexpected error occurred: ${exception.localizedMessage ?: "Unknown error"}",
                        errorType = ErrorType.UNKNOWN,
                        canRetry = true
                    )
                }
            }
        }
    }

    /**
     * Get user-friendly error message with action suggestions
     */
    fun getErrorMessageWithSuggestion(error: UiState.Error): String {
        val suggestion = when (error.errorType) {
            ErrorType.NETWORK -> "\n\n💡 Suggestion: Check your WiFi or mobile data connection."
            ErrorType.API_KEY -> "\n\n💡 Suggestion: Contact the app developer to fix the API configuration."
            ErrorType.QUOTA_EXCEEDED -> "\n\n💡 Suggestion: Wait a few minutes before trying again."
            ErrorType.CONTENT_POLICY -> "\n\n💡 Suggestion: Try a different image or modify your prompt."
            ErrorType.IMAGE_SIZE -> "\n\n💡 Suggestion: Take a new photo or choose a different image."
            ErrorType.SERVER_ERROR -> "\n\n💡 Suggestion: The service is temporarily unavailable. Try again shortly."
            ErrorType.UNKNOWN -> "\n\n💡 Suggestion: Try again or restart the app if the problem persists."
        }
        
        return error.errorMessage + suggestion
    }
} 
package com.ravidor.forksure

import android.content.Context
import android.util.Log
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

// Centralized constants imports
import com.ravidor.forksure.AppConstants

/**
 * Enhanced error handler with security integration
 */
@Stable
object EnhancedErrorHandler {
    private const val TAG = AppConstants.TAG_ENHANCED_ERROR_HANDLER
    
    /**
     * Processes and categorizes errors with security considerations
     */
    fun handleError(
        context: Context,
        error: Throwable,
        userInput: String? = null,
        aiResponse: String? = null
    ): EnhancedErrorResult {
        
        // Log error for debugging (sanitized)
        val sanitizedInput = userInput?.take(100)?.replace(Regex("[<>\"'&]"), "")
        Log.e(TAG, "Error occurred: ${error.javaClass.simpleName} - ${error.message}")
        if (sanitizedInput != null) {
            Log.d(TAG, "User input (sanitized): $sanitizedInput")
        }
        
        // Check security environment
        val securityCheck = SecurityManager.checkSecurityEnvironment(context)
        if (securityCheck is SecurityEnvironmentResult.Insecure) {
            Log.w(TAG, "Insecure environment detected: ${securityCheck.issues}")
        }
        
        // Categorize error
        val errorCategory = categorizeError(error)
        
        // Generate appropriate response
        return when (errorCategory) {
            ErrorCategory.NETWORK -> handleNetworkError(error)
            ErrorCategory.AUTHENTICATION -> handleSecurityError(error)
            ErrorCategory.RATE_LIMIT -> handleRateLimitError(error)
            ErrorCategory.INPUT_VALIDATION -> handleInputValidationError(error, userInput)
            ErrorCategory.SERVER_ERROR -> handleAIResponseError(error, aiResponse)
            ErrorCategory.CLIENT_ERROR -> handleSystemError(error)
            ErrorCategory.UNKNOWN -> handleUnknownError(error)
        }
    }
    
    /**
     * Validates and processes AI responses with security checks
     */
    fun processAIResponse(response: String): AIResponseProcessingResult {
        // Validate response safety
        val validationResult = SecurityManager.validateAIResponse(response)
        
        return when (validationResult) {
            is AIResponseValidationResult.Valid -> {
                AIResponseProcessingResult.Success(validationResult.response)
            }
            is AIResponseValidationResult.RequiresWarning -> {
                AIResponseProcessingResult.SuccessWithWarning(
                    validationResult.response,
                    validationResult.warning
                )
            }
            is AIResponseValidationResult.Suspicious -> {
                AIResponseProcessingResult.Warning(
                    response,
                    validationResult.warning,
                    "The AI response may contain inaccuracies. Please verify the information with reliable sources."
                )
            }
            is AIResponseValidationResult.Unsafe -> {
                AIResponseProcessingResult.Blocked(
                    validationResult.reason,
                    validationResult.warning
                )
            }
            is AIResponseValidationResult.Invalid -> {
                AIResponseProcessingResult.Error(
                    validationResult.reason,
                    validationResult.message
                )
            }
        }
    }
    
    /**
     * Validates user input before processing
     */
    fun validateUserInput(input: String): UserInputValidationResult {
        val validationResult = SecurityManager.validateInput(input)
        
        return when (validationResult) {
            is InputValidationResult.Valid -> {
                UserInputValidationResult.Valid(validationResult.sanitizedInput)
            }
            is InputValidationResult.Invalid -> {
                UserInputValidationResult.Invalid(validationResult.reason)
            }
        }
    }
    
    // Private helper methods
    private fun categorizeError(error: Throwable): ErrorCategory {
        return when (error) {
            is UnknownHostException, is SocketTimeoutException -> ErrorCategory.NETWORK
            is SSLException -> ErrorCategory.AUTHENTICATION
            is SecurityException -> ErrorCategory.AUTHENTICATION
            is IllegalArgumentException -> {
                if (error.message?.contains("rate limit", ignoreCase = true) == true) {
                    ErrorCategory.RATE_LIMIT
                } else {
                    ErrorCategory.INPUT_VALIDATION
                }
            }
            is IllegalStateException -> ErrorCategory.CLIENT_ERROR
            else -> {
                // Check error message for specific patterns
                val message = error.message?.lowercase() ?: ""
                when {
                    message.contains("network") || message.contains("connection") -> ErrorCategory.NETWORK
                    message.contains("security") || message.contains("permission") -> ErrorCategory.AUTHENTICATION
                    message.contains("rate") || message.contains("limit") -> ErrorCategory.RATE_LIMIT
                    message.contains("input") || message.contains("validation") -> ErrorCategory.INPUT_VALIDATION
                    message.contains("response") || message.contains("ai") -> ErrorCategory.SERVER_ERROR
                    else -> ErrorCategory.UNKNOWN
                }
            }
        }
    }
    
    private fun handleNetworkError(error: Throwable): EnhancedErrorResult {
        val message = when (error) {
            is UnknownHostException -> "No internet connection. Please check your network and try again."
            is SocketTimeoutException -> "Request timed out. Please check your connection and try again."
            else -> "Network error occurred. Please check your internet connection."
        }
        
        return EnhancedErrorResult.Recoverable(
            title = "Network Error",
            message = message,
            suggestion = "Check your internet connection and try again.",
            canRetry = true,
            retryDelay = 5000
        )
    }
    
    private fun handleSecurityError(error: Throwable): EnhancedErrorResult {
        Log.w(TAG, "Security error: ${error.message}")
        
        return EnhancedErrorResult.Critical(
            title = "Security Error",
            message = "A security issue was detected. Please ensure you're using the app in a secure environment.",
            suggestion = "Try restarting the app or contact support if the issue persists.",
            requiresUserAction = true
        )
    }
    
    private fun handleRateLimitError(error: Throwable): EnhancedErrorResult {
        return EnhancedErrorResult.Temporary(
            title = "Rate Limit Exceeded",
            message = "You've made too many requests. Please wait before trying again.",
            suggestion = "Wait a few minutes before making another request.",
            retryAfter = 60000
        )
    }
    
    private fun handleInputValidationError(error: Throwable, userInput: String?): EnhancedErrorResult {
        val message = if (userInput != null && userInput.length > 1000) {
            "Your input is too long. Please shorten your request and try again."
        } else {
            "Invalid input detected. Please check your request and try again."
        }
        
        return EnhancedErrorResult.UserError(
            title = "Input Error",
            message = message,
            suggestion = "Please modify your input and try again.",
            inputRelated = true
        )
    }
    
    private fun handleAIResponseError(error: Throwable, aiResponse: String?): EnhancedErrorResult {
        return EnhancedErrorResult.ServiceError(
            title = "AI Service Error",
            message = "The AI service encountered an issue processing your request.",
            suggestion = "Please try again with a different prompt or contact support.",
            serviceRelated = true
        )
    }
    
    private fun handleSystemError(error: Throwable): EnhancedErrorResult {
        return EnhancedErrorResult.Critical(
            title = "System Error",
            message = "A system error occurred. The app may need to be restarted.",
            suggestion = "Please restart the app. If the problem persists, contact support.",
            requiresUserAction = true
        )
    }
    
    private fun handleUnknownError(error: Throwable): EnhancedErrorResult {
        return EnhancedErrorResult.Unknown(
            title = "Unexpected Error",
            message = "An unexpected error occurred. Please try again.",
            suggestion = "If the problem persists, please contact support.",
            errorCode = error.javaClass.simpleName
        )
    }
}

/**
 * Categories of errors for better handling
 */
@Stable
enum class ErrorCategory {
    NETWORK,
    AUTHENTICATION,
    RATE_LIMIT,
    INPUT_VALIDATION,
    SERVER_ERROR,
    CLIENT_ERROR,
    UNKNOWN
}

// Enhanced error result types
@Immutable
sealed class EnhancedErrorResult {
    @Immutable
    data class Recoverable(
        val title: String,
        val message: String,
        val suggestion: String,
        val canRetry: Boolean,
        val retryDelay: Long = 0
    ) : EnhancedErrorResult()
    
    @Immutable
    data class Temporary(
        val title: String,
        val message: String,
        val suggestion: String,
        val retryAfter: Long
    ) : EnhancedErrorResult()
    
    @Immutable
    data class UserError(
        val title: String,
        val message: String,
        val suggestion: String,
        val inputRelated: Boolean
    ) : EnhancedErrorResult()
    
    @Immutable
    data class ServiceError(
        val title: String,
        val message: String,
        val suggestion: String,
        val serviceRelated: Boolean
    ) : EnhancedErrorResult()
    
    @Immutable
    data class Critical(
        val title: String,
        val message: String,
        val suggestion: String,
        val requiresUserAction: Boolean
    ) : EnhancedErrorResult()
    
    @Immutable
    data class Unknown(
        val title: String,
        val message: String,
        val suggestion: String,
        val errorCode: String
    ) : EnhancedErrorResult()
}

// AI response processing results
@Immutable
sealed class AIResponseProcessingResult {
    @Immutable
    data class Success(val response: String) : AIResponseProcessingResult()
    @Immutable
    data class SuccessWithWarning(val response: String, val warning: String) : AIResponseProcessingResult()
    @Immutable
    data class Warning(val response: String, val warning: String, val details: String) : AIResponseProcessingResult()
    @Immutable
    data class Blocked(val reason: String, val message: String) : AIResponseProcessingResult()
    @Immutable
    data class Error(val reason: String, val message: String) : AIResponseProcessingResult()
}

// User input validation results
@Immutable
sealed class UserInputValidationResult {
    @Immutable
    data class Valid(val sanitizedInput: String) : UserInputValidationResult()
    @Immutable
    data class Invalid(val reason: String) : UserInputValidationResult()
} 
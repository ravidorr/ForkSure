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
            Log.w(TAG, "Insecure environment detected: ${securityCheck.details}")
            return EnhancedErrorResult.Critical(
                title = context.getString(R.string.error_title_security),
                message = context.getString(R.string.error_security_detected),
                suggestion = context.getString(R.string.suggestion_restart_or_contact),
                requiresUserAction = true
            )
        }
        
        // Categorize error
        val errorCategory = categorizeError(error)
        
        // Generate appropriate response
        return when (errorCategory) {
            ErrorCategory.NETWORK -> handleNetworkError(context, error)
            ErrorCategory.AUTHENTICATION -> handleSecurityError(context, error)
            ErrorCategory.RATE_LIMIT -> handleRateLimitError(context, error)
            ErrorCategory.INPUT_VALIDATION -> handleInputValidationError(context, error, userInput)
            ErrorCategory.SERVER_ERROR -> handleAIResponseError(context, error, aiResponse)
            ErrorCategory.CLIENT_ERROR -> handleSystemError(context, error)
            ErrorCategory.UNKNOWN -> handleUnknownError(context, error)
        }
    }
    
    /**
     * Validates and processes AI responses with security checks
     */
    fun processAIResponse(context: Context, response: String): AIResponseProcessingResult {
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
                    context.getString(R.string.ai_response_warning)
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
    
    private fun handleNetworkError(context: Context, error: Throwable): EnhancedErrorResult {
        val message = when (error) {
            is UnknownHostException -> context.getString(R.string.error_no_internet_connection)
            is SocketTimeoutException -> context.getString(R.string.error_request_timeout)
            else -> context.getString(R.string.error_network_general)
        }
        
        return EnhancedErrorResult.Recoverable(
            title = context.getString(R.string.error_title_network),
            message = message,
            suggestion = context.getString(R.string.suggestion_check_connection_retry),
            canRetry = true,
            retryDelay = 5000
        )
    }
    
    private fun handleSecurityError(context: Context, error: Throwable): EnhancedErrorResult {
        Log.w(TAG, "Security error: ${error.message}")
        
        return EnhancedErrorResult.Critical(
            title = context.getString(R.string.error_title_security),
            message = context.getString(R.string.error_security_detected),
            suggestion = context.getString(R.string.suggestion_restart_or_contact),
            requiresUserAction = true
        )
    }
    
    private fun handleRateLimitError(context: Context, error: Throwable): EnhancedErrorResult {
        return EnhancedErrorResult.Temporary(
            title = context.getString(R.string.error_title_rate_limit),
            message = context.getString(R.string.error_rate_limit_message),
            suggestion = context.getString(R.string.suggestion_wait_minutes),
            retryAfter = 60000
        )
    }
    
    private fun handleInputValidationError(context: Context, error: Throwable, userInput: String?): EnhancedErrorResult {
        val message = if (userInput != null && userInput.length > 1000) {
            context.getString(R.string.error_input_too_long)
        } else {
            context.getString(R.string.error_input_invalid)
        }
        
        return EnhancedErrorResult.UserError(
            title = context.getString(R.string.error_title_input),
            message = message,
            suggestion = context.getString(R.string.suggestion_modify_input),
            inputRelated = true
        )
    }
    
    private fun handleAIResponseError(context: Context, error: Throwable, aiResponse: String?): EnhancedErrorResult {
        return EnhancedErrorResult.ServiceError(
            title = context.getString(R.string.error_title_ai_service),
            message = context.getString(R.string.error_ai_service_issue),
            suggestion = context.getString(R.string.suggestion_try_different_prompt),
            serviceRelated = true
        )
    }
    
    private fun handleSystemError(context: Context, error: Throwable): EnhancedErrorResult {
        return EnhancedErrorResult.Critical(
            title = context.getString(R.string.error_title_system),
            message = context.getString(R.string.error_system_general),
            suggestion = context.getString(R.string.suggestion_restart_app),
            requiresUserAction = true
        )
    }
    
    private fun handleUnknownError(context: Context, error: Throwable): EnhancedErrorResult {
        return EnhancedErrorResult.Unknown(
            title = context.getString(R.string.error_title_unexpected),
            message = context.getString(R.string.error_unexpected_general),
            suggestion = context.getString(R.string.suggestion_contact_support),
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
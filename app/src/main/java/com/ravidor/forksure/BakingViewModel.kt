package com.ravidor.forksure

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Centralized constants imports
import com.ravidor.forksure.AppConstants

class BakingViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey
    )

    // Store last request for retry functionality
    private var lastBitmap: Bitmap? = null
    private var lastPrompt: String? = null
    private var lastContext: Context? = null
    private var requestCount = 0

    companion object {
        private const val TAG = AppConstants.TAG_BAKING_VIEW_MODEL
    }

    fun sendPrompt(
        bitmap: Bitmap,
        prompt: String,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                // 1. Check security environment
                val securityCheck = SecurityManager.checkSecurityEnvironment(context)
                if (securityCheck is SecurityEnvironmentResult.Insecure) {
                    Log.w(TAG, "Security issues detected: ${securityCheck.issues}")
                    _uiState.value = UiState.Error(
                        "Security validation failed. Please ensure you're using the app in a secure environment.",
                        ErrorType.UNKNOWN,
                        false
                    )
                    return@launch
                }

                // 2. Validate user input
                val inputValidation = EnhancedErrorHandler.validateUserInput(prompt)
                when (inputValidation) {
                    is UserInputValidationResult.Invalid -> {
                        _uiState.value = UiState.Error(
                            inputValidation.reason,
                            ErrorType.CONTENT_POLICY,
                            false
                        )
                        return@launch
                    }
                    is UserInputValidationResult.Valid -> {
                        // Use sanitized input
                        val sanitizedPrompt = inputValidation.sanitizedInput
                        processSafeRequest(bitmap, sanitizedPrompt, context)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendPrompt", e)
                val enhancedError = EnhancedErrorHandler.handleError(context, e, prompt)
                handleEnhancedError(enhancedError)
            }
        }
    }

    private suspend fun processSafeRequest(
        bitmap: Bitmap,
        sanitizedPrompt: String,
        context: Context
    ) {
        try {
            // 3. Check rate limiting
            val rateLimitResult = SecurityManager.checkRateLimit(context, "ai_requests")
            when (rateLimitResult) {
                is RateLimitResult.Blocked -> {
                    _uiState.value = UiState.Error(
                        rateLimitResult.reason,
                        ErrorType.QUOTA_EXCEEDED,
                        false
                    )
                    return
                }
                is RateLimitResult.Allowed -> {
                    Log.d(TAG, "Rate limit check passed. Requests remaining: ${rateLimitResult.requestsRemaining}")
                }
            }

            // Store for potential retry (only after validation)
            lastBitmap = bitmap
            lastPrompt = sanitizedPrompt
            lastContext = context
            requestCount++

            _uiState.value = UiState.Loading

            // 4. Make AI request with timeout and error handling
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = generativeModel.generateContent(
                        content {
                            image(bitmap)
                            text(sanitizedPrompt)
                        }
                    )

                    response.text?.let { outputContent ->
                        // 5. Validate AI response for safety
                        val responseValidation = EnhancedErrorHandler.processAIResponse(outputContent)
                        handleAIResponse(responseValidation)
                    } ?: run {
                        _uiState.value = UiState.Error(
                            "No response received from AI service. Please try again.",
                            ErrorType.SERVER_ERROR,
                            true
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AI request failed", e)
                    val enhancedError = EnhancedErrorHandler.handleError(context, e, sanitizedPrompt, null)
                    handleEnhancedError(enhancedError)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in processSafeRequest", e)
            val enhancedError = EnhancedErrorHandler.handleError(context, e, sanitizedPrompt)
            handleEnhancedError(enhancedError)
        }
    }

    private fun handleAIResponse(responseValidation: AIResponseProcessingResult) {
        when (responseValidation) {
            is AIResponseProcessingResult.Success -> {
                _uiState.value = UiState.Success(responseValidation.response)
            }
            is AIResponseProcessingResult.SuccessWithWarning -> {
                // Show response with warning
                val responseWithWarning = "${responseValidation.response}\n\n${responseValidation.warning}"
                _uiState.value = UiState.Success(responseWithWarning)
            }
            is AIResponseProcessingResult.Warning -> {
                // Show response with warning details
                val responseWithWarning = "${responseValidation.response}\n\n⚠️ ${responseValidation.warning}\n\n${responseValidation.details}"
                _uiState.value = UiState.Success(responseWithWarning)
            }
            is AIResponseProcessingResult.Blocked -> {
                _uiState.value = UiState.Error(
                    "Response blocked: ${responseValidation.message}",
                    ErrorType.CONTENT_POLICY,
                    false
                )
            }
            is AIResponseProcessingResult.Error -> {
                _uiState.value = UiState.Error(
                    "AI response error: ${responseValidation.message}",
                    ErrorType.SERVER_ERROR,
                    true
                )
            }
        }
    }

    private fun handleEnhancedError(enhancedError: EnhancedErrorResult) {
        when (enhancedError) {
            is EnhancedErrorResult.Recoverable -> {
                _uiState.value = UiState.Error(
                    "${enhancedError.message} ${enhancedError.suggestion}",
                    ErrorType.NETWORK,
                    enhancedError.canRetry
                )
            }
            is EnhancedErrorResult.Temporary -> {
                _uiState.value = UiState.Error(
                    "${enhancedError.message} ${enhancedError.suggestion}",
                    ErrorType.QUOTA_EXCEEDED,
                    false
                )
            }
            is EnhancedErrorResult.UserError -> {
                _uiState.value = UiState.Error(
                    "${enhancedError.message} ${enhancedError.suggestion}",
                    ErrorType.CONTENT_POLICY,
                    false
                )
            }
            is EnhancedErrorResult.ServiceError -> {
                _uiState.value = UiState.Error(
                    "${enhancedError.message} ${enhancedError.suggestion}",
                    ErrorType.SERVER_ERROR,
                    true
                )
            }
            is EnhancedErrorResult.Critical -> {
                _uiState.value = UiState.Error(
                    "${enhancedError.message} ${enhancedError.suggestion}",
                    ErrorType.UNKNOWN,
                    false
                )
            }
            is EnhancedErrorResult.Unknown -> {
                _uiState.value = UiState.Error(
                    "${enhancedError.message} ${enhancedError.suggestion}",
                    ErrorType.UNKNOWN,
                    true
                )
            }
        }
    }

    fun retryLastRequest() {
        val bitmap = lastBitmap
        val prompt = lastPrompt
        val context = lastContext
        
        if (bitmap != null && prompt != null && context != null) {
            Log.d(TAG, "Retrying last request (attempt ${requestCount + 1})")
            sendPrompt(bitmap, prompt, context)
        } else {
            Log.w(TAG, "Cannot retry: missing request data")
            _uiState.value = UiState.Error(
                "Cannot retry request. Please try again with a new request.",
                ErrorType.UNKNOWN,
                false
            )
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Initial
        }
    }

    fun getSecurityStatus(context: Context): SecurityEnvironmentResult {
        return SecurityManager.checkSecurityEnvironment(context)
    }

    fun getRequestCount(): Int = requestCount

    override fun onCleared() {
        super.onCleared()
        // Clear sensitive data
        lastBitmap = null
        lastPrompt = null
        lastContext = null
        Log.d(TAG, "ViewModel cleared, sensitive data removed")
    }
}
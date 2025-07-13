package com.ravidor.forksure

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.SecurityRepository
import com.ravidor.forksure.repository.PreferencesCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Centralized constants imports
import com.ravidor.forksure.AppConstants

@HiltViewModel
class BakingViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val securityRepository: SecurityRepository,
    private val preferencesCacheManager: PreferencesCacheManager // <-- Injected
) : ViewModel() {
    
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    // Store last request for retry functionality
    private var lastBitmap: Bitmap? = null
    private var lastPrompt: String? = null
    private var requestCount = 0

    companion object {
        private const val TAG = AppConstants.TAG_BAKING_VIEW_MODEL
    }

    fun sendPrompt(
        bitmap: Bitmap,
        prompt: String
    ) {
        viewModelScope.launch {
            try {
                // 1. Check security environment
                val securityCheck = securityRepository.checkSecurityEnvironment()
                if (securityCheck is SecurityEnvironmentResult.Insecure) {
                    Log.w(TAG, "Security issues detected: ${securityCheck.details}")
                    _uiState.value = UiState.Error(
                        "Security validation failed. Please ensure you're using the app in a secure environment.",
                        ErrorType.UNKNOWN,
                        false
                    )
                    return@launch
                }

                // 2. Validate user input
                val inputValidation = securityRepository.validateUserInput(prompt)
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
                        processSafeRequest(bitmap, sanitizedPrompt)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendPrompt", e)
                handleGenericError(e)
            }
        }
    }

    private suspend fun processSafeRequest(
        bitmap: Bitmap,
        sanitizedPrompt: String
    ) {
        try {
            // 3. Check rate limiting
            val rateLimitResult = securityRepository.checkRateLimit("ai_requests")
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
            requestCount++

            _uiState.value = UiState.Loading

            // 4. Make AI request through repository
            val responseValidation = aiRepository.generateContent(bitmap, sanitizedPrompt)
            handleAIResponse(responseValidation)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in processSafeRequest", e)
            handleGenericError(e)
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

    private fun handleGenericError(error: Throwable) {
        _uiState.value = UiState.Error(
            "An unexpected error occurred: ${error.message}",
            ErrorType.UNKNOWN,
            true
        )
    }

    fun retryLastRequest() {
        val bitmap = lastBitmap
        val prompt = lastPrompt
        
        if (bitmap != null && prompt != null) {
            Log.d(TAG, "Retrying last request (attempt ${requestCount + 1})")
            sendPrompt(bitmap, prompt)
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

    suspend fun getSecurityStatus(): SecurityEnvironmentResult {
        return securityRepository.checkSecurityEnvironment()
    }

    fun getRequestCount(): Int = requestCount

    override fun onCleared() {
        super.onCleared()
        // Clear sensitive data
        lastBitmap = null
        lastPrompt = null
        Log.d(TAG, "ViewModel cleared, sensitive data removed")
    }
}
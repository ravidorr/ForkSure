package com.ravidor.forksure.repository

import android.graphics.Bitmap
import com.ravidor.forksure.AIResponseProcessingResult
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of AIRepository for Android integration testing
 * Provides controllable responses and behavior for testing scenarios
 */
@Singleton
class FakeAIRepository @Inject constructor() : AIRepository {

    // Test configuration
    var shouldReturnError = false
    var shouldReturnBlocked = false
    var shouldReturnWarning = false
    var customResponse = "Test AI response"
    var delayMs = 0L
    var errorMessage = "Test error"

    override suspend fun generateContent(bitmap: Bitmap, prompt: String): AIResponseProcessingResult {
        if (delayMs > 0) {
            delay(delayMs)
        }

        return when {
            shouldReturnError -> AIResponseProcessingResult.Error(
                reason = "Test error",
                message = errorMessage
            )
            shouldReturnBlocked -> AIResponseProcessingResult.Blocked(
                reason = "Test blocked",
                message = "Content blocked for testing"
            )
            shouldReturnWarning -> AIResponseProcessingResult.Warning(
                response = customResponse,
                warning = "Test warning",
                details = "This is a test warning"
            )
            else -> AIResponseProcessingResult.Success(customResponse)
        }
    }

    // Test helper methods
    fun reset() {
        shouldReturnError = false
        shouldReturnBlocked = false
        shouldReturnWarning = false
        customResponse = "Test AI response"
        delayMs = 0L
        errorMessage = "Test error"
    }

    fun setSuccessResponse(response: String) {
        reset()
        customResponse = response
    }

    fun setErrorResponse(error: String) {
        reset()
        shouldReturnError = true
        errorMessage = error
    }

    fun setBlockedResponse() {
        reset()
        shouldReturnBlocked = true
    }

    fun setWarningResponse(response: String, warning: String) {
        reset()
        shouldReturnWarning = true
        customResponse = response
    }

    fun setDelay(delayMs: Long) {
        this.delayMs = delayMs
    }
} 
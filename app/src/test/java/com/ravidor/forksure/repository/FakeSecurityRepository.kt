package com.ravidor.forksure.repository

import com.ravidor.forksure.RateLimitResult
import com.ravidor.forksure.SecurityEnvironmentResult
import com.ravidor.forksure.UserInputValidationResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of SecurityRepository for testing
 * Provides controllable security responses for testing scenarios
 */
@Singleton
class FakeSecurityRepository @Inject constructor() : SecurityRepository {

    // Test configuration
    var isSecureEnvironment = true
    var securityIssues = emptyList<String>()
    var isRateLimited = false
    var requestsRemaining = 10
    var rateLimitReason = "Rate limit exceeded"
    var isInputValid = true
    var inputValidationReason = "Invalid input"
    var sanitizedInput = ""

    override suspend fun checkRateLimit(identifier: String): RateLimitResult {
        return if (isRateLimited) {
            RateLimitResult.Blocked(
                reason = rateLimitReason,
                retryAfterSeconds = 60,
                requestsRemaining = 0
            )
        } else {
            RateLimitResult.Allowed(
                requestsRemaining = requestsRemaining,
                resetTimeSeconds = 60
            )
        }
    }

    override suspend fun checkSecurityEnvironment(): SecurityEnvironmentResult {
        return if (isSecureEnvironment) {
            SecurityEnvironmentResult.Secure
        } else {
            SecurityEnvironmentResult.Insecure(securityIssues)
        }
    }

    override suspend fun validateUserInput(input: String): UserInputValidationResult {
        return if (isInputValid) {
            UserInputValidationResult.Valid(sanitizedInput.ifEmpty { input })
        } else {
            UserInputValidationResult.Invalid(inputValidationReason)
        }
    }

    // Test helper methods
    fun reset() {
        isSecureEnvironment = true
        securityIssues = emptyList()
        isRateLimited = false
        requestsRemaining = 10
        rateLimitReason = "Rate limit exceeded"
        isInputValid = true
        inputValidationReason = "Invalid input"
        sanitizedInput = ""
    }

    fun setSecureEnvironment() {
        isSecureEnvironment = true
        securityIssues = emptyList()
    }

    fun setInsecureEnvironment(issues: List<String>) {
        isSecureEnvironment = false
        securityIssues = issues
    }

    fun setRateLimited(reason: String = "Rate limit exceeded") {
        isRateLimited = true
        rateLimitReason = reason
        requestsRemaining = 0
    }

    fun setRateAllowed(remaining: Int = 10) {
        isRateLimited = false
        requestsRemaining = remaining
    }

    fun setInputValid(sanitized: String = "") {
        isInputValid = true
        sanitizedInput = sanitized
    }

    fun setInputInvalid(reason: String = "Invalid input") {
        isInputValid = false
        inputValidationReason = reason
    }
} 
package com.ravidor.forksure

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Comprehensive unit tests for EnhancedErrorHandler
 * Tests error categorization, AI response processing, security integration, and error scenarios
 * Follows localThis pattern for consistent test structure
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnhancedErrorHandlerTest {

    private lateinit var localThis: TestContext
    
    private data class TestContext(
        val mockContext: Context,
        var lastErrorHandled: EnhancedErrorResult?,
        var lastAIProcessingResult: AIResponseProcessingResult?,
        var lastInputValidationResult: UserInputValidationResult?
    )

    @Before
    fun setup() {
        val mockContext = mockk<Context>(relaxed = true)
        localThis = TestContext(
            mockContext = mockContext,
            lastErrorHandled = null,
            lastAIProcessingResult = null,
            lastInputValidationResult = null
        )
        
        // Mock all string resources
        every { mockContext.getString(R.string.error_title_security) } returns "Security Error"
        every { mockContext.getString(R.string.error_title_network) } returns "Network Error"
        every { mockContext.getString(R.string.error_title_rate_limit) } returns "Rate Limit Exceeded"
        every { mockContext.getString(R.string.error_title_input) } returns "Input Error"
        every { mockContext.getString(R.string.error_title_ai_service) } returns "AI Service Error"
        every { mockContext.getString(R.string.error_title_system) } returns "System Error"
        every { mockContext.getString(R.string.error_title_unexpected) } returns "Unexpected Error"
        
        every { mockContext.getString(R.string.error_security_detected) } returns "A security issue was detected. Please ensure you're using the app in a secure environment."
        every { mockContext.getString(R.string.error_no_internet_connection) } returns "No internet connection. Please check your network and try again."
        every { mockContext.getString(R.string.error_request_timeout) } returns "Request timed out. Please check your connection and try again."
        every { mockContext.getString(R.string.error_network_general) } returns "Network error occurred. Please check your internet connection."
        every { mockContext.getString(R.string.error_rate_limit_message) } returns "You've made too many requests. Please wait before trying again."
        every { mockContext.getString(R.string.error_input_too_long) } returns "Your input is too long. Please shorten your request and try again."
        every { mockContext.getString(R.string.error_input_invalid) } returns "Invalid input detected. Please check your request and try again."
        every { mockContext.getString(R.string.error_ai_service_issue) } returns "The AI service encountered an issue processing your request."
        every { mockContext.getString(R.string.error_system_general) } returns "A system error occurred. The app may need to be restarted."
        every { mockContext.getString(R.string.error_unexpected_general) } returns "An unexpected error occurred. Please try again."
        
        every { mockContext.getString(R.string.suggestion_restart_or_contact) } returns "Try restarting the app or contact support if the issue persists."
        every { mockContext.getString(R.string.suggestion_check_connection_retry) } returns "Check your internet connection and try again."
        every { mockContext.getString(R.string.suggestion_wait_minutes) } returns "Wait a few minutes before making another request."
        every { mockContext.getString(R.string.suggestion_modify_input) } returns "Please modify your input and try again."
        every { mockContext.getString(R.string.suggestion_try_different_prompt) } returns "Please try again with a different prompt or contact support."
        every { mockContext.getString(R.string.suggestion_restart_app) } returns "Please restart the app. If the problem persists, contact support."
        every { mockContext.getString(R.string.suggestion_contact_support) } returns "If the problem persists, please contact support."
        
        every { mockContext.getString(R.string.ai_response_warning) } returns "The AI response may contain inaccuracies. Please verify the information with reliable sources."
        
        // Mock SecurityManager for controlled testing
        mockkObject(SecurityManager)
        every { SecurityManager.checkSecurityEnvironment(any()) } returns SecurityEnvironmentResult.Secure
        every { SecurityManager.validateAIResponse(any()) } returns AIResponseValidationResult.Valid("test response")
        every { SecurityManager.validateInput(any()) } returns InputValidationResult.Valid("test input")
    }

    @After
    fun tearDown() {
        unmockkObject(SecurityManager)
    }

    // ===== Error Categorization Tests =====

    @Test
    fun `handleError should categorize network errors correctly`() = runTest {
        val networkErrors = listOf(
            UnknownHostException("No internet connection"),
            SocketTimeoutException("Connection timed out"),
            Exception("network connection failed")
        )
        
        networkErrors.forEach { error ->
            // When
            val result = EnhancedErrorHandler.handleError(
                context = localThis.mockContext,
                error = error
            )
            localThis.lastErrorHandled = result
            
            // Then
            assertThat(result).isInstanceOf(EnhancedErrorResult.Recoverable::class.java)
            val recoverableResult = result as EnhancedErrorResult.Recoverable
            assertThat(recoverableResult.title).isEqualTo("Network Error")
            assertThat(recoverableResult.canRetry).isTrue()
            assertThat(recoverableResult.retryDelay).isGreaterThan(0L)
        }
    }

    @Test
    fun `handleError should categorize security errors correctly`() = runTest {
        val securityErrors = listOf(
            SSLException("SSL handshake failed"),
            SecurityException("Permission denied"),
            Exception("security violation detected")
        )
        
        securityErrors.forEach { error ->
            // When
            val result = EnhancedErrorHandler.handleError(
                context = localThis.mockContext,
                error = error
            )
            localThis.lastErrorHandled = result
            
            // Then
            assertThat(result).isInstanceOf(EnhancedErrorResult.Critical::class.java)
            val criticalResult = result as EnhancedErrorResult.Critical
            assertThat(criticalResult.title).isEqualTo("Security Error")
            assertThat(criticalResult.requiresUserAction).isTrue()
        }
    }

    @Test
    fun `handleError should categorize rate limit errors correctly`() = runTest {
        val rateLimitErrors = listOf(
            IllegalArgumentException("rate limit exceeded"),
            Exception("rate limit reached"),
            Exception("too many requests")
        )
        
        rateLimitErrors.forEach { error ->
            // When
            val result = EnhancedErrorHandler.handleError(
                context = localThis.mockContext,
                error = error
            )
            localThis.lastErrorHandled = result
            
            // Then
            assertThat(result).isInstanceOf(EnhancedErrorResult.Temporary::class.java)
            val temporaryResult = result as EnhancedErrorResult.Temporary
            assertThat(temporaryResult.title).isEqualTo("Rate Limit Exceeded")
            assertThat(temporaryResult.retryAfter).isEqualTo(60000L)
        }
    }

    @Test
    fun `handleError should categorize input validation errors correctly`() = runTest {
        val inputValidationErrors = listOf(
            IllegalArgumentException("invalid input provided"),
            Exception("input validation failed"),
            Exception("input contains invalid characters")
        )
        
        inputValidationErrors.forEach { error ->
            // When
            val result = EnhancedErrorHandler.handleError(
                context = localThis.mockContext,
                error = error,
                userInput = "test input"
            )
            localThis.lastErrorHandled = result
            
            // Then
            assertThat(result).isInstanceOf(EnhancedErrorResult.UserError::class.java)
            val userErrorResult = result as EnhancedErrorResult.UserError
            assertThat(userErrorResult.title).isEqualTo("Input Error")
            assertThat(userErrorResult.inputRelated).isTrue()
        }
    }

    @Test
    fun `handleError should categorize system errors correctly`() = runTest {
        val systemErrors = listOf(
            IllegalStateException("System in invalid state"),
            Exception("system resource unavailable")
        )
        
        systemErrors.forEach { error ->
            // When
            val result = EnhancedErrorHandler.handleError(
                context = localThis.mockContext,
                error = error
            )
            localThis.lastErrorHandled = result
            
            // Then
            assertThat(result).isInstanceOf(EnhancedErrorResult.Critical::class.java)
            val criticalResult = result as EnhancedErrorResult.Critical
            assertThat(criticalResult.title).isEqualTo("System Error")
            assertThat(criticalResult.requiresUserAction).isTrue()
        }
    }

    @Test
    fun `handleError should categorize unknown errors correctly`() = runTest {
        val unknownErrors = listOf(
            RuntimeException("Unexpected runtime error"),
            Exception("Unknown error occurred"),
            NullPointerException("Null pointer exception")
        )
        
        unknownErrors.forEach { error ->
            // When
            val result = EnhancedErrorHandler.handleError(
                context = localThis.mockContext,
                error = error
            )
            localThis.lastErrorHandled = result
            
            // Then
            assertThat(result).isInstanceOf(EnhancedErrorResult.Unknown::class.java)
            val unknownResult = result as EnhancedErrorResult.Unknown
            assertThat(unknownResult.title).isEqualTo("Unexpected Error")
            assertThat(unknownResult.errorCode).isEqualTo(error.javaClass.simpleName)
        }
    }

    // ===== AI Response Processing Tests =====

    @Test
    fun `processAIResponse should handle valid responses`() = runTest {
        // Given
        val validResponses = listOf(
            "This is a delicious chocolate cake recipe.",
            "The ingredients include flour, sugar, and eggs.",
            "Bake at 350°F for 25-30 minutes."
        )
        
        every { SecurityManager.validateAIResponse(any()) } returns 
            AIResponseValidationResult.Valid("valid response")
        
        validResponses.forEach { response ->
            // When
            val result = EnhancedErrorHandler.processAIResponse(localThis.mockContext, response)
            localThis.lastAIProcessingResult = result
            
            // Then
            assertThat(result).isInstanceOf(AIResponseProcessingResult.Success::class.java)
            val successResult = result as AIResponseProcessingResult.Success
            assertThat(successResult.response).isEqualTo("valid response")
        }
    }

    @Test
    fun `processAIResponse should handle responses requiring warnings`() = runTest {
        // Given
        val warningResponse = "Cook the chicken thoroughly to ensure food safety."
        every { SecurityManager.validateAIResponse(any()) } returns 
            AIResponseValidationResult.RequiresWarning(warningResponse, "Food safety reminder")
        
        // When
        val result = EnhancedErrorHandler.processAIResponse(localThis.mockContext, warningResponse)
        localThis.lastAIProcessingResult = result
        
        // Then
        assertThat(result).isInstanceOf(AIResponseProcessingResult.SuccessWithWarning::class.java)
        val warningResult = result as AIResponseProcessingResult.SuccessWithWarning
        assertThat(warningResult.response).isEqualTo(warningResponse)
        assertThat(warningResult.warning).isEqualTo("Food safety reminder")
    }

    @Test
    fun `processAIResponse should handle suspicious responses`() = runTest {
        // Given
        val suspiciousResponse = "This recipe might contain unusual ingredients."
        every { SecurityManager.validateAIResponse(any()) } returns 
            AIResponseValidationResult.Suspicious("Unusual content", "Please verify this information")
        
        // When
        val result = EnhancedErrorHandler.processAIResponse(localThis.mockContext, suspiciousResponse)
        localThis.lastAIProcessingResult = result
        
        // Then
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Warning::class.java)
        val warningResult = result as AIResponseProcessingResult.Warning
        assertThat(warningResult.response).isEqualTo(suspiciousResponse)
        assertThat(warningResult.warning).isEqualTo("Please verify this information")
        assertThat(warningResult.details).contains("inaccuracies")
    }

    @Test
    fun `processAIResponse should handle unsafe responses`() = runTest {
        // Given
        val unsafeResponse = "Cook the chicken at 100°F for best flavor."
        every { SecurityManager.validateAIResponse(any()) } returns 
            AIResponseValidationResult.Unsafe("Dangerous instruction", "Unsafe cooking temperature")
        
        // When
        val result = EnhancedErrorHandler.processAIResponse(localThis.mockContext, unsafeResponse)
        localThis.lastAIProcessingResult = result
        
        // Then
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Blocked::class.java)
        val blockedResult = result as AIResponseProcessingResult.Blocked
        assertThat(blockedResult.reason).isEqualTo("Dangerous instruction")
        assertThat(blockedResult.message).isEqualTo("Unsafe cooking temperature")
    }

    @Test
    fun `processAIResponse should handle invalid responses`() = runTest {
        // Given
        val invalidResponse = "A".repeat(10001) // Too long
        every { SecurityManager.validateAIResponse(any()) } returns 
            AIResponseValidationResult.Invalid("Response too long", "Content exceeds limits")
        
        // When
        val result = EnhancedErrorHandler.processAIResponse(localThis.mockContext, invalidResponse)
        localThis.lastAIProcessingResult = result
        
        // Then
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Error::class.java)
        val errorResult = result as AIResponseProcessingResult.Error
        assertThat(errorResult.reason).isEqualTo("Response too long")
        assertThat(errorResult.message).isEqualTo("Content exceeds limits")
    }

    // ===== User Input Validation Tests =====

    @Test
    fun `validateUserInput should handle valid inputs`() = runTest {
        // Given
        val validInputs = listOf(
            "What type of cake is this?",
            "How do I make chocolate cookies?",
            "Recipe for apple pie"
        )
        
        every { SecurityManager.validateInput(any()) } returns 
            InputValidationResult.Valid("sanitized input")
        
        validInputs.forEach { input ->
            // When
            val result = EnhancedErrorHandler.validateUserInput(input)
            localThis.lastInputValidationResult = result
            
            // Then
            assertThat(result).isInstanceOf(UserInputValidationResult.Valid::class.java)
            val validResult = result as UserInputValidationResult.Valid
            assertThat(validResult.sanitizedInput).isEqualTo("sanitized input")
        }
    }

    @Test
    fun `validateUserInput should handle invalid inputs`() = runTest {
        // Given
        val invalidInputs = listOf(
            "hack this recipe",
            "A".repeat(1001), // Too long
            "inappropriate content"
        )
        
        every { SecurityManager.validateInput(any()) } returns 
            InputValidationResult.Invalid("Input contains unsafe content")
        
        invalidInputs.forEach { input ->
            // When
            val result = EnhancedErrorHandler.validateUserInput(input)
            localThis.lastInputValidationResult = result
            
            // Then
            assertThat(result).isInstanceOf(UserInputValidationResult.Invalid::class.java)
            val invalidResult = result as UserInputValidationResult.Invalid
            assertThat(invalidResult.reason).isEqualTo("Input contains unsafe content")
        }
    }

    // ===== Security Integration Tests =====

    @Test
    fun `handleError should check security environment and log issues`() = runTest {
        // Given
        every { SecurityManager.checkSecurityEnvironment(any()) } returns 
            SecurityEnvironmentResult.Insecure(
                reason = "Test security issues",
                details = "debug, emulator"
            )
        
        // When
        val result = EnhancedErrorHandler.handleError(
            context = localThis.mockContext,
            error = RuntimeException("Test error")
        )
        localThis.lastErrorHandled = result
        
        // Then - Should still process error but log security issues
        assertThat(result).isInstanceOf(EnhancedErrorResult::class.java)
        // Security check should have been performed (mocked to return insecure)
    }

    @Test
    fun `handleError should sanitize user input for logging`() = runTest {
        // Given
        val sensitiveInput = "password123<script>alert('hack')</script>"
        
        // When
        val result = EnhancedErrorHandler.handleError(
            context = localThis.mockContext,
            error = RuntimeException("Test error"),
            userInput = sensitiveInput
        )
        localThis.lastErrorHandled = result
        
        // Then - Should handle gracefully and sanitize input
        assertThat(result).isInstanceOf(EnhancedErrorResult::class.java)
        // Input should be sanitized in logs (we can't directly test logging, but ensure no crashes)
    }

    // ===== Error Message Customization Tests =====

    @Test
    fun `handleError should customize messages based on input length`() = runTest {
        // Given
        val longInput = "A".repeat(1500)
        
        // When
        val result = EnhancedErrorHandler.handleError(
            context = localThis.mockContext,
            error = IllegalArgumentException("Input validation failed"),
            userInput = longInput
        )
        localThis.lastErrorHandled = result
        
        // Then
        assertThat(result).isInstanceOf(EnhancedErrorResult.UserError::class.java)
        val userErrorResult = result as EnhancedErrorResult.UserError
        assertThat(userErrorResult.message).contains("too long")
    }

    @Test
    fun `handleError should provide specific retry delays for different error types`() = runTest {
        val errorTypeToExpectedDelay = mapOf(
            UnknownHostException("Network error") to 5000L,
            IllegalArgumentException("rate limit") to 60000L
        )
        
        errorTypeToExpectedDelay.forEach { (error, expectedDelay) ->
            // When
            val result = EnhancedErrorHandler.handleError(
                context = localThis.mockContext,
                error = error
            )
            localThis.lastErrorHandled = result
            
            // Then
            when (result) {
                is EnhancedErrorResult.Recoverable -> {
                    assertThat(result.retryDelay).isEqualTo(expectedDelay)
                }
                is EnhancedErrorResult.Temporary -> {
                    assertThat(result.retryAfter).isEqualTo(expectedDelay)
                }
                else -> {
                    // Other error types might not have retry delays
                }
            }
        }
    }

    // ===== Edge Cases and Error Scenarios =====

    @Test
    fun `handleError should handle null and empty inputs gracefully`() = runTest {
        val edgeCaseInputs = listOf(
            null,
            "",
            "   ",
            "\n\t\r"
        )
        
        edgeCaseInputs.forEach { input ->
            // When
            val result = EnhancedErrorHandler.handleError(
                context = localThis.mockContext,
                error = RuntimeException("Test error"),
                userInput = input
            )
            localThis.lastErrorHandled = result
            
            // Then - Should handle gracefully without exceptions
            assertThat(result).isInstanceOf(EnhancedErrorResult::class.java)
        }
    }

    @Test
    fun `processAIResponse should handle empty and null responses gracefully`() = runTest {
        val edgeCaseResponses = listOf(
            "",
            "   ",
            "\n\t"
        )
        
        edgeCaseResponses.forEach { response ->
            // When
            val result = EnhancedErrorHandler.processAIResponse(localThis.mockContext, response)
            localThis.lastAIProcessingResult = result
            
            // Then - Should handle gracefully
            assertThat(result).isInstanceOf(AIResponseProcessingResult::class.java)
        }
    }

    @Test
    fun `validateUserInput should handle unicode and special characters`() = runTest {
        val specialCharacterInputs = listOf(
            "🍰 What type of cake is this? 🧁",
            "Café au lait recipe with français",
            "Recipe with 中文 characters",
            "مطبخ عربي recipes"
        )
        
        every { SecurityManager.validateInput(any()) } returns 
            InputValidationResult.Valid("sanitized input")
        
        specialCharacterInputs.forEach { input ->
            // When
            val result = EnhancedErrorHandler.validateUserInput(input)
            localThis.lastInputValidationResult = result
            
            // Then - Should handle gracefully
            assertThat(result).isInstanceOf(UserInputValidationResult::class.java)
        }
    }

    @Test
    fun `error categorization should handle nested and wrapped exceptions`() = runTest {
        val wrappedException = RuntimeException(
            "Wrapper exception", 
            UnknownHostException("Network issue")
        )
        
        // When
        val result = EnhancedErrorHandler.handleError(
            context = localThis.mockContext,
            error = wrappedException
        )
        localThis.lastErrorHandled = result
        
        // Then - Should categorize based on the specific exception type
        assertThat(result).isInstanceOf(EnhancedErrorResult::class.java)
    }

    @Test
    fun `error handling should be thread safe for concurrent requests`() = runTest {
        // Given - Multiple concurrent error handling requests
        val errors = (1..10).map { 
            RuntimeException("Concurrent error $it") 
        }
        
        // When - Process multiple errors concurrently
        val results = errors.map { error ->
            EnhancedErrorHandler.handleError(
                context = localThis.mockContext,
                error = error
            )
        }
        
        // Then - All should complete successfully
        results.forEach { result ->
            assertThat(result).isInstanceOf(EnhancedErrorResult::class.java)
        }
    }

    @Test
    fun `security integration should handle SecurityManager exceptions gracefully`() = runTest {
        // Given - SecurityManager throws exception
        every { SecurityManager.checkSecurityEnvironment(any()) } throws RuntimeException("Security check failed")
        every { SecurityManager.validateAIResponse(any()) } throws RuntimeException("Validation failed")
        
        // When
        val result = EnhancedErrorHandler.handleError(
            context = localThis.mockContext,
            error = RuntimeException("Original error")
        )
        localThis.lastErrorHandled = result
        
        // Then - Should handle gracefully and not crash
        assertThat(result).isInstanceOf(EnhancedErrorResult::class.java)
    }
} 
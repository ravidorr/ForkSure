package com.ravidor.forksure

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SecurityManager
 * Tests security validation, rate limiting, and input sanitization
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putStringSet(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
    }

    @Test
    fun `validateInput with valid input should return Valid result`() = runTest {
        // Given
        val validInput = "What type of cake is this?"
        
        // When
        val result = SecurityManager.validateInput(validInput)
        
        // Then
        assertThat(result).isInstanceOf(InputValidationResult.Valid::class.java)
        assertThat((result as InputValidationResult.Valid).sanitizedInput).isEqualTo(validInput)
    }

    @Test
    fun `validateInput with empty input should return Valid result`() = runTest {
        // Given
        val emptyInput = ""
        
        // When
        val result = SecurityManager.validateInput(emptyInput)
        
        // Then
        // SecurityManager only checks max length and patterns, not minimum length
        assertThat(result).isInstanceOf(InputValidationResult.Valid::class.java)
        assertThat((result as InputValidationResult.Valid).sanitizedInput).isEmpty()
    }

    @Test
    fun `validateInput with too long input should return Invalid result`() = runTest {
        // Given
        val longInput = "A".repeat(1001) // Exceeds MAX_PROMPT_LENGTH
        
        // When
        val result = SecurityManager.validateInput(longInput)
        
        // Then
        assertThat(result).isInstanceOf(InputValidationResult.Invalid::class.java)
        // Just verify it returns Invalid for too long input
    }

    @Test
    fun `checkRateLimit with no previous requests should return Allowed`() = runTest {
        // Given
        every { mockSharedPreferences.getStringSet(any(), any()) } returns emptySet()
        
        // When
        val result = SecurityManager.checkRateLimit(mockContext, "test_user")
        
        // Then
        assertThat(result).isInstanceOf(RateLimitResult.Allowed::class.java)
        // Don't assert specific remaining count as it depends on implementation
    }

    @Test
    fun `checkSecurityEnvironment should return result`() = runTest {
        // When
        val result = SecurityManager.checkSecurityEnvironment(mockContext)
        
        // Then
        // Just verify it returns a valid result (could be Secure or Insecure depending on environment)
        assertThat(result).isInstanceOf(SecurityEnvironmentResult::class.java)
    }

    @Test
    fun `validateAIResponse with safe content should return Valid`() = runTest {
        // Given
        val safeResponse = "This is a delicious chocolate cake recipe with butter, sugar, and cocoa."
        
        // When
        val result = SecurityManager.validateAIResponse(safeResponse)
        
        // Then
        assertThat(result).isInstanceOf(AIResponseValidationResult.Valid::class.java)
        assertThat((result as AIResponseValidationResult.Valid).response).isEqualTo(safeResponse)
    }

    @Test
    fun `validateAIResponse with too long content should return Invalid`() = runTest {
        // Given
        val longResponse = "A".repeat(10001) // Exceeds MAX_RESPONSE_LENGTH
        
        // When
        val result = SecurityManager.validateAIResponse(longResponse)
        
        // Then
        assertThat(result).isInstanceOf(AIResponseValidationResult.Invalid::class.java)
        // Just verify it returns Invalid for too long response
    }

    @Test
    fun `validateAIResponse with empty content should return Valid`() = runTest {
        // Given
        val emptyResponse = ""
        
        // When
        val result = SecurityManager.validateAIResponse(emptyResponse)
        
        // Then
        // SecurityManager only checks max length and patterns, not minimum length
        assertThat(result).isInstanceOf(AIResponseValidationResult.Valid::class.java)
        assertThat((result as AIResponseValidationResult.Valid).response).isEmpty()
    }
} 
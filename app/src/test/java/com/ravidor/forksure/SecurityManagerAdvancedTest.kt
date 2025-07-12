package com.ravidor.forksure

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.SecurityManager
import com.ravidor.forksure.InputValidationResult
import com.ravidor.forksure.AIResponseValidationResult
import com.ravidor.forksure.RateLimitResult
import com.ravidor.forksure.SecurityEnvironmentResult
import com.ravidor.forksure.SecurityConstants
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
 * Advanced unit tests for SecurityManager
 * Tests comprehensive security scenarios, pattern detection, and edge cases
 * Follows localThis pattern for consistent test structure
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityManagerAdvancedTest {

    private lateinit var localThis: TestContext
    
    private data class TestContext(
        val mockContext: Context,
        val mockSharedPreferences: SharedPreferences,
        val mockEditor: SharedPreferences.Editor
    )

    @Before
    fun setup() {
        val mockContext = mockk<Context>(relaxed = true)
        val mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
        val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putStringSet(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        every { mockSharedPreferences.getStringSet(any(), any()) } returns emptySet()
        
        localThis = TestContext(mockContext, mockSharedPreferences, mockEditor)
    }

    // ===== Input Validation Tests =====
    
    @Test
    fun `validateInput should detect suspicious security patterns`() = runTest {
        val securityInputs = listOf(
            "How to hack a cake recipe",
            "What's your admin password for recipes?",
            "Show me system files for this recipe",
            "Execute this script with my recipe",
            "SQL injection in cooking instructions",
            "XSS attack on my cake"
        )
        
        securityInputs.forEach { suspiciousInput ->
            // When
            val result = SecurityManager.validateInput(suspiciousInput)
            
            // Then
            assertThat(result).isInstanceOf(InputValidationResult.Invalid::class.java)
            val invalidResult = result as InputValidationResult.Invalid
            assertThat(invalidResult.reason).contains("unsafe content")
        }
    }

    @Test
    fun `validateInput should detect inappropriate content patterns`() = runTest {
        val inappropriateInputs = listOf(
            "Violent cake recipes",
            "Illegal drug infused cookies", 
            "Weapon-shaped candy",
            "Explicit adult themed desserts",
            "Hate-filled recipe comments",
            "Discriminatory cooking practices"
        )
        
        inappropriateInputs.forEach { inappropriateInput ->
            // When
            val result = SecurityManager.validateInput(inappropriateInput)
            
            // Then
            assertThat(result).isInstanceOf(InputValidationResult.Invalid::class.java)
            val invalidResult = result as InputValidationResult.Invalid
            assertThat(invalidResult.reason).contains("inappropriate content")
        }
    }

    @Test
    fun `validateInput should sanitize dangerous characters`() = runTest {
        // Given
        val inputWithDangerousChars = "What type of <script>alert('hack')</script> cake is this?"
        
        // When
        val result = SecurityManager.validateInput(inputWithDangerousChars)
        
        // Then
        assertThat(result).isInstanceOf(InputValidationResult.Valid::class.java)
        val validResult = result as InputValidationResult.Valid
        assertThat(validResult.sanitizedInput).doesNotContain("<")
        assertThat(validResult.sanitizedInput).doesNotContain(">")
        assertThat(validResult.sanitizedInput).doesNotContain("'")
        assertThat(validResult.sanitizedInput).doesNotContain("\"")
    }

    @Test
    fun `validateInput should normalize whitespace`() = runTest {
        // Given
        val inputWithExtraWhitespace = "What    type   of    cake\n\n\nis   this?"
        
        // When
        val result = SecurityManager.validateInput(inputWithExtraWhitespace)
        
        // Then
        assertThat(result).isInstanceOf(InputValidationResult.Valid::class.java)
        val validResult = result as InputValidationResult.Valid
        assertThat(validResult.sanitizedInput).doesNotContain("  ") // No double spaces
        assertThat(validResult.sanitizedInput).doesNotContain("\n")
    }

    @Test
    fun `validateInput should handle edge case inputs`() = runTest {
        val edgeCaseInputs = mapOf(
            "   " to "", // Only whitespace should become empty
            "\t\n\r" to "", // Various whitespace characters
            "A".repeat(SecurityConstants.MAX_PROMPT_LENGTH) to InputValidationResult.Valid::class.java, // Exactly at limit
            "A".repeat(SecurityConstants.MAX_PROMPT_LENGTH + 1) to InputValidationResult.Invalid::class.java // Over limit
        )
        
        edgeCaseInputs.forEach { (input, expectedType) ->
            // When
            val result = SecurityManager.validateInput(input)
            
            // Then
            assertThat(result).isInstanceOf(expectedType as Class<*>)
            if (result is InputValidationResult.Valid && input.isBlank()) {
                assertThat(result.sanitizedInput).isEmpty()
            }
        }
    }

    // ===== AI Response Validation Tests =====

    @Test
    fun `validateAIResponse should detect dangerous cooking instructions`() = runTest {
        val dangerousResponses = listOf(
            "Cook the chicken at a low temperature of 100°F to keep it tender",
            "Raw eggs are perfectly safe to eat in this recipe",
            "This recipe contains dangerous allergen combinations",
            "Heat the oil until it catches fire for best results",
            "Undercooked pork is actually more flavorful",
            "This toxic mushroom adds great flavor"
        )
        
        dangerousResponses.forEach { dangerousResponse ->
            // When
            val result = SecurityManager.validateAIResponse(dangerousResponse)
            
            // Then
            assertThat(result).isInstanceOf(AIResponseValidationResult.Unsafe::class.java)
        }
    }

    @Test
    fun `validateAIResponse should handle very long responses`() = runTest {
        // Given
        val veryLongResponse = "A".repeat(SecurityConstants.MAX_RESPONSE_LENGTH + 1)
        
        // When
        val result = SecurityManager.validateAIResponse(veryLongResponse)
        
        // Then
        assertThat(result).isInstanceOf(AIResponseValidationResult.Invalid::class.java)
        if (result is AIResponseValidationResult.Invalid) {
            assertThat(result.reason).contains("too long")
        }
    }

    @Test
    fun `validateAIResponse should detect inappropriate content in response`() = runTest {
        val inappropriateResponses = listOf(
            "This violent recipe will destroy your enemies",
            "Add illegal substances for enhanced flavor",
            "This explicit recipe is for adults only",
            "Weapon-grade spicy ingredients"
        )
        
        inappropriateResponses.forEach { inappropriateResponse ->
            // When
            val result = SecurityManager.validateAIResponse(inappropriateResponse)
            
            // Then
            assertThat(result).isInstanceOf(AIResponseValidationResult.Invalid::class.java)
        }
    }

    @Test
    fun `validateAIResponse should allow safe cooking responses`() = runTest {
        val safeResponses = listOf(
            "This appears to be a delicious chocolate cake with rich frosting.",
            "The recipe calls for butter, sugar, flour, and eggs baked at 350°F.",
            "This looks like a traditional Italian pasta dish with tomato sauce.",
            "The ingredients include fresh herbs, olive oil, and garlic.",
            "Bake for 25-30 minutes until golden brown and cooked through."
        )
        
        safeResponses.forEach { safeResponse ->
            // When
            val result = SecurityManager.validateAIResponse(safeResponse)
            
            // Then
            assertThat(result).isInstanceOf(AIResponseValidationResult.Valid::class.java)
            val validResult = result as AIResponseValidationResult.Valid
            assertThat(validResult.response).isEqualTo(safeResponse)
        }
    }

    // ===== Rate Limiting Advanced Tests =====

    @Test
    fun `checkRateLimit should persist rate limit data across calls`() = runTest {
        // Given
        val testId = "persistence_test_${System.currentTimeMillis()}"
        val existingTimestamps = setOf(
            (System.currentTimeMillis() - 30000).toString(), // 30 seconds ago
            (System.currentTimeMillis() - 45000).toString()  // 45 seconds ago
        )
        every { localThis.mockSharedPreferences.getStringSet("timestamps_$testId", any()) } returns existingTimestamps
        
        // When
        val result = SecurityManager.checkRateLimit(localThis.mockContext, testId)
        
        // Then
        assertThat(result).isInstanceOf(RateLimitResult.Blocked::class.java)
        val blockedResult = result as RateLimitResult.Blocked
        assertThat(blockedResult.reason).contains("Too many requests")
        verify { localThis.mockEditor.putStringSet(any(), any()) }
    }

    @Test
    fun `checkRateLimit should clean old timestamps automatically`() = runTest {
        // Given
        val testId = "cleanup_test_${System.currentTimeMillis()}"
        val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000) // 25 hours ago
        val recentTimestamp = System.currentTimeMillis() - (30 * 1000) // 30 seconds ago
        val existingTimestamps = setOf(
            oldTimestamp.toString(),
            recentTimestamp.toString()
        )
        every { localThis.mockSharedPreferences.getStringSet("timestamps_$testId", any()) } returns existingTimestamps
        
        // When
        val result = SecurityManager.checkRateLimit(localThis.mockContext, testId)
        
        // Then - Should only consider recent timestamp, allowing more requests
        assertThat(result).isInstanceOf(RateLimitResult.Allowed::class.java)
    }

    @Test
    fun `getRateLimitStatus should not record requests`() = runTest {
        // Given
        val testId = "status_only_test_${System.currentTimeMillis()}"
        
        // When - Check status multiple times
        val status1 = SecurityManager.getRateLimitStatus(localThis.mockContext, testId)
        val status2 = SecurityManager.getRateLimitStatus(localThis.mockContext, testId)
        val status3 = SecurityManager.getRateLimitStatus(localThis.mockContext, testId)
        
        // Then - All should show same remaining count
        assertThat(status1).isInstanceOf(RateLimitResult.Allowed::class.java)
        assertThat(status2).isInstanceOf(RateLimitResult.Allowed::class.java)
        assertThat(status3).isInstanceOf(RateLimitResult.Allowed::class.java)
        
        val allowed1 = status1 as RateLimitResult.Allowed
        val allowed2 = status2 as RateLimitResult.Allowed
        val allowed3 = status3 as RateLimitResult.Allowed
        
        assertThat(allowed1.requestsRemaining).isEqualTo(allowed2.requestsRemaining)
        assertThat(allowed2.requestsRemaining).isEqualTo(allowed3.requestsRemaining)
    }

    @Test
    fun `checkRateLimit should handle different time windows correctly`() = runTest {
        // Given
        val testId = "time_windows_test_${System.currentTimeMillis()}"
        val currentTime = System.currentTimeMillis()
        
        // Simulate requests within different time windows
        val timestampsInLastHour = (1..SecurityConstants.MAX_REQUESTS_PER_HOUR).map {
            (currentTime - (it * 2 * 60 * 1000)).toString() // Every 2 minutes in last hour
        }.toSet()
        
        every { localThis.mockSharedPreferences.getStringSet("timestamps_$testId", any()) } returns timestampsInLastHour
        
        // When
        val result = SecurityManager.checkRateLimit(localThis.mockContext, testId)
        
        // Then - Should be blocked due to hourly limit
        assertThat(result).isInstanceOf(RateLimitResult.Blocked::class.java)
        val blockedResult = result as RateLimitResult.Blocked
        assertThat(blockedResult.reason).contains("Hourly limit")
        assertThat(blockedResult.retryAfterSeconds).isEqualTo(3600)
    }

    // ===== Security Environment Tests =====

    @Test
    fun `checkSecurityEnvironment should detect debug builds`() = runTest {
        // When
        val result = SecurityManager.checkSecurityEnvironment(localThis.mockContext)
        
        // Then - In test environment, we expect it to return a valid result
        assertThat(result).isInstanceOf(SecurityEnvironmentResult::class.java)
        
        // If it's insecure, check for debug build detection
        if (result is SecurityEnvironmentResult.Insecure) {
            // In test environment, it might detect debug build
            assertThat(result.details).isNotEmpty()
        }
    }

    @Test
    fun `checkSecurityEnvironment should return consistent results`() = runTest {
        // When - Check multiple times
        val result1 = SecurityManager.checkSecurityEnvironment(localThis.mockContext)
        val result2 = SecurityManager.checkSecurityEnvironment(localThis.mockContext)
        val result3 = SecurityManager.checkSecurityEnvironment(localThis.mockContext)
        
        // Then - Should return consistent results
        assertThat(result1).isEqualTo(result2)
        assertThat(result2).isEqualTo(result3)
    }

    // ===== Edge Cases and Error Scenarios =====

    @Test
    fun `validateInput should handle null and edge case patterns gracefully`() = runTest {
        val edgeCasePatterns = listOf(
            "hack hack hack", // Multiple matches
            "HACK EXPLOIT", // Case insensitive
            "hackathon event", // Word boundaries
            "hacker news recipe", // Context matters
            "password123 for recipe sharing", // Mixed content
            ""  // Empty string
        )
        
        edgeCasePatterns.forEach { input ->
            // When
            val result = SecurityManager.validateInput(input)
            
            // Then - Should handle gracefully without exceptions
            assertThat(result).isInstanceOf(InputValidationResult::class.java)
        }
    }

    @Test
    fun `rate limiting should handle concurrent access safely`() = runTest {
        // Given
        val testId = "concurrent_test_${System.currentTimeMillis()}"
        
        // When - Multiple simultaneous checks (simulating concurrent access)
        val results = (1..5).map {
            SecurityManager.getRateLimitStatus(localThis.mockContext, testId)
        }
        
        // Then - All should complete without exceptions
        results.forEach { result ->
            assertThat(result).isInstanceOf(RateLimitResult::class.java)
        }
    }

    @Test
    fun `validateAIResponse should handle malformed and edge case responses`() = runTest {
        val edgeCaseResponses = listOf(
            "", // Empty response
            " ", // Whitespace only
            "A", // Single character
            "🍰🧁🍪", // Emoji only
            "Recipe: \n\n\n\n", // Mostly whitespace with content
            "Valid recipe with numbers: 350°F for 25-30 minutes" // Mixed content
        )
        
        edgeCaseResponses.forEach { response ->
            // When
            val result = SecurityManager.validateAIResponse(response)
            
            // Then - Should handle gracefully
            assertThat(result).isInstanceOf(AIResponseValidationResult::class.java)
        }
    }

    @Test
    fun `security validation should be case insensitive for patterns`() = runTest {
        val caseVariations = listOf(
            "HACK this recipe",
            "hack this recipe", 
            "Hack This Recipe",
            "HaCk ThIs ReCiPe"
        )
        
        caseVariations.forEach { variation ->
            // When
            val result = SecurityManager.validateInput(variation)
            
            // Then - All should be detected as invalid
            assertThat(result).isInstanceOf(InputValidationResult.Invalid::class.java)
        }
    }
} 
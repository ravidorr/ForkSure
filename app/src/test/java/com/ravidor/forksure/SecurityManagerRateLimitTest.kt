package com.ravidor.forksure

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for SecurityManager rate limiting functionality
 * Specifically testing the difference between checkRateLimit and getRateLimitStatus
 */
class SecurityManagerRateLimitTest {

    private val mockContext = mockk<Context>()
    private val mockPrefs = mockk<SharedPreferences>()
    private val mockEditor = mockk<SharedPreferences.Editor>()

    init {
        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.getStringSet(any(), any()) } returns emptySet()
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putStringSet(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
    }

    @Test
    fun `getRateLimitStatus should not consume requests`() = runTest {
        val testId = "test1_${System.currentTimeMillis()}"
        
        // Check status multiple times - should not consume requests
        val status1 = SecurityManager.getRateLimitStatus(mockContext, testId)
        val status2 = SecurityManager.getRateLimitStatus(mockContext, testId)
        val status3 = SecurityManager.getRateLimitStatus(mockContext, testId)

        // All should show the same number of requests remaining
        assertTrue("First status check should be allowed", status1 is RateLimitResult.Allowed)
        assertTrue("Second status check should be allowed", status2 is RateLimitResult.Allowed)
        assertTrue("Third status check should be allowed", status3 is RateLimitResult.Allowed)

        val allowed1 = status1 as RateLimitResult.Allowed
        val allowed2 = status2 as RateLimitResult.Allowed
        val allowed3 = status3 as RateLimitResult.Allowed

        // All should show the same number of requests remaining
        assertEquals("Status checks should not consume requests", 
            allowed1.requestsRemaining, allowed2.requestsRemaining)
        assertEquals("Status checks should not consume requests", 
            allowed2.requestsRemaining, allowed3.requestsRemaining)
        assertTrue("Should show positive requests available", allowed1.requestsRemaining > 0)
    }

    @Test
    fun `checkRateLimit should consume requests`() = runTest {
        val testId = "test2_${System.currentTimeMillis()}"
        
        // Make actual requests - should consume requests
        val request1 = SecurityManager.checkRateLimit(mockContext, testId)
        val request2 = SecurityManager.checkRateLimit(mockContext, testId)

        assertTrue("First request should be allowed", request1 is RateLimitResult.Allowed)
        assertTrue("Second request should be allowed", request2 is RateLimitResult.Allowed)

        val allowed1 = request1 as RateLimitResult.Allowed
        val allowed2 = request2 as RateLimitResult.Allowed

        // Requests should consume the limit
        assertTrue("First request should show fewer remaining", allowed1.requestsRemaining >= 0)
        assertTrue("Second request should show fewer or equal remaining", allowed2.requestsRemaining <= allowed1.requestsRemaining)
    }

    @Test
    fun `status check vs actual request behavior`() = runTest {
        val testId = "test3_${System.currentTimeMillis()}"
        
        // Status check should not consume
        val status1 = SecurityManager.getRateLimitStatus(mockContext, testId)
        assertTrue("Status check should be allowed", status1 is RateLimitResult.Allowed)
        
        // Another status check should show same result
        val status2 = SecurityManager.getRateLimitStatus(mockContext, testId)
        assertTrue("Second status check should be allowed", status2 is RateLimitResult.Allowed)
        assertEquals("Status checks should show same remaining count", 
            (status1 as RateLimitResult.Allowed).requestsRemaining,
            (status2 as RateLimitResult.Allowed).requestsRemaining)
        
        // Actual request should consume
        val request1 = SecurityManager.checkRateLimit(mockContext, testId)
        assertTrue("Request should be allowed", request1 is RateLimitResult.Allowed)
        
        // Status check after request should show reduced count
        val status3 = SecurityManager.getRateLimitStatus(mockContext, testId)
        assertTrue("Status after request should be allowed", status3 is RateLimitResult.Allowed)
        assertTrue("Status should show fewer requests remaining",
            (status3 as RateLimitResult.Allowed).requestsRemaining < status1.requestsRemaining)
    }
} 
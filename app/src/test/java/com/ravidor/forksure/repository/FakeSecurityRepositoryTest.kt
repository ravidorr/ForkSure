package com.ravidor.forksure.repository

import com.ravidor.forksure.RateLimitResult
import com.ravidor.forksure.SecurityEnvironmentResult
import com.ravidor.forksure.UserInputValidationResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSecurityRepositoryTest {

    private val fake = FakeSecurityRepository()

    @Test
    fun setInsecureEnvironment_reportsIssues() = runTest {
        fake.setInsecureEnvironment(listOf("rooted", "debuggable"))

        val result = fake.checkSecurityEnvironment()

        assertEquals("rooted, debuggable", (result as SecurityEnvironmentResult.Insecure).details)
    }

    @Test
    fun setSecureEnvironment_reportsSecure() = runTest {
        fake.setInsecureEnvironment(listOf("rooted"))
        fake.setSecureEnvironment()

        assertEquals(SecurityEnvironmentResult.Secure, fake.checkSecurityEnvironment())
    }

    @Test
    fun setRateLimited_blocksWithReason() = runTest {
        fake.setRateLimited("slow down")

        val result = fake.checkRateLimit("id")

        assertEquals("slow down", (result as RateLimitResult.Blocked).reason)
    }

    @Test
    fun setRateAllowed_allowsWithRemaining() = runTest {
        fake.setRateLimited()
        fake.setRateAllowed(3)

        val result = fake.checkRateLimit("id")

        assertEquals(3, (result as RateLimitResult.Allowed).requestsRemaining)
    }

    @Test
    fun setInputValid_returnsSanitizedInput() = runTest {
        fake.setInputValid("clean")

        val result = fake.validateUserInput("dirty")

        assertEquals("clean", (result as UserInputValidationResult.Valid).sanitizedInput)
    }

    @Test
    fun setInputInvalid_returnsReason() = runTest {
        fake.setInputInvalid("bad")

        val result = fake.validateUserInput("x")

        assertEquals("bad", (result as UserInputValidationResult.Invalid).reason)
    }

    @Test
    fun reset_restoresDefaults() = runTest {
        fake.setInsecureEnvironment(listOf("rooted"))
        fake.setRateLimited()
        fake.setInputInvalid()

        fake.reset()

        assertEquals(SecurityEnvironmentResult.Secure, fake.checkSecurityEnvironment())
        assertTrue(fake.checkRateLimit("id") is RateLimitResult.Allowed)
        assertTrue(fake.validateUserInput("x") is UserInputValidationResult.Valid)
    }
}

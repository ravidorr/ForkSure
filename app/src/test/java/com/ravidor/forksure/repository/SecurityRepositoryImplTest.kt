package com.ravidor.forksure.repository

import android.content.Context
import com.ravidor.forksure.InputValidationResult
import com.ravidor.forksure.RateLimitResult
import com.ravidor.forksure.SecurityEnvironmentResult
import com.ravidor.forksure.SecurityManager
import com.ravidor.forksure.UserInputValidationResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityRepositoryImplTest {

    private fun repo(): SecurityRepositoryImpl {
        val ctx = mockk<Context>(relaxed = true)
        return SecurityRepositoryImpl(ctx)
    }

    @Test
    fun validateUserInput_mapsValid() = runTest {
        mockkObject(SecurityManager)
        every { SecurityManager.validateInput("hello") } returns InputValidationResult.Valid("hello")

        val result = repo().validateUserInput("hello")
        assertEquals(UserInputValidationResult.Valid("hello"), result)
    }

    @Test
    fun validateUserInput_mapsInvalid() = runTest {
        mockkObject(SecurityManager)
        every { SecurityManager.validateInput("bad") } returns InputValidationResult.Invalid("nope")

        val result = repo().validateUserInput("bad")
        assertEquals(UserInputValidationResult.Invalid("nope"), result)
    }

    @Test
    fun checkRateLimit_delegates() = runTest {
        mockkObject(SecurityManager)
        val ctx = mockk<Context>(relaxed = true)
coEvery { SecurityManager.checkRateLimit(ctx, any()) } returns RateLimitResult.Allowed(5, 60)
        val repo = SecurityRepositoryImpl(ctx)

        val res = repo.checkRateLimit("id")
        assertEquals(RateLimitResult.Allowed(5, 60), res)
    }

    @Test
    fun checkSecurityEnvironment_delegates() = runTest {
        mockkObject(SecurityManager)
        val ctx = mockk<Context>(relaxed = true)
        coEvery { SecurityManager.checkSecurityEnvironment() } returns SecurityEnvironmentResult.Secure
        val repo = SecurityRepositoryImpl(ctx)

        val res = repo.checkSecurityEnvironment()
        assertEquals(SecurityEnvironmentResult.Secure, res)
    }
}

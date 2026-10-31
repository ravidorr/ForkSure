package com.ravidor.forksure.repository

import android.graphics.Bitmap
import com.ravidor.forksure.AIResponseProcessingResult
import io.mockk.mockk
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAIRepositoryTest {

    private val bitmap = mockk<Bitmap>(relaxed = true)
    private val fake = FakeAIRepository()

    @Test
    fun setSuccessResponse_returnsSuccessWithResponse() = runTest {
        fake.shouldReturnError = true
        fake.setSuccessResponse("tasty")

        val result = fake.generateContent(bitmap, "prompt")

        assertEquals(AIResponseProcessingResult.Success("tasty"), result)
    }

    @Test
    fun setErrorResponse_returnsErrorWithMessage() = runTest {
        fake.setErrorResponse("boom")

        val result = fake.generateContent(bitmap, "prompt")

        assertTrue(result is AIResponseProcessingResult.Error)
        assertEquals("boom", (result as AIResponseProcessingResult.Error).message)
    }

    @Test
    fun setBlockedResponse_returnsBlocked() = runTest {
        fake.setBlockedResponse()

        assertTrue(fake.generateContent(bitmap, "prompt") is AIResponseProcessingResult.Blocked)
    }

    @Test
    fun setWarningResponse_returnsWarningWithResponse() = runTest {
        fake.setWarningResponse("careful")

        val result = fake.generateContent(bitmap, "prompt")

        assertTrue(result is AIResponseProcessingResult.Warning)
        assertEquals("careful", (result as AIResponseProcessingResult.Warning).response)
    }

    @Test
    fun setDelay_delaysGeneration() = runTest {
        fake.setDelay(500L)

        fake.generateContent(bitmap, "prompt")

        assertEquals(500L, currentTime)
    }
}

package com.ravidor.forksure.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.AIResponseProcessingResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AIRepositoryImpl
 * Tests AI content generation and response processing
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AIRepositoryImplTest {

    private lateinit var mockGenerativeModel: GenerativeModel
    private lateinit var repository: AIRepositoryImpl
    private val mockBitmap = mockk<Bitmap>(relaxed = true)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        mockGenerativeModel = mockk(relaxed = true)
        repository = AIRepositoryImpl(context, mockGenerativeModel)
    }

    @Test
    fun `generateContent with successful response should return success`() = runTest {
        // Given
        val responseText = "This is a delicious chocolate cake recipe..."
        val mockResponse = mockk<GenerateContentResponse> {
            coEvery { text } returns responseText
        }
        coEvery { mockGenerativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } returns mockResponse

        // When
        val result = repository.generateContent(mockBitmap, "What is this?")

        // Then
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Success::class.java)
        assertThat((result as AIResponseProcessingResult.Success).response).isEqualTo(responseText)
    }

    @Test
    fun `generateContent with null response should return error`() = runTest {
        // Given
        val mockResponse = mockk<GenerateContentResponse> {
            coEvery { text } returns null
        }
        coEvery { mockGenerativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } returns mockResponse

        // When
        val result = repository.generateContent(mockBitmap, "What is this?")

        // Then
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Error::class.java)
        val errorResult = result as AIResponseProcessingResult.Error
        assertThat(errorResult.reason).isEqualTo("No response received")
        assertThat(errorResult.message).contains("No response received from AI service")
    }

    @Test
    fun `generateContent with exception should return error`() = runTest {
        // Given
        val exceptionMessage = "Network error"
        coEvery { mockGenerativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } throws RuntimeException(exceptionMessage)

        // When
        val result = repository.generateContent(mockBitmap, "What is this?")

        // Then
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Error::class.java)
        val errorResult = result as AIResponseProcessingResult.Error
        assertThat(errorResult.reason).isEqualTo("AI request failed")
        assertThat(errorResult.message).isEqualTo(exceptionMessage)
    }

    @Test
    fun `generateContent with empty prompt should still work`() = runTest {
        // Given
        val responseText = "I can see baked goods in the image"
        val mockResponse = mockk<GenerateContentResponse> {
            coEvery { text } returns responseText
        }
        coEvery { mockGenerativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } returns mockResponse

        // When
        val result = repository.generateContent(mockBitmap, "")

        // Then
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Success::class.java)
        assertThat((result as AIResponseProcessingResult.Success).response).isEqualTo(responseText)
    }

    @Test
    fun `generateContent with long prompt should work`() = runTest {
        // Given
        val longPrompt = "A".repeat(1000)
        val responseText = "Recipe analysis complete"
        val mockResponse = mockk<GenerateContentResponse> {
            coEvery { text } returns responseText
        }
        coEvery { mockGenerativeModel.generateContent(any<com.google.ai.client.generativeai.type.Content>()) } returns mockResponse

        // When
        val result = repository.generateContent(mockBitmap, longPrompt)

        // Then
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Success::class.java)
        assertThat((result as AIResponseProcessingResult.Success).response).isEqualTo(responseText)
    }
} 
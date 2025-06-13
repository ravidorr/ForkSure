package com.ravidor.forksure

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.repository.FakeAIRepository
import com.ravidor.forksure.repository.FakeSecurityRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Comprehensive unit tests for BakingViewModel
 * Tests all functionality including success, error, and edge cases
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class BakingViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var fakeAIRepository: FakeAIRepository

    @Inject
    lateinit var fakeSecurityRepository: FakeSecurityRepository

    private lateinit var viewModel: BakingViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val mockBitmap = mockk<Bitmap>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        hiltRule.inject()
        
        // Reset fake repositories to default state
        fakeAIRepository.reset()
        fakeSecurityRepository.reset()
        
        viewModel = BakingViewModel(fakeAIRepository, fakeSecurityRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Initial`() = runTest {
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(UiState.Initial)
        }
    }

    @Test
    fun `sendPrompt with valid input should return success`() = runTest {
        // Given
        val testResponse = "Test recipe response"
        fakeAIRepository.setSuccessResponse(testResponse)
        
        // When
        viewModel.sendPrompt(mockBitmap, "What is this?")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Success::class.java)
            assertThat((state as UiState.Success).outputText).isEqualTo(testResponse)
        }
    }

    @Test
    fun `sendPrompt should show loading state during processing`() = runTest {
        // Given
        fakeAIRepository.setDelay(100L)
        
        // When
        viewModel.sendPrompt(mockBitmap, "What is this?")
        
        // Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(UiState.Initial)
            assertThat(awaitItem()).isEqualTo(UiState.Loading)
            
            advanceUntilIdle()
            assertThat(awaitItem()).isInstanceOf(UiState.Success::class.java)
        }
    }

    @Test
    fun `sendPrompt with insecure environment should return error`() = runTest {
        // Given
        fakeSecurityRepository.setInsecureEnvironment(listOf("Root detected"))
        
        // When
        viewModel.sendPrompt(mockBitmap, "What is this?")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Error::class.java)
            val errorState = state as UiState.Error
            assertThat(errorState.errorMessage).contains("Security validation failed")
            assertThat(errorState.canRetry).isFalse()
        }
    }

    @Test
    fun `sendPrompt with invalid input should return error`() = runTest {
        // Given
        fakeSecurityRepository.setInputInvalid("Input contains prohibited content")
        
        // When
        viewModel.sendPrompt(mockBitmap, "Invalid input")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Error::class.java)
            val errorState = state as UiState.Error
            assertThat(errorState.errorMessage).isEqualTo("Input contains prohibited content")
            assertThat(errorState.errorType).isEqualTo(ErrorType.CONTENT_POLICY)
        }
    }

    @Test
    fun `sendPrompt with rate limit should return error`() = runTest {
        // Given
        fakeSecurityRepository.setRateLimited("Too many requests")
        
        // When
        viewModel.sendPrompt(mockBitmap, "What is this?")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Error::class.java)
            val errorState = state as UiState.Error
            assertThat(errorState.errorMessage).isEqualTo("Too many requests")
            assertThat(errorState.errorType).isEqualTo(ErrorType.QUOTA_EXCEEDED)
        }
    }

    @Test
    fun `sendPrompt with AI error should return error`() = runTest {
        // Given
        fakeAIRepository.setErrorResponse("AI service unavailable")
        
        // When
        viewModel.sendPrompt(mockBitmap, "What is this?")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Error::class.java)
            val errorState = state as UiState.Error
            assertThat(errorState.errorMessage).contains("AI service unavailable")
            assertThat(errorState.errorType).isEqualTo(ErrorType.SERVER_ERROR)
        }
    }

    @Test
    fun `sendPrompt with blocked content should return error`() = runTest {
        // Given
        fakeAIRepository.setBlockedResponse()
        
        // When
        viewModel.sendPrompt(mockBitmap, "What is this?")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Error::class.java)
            val errorState = state as UiState.Error
            assertThat(errorState.errorMessage).contains("Response blocked")
            assertThat(errorState.errorType).isEqualTo(ErrorType.CONTENT_POLICY)
        }
    }

    @Test
    fun `sendPrompt with warning should include warning in response`() = runTest {
        // Given
        val response = "Recipe response"
        val warning = "Test warning"
        fakeAIRepository.setWarningResponse(response, warning)
        
        // When
        viewModel.sendPrompt(mockBitmap, "What is this?")
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Success::class.java)
            val successState = state as UiState.Success
            assertThat(successState.outputText).contains(response)
            assertThat(successState.outputText).contains(warning)
        }
    }

    @Test
    fun `retryLastRequest should resend last request`() = runTest {
        // Given
        fakeAIRepository.setSuccessResponse("First response")
        viewModel.sendPrompt(mockBitmap, "What is this?")
        advanceUntilIdle()
        
        // Change response for retry
        fakeAIRepository.setSuccessResponse("Retry response")
        
        // When
        viewModel.retryLastRequest()
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Success::class.java)
            assertThat((state as UiState.Success).outputText).isEqualTo("Retry response")
        }
    }

    @Test
    fun `retryLastRequest without previous request should return error`() = runTest {
        // When
        viewModel.retryLastRequest()
        advanceUntilIdle()
        
        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Error::class.java)
            assertThat((state as UiState.Error).errorMessage).contains("Cannot retry request")
        }
    }

    @Test
    fun `clearError should reset state to Initial`() = runTest {
        // Given - set error state
        fakeSecurityRepository.setInputInvalid("Invalid")
        viewModel.sendPrompt(mockBitmap, "Invalid")
        advanceUntilIdle()
        
        // When
        viewModel.clearError()
        
        // Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(UiState.Initial)
        }
    }

    @Test
    fun `getSecurityStatus should return security environment result`() = runTest {
        // Given
        fakeSecurityRepository.setSecureEnvironment()
        
        // When
        val result = viewModel.getSecurityStatus()
        
        // Then
        assertThat(result).isEqualTo(SecurityEnvironmentResult.Secure)
    }

    @Test
    fun `getRequestCount should track number of requests`() = runTest {
        // Given
        assertThat(viewModel.getRequestCount()).isEqualTo(0)
        
        // When
        viewModel.sendPrompt(mockBitmap, "First request")
        advanceUntilIdle()
        
        // Then
        assertThat(viewModel.getRequestCount()).isEqualTo(1)
        
        // When
        viewModel.sendPrompt(mockBitmap, "Second request")
        advanceUntilIdle()
        
        // Then
        assertThat(viewModel.getRequestCount()).isEqualTo(2)
    }

    @Test
    fun `multiple concurrent requests should be handled properly`() = runTest {
        // Given
        fakeAIRepository.setDelay(50L)
        
        // When - send multiple requests
        viewModel.sendPrompt(mockBitmap, "Request 1")
        viewModel.sendPrompt(mockBitmap, "Request 2")
        viewModel.sendPrompt(mockBitmap, "Request 3")
        advanceUntilIdle()
        
        // Then - should handle gracefully
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(UiState.Success::class.java)
        }
    }
} 
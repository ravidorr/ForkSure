package com.ravidor.forksure

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
class ErrorHandlerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        mockkObject(ErrorHandler)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun handleError_networkUnavailable_returnsNetworkError_noInternet() {
        every { ErrorHandler.isNetworkAvailable(any()) } returns false
        val err = ErrorHandler.handleError(UnknownHostException("no route"), context)
        assertThat(err.errorType).isEqualTo(ErrorType.NETWORK)
        assertThat(err.canRetry).isTrue()
        assertThat(err.errorMessage).contains("No internet connection")
    }

    @Test
    fun handleError_timeout_withNetwork_returnsNetworkError_timeout() {
        every { ErrorHandler.isNetworkAvailable(any()) } returns true
        val err = ErrorHandler.handleError(SocketTimeoutException("timeout"), context)
        assertThat(err.errorType).isEqualTo(ErrorType.NETWORK)
        assertThat(err.errorMessage).contains("Network timeout")
    }

    @Test
    fun handleError_apiKeyIssue_returnsApiKeyError_noRetry() {
        every { ErrorHandler.isNetworkAvailable(any()) } returns true
        val err = ErrorHandler.handleError(Exception("API key invalid"), context)
        assertThat(err.errorType).isEqualTo(ErrorType.API_KEY)
        assertThat(err.canRetry).isFalse()
    }

    @Test
    fun handleError_quota_returnsRateLimit_withRetry() {
        every { ErrorHandler.isNetworkAvailable(any()) } returns true
        val err = ErrorHandler.handleError(Exception("Rate limit exceeded"), context)
        assertThat(err.errorType).isEqualTo(ErrorType.QUOTA_EXCEEDED)
        assertThat(err.canRetry).isTrue()
    }

    @Test
    fun getErrorMessageWithSuggestion_appendsSuggestion_byType() {
        val base = UiState.Error("msg", ErrorType.SERVER_ERROR, canRetry = true)
        val withSuggestion = ErrorHandler.getErrorMessageWithSuggestion(base)
        assertThat(withSuggestion).contains("Suggestion")
        // Should include the server error suggestion text
        assertThat(withSuggestion).contains("temporarily unavailable")
    }
}
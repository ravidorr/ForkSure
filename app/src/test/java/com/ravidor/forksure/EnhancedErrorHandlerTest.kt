package com.ravidor.forksure

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.common.truth.Truth.assertThat

@RunWith(RobolectricTestRunner::class)
class EnhancedErrorHandlerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()
        mockkObject(SecurityManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun handleError_returnsCritical_whenSecurityEnvironmentInsecure() = runTest {
        coEvery { SecurityManager.checkSecurityEnvironment(any()) } returns SecurityEnvironmentResult.Insecure(
            reason = "Security concerns detected",
            details = "Root detected"
        )
        val result = EnhancedErrorHandler.handleError(context, IllegalStateException("boom"))
        assertThat(result).isInstanceOf(EnhancedErrorResult.Critical::class.java)
        val critical = result as EnhancedErrorResult.Critical
        assertThat(critical.requiresUserAction).isTrue()
        // Basic sanity: title/message come from resources and are non-empty
        assertThat(critical.title).isNotEmpty()
        assertThat(critical.message).isNotEmpty()
    }

    @Test
    fun handleError_networkException_returnsRecoverable() = runTest {
        coEvery { SecurityManager.checkSecurityEnvironment(any()) } returns SecurityEnvironmentResult.Secure
        val result = EnhancedErrorHandler.handleError(context, java.net.UnknownHostException("dns"))
        assertThat(result).isInstanceOf(EnhancedErrorResult.Recoverable::class.java)
        val rec = result as EnhancedErrorResult.Recoverable
        assertThat(rec.canRetry).isTrue()
        assertThat(rec.retryDelay).isAtLeast(0)
    }

    @Test
    fun processAIResponse_mapsValidationResults_correctly() {
        every { SecurityManager.validateAIResponse("valid") } returns AIResponseValidationResult.Valid("valid")
        every { SecurityManager.validateAIResponse("warn") } returns AIResponseValidationResult.RequiresWarning("payload","warnmsg")
        every { SecurityManager.validateAIResponse("unsafe") } returns AIResponseValidationResult.Unsafe("why","warn")
        every { SecurityManager.validateAIResponse("invalid") } returns AIResponseValidationResult.Invalid("why","msg")

        assertThat(EnhancedErrorHandler.processAIResponse(context, "valid")).isInstanceOf(AIResponseProcessingResult.Success::class.java)
        assertThat(EnhancedErrorHandler.processAIResponse(context, "warn")).isInstanceOf(AIResponseProcessingResult.SuccessWithWarning::class.java)
        assertThat(EnhancedErrorHandler.processAIResponse(context, "unsafe")).isInstanceOf(AIResponseProcessingResult.Blocked::class.java)
        assertThat(EnhancedErrorHandler.processAIResponse(context, "invalid")).isInstanceOf(AIResponseProcessingResult.Error::class.java)
    }

    @Test
    fun validateUserInput_mapsSecurityValidation_correctly() {
        every { SecurityManager.validateInput("ok") } returns InputValidationResult.Valid("ok")
        every { SecurityManager.validateInput("bad") } returns InputValidationResult.Invalid("nope")

        val v1 = EnhancedErrorHandler.validateUserInput("ok")
        val v2 = EnhancedErrorHandler.validateUserInput("bad")
        assertThat(v1).isInstanceOf(UserInputValidationResult.Valid::class.java)
        assertThat((v1 as UserInputValidationResult.Valid).sanitizedInput).isEqualTo("ok")
        assertThat(v2).isInstanceOf(UserInputValidationResult.Invalid::class.java)
        assertThat((v2 as UserInputValidationResult.Invalid).reason).isEqualTo("nope")
    }
}

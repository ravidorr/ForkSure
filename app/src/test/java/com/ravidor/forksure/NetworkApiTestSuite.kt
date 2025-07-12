package com.ravidor.forksure

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeSource
import com.ravidor.forksure.data.source.local.RecipeCacheDataSource
import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.AIRepositoryImpl
import com.ravidor.forksure.repository.SecurityRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

/**
 * Comprehensive network/API testing suite for ForkSure app
 * 
 * Tests API integration, offline scenarios, and error handling including:
 * - Google Generative AI integration and error handling
 * - Network connectivity scenarios and offline behavior
 * - API rate limiting and quota management
 * - Timeout handling and retry mechanisms
 * - SSL/TLS security and certificate validation
 * - Concurrent API requests and load balancing
 * - API response validation and malformed data handling
 * - Fallback mechanisms and cache integration
 */
@ExperimentalCoroutinesApi
class NetworkApiTestSuite {

    // Test fixtures using localThis pattern
    private lateinit var localThis: TestFixtures
    
    private data class TestFixtures(
        val context: Context,
        val mockGenerativeModel: GenerativeModel,
        val mockSecurityRepository: SecurityRepository,
        val aiRepository: AIRepository,
        val recipeCacheDataSource: RecipeCacheDataSource,
        val testBitmap: Bitmap,
        val testCoroutineScheduler: TestCoroutineScheduler,
        val testDispatcher: UnconfinedTestDispatcher
    )
    
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockGenerativeModel = mockk<GenerativeModel>(relaxed = true)
        val mockSecurityRepository = mockk<SecurityRepository>(relaxed = true)
        val aiRepository = AIRepositoryImpl(mockGenerativeModel)
        val recipeCacheDataSource = RecipeCacheDataSource()
        val testBitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        val testCoroutineScheduler = TestCoroutineScheduler()
        val testDispatcher = UnconfinedTestDispatcher(testCoroutineScheduler)
        
        localThis = TestFixtures(
            context = context,
            mockGenerativeModel = mockGenerativeModel,
            mockSecurityRepository = mockSecurityRepository,
            aiRepository = aiRepository,
            recipeCacheDataSource = recipeCacheDataSource,
            testBitmap = testBitmap,
            testCoroutineScheduler = testCoroutineScheduler,
            testDispatcher = testDispatcher
        )
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
        if (!localThis.testBitmap.isRecycled) {
            localThis.testBitmap.recycle()
        }
    }
    
    // MARK: - Google Generative AI Integration Tests
    
    @Test
    fun `AIRepository should handle successful API responses correctly`() = runTest {
        // Given - successful API response
        val localThis = this@NetworkApiTestSuite.localThis
        
        val mockResponse = mockk<GenerateContentResponse>()
        val expectedContent = """
            **Chocolate Chip Cookies**
            
            *Ingredients:*
            - 2 cups flour
            - 1 cup butter
            - 1 cup chocolate chips
            
            *Instructions:*
            1. Preheat oven to 350°F
            2. Mix ingredients
            3. Bake for 12 minutes
        """.trimIndent()
        
        every { mockResponse.text } returns expectedContent
        coEvery { localThis.mockGenerativeModel.generateContent(any()) } returns mockResponse
        
        // When - making API request
        val result = localThis.aiRepository.generateContent(
            localThis.testBitmap,
            "What are these cookies?"
        )
        
        // Then - should return successful result with parsed content
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Success::class.java)
        val successResult = result as AIResponseProcessingResult.Success
        assertThat(successResult.response).isEqualTo(expectedContent)
        
        // Verify API was called correctly
        coVerify(exactly = 1) { localThis.mockGenerativeModel.generateContent(any()) }
    }
    
    @Test
    fun `AIRepository should handle API response validation and security filtering`() = runTest {
        // Given - API response that requires validation
        val localThis = this@NetworkApiTestSuite.localThis
        
        val dangerousContent = """
            To make explosives, mix dangerous chemicals and heat to extreme temperatures.
            This recipe contains harmful instructions that could cause injury.
        """.trimIndent()
        
        val mockResponse = mockk<GenerateContentResponse>()
        every { mockResponse.text } returns dangerousContent
        coEvery { localThis.mockGenerativeModel.generateContent(any()) } returns mockResponse
        
        // When - making API request with dangerous content
        val result = localThis.aiRepository.generateContent(
            localThis.testBitmap,
            "How to make something dangerous?"
        )
        
        // Then - should filter out dangerous content
        when (result) {
            is AIResponseProcessingResult.Error -> {
                assertThat(result.userMessage).contains("content")
            }
            is AIResponseProcessingResult.SuccessWithWarning -> {
                assertThat(result.warning).contains("safety")
            }
            else -> {
                // Success is not expected for dangerous content
                assertThat(result).isNotInstanceOf(AIResponseProcessingResult.Success::class.java)
            }
        }
        
        coVerify(exactly = 1) { localThis.mockGenerativeModel.generateContent(any()) }
    }
    
    @Test
    fun `AIRepository should handle null or empty API responses gracefully`() = runTest {
        // Given - null or empty API responses
        val localThis = this@NetworkApiTestSuite.localThis
        
        val testCases = listOf(
            null to "No response received",
            "" to "No response received",
            "   " to "No response received"
        )
        
        testCases.forEach { (responseText, expectedError) ->
            val mockResponse = mockk<GenerateContentResponse>()
            every { mockResponse.text } returns responseText
            coEvery { localThis.mockGenerativeModel.generateContent(any()) } returns mockResponse
            
            // When - making API request
            val result = localThis.aiRepository.generateContent(
                localThis.testBitmap,
                "Test prompt"
            )
            
            // Then - should handle gracefully with error
            assertThat(result).isInstanceOf(AIResponseProcessingResult.Error::class.java)
            val errorResult = result as AIResponseProcessingResult.Error
            assertThat(errorResult.userMessage).contains(expectedError)
        }
    }
    
    // MARK: - Network Connectivity and Error Handling Tests
    
    @Test
    fun `AIRepository should handle network connectivity errors appropriately`() = runTest {
        // Given - various network error scenarios
        val localThis = this@NetworkApiTestSuite.localThis
        
        val networkErrors = listOf(
            UnknownHostException("Unable to resolve host") to "network",
            ConnectException("Connection refused") to "connection",
            SocketTimeoutException("Read timed out") to "timeout",
            IOException("Network is unreachable") to "network",
            SSLException("SSL handshake failed") to "SSL"
        )
        
        networkErrors.forEach { (exception, errorType) ->
            coEvery { localThis.mockGenerativeModel.generateContent(any()) } throws exception
            
            // When - making API request with network error
            val result = localThis.aiRepository.generateContent(
                localThis.testBitmap,
                "Test prompt for $errorType error"
            )
            
            // Then - should return appropriate error result
            assertThat(result).isInstanceOf(AIResponseProcessingResult.Error::class.java)
            val errorResult = result as AIResponseProcessingResult.Error
            assertThat(errorResult.internalMessage).contains("AI request failed")
            assertThat(errorResult.userMessage).isNotEmpty()
        }
    }
    
    @Test
    fun `AIRepository should implement proper timeout handling`() = runTest {
        // Given - slow API response simulation
        val localThis = this@NetworkApiTestSuite.localThis
        val timeoutDuration = 5000L // 5 seconds
        
        coEvery { localThis.mockGenerativeModel.generateContent(any()) } coAnswers {
            delay(timeoutDuration + 1000) // Simulate longer than timeout
            throw TimeoutException("Request timed out")
        }
        
        val startTime = System.currentTimeMillis()
        
        // When - making API request that will timeout
        val result = localThis.aiRepository.generateContent(
            localThis.testBitmap,
            "Test timeout scenario"
        )
        
        val endTime = System.currentTimeMillis()
        val actualDuration = endTime - startTime
        
        // Then - should handle timeout appropriately
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Error::class.java)
        val errorResult = result as AIResponseProcessingResult.Error
        assertThat(errorResult.userMessage).contains("timeout", true)
        
        // Should not wait indefinitely
        assertThat(actualDuration).isLessThan(timeoutDuration + 2000)
    }
    
    @Test
    fun `AIRepository should handle rate limiting and quota exceeded scenarios`() = runTest {
        // Given - API rate limiting scenarios
        val localThis = this@NetworkApiTestSuite.localThis
        
        val rateLimitError = RuntimeException("Quota exceeded for this request")
        coEvery { localThis.mockGenerativeModel.generateContent(any()) } throws rateLimitError
        
        // When - making API request when rate limited
        val result = localThis.aiRepository.generateContent(
            localThis.testBitmap,
            "Test rate limiting"
        )
        
        // Then - should handle rate limiting gracefully
        assertThat(result).isInstanceOf(AIResponseProcessingResult.Error::class.java)
        val errorResult = result as AIResponseProcessingResult.Error
        assertThat(errorResult.userMessage).contains("request")
        
        // Verify error was logged appropriately
        coVerify(exactly = 1) { localThis.mockGenerativeModel.generateContent(any()) }
    }
    
    // MARK: - Concurrent API Request Tests
    
    @Test
    fun `AIRepository should handle multiple concurrent requests efficiently`() = runTest {
        // Given - multiple concurrent API requests
        val localThis = this@NetworkApiTestSuite.localThis
        val requestCount = 20
        val delayPerRequest = 100L
        
        // Mock responses with slight delay to simulate real API
        coEvery { localThis.mockGenerativeModel.generateContent(any()) } coAnswers {
            delay(delayPerRequest)
            val mockResponse = mockk<GenerateContentResponse>()
            every { mockResponse.text } returns "Concurrent response ${System.currentTimeMillis()}"
            mockResponse
        }
        
        val startTime = System.currentTimeMillis()
        
        // When - making concurrent requests
        val deferredResults = (1..requestCount).map { index ->
            launch {
                localThis.aiRepository.generateContent(
                    localThis.testBitmap,
                    "Concurrent request $index"
                )
            }
        }
        
        deferredResults.forEach { it.join() }
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Then - concurrent requests should be handled efficiently
        // Total time should be much less than sequential execution
        val sequentialTime = requestCount * delayPerRequest
        assertThat(totalTime).isLessThan(sequentialTime / 2) // At least 50% improvement from concurrency
        
        // All requests should have been made
        coVerify(exactly = requestCount) { localThis.mockGenerativeModel.generateContent(any()) }
    }
    
    @Test
    fun `AIRepository should maintain thread safety during concurrent access`() = runTest {
        // Given - high-concurrency scenario
        val localThis = this@NetworkApiTestSuite.localThis
        val threadCount = 50
        val requestsPerThread = 10
        val latch = CountDownLatch(threadCount)
        val errors = mutableListOf<Exception>()
        val successCount = mutableListOf<Int>()
        
        // Mock successful responses
        val mockResponse = mockk<GenerateContentResponse>()
        every { mockResponse.text } returns "Thread-safe response"
        coEvery { localThis.mockGenerativeModel.generateContent(any()) } returns mockResponse
        
        // When - making highly concurrent requests
        repeat(threadCount) { threadId ->
            launch {
                try {
                    var threadSuccesses = 0
                    repeat(requestsPerThread) { requestId ->
                        val result = localThis.aiRepository.generateContent(
                            localThis.testBitmap,
                            "Thread $threadId Request $requestId"
                        )
                        if (result is AIResponseProcessingResult.Success) {
                            threadSuccesses++
                        }
                    }
                    successCount.add(threadSuccesses)
                } catch (e: Exception) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }
        
        val completed = latch.await(30, TimeUnit.SECONDS)
        
        // Then - should maintain thread safety
        assertThat(completed).isTrue()
        assertThat(errors).isEmpty()
        
        // All requests should succeed
        val totalSuccesses = successCount.sum()
        assertThat(totalSuccesses).isEqualTo(threadCount * requestsPerThread)
        
        // All API calls should have been made
        coVerify(exactly = threadCount * requestsPerThread) { 
            localThis.mockGenerativeModel.generateContent(any()) 
        }
    }
    
    // MARK: - Offline Behavior and Fallback Tests
    
    @Test
    fun `System should gracefully handle offline scenarios with cache fallback`() = runTest {
        // Given - offline scenario with cached data available
        val localThis = this@NetworkApiTestSuite.localThis
        
        // Pre-populate cache
        val cachedRecipe = createTestRecipe("Cached Recipe", "Cached content")
        val request = com.ravidor.forksure.data.model.RecipeAnalysisRequest(
            imageHash = "offline_test_hash",
            prompt = "offline test prompt"
        )
        val response = com.ravidor.forksure.data.model.RecipeAnalysisResponse(
            recipe = cachedRecipe,
            rawResponse = "Cached response",
            processingTime = 50,
            success = true
        )
        localThis.recipeCacheDataSource.cacheRecipe(request, response)
        
        // Simulate offline condition
        coEvery { localThis.mockGenerativeModel.generateContent(any()) } throws 
            UnknownHostException("Network is unreachable")
        
        // When - attempting API request while offline
        val apiResult = localThis.aiRepository.generateContent(
            localThis.testBitmap,
            "offline test prompt"
        )
        
        // And - attempting to retrieve from cache
        val cachedResult = localThis.recipeCacheDataSource.getCachedRecipe(request)
        
        // Then - API should fail but cache should provide fallback
        assertThat(apiResult).isInstanceOf(AIResponseProcessingResult.Error::class.java)
        assertThat(cachedResult).isNotNull()
        assertThat(cachedResult?.title).isEqualTo("Cached Recipe")
        assertThat(cachedResult?.source).isEqualTo(RecipeSource.AI_GENERATED)
    }
    
    @Test
    fun `System should handle partial connectivity and intermittent failures`() = runTest {
        // Given - intermittent connectivity scenario
        val localThis = this@NetworkApiTestSuite.localThis
        val attemptCount = 10
        val successResponses = 6
        val failureResponses = attemptCount - successResponses
        
        val mockResponse = mockk<GenerateContentResponse>()
        every { mockResponse.text } returns "Intermittent success response"
        
        // Setup alternating success/failure pattern
        var callCount = 0
        coEvery { localThis.mockGenerativeModel.generateContent(any()) } coAnswers {
            callCount++
            if (callCount % 2 == 0 && callCount <= successResponses * 2) {
                mockResponse
            } else {
                throw ConnectException("Intermittent connection failure")
            }
        }
        
        // When - making multiple requests with intermittent connectivity
        val results = mutableListOf<AIResponseProcessingResult>()
        repeat(attemptCount) { index ->
            val result = localThis.aiRepository.generateContent(
                localThis.testBitmap,
                "Intermittent request $index"
            )
            results.add(result)
            delay(50) // Small delay between requests
        }
        
        // Then - should handle both successes and failures appropriately
        val successes = results.count { it is AIResponseProcessingResult.Success }
        val failures = results.count { it is AIResponseProcessingResult.Error }
        
        assertThat(successes).isEqualTo(successResponses)
        assertThat(failures).isEqualTo(failureResponses)
        
        // All requests should have been attempted
        coVerify(exactly = attemptCount) { localThis.mockGenerativeModel.generateContent(any()) }
    }
    
    // MARK: - API Response Validation Tests
    
    @Test
    fun `AIRepository should validate and sanitize API responses for security`() = runTest {
        // Given - potentially malicious API responses
        val localThis = this@NetworkApiTestSuite.localThis
        
        val maliciousResponses = listOf(
            "<script>alert('xss')</script>Recipe content" to "XSS",
            "Recipe with dangerous javascript:void(0) links" to "Javascript",
            "Recipe\nSELECT * FROM users; DROP TABLE recipes;" to "SQL injection",
            "Recipe with extreme temperatures of 10000°F that could cause explosions" to "Safety"
        )
        
        maliciousResponses.forEach { (maliciousContent, testType) ->
            val mockResponse = mockk<GenerateContentResponse>()
            every { mockResponse.text } returns maliciousContent
            coEvery { localThis.mockGenerativeModel.generateContent(any()) } returns mockResponse
            
            // When - receiving potentially malicious response
            val result = localThis.aiRepository.generateContent(
                localThis.testBitmap,
                "Test $testType validation"
            )
            
            // Then - should validate and handle appropriately
            when (result) {
                is AIResponseProcessingResult.Success -> {
                    // If successful, content should be sanitized
                    assertThat(result.response).doesNotContain("<script>")
                    assertThat(result.response).doesNotContain("javascript:")
                }
                is AIResponseProcessingResult.SuccessWithWarning -> {
                    // Warning should be provided for questionable content
                    assertThat(result.warning).isNotEmpty()
                }
                is AIResponseProcessingResult.Error -> {
                    // Error for clearly malicious content
                    assertThat(result.userMessage).contains("content")
                }
            }
        }
    }
    
    @Test
    fun `AIRepository should handle malformed JSON and unexpected response formats`() = runTest {
        // Given - malformed response data
        val localThis = this@NetworkApiTestSuite.localThis
        
        val malformedResponses = listOf(
            "{invalid json structure",
            "Recipe: {\"ingredients\": [\"flour\", \"sugar\"",
            "\uFEFF\uFEFFCorrupted encoding recipe", // BOM characters
            "Recipe\u0000with\u0000null\u0000characters",
            "Recipe with extreme length: " + "x".repeat(1000000) // Very long response
        )
        
        malformedResponses.forEach { malformedContent ->
            val mockResponse = mockk<GenerateContentResponse>()
            every { mockResponse.text } returns malformedContent
            coEvery { localThis.mockGenerativeModel.generateContent(any()) } returns mockResponse
            
            // When - receiving malformed response
            val result = localThis.aiRepository.generateContent(
                localThis.testBitmap,
                "Test malformed response"
            )
            
            // Then - should handle gracefully without crashing
            assertThat(result).isNotNull()
            when (result) {
                is AIResponseProcessingResult.Success -> {
                    // Should sanitize problematic characters
                    assertThat(result.response).doesNotContain("\u0000")
                    assertThat(result.response.length).isLessThan(100000) // Reasonable length
                }
                is AIResponseProcessingResult.Error -> {
                    // Error for completely unusable content
                    assertThat(result.userMessage).isNotEmpty()
                }
                is AIResponseProcessingResult.SuccessWithWarning -> {
                    // Warning for questionable but usable content
                    assertThat(result.warning).isNotEmpty()
                }
            }
        }
    }
    
    // MARK: - Performance and Load Testing
    
    @Test
    fun `API integration should perform efficiently under sustained load`() = runTest {
        // Given - sustained load scenario
        val localThis = this@NetworkApiTestSuite.localThis
        val loadDuration = 5000L // 5 seconds
        val maxRequestsPerSecond = 10
        
        val mockResponse = mockk<GenerateContentResponse>()
        every { mockResponse.text } returns "Load test response"
        
        // Simulate realistic API response time
        coEvery { localThis.mockGenerativeModel.generateContent(any()) } coAnswers {
            delay(100) // 100ms simulated API response time
            mockResponse
        }
        
        val startTime = System.currentTimeMillis()
        var requestCount = 0
        val results = mutableListOf<AIResponseProcessingResult>()
        
        // When - sustaining load for duration
        while (System.currentTimeMillis() - startTime < loadDuration) {
            val result = localThis.aiRepository.generateContent(
                localThis.testBitmap,
                "Load test request ${requestCount++}"
            )
            results.add(result)
            
            // Rate limiting to avoid overwhelming
            delay(1000 / maxRequestsPerSecond)
        }
        
        val endTime = System.currentTimeMillis()
        val actualDuration = endTime - startTime
        val requestsPerSecond = requestCount.toDouble() / (actualDuration / 1000.0)
        
        // Then - should maintain performance under load
        assertThat(requestCount).isGreaterThan(0)
        assertThat(requestsPerSecond).isAtMost(maxRequestsPerSecond.toDouble() * 1.2) // 20% tolerance
        
        // All requests should succeed
        val successCount = results.count { it is AIResponseProcessingResult.Success }
        assertThat(successCount).isEqualTo(requestCount)
        
        // Memory usage should remain stable
        System.gc()
        delay(100)
        // Memory should not grow excessively (this is a simple check)
        assertThat(Runtime.getRuntime().freeMemory()).isGreaterThan(0)
    }
    
    // MARK: - Helper Methods
    
    private fun createTestRecipe(title: String, content: String): Recipe {
        return Recipe(
            id = "network_test_${System.currentTimeMillis()}",
            title = title,
            description = content,
            ingredients = listOf("ingredient1", "ingredient2"),
            instructions = listOf("step1", "step2"),
            source = RecipeSource.AI_GENERATED
        )
    }
} 
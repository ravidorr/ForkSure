package com.ravidor.forksure

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeAnalysisRequest
import com.ravidor.forksure.data.model.RecipeAnalysisResponse
import com.ravidor.forksure.data.model.RecipeSource
import com.ravidor.forksure.data.model.UserPreferences
import com.ravidor.forksure.data.source.local.PreferencesDataSource
import com.ravidor.forksure.data.source.local.RecipeCacheDataSource
import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.RecipeRepository
import com.ravidor.forksure.repository.RecipeRepositoryImpl
import com.ravidor.forksure.state.MainScreenState
import com.ravidor.forksure.state.NavigationState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Comprehensive stress testing suite for ForkSure app
 * 
 * Tests high load scenarios, edge cases, and resource exhaustion including:
 * - Extreme load testing with thousands of concurrent operations
 * - Memory pressure and resource exhaustion scenarios
 * - Edge case input validation and boundary testing
 * - System stability under prolonged stress
 * - Recovery mechanisms and graceful degradation
 * - Performance under resource constraints
 * - Concurrent access stress testing
 * - Large data set handling and processing
 */
@ExperimentalCoroutinesApi
class StressTestSuite {

    // Test fixtures using localThis pattern
    private lateinit var localThis: TestFixtures
    
    private data class TestFixtures(
        val context: Context,
        val mockAIRepository: AIRepository,
        val mockPreferencesDataSource: PreferencesDataSource,
        val recipeCacheDataSource: RecipeCacheDataSource,
        val recipeRepository: RecipeRepository,
        val mainScreenState: MainScreenState,
        val navigationState: NavigationState,
        val testBitmaps: List<Bitmap>,
        val stressMetrics: StressMetrics,
        val testCoroutineScheduler: TestCoroutineScheduler
    )
    
    private data class StressMetrics(
        val operationCount: AtomicInteger = AtomicInteger(0),
        val successCount: AtomicInteger = AtomicInteger(0),
        val errorCount: AtomicInteger = AtomicInteger(0),
        val totalExecutionTime: AtomicLong = AtomicLong(0),
        val peakMemoryUsage: AtomicLong = AtomicLong(0),
        val averageResponseTime: AtomicLong = AtomicLong(0)
    )
    
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockAIRepository = mockk<AIRepository>(relaxed = true)
        val mockPreferencesDataSource = mockk<PreferencesDataSource>(relaxed = true)
        val recipeCacheDataSource = RecipeCacheDataSource()
        val recipeRepository = RecipeRepositoryImpl(mockAIRepository, recipeCacheDataSource)
        val mainScreenState = MainScreenState()
        val navigationState = NavigationState()
        val testBitmaps = createTestBitmaps()
        val stressMetrics = StressMetrics()
        val testCoroutineScheduler = TestCoroutineScheduler()
        
        localThis = TestFixtures(
            context = context,
            mockAIRepository = mockAIRepository,
            mockPreferencesDataSource = mockPreferencesDataSource,
            recipeCacheDataSource = recipeCacheDataSource,
            recipeRepository = recipeRepository,
            mainScreenState = mainScreenState,
            navigationState = navigationState,
            testBitmaps = testBitmaps,
            stressMetrics = stressMetrics,
            testCoroutineScheduler = testCoroutineScheduler
        )
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
        localThis.testBitmaps.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        
        // Force garbage collection after stress tests
        repeat(3) {
            System.gc()
            Thread.sleep(100)
        }
    }
    
    // MARK: - Extreme Load Testing
    
    @Test
    fun `System should handle extreme concurrent load without failure`() = runTest {
        // Given - extreme concurrent load scenario
        val localThis = this@StressTestSuite.localThis
        val concurrentOperations = 1000
        val operationsPerThread = 50
        val threadCount = concurrentOperations / operationsPerThread
        
        // Mock fast AI responses for load testing
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Load test recipe content")
        
        val startTime = System.currentTimeMillis()
        val initialMemory = getMemoryUsage()
        
        // When - executing extreme concurrent load
        val deferredOperations = (1..threadCount).map { threadId ->
            async {
                repeat(operationsPerThread) { opId ->
                    try {
                        val bitmap = localThis.testBitmaps[opId % localThis.testBitmaps.size]
                        val prompt = "Extreme load test $threadId-$opId"
                        
                        val result = localThis.recipeRepository.analyzeRecipe(bitmap, prompt).first()
                        
                        localThis.stressMetrics.operationCount.incrementAndGet()
                        if (result.success) {
                            localThis.stressMetrics.successCount.incrementAndGet()
                        } else {
                            localThis.stressMetrics.errorCount.incrementAndGet()
                        }
                        
                        // Track memory usage
                        val currentMemory = getMemoryUsage()
                        updatePeakMemory(localThis.stressMetrics, currentMemory)
                        
                    } catch (e: Exception) {
                        localThis.stressMetrics.errorCount.incrementAndGet()
                    }
                }
            }
        }
        
        val results = deferredOperations.awaitAll()
        val endTime = System.currentTimeMillis()
        val finalMemory = getMemoryUsage()
        
        // Then - system should handle extreme load gracefully
        val totalTime = endTime - startTime
        val totalOperations = localThis.stressMetrics.operationCount.get()
        val successRate = localThis.stressMetrics.successCount.get().toDouble() / totalOperations
        val memoryIncrease = finalMemory - initialMemory
        
        assertThat(totalOperations).isEqualTo(concurrentOperations)
        assertThat(successRate).isAtLeast(0.95) // At least 95% success rate under extreme load
        assertThat(totalTime).isLessThan(30000) // Under 30 seconds for 1000 operations
        assertThat(memoryIncrease).isLessThan(500 * 1024 * 1024) // Under 500MB memory increase
        
        // Verify all operations were attempted
        coVerify(exactly = concurrentOperations) { 
            localThis.mockAIRepository.generateContent(any(), any()) 
        }
    }
    
    @Test
    fun `Recipe cache should handle massive data sets without degradation`() = runTest {
        // Given - massive dataset scenario
        val localThis = this@StressTestSuite.localThis
        val massiveDataSize = 10000
        val batchSize = 500
        
        val startTime = System.currentTimeMillis()
        val initialMemory = getMemoryUsage()
        
        // When - adding massive amounts of data to cache
        repeat(massiveDataSize / batchSize) { batchIndex ->
            val batchOperations = (1..batchSize).map { itemIndex ->
                async {
                    val globalIndex = batchIndex * batchSize + itemIndex
                    val request = RecipeAnalysisRequest(
                        imageHash = "massive_hash_$globalIndex",
                        prompt = "massive_prompt_$globalIndex"
                    )
                    
                    // Create large recipe data
                    val largeContent = "Massive recipe content ".repeat(100) + globalIndex
                    val recipe = createTestRecipe("Massive Recipe $globalIndex", largeContent)
                    val response = createTestResponse(recipe)
                    
                    localThis.recipeCacheDataSource.cacheRecipe(request, response)
                    
                    // Occasionally verify cache operations
                    if (globalIndex % 1000 == 0) {
                        localThis.recipeCacheDataSource.getCachedRecipe(request)
                    }
                }
            }
            
            batchOperations.awaitAll()
            
            // Force garbage collection every few batches
            if (batchIndex % 5 == 0) {
                System.gc()
                delay(10)
            }
        }
        
        val endTime = System.currentTimeMillis()
        val finalMemory = getMemoryUsage()
        
        // Then - cache should handle massive datasets efficiently
        val totalTime = endTime - startTime
        val memoryIncrease = finalMemory - initialMemory
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        
        // Cache should implement proper LRU eviction
        assertThat(cacheStats.totalEntries).isAtMost(50) // Should not exceed LRU limit
        assertThat(cacheStats.evictionCount).isGreaterThan(0) // Should have evicted old entries
        
        // Performance should remain acceptable
        assertThat(totalTime).isLessThan(60000) // Under 1 minute for 10K operations
        assertThat(memoryIncrease).isLessThan(200 * 1024 * 1024) // Under 200MB due to LRU
        
        // Cache operations should remain fast
        val averageTimePerOperation = totalTime.toDouble() / massiveDataSize
        assertThat(averageTimePerOperation).isLessThan(10) // Under 10ms per operation average
    }
    
    // MARK: - Memory Pressure Testing
    
    @Test
    fun `System should gracefully handle memory pressure scenarios`() = runTest {
        // Given - memory pressure simulation
        val localThis = this@StressTestSuite.localThis
        val memoryPressureOperations = 1000
        val largeDataSize = 1024 * 1024 // 1MB per operation
        
        // Mock responses that create memory pressure
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Large response content".repeat(10000))
        
        val initialMemory = getMemoryUsage()
        val memoryPressureData = mutableListOf<ByteArray>()
        
        // When - creating memory pressure while running operations
        val operations = (1..memoryPressureOperations).map { index ->
            async {
                try {
                    // Create memory pressure
                    if (index % 10 == 0) {
                        memoryPressureData.add(ByteArray(largeDataSize))
                    }
                    
                    // Perform recipe analysis under memory pressure
                    val bitmap = localThis.testBitmaps[index % localThis.testBitmaps.size]
                    val result = localThis.recipeRepository.analyzeRecipe(
                        bitmap, 
                        "Memory pressure test $index"
                    ).first()
                    
                    // Track memory usage
                    val currentMemory = getMemoryUsage()
                    updatePeakMemory(localThis.stressMetrics, currentMemory)
                    
                    if (result.success) {
                        localThis.stressMetrics.successCount.incrementAndGet()
                    } else {
                        localThis.stressMetrics.errorCount.incrementAndGet()
                    }
                    
                    // Trigger garbage collection occasionally
                    if (index % 100 == 0) {
                        System.gc()
                        delay(10)
                    }
                    
                } catch (e: OutOfMemoryError) {
                    // Handle memory exhaustion gracefully
                    localThis.stressMetrics.errorCount.incrementAndGet()
                    System.gc()
                } catch (e: Exception) {
                    localThis.stressMetrics.errorCount.incrementAndGet()
                }
            }
        }
        
        operations.awaitAll()
        
        // Force cleanup
        memoryPressureData.clear()
        repeat(3) {
            System.gc()
            delay(100)
        }
        
        val finalMemory = getMemoryUsage()
        
        // Then - system should handle memory pressure gracefully
        val successRate = localThis.stressMetrics.successCount.get().toDouble() / memoryPressureOperations
        val peakMemory = localThis.stressMetrics.peakMemoryUsage.get()
        
        assertThat(successRate).isAtLeast(0.80) // At least 80% success under memory pressure
        assertThat(finalMemory).isLessThan(initialMemory + 100 * 1024 * 1024) // Memory should recover
        
        // Peak memory should be tracked
        assertThat(peakMemory).isGreaterThan(initialMemory)
    }
    
    @Test
    fun `State management should remain stable under resource exhaustion`() = runTest {
        // Given - resource exhaustion scenario
        val localThis = this@StressTestSuite.localThis
        val exhaustionOperations = 5000
        val rapidStateChanges = 100
        
        val startTime = System.currentTimeMillis()
        
        // When - performing rapid state changes under resource pressure
        val stateOperations = (1..exhaustionOperations).map { index ->
            async {
                try {
                    // Rapid state changes
                    repeat(rapidStateChanges) { changeIndex ->
                        val globalIndex = index * rapidStateChanges + changeIndex
                        
                        // Main screen state changes
                        localThis.mainScreenState.updatePrompt("Exhaustion test $globalIndex")
                        localThis.mainScreenState.selectSampleImage(globalIndex % 3)
                        
                        if (globalIndex % 2 == 0) {
                            val bitmap = localThis.testBitmaps[globalIndex % localThis.testBitmaps.size]
                            localThis.mainScreenState.updateCapturedImage(bitmap)
                            localThis.navigationState.updateCapturedImage(bitmap)
                        }
                        
                        // Navigation state changes
                        if (globalIndex % 3 == 0) {
                            localThis.navigationState.clearCapturedImage()
                        }
                        
                        // Dialog state changes
                        if (globalIndex % 10 == 0) {
                            localThis.mainScreenState.showReportDialog()
                            localThis.mainScreenState.hideReportDialog()
                        }
                    }
                    
                    localThis.stressMetrics.successCount.incrementAndGet()
                    
                } catch (e: Exception) {
                    localThis.stressMetrics.errorCount.incrementAndGet()
                }
            }
        }
        
        stateOperations.awaitAll()
        val endTime = System.currentTimeMillis()
        
        // Then - state should remain consistent despite exhaustion
        val totalTime = endTime - startTime
        val successRate = localThis.stressMetrics.successCount.get().toDouble() / exhaustionOperations
        
        assertThat(successRate).isAtLeast(0.95) // At least 95% success rate
        assertThat(totalTime).isLessThan(30000) // Under 30 seconds
        
        // State should be consistent after exhaustion
        assertThat(localThis.mainScreenState.prompt).isNotEmpty()
        assertThat(localThis.mainScreenState.selectedImageIndex).isAtLeast(-1)
        assertThat(localThis.mainScreenState.showReportDialog).isFalse()
    }
    
    // MARK: - Edge Case and Boundary Testing
    
    @Test
    fun `System should handle extreme edge case inputs without failure`() = runTest {
        // Given - extreme edge case inputs
        val localThis = this@StressTestSuite.localThis
        
        val extremeInputs = listOf(
            // Empty and null-like inputs
            "",
            " ",
            "\n\n\n",
            "\t\t\t",
            
            // Extremely long inputs
            "a".repeat(1000000),
            "Test prompt " + "very long content ".repeat(50000),
            
            // Unicode and special characters
            "🍪🧁🎂" + "🍰".repeat(1000),
            "Recipe with émojis: 👨‍🍳👩‍🍳🥧🍞🥖",
            "\uD83D\uDE00".repeat(500), // Emoji flood
            
            // Control characters and unusual Unicode
            "Recipe\u0000with\u0001null\u0002characters",
            "Test\u200B\u200C\u200D\uFEFFwith invisible chars",
            
            // Script injection attempts
            "<script>alert('test')</script>Recipe",
            "javascript:void(0)Recipe content",
            "${'\$'}{malicious}Recipe",
            
            // SQL injection attempts
            "'; DROP TABLE recipes; --",
            "UNION SELECT * FROM users",
            
            // Path traversal attempts
            "../../../etc/passwd",
            "..\\..\\windows\\system32",
            
            // XML/JSON injection
            "<xml><recipe>test</recipe></xml>",
            "{\"malicious\": \"json\", \"recipe\": \"content\"}",
            
            // Buffer overflow attempts
            "A".repeat(Integer.MAX_VALUE / 1000), // Large but not crash-inducing
            
            // Format string attacks
            "%s%s%s%s%s%s%s%s",
            "%x%x%x%x%x%x%x%x",
            
            // Newline and encoding variations
            "Recipe\r\nwith\r\ndifferent\r\nnewlines",
            "Recipe\u2028with\u2029unicode\u2028newlines"
        )
        
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Safe recipe response")
        
        // When - testing all extreme edge cases
        extremeInputs.forEachIndexed { index, extremeInput ->
            try {
                val bitmap = localThis.testBitmaps[index % localThis.testBitmaps.size]
                
                // Test with repository
                val result = localThis.recipeRepository.analyzeRecipe(bitmap, extremeInput).first()
                
                // Test with state management
                localThis.mainScreenState.updatePrompt(extremeInput)
                
                localThis.stressMetrics.operationCount.incrementAndGet()
                
                // Should not crash - any result is acceptable
                assertThat(result).isNotNull()
                
                localThis.stressMetrics.successCount.incrementAndGet()
                
            } catch (e: OutOfMemoryError) {
                // OOM is acceptable for extreme inputs
                localThis.stressMetrics.errorCount.incrementAndGet()
                System.gc()
            } catch (e: Exception) {
                // Other exceptions should be handled gracefully
                localThis.stressMetrics.errorCount.incrementAndGet()
            }
        }
        
        // Then - system should handle all edge cases without crashing
        val totalOperations = localThis.stressMetrics.operationCount.get()
        val successRate = localThis.stressMetrics.successCount.get().toDouble() / totalOperations
        
        assertThat(totalOperations).isEqualTo(extremeInputs.size)
        assertThat(successRate).isAtLeast(0.70) // At least 70% should be handled gracefully
        
        // State should remain functional after edge case testing
        assertThat(localThis.mainScreenState.isAnalyzeEnabled).isNotNull()
    }
    
    @Test
    fun `System should handle boundary conditions for all numeric limits`() = runTest {
        // Given - boundary condition testing
        val localThis = this@StressTestSuite.localThis
        
        val boundaryConditions = listOf(
            // Integer boundaries
            Int.MIN_VALUE to "Integer minimum",
            Int.MAX_VALUE to "Integer maximum",
            0 to "Zero",
            -1 to "Negative one",
            1 to "Positive one",
            
            // Cache size boundaries
            0 to "Zero cache",
            1 to "Single cache",
            10000 to "Large cache",
            
            // String length boundaries
            0 to "Empty string length",
            1 to "Single character",
            65535 to "Large string length"
        )
        
        // When - testing boundary conditions
        boundaryConditions.forEach { (value, description) ->
            try {
                // Test cache size boundaries
                if (value in 0..10000) {
                    repeat(minOf(value, 100)) { index ->
                        val request = RecipeAnalysisRequest(
                            imageHash = "boundary_hash_${value}_$index",
                            prompt = "boundary_prompt_${value}_$index"
                        )
                        val recipe = createTestRecipe("Boundary Recipe $index", description)
                        val response = createTestResponse(recipe)
                        localThis.recipeCacheDataSource.cacheRecipe(request, response)
                    }
                }
                
                // Test string length boundaries
                if (value in 0..1000) { // Reasonable limit for testing
                    val testString = "X".repeat(maxOf(0, value))
                    localThis.mainScreenState.updatePrompt(testString)
                    localThis.mainScreenState.updateResult(testString)
                }
                
                localThis.stressMetrics.successCount.incrementAndGet()
                
            } catch (e: OutOfMemoryError) {
                // Expected for extreme values
                localThis.stressMetrics.errorCount.incrementAndGet()
                System.gc()
            } catch (e: Exception) {
                // Should handle gracefully
                localThis.stressMetrics.errorCount.incrementAndGet()
            }
        }
        
        // Then - boundary conditions should be handled properly
        val successRate = localThis.stressMetrics.successCount.get().toDouble() / boundaryConditions.size
        assertThat(successRate).isAtLeast(0.80) // At least 80% of boundaries handled
        
        // Cache should remain functional
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        assertThat(cacheStats.totalEntries).isAtMost(50) // LRU should limit size
    }
    
    // MARK: - Long-Term Stability Testing
    
    @Test
    fun `System should maintain stability during prolonged stress testing`() = runTest {
        // Given - prolonged stress test scenario
        val localThis = this@StressTestSuite.localThis
        val testDuration = 10000L // 10 seconds of continuous stress
        val operationsPerSecond = 50
        val operationInterval = 1000L / operationsPerSecond
        
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Prolonged test response")
        
        val startTime = System.currentTimeMillis()
        var operationCount = 0
        val initialMemory = getMemoryUsage()
        
        // When - running prolonged stress test
        withTimeout(testDuration + 5000) { // Add buffer for timeout
            while (System.currentTimeMillis() - startTime < testDuration) {
                try {
                    val currentTime = System.currentTimeMillis()
                    operationCount++
                    
                    // Mix of different operations
                    when (operationCount % 4) {
                        0 -> {
                            // Recipe analysis
                            val bitmap = localThis.testBitmaps[operationCount % localThis.testBitmaps.size]
                            val result = localThis.recipeRepository.analyzeRecipe(
                                bitmap, 
                                "Prolonged test $operationCount"
                            ).first()
                            
                            if (result.success) {
                                localThis.stressMetrics.successCount.incrementAndGet()
                            }
                        }
                        1 -> {
                            // State management
                            localThis.mainScreenState.updatePrompt("Prolonged prompt $operationCount")
                            localThis.mainScreenState.selectSampleImage(operationCount % 3)
                            localThis.stressMetrics.successCount.incrementAndGet()
                        }
                        2 -> {
                            // Cache operations
                            val request = RecipeAnalysisRequest(
                                imageHash = "prolonged_hash_$operationCount",
                                prompt = "prolonged_prompt_$operationCount"
                            )
                            localThis.recipeCacheDataSource.getCachedRecipe(request)
                            localThis.stressMetrics.successCount.incrementAndGet()
                        }
                        3 -> {
                            // Navigation state
                            if (operationCount % 2 == 0) {
                                val bitmap = localThis.testBitmaps[operationCount % localThis.testBitmaps.size]
                                localThis.navigationState.updateCapturedImage(bitmap)
                            } else {
                                localThis.navigationState.clearCapturedImage()
                            }
                            localThis.stressMetrics.successCount.incrementAndGet()
                        }
                    }
                    
                    // Track memory and performance
                    updatePeakMemory(localThis.stressMetrics, getMemoryUsage())
                    
                    // Periodic cleanup
                    if (operationCount % 500 == 0) {
                        System.gc()
                        delay(5)
                    }
                    
                    // Maintain operation rate
                    delay(operationInterval)
                    
                } catch (e: Exception) {
                    localThis.stressMetrics.errorCount.incrementAndGet()
                }
            }
        }
        
        val endTime = System.currentTimeMillis()
        val finalMemory = getMemoryUsage()
        
        // Then - system should maintain stability throughout
        val actualDuration = endTime - startTime
        val successRate = localThis.stressMetrics.successCount.get().toDouble() / operationCount
        val memoryIncrease = finalMemory - initialMemory
        val operationsPerSecondActual = operationCount.toDouble() / (actualDuration / 1000.0)
        
        assertThat(operationCount).isGreaterThan(0)
        assertThat(successRate).isAtLeast(0.90) // At least 90% success during prolonged stress
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024) // Memory should not grow excessively
        assertThat(operationsPerSecondActual).isGreaterThan(operationsPerSecond * 0.8) // Should maintain rate
        
        // System should still be responsive after stress
        localThis.mainScreenState.updatePrompt("Post-stress test")
        assertThat(localThis.mainScreenState.prompt).isEqualTo("Post-stress test")
    }
    
    // MARK: - Recovery and Graceful Degradation Tests
    
    @Test
    fun `System should recover gracefully from cascading failures`() = runTest {
        // Given - cascading failure scenario
        val localThis = this@StressTestSuite.localThis
        val failureScenarios = 20
        val operationsPerScenario = 50
        
        // When - simulating cascading failures with recovery
        repeat(failureScenarios) { scenarioIndex ->
            // Introduce different types of failures
            when (scenarioIndex % 4) {
                0 -> {
                    // AI repository failures
                    coEvery { localThis.mockAIRepository.generateContent(any(), any()) } throws 
                        RuntimeException("Simulated AI failure $scenarioIndex")
                }
                1 -> {
                    // Memory pressure
                    val memoryPressure = mutableListOf<ByteArray>()
                    repeat(100) { memoryPressure.add(ByteArray(1024 * 1024)) }
                    System.gc()
                }
                2 -> {
                    // Slow responses
                    coEvery { localThis.mockAIRepository.generateContent(any(), any()) } coAnswers {
                        delay(5000) // 5 second delay
                        AIResponseProcessingResult.Success("Slow response")
                    }
                }
                3 -> {
                    // Normal operation for recovery
                    coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
                        AIResponseProcessingResult.Success("Recovery response")
                }
            }
            
            // Perform operations under failure conditions
            val scenarioOperations = (1..operationsPerScenario).map { opIndex ->
                async {
                    try {
                        val bitmap = localThis.testBitmaps[opIndex % localThis.testBitmaps.size]
                        val result = withTimeout(6000) { // 6 second timeout
                            localThis.recipeRepository.analyzeRecipe(
                                bitmap, 
                                "Failure scenario $scenarioIndex-$opIndex"
                            ).first()
                        }
                        
                        localThis.stressMetrics.successCount.incrementAndGet()
                        
                    } catch (e: Exception) {
                        localThis.stressMetrics.errorCount.incrementAndGet()
                    }
                }
            }
            
            scenarioOperations.awaitAll()
            
            // Allow recovery time
            delay(100)
            System.gc()
        }
        
        // Then - system should demonstrate recovery capability
        val totalOperations = failureScenarios * operationsPerScenario
        val successRate = localThis.stressMetrics.successCount.get().toDouble() / totalOperations
        
        // Even with failures, some operations should succeed (recovery scenarios)
        assertThat(successRate).isAtLeast(0.20) // At least 20% should succeed through recovery
        
        // System should still be functional after cascading failures
        localThis.mainScreenState.updatePrompt("Post-failure test")
        assertThat(localThis.mainScreenState.prompt).isEqualTo("Post-failure test")
        assertThat(localThis.mainScreenState.isAnalyzeEnabled).isNotNull()
    }
    
    // MARK: - Helper Methods
    
    private fun createTestBitmaps(): List<Bitmap> {
        return listOf(
            Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(50, 200, Bitmap.Config.RGB_565),
            Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888)
        )
    }
    
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun updatePeakMemory(metrics: StressMetrics, currentMemory: Long) {
        var currentPeak = metrics.peakMemoryUsage.get()
        while (currentMemory > currentPeak) {
            if (metrics.peakMemoryUsage.compareAndSet(currentPeak, currentMemory)) {
                break
            }
            currentPeak = metrics.peakMemoryUsage.get()
        }
    }
    
    private fun createTestRecipe(title: String, content: String): Recipe {
        return Recipe(
            id = "stress_test_${System.currentTimeMillis()}_${Random.nextInt()}",
            title = title,
            description = content,
            ingredients = listOf("ingredient1", "ingredient2"),
            instructions = listOf("step1", "step2"),
            source = RecipeSource.AI_GENERATED,
            createdAt = Date()
        )
    }
    
    private fun createTestResponse(recipe: Recipe): RecipeAnalysisResponse {
        return RecipeAnalysisResponse(
            recipe = recipe,
            rawResponse = "stress_test_response",
            processingTime = Random.nextLong(50, 200),
            success = true
        )
    }
} 
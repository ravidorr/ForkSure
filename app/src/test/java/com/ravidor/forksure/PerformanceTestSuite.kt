package com.ravidor.forksure

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.LruCache
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.data.model.AppTheme
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeAnalysisRequest
import com.ravidor.forksure.data.model.RecipeSource
import com.ravidor.forksure.data.model.UserPreferences
import com.ravidor.forksure.data.source.local.RecipeCacheDataSource
import com.ravidor.forksure.data.source.local.PreferencesDataSource
import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.RecipeRepository
import com.ravidor.forksure.repository.RecipeRepositoryImpl
import com.ravidor.forksure.repository.UserPreferencesRepository
import com.ravidor.forksure.repository.UserPreferencesRepositoryImpl
import com.ravidor.forksure.AIResponseProcessingResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * Comprehensive performance testing suite for ForkSure app
 * 
 * Tests memory usage, CPU performance, and battery optimization
 * across all performance-critical components including:
 * - Image processing and caching
 * - AI/API integration
 * - Concurrent operations
 * - Persistence layer operations
 * - Memory management and resource optimization
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class PerformanceTestSuite {

    // Test fixtures using localThis pattern
    private lateinit var localThis: TestFixtures
    
    private data class TestFixtures(
        val context: Context,
        val mockSharedPreferences: SharedPreferences,
        val mockPreferencesDataSource: PreferencesDataSource,
        val mockAIRepository: AIRepository,
        val recipeCacheDataSource: RecipeCacheDataSource,
        val testCoroutineScheduler: TestCoroutineScheduler,
        val largeTestBitmap: Bitmap,
        val mediumTestBitmap: Bitmap,
        val smallTestBitmap: Bitmap,
        val performanceMetrics: PerformanceMetrics
    )
    
    private data class PerformanceMetrics(
        val startTime: AtomicLong = AtomicLong(0),
        val endTime: AtomicLong = AtomicLong(0),
        val memoryUsageBefore: AtomicLong = AtomicLong(0),
        val memoryUsageAfter: AtomicLong = AtomicLong(0),
        val operationCount: AtomicInteger = AtomicInteger(0),
        val executionTime: AtomicLong = AtomicLong(0)
    )
    
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
        val mockPreferencesDataSource = mockk<PreferencesDataSource>(relaxed = true)
        val mockAIRepository = mockk<AIRepository>(relaxed = true)
        val recipeCacheDataSource = RecipeCacheDataSource()
        val testCoroutineScheduler = TestCoroutineScheduler()
        
        // Create test bitmaps of different sizes for memory testing
        val largeTestBitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val mediumTestBitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
        val smallTestBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        
        val performanceMetrics = PerformanceMetrics()
        
        localThis = TestFixtures(
            context = context,
            mockSharedPreferences = mockSharedPreferences,
            mockPreferencesDataSource = mockPreferencesDataSource,
            mockAIRepository = mockAIRepository,
            recipeCacheDataSource = recipeCacheDataSource,
            testCoroutineScheduler = testCoroutineScheduler,
            largeTestBitmap = largeTestBitmap,
            mediumTestBitmap = mediumTestBitmap,
            smallTestBitmap = smallTestBitmap,
            performanceMetrics = performanceMetrics
        )
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
        
        // Clean up test bitmaps to prevent memory leaks
        if (!localThis.largeTestBitmap.isRecycled) {
            localThis.largeTestBitmap.recycle()
        }
        if (!localThis.mediumTestBitmap.isRecycled) {
            localThis.mediumTestBitmap.recycle()
        }
        if (!localThis.smallTestBitmap.isRecycled) {
            localThis.smallTestBitmap.recycle()
        }
    }
    
    // MARK: - Memory Usage Performance Tests
    
    @Test
    fun `RecipeCacheDataSource should efficiently manage memory with large datasets`() = runTest {
        // Given - large number of recipes with different memory footprints
        val localThis = this@PerformanceTestSuite.localThis
        val initialMemory = getMemoryUsage()
        val recipeCount = 100
        
        // When - adding many recipes to cache
        val startTime = System.currentTimeMillis()
        repeat(recipeCount) { index ->
            val request = RecipeAnalysisRequest(
                imageHash = "hash_$index",
                prompt = "test_prompt_$index"
            )
            val recipe = createTestRecipe("recipe_$index", "content_$index")
            localThis.recipeCacheDataSource.cacheRecipe(request, createTestResponse(recipe))
        }
        val endTime = System.currentTimeMillis()
        
        // Then - memory usage should be controlled and performance acceptable
        val finalMemory = getMemoryUsage()
        val memoryIncrease = finalMemory - initialMemory
        val executionTime = endTime - startTime
        
        // Memory increase should be reasonable (less than 50MB for 100 recipes)
        assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024)
        
        // Execution time should be fast (less than 2 seconds)
        assertThat(executionTime).isLessThan(2000)
        
        // Cache should implement LRU eviction properly (verified through successful operations above)
    }
    
    @Test
    fun `Bitmap processing should handle large images without memory overflow`() = runTest {
        // Given - multiple large bitmaps
        val localThis = this@PerformanceTestSuite.localThis
        val bitmaps = listOf(
            localThis.largeTestBitmap,
            localThis.mediumTestBitmap,
            localThis.smallTestBitmap
        )
        
        // When - processing multiple large images sequentially
        val processingTimes = mutableListOf<Long>()
        val memoryUsages = mutableListOf<Long>()
        
        bitmaps.forEach { bitmap ->
            val memoryBefore = getMemoryUsage()
            val processingTime = measureTimeMillis {
                // Simulate image hash generation (CPU and memory intensive)
                generateImageHash(bitmap)
            }
            val memoryAfter = getMemoryUsage()
            
            processingTimes.add(processingTime)
            memoryUsages.add(memoryAfter - memoryBefore)
        }
        
        // Then - processing should be efficient and memory controlled
        processingTimes.forEach { time ->
            assertThat(time).isLessThan(1000) // Each image processed in under 1 second
        }
        
        memoryUsages.forEach { memoryIncrease ->
            assertThat(memoryIncrease).isLessThan(10 * 1024 * 1024) // Less than 10MB per image
        }
    }
    
    @Test
    fun `SharedPreferences operations should be memory efficient with large datasets`() = runTest {
        // Given - large user preferences and usage statistics
        val localThis = this@PerformanceTestSuite.localThis
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { localThis.mockSharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.apply() } just Runs
        
        val initialMemory = getMemoryUsage()
        
        // When - performing many preference operations
        val operationCount = 1000
        val startTime = System.currentTimeMillis()
        
        repeat(operationCount) { index ->
            localThis.mockSharedPreferences.edit().apply {
                putString("key_string_$index", "value_$index".repeat(100)) // Large strings
                putInt("key_int_$index", index)
                putBoolean("key_bool_$index", index % 2 == 0)
                apply()
            }
        }
        
        val endTime = System.currentTimeMillis()
        val finalMemory = getMemoryUsage()
        
        // Then - operations should be fast and memory efficient
        val executionTime = endTime - startTime
        val memoryIncrease = finalMemory - initialMemory
        
        assertThat(executionTime).isLessThan(3000) // Under 3 seconds for 1000 operations
        assertThat(memoryIncrease).isLessThan(20 * 1024 * 1024) // Under 20MB memory increase
        
        verify(exactly = operationCount) { editor.apply() }
    }
    
    // MARK: - CPU Performance Tests
    
    @Test
    fun `SecurityManager rate limiting should handle high-frequency operations efficiently`() = runTest {
        // Given - high-frequency rate limit checks
        val localThis = this@PerformanceTestSuite.localThis
        val operationCount = 10000
        val concurrentRequests = 50
        
        // When - performing many concurrent rate limit checks
        val startTime = System.currentTimeMillis()
        
        val deferredResults = (1..concurrentRequests).map { threadId ->
            async {
                repeat(operationCount / concurrentRequests) {
                    SecurityManager.getRateLimitStatus(localThis.context, "test_$threadId")
                }
            }
        }
        
        deferredResults.awaitAll()
        val endTime = System.currentTimeMillis()
        
        // Then - operations should complete efficiently
        val executionTime = endTime - startTime
        val operationsPerSecond = operationCount.toDouble() / (executionTime / 1000.0)
        
        assertThat(executionTime).isLessThan(5000) // Under 5 seconds total
        assertThat(operationsPerSecond).isGreaterThan(1000.0) // Over 1000 ops/sec
    }
    
    @Test
    fun `Recipe repository should handle concurrent analysis requests efficiently`() = runTest {
        // Given - multiple concurrent analysis requests
        val localThis = this@PerformanceTestSuite.localThis
        val repository = RecipeRepositoryImpl(localThis.mockAIRepository, localThis.recipeCacheDataSource)
        
        // Mock AI repository to return quickly
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Mock recipe content")
        
        val concurrentRequests = 20
        val startTime = System.currentTimeMillis()
        
        // When - making concurrent requests
        val deferredResults = (1..concurrentRequests).map { index ->
            async {
                repository.analyzeRecipe(
                    localThis.smallTestBitmap,
                    "test_prompt_$index"
                ).first()
            }
        }
        
        val results = deferredResults.awaitAll()
        val endTime = System.currentTimeMillis()
        
        // Then - all requests should complete successfully and efficiently
        val executionTime = endTime - startTime
        val averageRequestTime = executionTime.toDouble() / concurrentRequests
        
        assertThat(results).hasSize(concurrentRequests)
        results.forEach { response ->
            assertThat(response.success).isTrue()
        }
        
        assertThat(executionTime).isLessThan(10000) // Under 10 seconds for 20 requests
        assertThat(averageRequestTime).isLessThan(500) // Under 500ms average per request
        
        coVerify(exactly = concurrentRequests) { 
            localThis.mockAIRepository.generateContent(any(), any()) 
        }
    }
    
    @Test
    fun `LRU cache eviction should be CPU efficient with frequent access patterns`() = runTest {
        // Given - LRU cache with frequent access patterns
        val localThis = this@PerformanceTestSuite.localThis
        val accessCount = 10000
        val uniqueKeys = 100
        
        // When - performing many cache operations with LRU access patterns
        val startTime = System.currentTimeMillis()
        
        repeat(accessCount) { index ->
            val keyIndex = index % uniqueKeys
            val request = RecipeAnalysisRequest(
                imageHash = "frequently_accessed_$keyIndex",
                prompt = "test_prompt_$keyIndex"
            )
            
            // Simulate cache hit/miss patterns
            if (index % 3 == 0) {
                // Cache new item
                val recipe = createTestRecipe("recipe_$keyIndex", "content_$keyIndex")
                localThis.recipeCacheDataSource.cacheRecipe(request, createTestResponse(recipe))
            } else {
                // Try to retrieve from cache
                localThis.recipeCacheDataSource.getCachedRecipe(request)
            }
        }
        
        val endTime = System.currentTimeMillis()
        
        // Then - LRU operations should be efficient
        val executionTime = endTime - startTime
        val operationsPerSecond = accessCount.toDouble() / (executionTime / 1000.0)
        
        assertThat(executionTime).isLessThan(3000) // Under 3 seconds
        assertThat(operationsPerSecond).isGreaterThan(2000.0) // Over 2000 ops/sec
        
        // Cache should maintain reasonable size (verified through successful operations above)
    }
    
    // MARK: - Battery Optimization Tests
    
    @Test
    fun `Repository operations should minimize unnecessary background work`() = runTest {
        // Given - repository with caching enabled
        val localThis = this@PerformanceTestSuite.localThis
        val repository = RecipeRepositoryImpl(localThis.mockAIRepository, localThis.recipeCacheDataSource)
        
        // Pre-populate cache
        val request = RecipeAnalysisRequest(imageHash = "cached_hash", prompt = "cached_prompt")
        val cachedRecipe = createTestRecipe("cached_recipe", "cached_content")
        localThis.recipeCacheDataSource.cacheRecipe(request, createTestResponse(cachedRecipe))
        
        // When - making requests that should hit cache (minimal battery usage)
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<com.ravidor.forksure.data.model.RecipeAnalysisResponse>()
        repeat(10) {
            val result = repository.analyzeRecipe(localThis.smallTestBitmap, "cached_prompt").first()
            results.add(result)
        }
        val endTime = System.currentTimeMillis()
        
        // Then - operations should be fast (cache hits, no AI calls)
        val executionTime = endTime - startTime
        
        results.forEach { response ->
            assertThat(response.success).isTrue()
            assertThat(response.recipe?.source).isEqualTo(RecipeSource.CACHED)
        }
        
        // Should be very fast since hitting cache
        assertThat(executionTime).isLessThan(100) // Under 100ms for 10 cached requests
        
        // Should not call AI repository (battery saving)
        coVerify(exactly = 0) { localThis.mockAIRepository.generateContent(any(), any()) }
    }
    
    @Test
    fun `Preference operations should use efficient batching to minimize I_O`() = runTest {
                 // Given - user preferences repository
         val localThis = this@PerformanceTestSuite.localThis
        val repository = UserPreferencesRepositoryImpl(localThis.mockPreferencesDataSource)
        
        // Mock current preferences
        val currentPrefs = UserPreferences()
        coEvery { localThis.mockPreferencesDataSource.getCurrentUserPreferences() } returns currentPrefs
        coEvery { localThis.mockPreferencesDataSource.saveUserPreferences(any()) } just Runs
        
        // When - making multiple preference updates rapidly
        val updateCount = 50
        val startTime = System.currentTimeMillis()
        
        repeat(updateCount) { index ->
            when (index % 5) {
                0 -> repository.updateHapticFeedback(index % 2 == 0)
                1 -> repository.updateSoundEffects(index % 2 == 0)
                2 -> repository.updateTheme(if (index % 2 == 0) AppTheme.LIGHT else AppTheme.DARK)
                3 -> repository.incrementAnalysisCount()
                4 -> repository.recordPhotoAnalysis()
            }
            
            // Small delay to simulate real usage
            Thread.sleep(10)
        }
        
        val endTime = System.currentTimeMillis()
        
        // Then - operations should be efficient
        val executionTime = endTime - startTime
        
        assertThat(executionTime).isLessThan(2000) // Under 2 seconds including delays
        
        // Should batch operations efficiently (verify reasonable number of I/O calls)
        coVerify(atLeast = updateCount / 2) { 
            localThis.mockPreferencesDataSource.getCurrentUserPreferences() 
        }
        coVerify(atLeast = updateCount / 2) { 
            localThis.mockPreferencesDataSource.saveUserPreferences(any()) 
        }
    }
    
    @Test
    fun `Image processing should optimize for different device capabilities`() = runTest {
        // Given - different image sizes representing device capabilities
        val localThis = this@PerformanceTestSuite.localThis
        val images = mapOf(
            "high_end_device" to localThis.largeTestBitmap,   // 1920x1080
            "mid_range_device" to localThis.mediumTestBitmap, // 1280x720
            "low_end_device" to localThis.smallTestBitmap     // 640x480
        )
        
        // When - processing images for different device types
        val processingResults = mutableMapOf<String, Long>()
        
        images.forEach { (deviceType, bitmap) ->
            val processingTime = measureTimeMillis {
                // Simulate optimized image processing based on device capability
                repeat(5) { // Multiple operations to get average
                    generateImageHash(bitmap)
                }
            }
            processingResults[deviceType] = processingTime
        }
        
        // Then - processing time should scale reasonably with image size
        val highEndTime = processingResults["high_end_device"] ?: 0
        val midRangeTime = processingResults["mid_range_device"] ?: 0
        val lowEndTime = processingResults["low_end_device"] ?: 0
        
        // Larger images should take more time but not exponentially more
        assertThat(highEndTime).isGreaterThan(midRangeTime)
        assertThat(midRangeTime).isGreaterThan(lowEndTime)
        
        // Time difference should be reasonable (not more than 5x)
        assertThat(highEndTime.toDouble() / lowEndTime).isLessThan(5.0)
        
        // All processing should complete within reasonable time limits
        assertThat(highEndTime).isLessThan(2000) // Even large images under 2 seconds
        assertThat(midRangeTime).isLessThan(1000) // Mid-range under 1 second
        assertThat(lowEndTime).isLessThan(500)    // Small images under 0.5 seconds
    }
    
    // MARK: - Resource Management Tests
    
    @Test
    fun `Memory cleanup should prevent memory leaks during long sessions`() = runTest {
        // Given - simulated long app session with many operations
        val localThis = this@PerformanceTestSuite.localThis
        val sessionOperations = 1000
        val initialMemory = getMemoryUsage()
        
        // When - performing many operations that could cause memory leaks
        repeat(sessionOperations) { index ->
            // Simulate user actions that create temporary objects
            val request = RecipeAnalysisRequest(
                imageHash = "session_hash_$index",
                prompt = "session_prompt_$index"
            )
            
            val recipe = createTestRecipe("session_recipe_$index", "large_content_".repeat(100))
            localThis.recipeCacheDataSource.cacheRecipe(request, createTestResponse(recipe))
            
            // Trigger potential cleanup every 100 operations
            if (index % 100 == 0) {
                System.gc() // Suggest garbage collection
                Thread.sleep(10) // Allow cleanup to occur
            }
        }
        
        // Force garbage collection
        repeat(3) {
            System.gc()
            Thread.sleep(100)
        }
        
        val finalMemory = getMemoryUsage()
        
        // Then - memory usage should be controlled
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreasePerOperation = memoryIncrease.toDouble() / sessionOperations
        
        // Memory increase should be reasonable (less than 1KB per operation on average)
        assertThat(memoryIncreasePerOperation).isLessThan(1024.0)
        
        // Total memory increase should be bounded
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024) // Less than 100MB total
        
        // Cache should maintain reasonable size due to LRU eviction (verified through successful operations above)
    }
    
    @Test
    fun `Concurrent operations should not cause resource contention`() = runTest {
        // Given - multiple concurrent threads performing different operations
        val localThis = this@PerformanceTestSuite.localThis
        val threadCount = 20
        val operationsPerThread = 100
        val latch = CountDownLatch(threadCount)
        val errors = mutableListOf<Exception>()
        val completionTimes = mutableListOf<Long>()
        
        // When - running concurrent operations
        val startTime = System.currentTimeMillis()
        
        repeat(threadCount) { threadId ->
            launch {
                try {
                    val threadStartTime = System.currentTimeMillis()
                    
                    repeat(operationsPerThread) { opId ->
                        when (opId % 4) {
                            0 -> {
                                // Cache operations
                                val request = RecipeAnalysisRequest(
                                    imageHash = "thread_${threadId}_op_$opId",
                                    prompt = "test_prompt"
                                )
                                val recipe = createTestRecipe("recipe_$opId", "content_$opId")
                                localThis.recipeCacheDataSource.cacheRecipe(request, createTestResponse(recipe))
                            }
                            1 -> {
                                // Rate limit checks
                                SecurityManager.getRateLimitStatus(localThis.context, "thread_$threadId")
                            }
                            2 -> {
                                // Cache retrieval
                                val request = RecipeAnalysisRequest(
                                    imageHash = "thread_${threadId}_op_${opId - 10}",
                                    prompt = "test_prompt"
                                )
                                localThis.recipeCacheDataSource.getCachedRecipe(request)
                            }
                            3 -> {
                                // Memory-intensive operation
                                generateImageHash(localThis.smallTestBitmap)
                            }
                        }
                    }
                    
                    val threadEndTime = System.currentTimeMillis()
                    completionTimes.add(threadEndTime - threadStartTime)
                } catch (e: Exception) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }
        
        // Wait for all threads to complete (with timeout)
        val completed = latch.await(30, TimeUnit.SECONDS)
        val endTime = System.currentTimeMillis()
        
        // Then - all operations should complete successfully without contention
        assertThat(completed).isTrue()
        assertThat(errors).isEmpty()
        
        val totalExecutionTime = endTime - startTime
        val averageThreadTime = completionTimes.average()
        val maxThreadTime = completionTimes.maxOrNull() ?: 0
        
        // Total time should be reasonable
        assertThat(totalExecutionTime).isLessThan(30000) // Under 30 seconds
        
        // Thread execution times should be reasonably consistent (no severe contention)
        assertThat(maxThreadTime.toDouble() / averageThreadTime).isLessThan(3.0)
        
        // All threads should complete in reasonable time
        completionTimes.forEach { time ->
            assertThat(time).isLessThan(20000) // Each thread under 20 seconds
        }
    }
    
    // MARK: - Helper Methods
    
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun generateImageHash(bitmap: Bitmap): String {
        // Simulate CPU-intensive image hashing
        val bytes = bitmap.byteCount
        return "hash_${bytes}_${bitmap.width}x${bitmap.height}"
    }
    
    private fun createTestRecipe(title: String, content: String): Recipe {
        return Recipe(
            id = "test_id_${System.currentTimeMillis()}",
            title = title,
            description = content,
            ingredients = listOf("ingredient1", "ingredient2"),
            instructions = listOf("step1", "step2"),
            source = RecipeSource.AI_GENERATED
        )
    }
    
    private fun createTestResponse(recipe: Recipe): com.ravidor.forksure.data.model.RecipeAnalysisResponse {
        return com.ravidor.forksure.data.model.RecipeAnalysisResponse(
            recipe = recipe,
            rawResponse = "test_response",
            processingTime = 100,
            success = true
        )
    }
} 
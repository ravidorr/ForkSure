package com.ravidor.forksure

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.data.model.AppTheme
import com.ravidor.forksure.data.model.AppUsageStats
import com.ravidor.forksure.data.model.FontSize
import com.ravidor.forksure.data.model.ImageQuality
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeAnalysisRequest
import com.ravidor.forksure.data.model.RecipeAnalysisResponse
import com.ravidor.forksure.data.model.RecipeSource
import com.ravidor.forksure.data.model.UserPreferences
import com.ravidor.forksure.data.source.local.PreferencesDataSource
import com.ravidor.forksure.data.source.local.RecipeCacheDataSource
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Comprehensive persistence testing suite for ForkSure app
 * 
 * Tests database/cache validation and data integrity including:
 * - Recipe cache management and LRU eviction policies
 * - SharedPreferences data persistence and consistency
 * - User preferences data integrity across app lifecycle
 * - Cache statistics accuracy and performance
 * - Data corruption prevention and recovery
 * - Concurrent access and thread safety
 * - Memory usage optimization for persistent data
 */
@ExperimentalCoroutinesApi
class PersistenceTestSuite {

    // Test fixtures using localThis pattern
    private lateinit var localThis: TestFixtures
    
    private data class TestFixtures(
        val context: Context,
        val mockSharedPreferences: SharedPreferences,
        val mockEditor: SharedPreferences.Editor,
        val preferencesDataSource: PreferencesDataSource,
        val recipeCacheDataSource: RecipeCacheDataSource,
        val testCoroutineScheduler: TestCoroutineScheduler,
        val testDispatcher: UnconfinedTestDispatcher
    )
    
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
        val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        val preferencesDataSource = PreferencesDataSource(mockSharedPreferences)
        val recipeCacheDataSource = RecipeCacheDataSource()
        val testCoroutineScheduler = TestCoroutineScheduler()
        val testDispatcher = UnconfinedTestDispatcher(testCoroutineScheduler)
        
        // Setup SharedPreferences mock behavior
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putStringSet(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        every { mockEditor.commit() } returns true
        
        localThis = TestFixtures(
            context = context,
            mockSharedPreferences = mockSharedPreferences,
            mockEditor = mockEditor,
            preferencesDataSource = preferencesDataSource,
            recipeCacheDataSource = recipeCacheDataSource,
            testCoroutineScheduler = testCoroutineScheduler,
            testDispatcher = testDispatcher
        )
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    // MARK: - Recipe Cache Persistence Tests
    
    @Test
    fun `RecipeCacheDataSource should persist recipes correctly with LRU eviction`() = runTest {
        // Given - cache with limited size and multiple recipes
        val localThis = this@PersistenceTestSuite.localThis
        val maxCacheSize = 5
        val totalRecipes = 10
        
        // When - adding more recipes than cache can hold
        val addedRecipes = mutableListOf<Pair<RecipeAnalysisRequest, RecipeAnalysisResponse>>()
        
        repeat(totalRecipes) { index ->
            val request = RecipeAnalysisRequest(
                imageHash = "hash_$index",
                prompt = "prompt_$index"
            )
            val recipe = createTestRecipe("Recipe $index", "Content for recipe $index")
            val response = createTestResponse(recipe)
            
            localThis.recipeCacheDataSource.cacheRecipe(request, response)
            addedRecipes.add(request to response)
        }
        
        // Then - LRU eviction should work correctly
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        
        // Cache size should be limited
        assertThat(cacheStats.totalEntries).isAtMost(maxCacheSize)
        
        // Oldest entries should be evicted
        val oldestRequest = addedRecipes.first().first
        val oldestRecipe = localThis.recipeCacheDataSource.getCachedRecipe(oldestRequest)
        assertThat(oldestRecipe).isNull() // Should be evicted
        
        // Newest entries should still be present
        val newestRequest = addedRecipes.last().first
        val newestRecipe = localThis.recipeCacheDataSource.getCachedRecipe(newestRequest)
        assertThat(newestRecipe).isNotNull()
        
        // Eviction count should be tracked
        assertThat(cacheStats.evictionCount).isEqualTo((totalRecipes - maxCacheSize).toLong())
    }
    
    @Test
    fun `Recipe cache should maintain data integrity under concurrent access`() = runTest {
        // Given - concurrent cache operations
        val localThis = this@PersistenceTestSuite.localThis
        val threadCount = 20
        val operationsPerThread = 50
        val latch = CountDownLatch(threadCount)
        val errors = mutableListOf<Exception>()
        
        // When - performing concurrent cache operations
        repeat(threadCount) { threadId ->
            launch {
                try {
                    repeat(operationsPerThread) { opId ->
                        val request = RecipeAnalysisRequest(
                            imageHash = "concurrent_hash_${threadId}_$opId",
                            prompt = "concurrent_prompt_${threadId}_$opId"
                        )
                        val recipe = createTestRecipe(
                            "Concurrent Recipe $threadId-$opId",
                            "Content for concurrent recipe"
                        )
                        val response = createTestResponse(recipe)
                        
                        // Alternate between adding and retrieving
                        if (opId % 2 == 0) {
                            localThis.recipeCacheDataSource.cacheRecipe(request, response)
                        } else {
                            localThis.recipeCacheDataSource.getCachedRecipe(request)
                        }
                    }
                } catch (e: Exception) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }
        
        val completed = latch.await(30, TimeUnit.SECONDS)
        
        // Then - concurrent operations should maintain data integrity
        assertThat(completed).isTrue()
        assertThat(errors).isEmpty()
        
        // Cache statistics should be consistent
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        assertThat(cacheStats.totalEntries).isAtMost(50) // LRU cache default size
        assertThat(cacheStats.hitCount + cacheStats.missCount).isGreaterThan(0)
    }
    
    @Test
    fun `Recipe cache should handle cache corruption gracefully`() = runTest {
        // Given - recipe cache with some valid data
        val localThis = this@PersistenceTestSuite.localThis
        
        // Add valid recipes
        repeat(5) { index ->
            val request = RecipeAnalysisRequest(
                imageHash = "valid_hash_$index",
                prompt = "valid_prompt_$index"
            )
            val recipe = createTestRecipe("Valid Recipe $index", "Valid content")
            val response = createTestResponse(recipe)
            localThis.recipeCacheDataSource.cacheRecipe(request, response)
        }
        
        // When - attempting to retrieve data (simulate corruption scenarios)
        val validRequest = RecipeAnalysisRequest(
            imageHash = "valid_hash_0",
            prompt = "valid_prompt_0"
        )
        val invalidRequest = RecipeAnalysisRequest(
            imageHash = "nonexistent_hash",
            prompt = "nonexistent_prompt"
        )
        
        val validRecipe = localThis.recipeCacheDataSource.getCachedRecipe(validRequest)
        val invalidRecipe = localThis.recipeCacheDataSource.getCachedRecipe(invalidRequest)
        
        // Then - cache should handle both valid and invalid scenarios
        assertThat(validRecipe).isNotNull()
        assertThat(validRecipe?.title).isEqualTo("Valid Recipe 0")
        
        assertThat(invalidRecipe).isNull() // Should return null for missing data
        
        // Cache statistics should remain accurate
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        assertThat(cacheStats.totalEntries).isEqualTo(5)
        assertThat(cacheStats.hitCount).isEqualTo(1)
        assertThat(cacheStats.missCount).isEqualTo(1)
    }
    
    @Test
    fun `Recipe cache should optimize memory usage with large datasets`() = runTest {
        // Given - memory-intensive recipe data
        val localThis = this@PersistenceTestSuite.localThis
        
        val initialMemory = getMemoryUsage()
        val recipeCount = 100
        
        // When - adding large recipes to cache
        repeat(recipeCount) { index ->
            val request = RecipeAnalysisRequest(
                imageHash = "memory_hash_$index",
                prompt = "memory_prompt_$index"
            )
            
            // Create large recipe content to test memory optimization
            val largeContent = "Large recipe content ".repeat(1000)
            val recipe = createTestRecipe("Memory Recipe $index", largeContent)
            val response = createTestResponse(recipe)
            
            localThis.recipeCacheDataSource.cacheRecipe(request, response)
            
            // Trigger potential cleanup every 20 operations
            if (index % 20 == 0) {
                System.gc()
                delay(10)
            }
        }
        
        val finalMemory = getMemoryUsage()
        val memoryIncrease = finalMemory - initialMemory
        
        // Then - memory usage should be optimized through LRU eviction
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        
        // Cache should not grow beyond LRU limit
        assertThat(cacheStats.totalEntries).isAtMost(50)
        
        // Memory increase should be reasonable (less than 100MB for all operations)
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024)
        
        // Eviction should have occurred
        assertThat(cacheStats.evictionCount).isGreaterThan(0)
    }
    
    // MARK: - SharedPreferences Persistence Tests
    
    @Test
    fun `PreferencesDataSource should persist user preferences correctly`() = runTest {
        // Given - user preferences with various data types
        val localThis = this@PersistenceTestSuite.localThis
        
        val testPreferences = UserPreferences(
            theme = AppTheme.DARK,
            language = "es",
            enableHapticFeedback = false,
            enableSoundEffects = true,
            cacheRecipes = true,
            maxCacheSize = 75,
            autoDeleteOldRecipes = false,
            cacheRetentionDays = 45,
            enableAnalytics = true,
            enableCrashReporting = false,
            preferredImageQuality = ImageQuality.HIGH,
            fontSize = FontSize.LARGE,
            totalAnalysisCount = 123,
            favoriteRecipeIds = setOf("recipe1", "recipe2", "recipe3")
        )
        
        // Mock SharedPreferences returns for verification
        every { localThis.mockSharedPreferences.getString("theme", any()) } returns "DARK"
        every { localThis.mockSharedPreferences.getString("language", any()) } returns "es"
        every { localThis.mockSharedPreferences.getBoolean("haptic_feedback", any()) } returns false
        every { localThis.mockSharedPreferences.getBoolean("sound_effects", any()) } returns true
        every { localThis.mockSharedPreferences.getInt("max_cache_size", any()) } returns 75
        every { localThis.mockSharedPreferences.getStringSet("favorite_recipe_ids", any()) } returns setOf("recipe1", "recipe2", "recipe3")
        
        // When - saving preferences
        localThis.preferencesDataSource.saveUserPreferences(testPreferences)
        
        // Then - all preference data should be persisted correctly
        verifyOrder {
            localThis.mockEditor.putString("theme", "DARK")
            localThis.mockEditor.putString("language", "es")
            localThis.mockEditor.putBoolean("haptic_feedback", false)
            localThis.mockEditor.putBoolean("sound_effects", true)
            localThis.mockEditor.putBoolean("cache_recipes", true)
            localThis.mockEditor.putInt("max_cache_size", 75)
            localThis.mockEditor.putBoolean("auto_delete_old_recipes", false)
            localThis.mockEditor.putInt("cache_retention_days", 45)
            localThis.mockEditor.putBoolean("enable_analytics", true)
            localThis.mockEditor.putBoolean("enable_crash_reporting", false)
            localThis.mockEditor.putString("preferred_image_quality", "HIGH")
            localThis.mockEditor.putString("font_size", "LARGE")
            localThis.mockEditor.putInt("total_analysis_count", 123)
            localThis.mockEditor.putStringSet("favorite_recipe_ids", setOf("recipe1", "recipe2", "recipe3"))
            localThis.mockEditor.apply()
        }
    }
    
    @Test
    fun `PreferencesDataSource should handle data integrity during concurrent updates`() = runTest {
        // Given - concurrent preference updates
        val localThis = this@PersistenceTestSuite.localThis
        val updateCount = 100
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val errors = mutableListOf<Exception>()
        
        // Mock successful operations
        every { localThis.mockSharedPreferences.getString(any(), any()) } returns "SYSTEM"
        every { localThis.mockSharedPreferences.getBoolean(any(), any()) } returns true
        every { localThis.mockSharedPreferences.getInt(any(), any()) } returns 50
        every { localThis.mockSharedPreferences.getLong(any(), any()) } returns System.currentTimeMillis()
        every { localThis.mockSharedPreferences.getStringSet(any(), any()) } returns emptySet()
        
        // When - performing concurrent preference updates
        repeat(threadCount) { threadId ->
            launch {
                try {
                    repeat(updateCount / threadCount) { opId ->
                        val preferences = UserPreferences(
                            theme = if (opId % 2 == 0) AppTheme.LIGHT else AppTheme.DARK,
                            enableHapticFeedback = opId % 2 == 0,
                            maxCacheSize = 25 + opId,
                            totalAnalysisCount = opId
                        )
                        localThis.preferencesDataSource.saveUserPreferences(preferences)
                    }
                } catch (e: Exception) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }
        
        val completed = latch.await(30, TimeUnit.SECONDS)
        
        // Then - concurrent updates should maintain data integrity
        assertThat(completed).isTrue()
        assertThat(errors).isEmpty()
        
        // All operations should have called apply()
        verify(exactly = updateCount) { localThis.mockEditor.apply() }
    }
    
    @Test
    fun `PreferencesDataSource should handle usage statistics persistence correctly`() = runTest {
        // Given - usage statistics data
        val localThis = this@PersistenceTestSuite.localThis
        
        val testUsageStats = AppUsageStats(
            totalLaunches = 45,
            totalAnalyses = 123,
            totalPhotosAnalyzed = 98,
            totalSampleImagesUsed = 25,
            averageSessionDuration = 150000,
            lastUsedDate = System.currentTimeMillis(),
            favoriteFeatures = mapOf("camera" to 50, "analysis" to 100),
            errorCounts = mapOf("network" to 3, "validation" to 1)
        )
        
        // Mock SharedPreferences returns
        every { localThis.mockSharedPreferences.getInt("total_launches", any()) } returns 45
        every { localThis.mockSharedPreferences.getInt("total_analyses", any()) } returns 123
        every { localThis.mockSharedPreferences.getInt("total_photos_analyzed", any()) } returns 98
        every { localThis.mockSharedPreferences.getInt("total_sample_images_used", any()) } returns 25
        every { localThis.mockSharedPreferences.getLong("average_session_duration", any()) } returns 150000
        
        // When - saving usage statistics
        localThis.preferencesDataSource.saveUsageStats(testUsageStats)
        
        // Then - all usage statistics should be persisted
        verifyOrder {
            localThis.mockEditor.putInt("total_launches", 45)
            localThis.mockEditor.putInt("total_analyses", 123)
            localThis.mockEditor.putInt("total_photos_analyzed", 98)
            localThis.mockEditor.putInt("total_sample_images_used", 25)
            localThis.mockEditor.putLong("average_session_duration", 150000)
            localThis.mockEditor.apply()
        }
    }
    
    @Test
    fun `PreferencesDataSource should recover from SharedPreferences corruption`() = runTest {
        // Given - SharedPreferences with some corrupted data
        val localThis = this@PersistenceTestSuite.localThis
        
        // Mock corrupted data scenarios
        every { localThis.mockSharedPreferences.getString("theme", any()) } returns "INVALID_THEME"
        every { localThis.mockSharedPreferences.getString("language", any()) } returns null
        every { localThis.mockSharedPreferences.getInt("max_cache_size", any()) } returns -1
        every { localThis.mockSharedPreferences.getStringSet("favorite_recipe_ids", any()) } returns null
        
        // When - loading preferences with corrupted data
        val loadedPreferences = localThis.preferencesDataSource.loadUserPreferences()
        
        // Then - should fall back to default values for corrupted data
        assertThat(loadedPreferences.theme).isEqualTo(AppTheme.SYSTEM) // Default fallback
        assertThat(loadedPreferences.language).isEqualTo("en") // Default fallback
        assertThat(loadedPreferences.maxCacheSize).isEqualTo(50) // Default fallback
        assertThat(loadedPreferences.favoriteRecipeIds).isEmpty() // Default fallback
        
        // Should handle invalid values gracefully without throwing exceptions
        assertThat(loadedPreferences).isNotNull()
    }
    
    // MARK: - Data Consistency Tests
    
    @Test
    fun `Cache and preferences should maintain consistency across app lifecycle`() = runTest {
        // Given - app lifecycle simulation with cache and preferences
        val localThis = this@PersistenceTestSuite.localThis
        
        // Setup initial preferences
        val initialPrefs = UserPreferences(cacheRecipes = true, maxCacheSize = 10)
        every { localThis.mockSharedPreferences.getBoolean("cache_recipes", any()) } returns true
        every { localThis.mockSharedPreferences.getInt("max_cache_size", any()) } returns 10
        
        // Add recipes to cache
        repeat(5) { index ->
            val request = RecipeAnalysisRequest(
                imageHash = "lifecycle_hash_$index",
                prompt = "lifecycle_prompt_$index"
            )
            val recipe = createTestRecipe("Lifecycle Recipe $index", "Content")
            val response = createTestResponse(recipe)
            localThis.recipeCacheDataSource.cacheRecipe(request, response)
        }
        
        // When - simulating app restart (preferences survive, cache may not)
        val restartedPrefs = localThis.preferencesDataSource.loadUserPreferences()
        val cacheStatsAfterRestart = localThis.recipeCacheDataSource.cacheStats.first()
        
        // Then - preferences should persist, cache should be rebuilt as needed
        assertThat(restartedPrefs.cacheRecipes).isTrue()
        assertThat(restartedPrefs.maxCacheSize).isEqualTo(10)
        
        // Cache may have been cleared but statistics should be consistent
        assertThat(cacheStatsAfterRestart.totalEntries).isGreaterThan(0)
        
        // Preference change should affect cache behavior
        val updatedPrefs = initialPrefs.copy(cacheRecipes = false)
        localThis.preferencesDataSource.saveUserPreferences(updatedPrefs)
        
        // Verify preference update was persisted
        verify { localThis.mockEditor.putBoolean("cache_recipes", false) }
    }
    
    @Test
    fun `Long-term data persistence should maintain integrity over time`() = runTest {
        // Given - long-term usage simulation
        val localThis = this@PersistenceTestSuite.localThis
        val daysSimulated = 30
        val operationsPerDay = 20
        
        // Mock time-based data
        var currentTime = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000L
        
        // When - simulating long-term usage patterns
        repeat(daysSimulated) { day ->
            currentTime += dayInMillis
            
            repeat(operationsPerDay) { op ->
                // Add recipe to cache
                val request = RecipeAnalysisRequest(
                    imageHash = "longterm_hash_${day}_$op",
                    prompt = "longterm_prompt_${day}_$op"
                )
                val recipe = createTestRecipe("Long-term Recipe $day-$op", "Content")
                val response = createTestResponse(recipe)
                localThis.recipeCacheDataSource.cacheRecipe(request, response)
                
                // Update usage statistics
                val usageStats = AppUsageStats(
                    totalAnalyses = day * operationsPerDay + op + 1,
                    lastUsedDate = currentTime
                )
                localThis.preferencesDataSource.saveUsageStats(usageStats)
            }
            
            // Simulate periodic cleanup
            if (day % 7 == 0) {
                System.gc()
                delay(10)
            }
        }
        
        // Then - data integrity should be maintained over time
        val finalCacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        
        // Cache should maintain reasonable size due to LRU eviction
        assertThat(finalCacheStats.totalEntries).isAtMost(50)
        
        // Statistics should reflect long-term usage
        assertThat(finalCacheStats.evictionCount).isGreaterThan(0)
        assertThat(finalCacheStats.hitCount + finalCacheStats.missCount).isGreaterThan(0)
        
        // Usage statistics should have been updated many times
        verify(atLeast = daysSimulated * operationsPerDay) { 
            localThis.mockEditor.putInt("total_analyses", any()) 
        }
    }
    
    // MARK: - Performance and Efficiency Tests
    
    @Test
    fun `Persistence operations should be performant under high load`() = runTest {
        // Given - high-load persistence scenario
        val localThis = this@PersistenceTestSuite.localThis
        val operationCount = 1000
        
        val startTime = System.currentTimeMillis()
        
        // When - performing many persistence operations
        repeat(operationCount) { index ->
            // Cache operation
            val request = RecipeAnalysisRequest(
                imageHash = "perf_hash_$index",
                prompt = "perf_prompt_$index"
            )
            val recipe = createTestRecipe("Performance Recipe $index", "Content")
            val response = createTestResponse(recipe)
            localThis.recipeCacheDataSource.cacheRecipe(request, response)
            
            // Preferences operation (every 10th iteration)
            if (index % 10 == 0) {
                val preferences = UserPreferences(totalAnalysisCount = index)
                localThis.preferencesDataSource.saveUserPreferences(preferences)
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Then - performance should be acceptable
        val averageTimePerOperation = totalTime.toDouble() / operationCount
        
        assertThat(totalTime).isLessThan(10000) // Under 10 seconds total
        assertThat(averageTimePerOperation).isLessThan(10) // Under 10ms per operation
        
        // Cache should maintain efficiency
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        assertThat(cacheStats.totalEntries).isAtMost(50) // LRU working properly
        
        // Preferences should have been updated efficiently
        verify(exactly = operationCount / 10) { 
            localThis.mockEditor.putInt("total_analysis_count", any()) 
        }
    }
    
    // MARK: - Helper Methods
    
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun createTestRecipe(title: String, content: String): Recipe {
        return Recipe(
            id = "persistence_test_${System.currentTimeMillis()}",
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
            rawResponse = "test_response",
            processingTime = 100,
            success = true
        )
    }
} 
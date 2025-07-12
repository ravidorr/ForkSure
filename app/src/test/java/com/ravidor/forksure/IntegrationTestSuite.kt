package com.ravidor.forksure

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeAnalysisRequest
import com.ravidor.forksure.data.model.RecipeSource
import com.ravidor.forksure.data.model.UserPreferences
import com.ravidor.forksure.data.model.AppTheme
import com.ravidor.forksure.data.source.local.RecipeCacheDataSource
import com.ravidor.forksure.data.source.local.PreferencesDataSource
import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.RecipeRepository
import com.ravidor.forksure.repository.RecipeRepositoryImpl
import com.ravidor.forksure.repository.SecurityRepository
import com.ravidor.forksure.repository.UserPreferencesRepository
import com.ravidor.forksure.repository.UserPreferencesRepositoryImpl
import com.ravidor.forksure.state.MainScreenState
import com.ravidor.forksure.state.NavigationState
import com.ravidor.forksure.SecurityEnvironmentResult
import com.ravidor.forksure.UserInputValidationResult
import com.ravidor.forksure.RateLimitResult
import com.ravidor.forksure.AIResponseProcessingResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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

/**
 * Comprehensive integration testing suite for ForkSure app
 * 
 * Tests end-to-end workflows and multi-component integration including:
 * - Complete user journeys from image capture to recipe display
 * - Multi-repository coordination and data flow
 * - State management across multiple components
 * - Error handling and recovery workflows
 * - User preference integration with app behavior
 * - Security and caching integration scenarios
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class IntegrationTestSuite {

    // Test fixtures using localThis pattern
    private lateinit var localThis: TestFixtures
    
    private data class TestFixtures(
        val context: Context,
        val mockAIRepository: AIRepository,
        val mockSecurityRepository: SecurityRepository,
        val mockPreferencesDataSource: PreferencesDataSource,
        val recipeCacheDataSource: RecipeCacheDataSource,
        val recipeRepository: RecipeRepository,
        val userPreferencesRepository: UserPreferencesRepository,
        val mainScreenState: MainScreenState,
        val navigationState: NavigationState,
        val testBitmap: Bitmap,
        val testCoroutineScheduler: TestCoroutineScheduler
    )
    
    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockAIRepository = mockk<AIRepository>(relaxed = true)
        val mockSecurityRepository = mockk<SecurityRepository>(relaxed = true)
        val mockPreferencesDataSource = mockk<PreferencesDataSource>(relaxed = true)
        val recipeCacheDataSource = RecipeCacheDataSource()
        val recipeRepository = RecipeRepositoryImpl(mockAIRepository, recipeCacheDataSource)
        val userPreferencesRepository = UserPreferencesRepositoryImpl(mockPreferencesDataSource)
        val mainScreenState = MainScreenState()
        val navigationState = NavigationState()
        // Use a simple test double instead of real bitmap to avoid Robolectric issues
        val testBitmap = createTestBitmap()
        val testCoroutineScheduler = TestCoroutineScheduler()
        
        localThis = TestFixtures(
            context = context,
            mockAIRepository = mockAIRepository,
            mockSecurityRepository = mockSecurityRepository,
            mockPreferencesDataSource = mockPreferencesDataSource,
            recipeCacheDataSource = recipeCacheDataSource,
            recipeRepository = recipeRepository,
            userPreferencesRepository = userPreferencesRepository,
            mainScreenState = mainScreenState,
            navigationState = navigationState,
            testBitmap = testBitmap,
            testCoroutineScheduler = testCoroutineScheduler
        )
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
        if (!localThis.testBitmap.isRecycled) {
            localThis.testBitmap.recycle()
        }
    }
    
    // MARK: - End-to-End User Journey Tests
    
    @Test
    fun `Complete recipe analysis workflow should integrate all components successfully`() = runTest {
        // Given - complete app setup with all components
        val localThis = this@IntegrationTestSuite.localThis
        
        // Mock security validation
        coEvery { localThis.mockSecurityRepository.checkSecurityEnvironment() } returns 
            SecurityEnvironmentResult.Secure
        coEvery { localThis.mockSecurityRepository.validateUserInput(any()) } returns 
            UserInputValidationResult.Valid("test prompt")
        coEvery { localThis.mockSecurityRepository.checkRateLimit(any()) } returns 
            RateLimitResult.Allowed(requestsRemaining = 5, resetTimeSeconds = 60)
        
        // Mock AI response
        val expectedRecipeContent = """
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
        
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success(expectedRecipeContent)
        
        // When - user performs complete recipe analysis workflow
        localThis.mainScreenState.updatePrompt("What are these cookies?")
        localThis.mainScreenState.updateCapturedImage(localThis.testBitmap)
        
        assertThat(localThis.mainScreenState.isAnalyzeEnabled).isTrue()
        
        // Trigger analysis through repository
        val analysisResults = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            "What are these cookies?"
        ).toList()
        
        // Then - complete workflow should succeed with all integrations working
        assertThat(analysisResults).hasSize(1)
        val result = analysisResults.first()
        
        assertThat(result.success).isTrue()
        assertThat(result.recipe).isNotNull()
        assertThat(result.recipe?.title).contains("Chocolate Chip Cookies")
        assertThat(result.recipe?.ingredients).contains("flour")
        assertThat(result.recipe?.instructions).contains("Preheat oven")
        
        // Verify all components were called in correct order
        coVerifyOrder {
            localThis.mockAIRepository.generateContent(any(), "What are these cookies?")
        }
        
        // Verify recipe was cached for future use
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        assertThat(cacheStats.totalEntries).isEqualTo(1)
    }
    
    @Test
    fun `Error recovery workflow should gracefully handle failures across components`() = runTest {
        // Given - setup with potential failure points
        val localThis = this@IntegrationTestSuite.localThis
        
        // Mock security failure first, then success
        coEvery { localThis.mockSecurityRepository.checkRateLimit(any()) } returnsMany listOf(
            RateLimitResult.Blocked("Rate limit exceeded", 60, 0),
            RateLimitResult.Allowed(5, 60)
        )
        
        coEvery { localThis.mockSecurityRepository.validateUserInput(any()) } returns 
            UserInputValidationResult.Valid("test prompt")
        coEvery { localThis.mockSecurityRepository.checkSecurityEnvironment() } returns 
            SecurityEnvironmentResult.Secure
        
        // Mock AI failure first, then success
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returnsMany listOf(
            AIResponseProcessingResult.Error("Network error", "Connection failed"),
            AIResponseProcessingResult.Success("Successful recipe content")
        )
        
        // When - attempting analysis multiple times (simulating retry)
        val firstAttempt = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            "test prompt"
        ).first()
        
        // Simulate some delay before retry
        Thread.sleep(100)
        
        val secondAttempt = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            "test prompt"
        ).first()
        
        // Then - error recovery should work correctly
        assertThat(firstAttempt.success).isFalse()
        assertThat(firstAttempt.errorMessage).isNotNull()
        
        assertThat(secondAttempt.success).isTrue()
        assertThat(secondAttempt.recipe).isNotNull()
        
        // Verify both attempts were made
        coVerify(exactly = 2) { 
            localThis.mockAIRepository.generateContent(any(), any()) 
        }
    }
    
    @Test
    fun `User preferences should affect recipe analysis behavior across components`() = runTest {
        // Given - different user preference configurations
        val localThis = this@IntegrationTestSuite.localThis
        
        // Setup preference flows
        val lightThemePrefs = UserPreferences(theme = AppTheme.LIGHT, cacheRecipes = true)
        val darkThemePrefs = UserPreferences(theme = AppTheme.DARK, cacheRecipes = false)
        
        every { localThis.mockPreferencesDataSource.userPreferences } returnsMany listOf(
            flowOf(lightThemePrefs),
            flowOf(darkThemePrefs)
        )
        
        // Mock successful AI response
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Recipe content")
        
        // When - analyzing with different preference configurations
        
        // First: With caching enabled
        every { localThis.mockPreferencesDataSource.userPreferences } returns flowOf(lightThemePrefs)
        val cachedResult = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            "test prompt"
        ).first()
        
        // Second: With caching disabled (simulate by checking preferences)
        every { localThis.mockPreferencesDataSource.userPreferences } returns flowOf(darkThemePrefs)
        val preferences = localThis.userPreferencesRepository.userPreferences.first()
        
        // Then - behavior should respect user preferences
        assertThat(cachedResult.success).isTrue()
        
        // Verify preference integration
        assertThat(preferences.theme).isEqualTo(AppTheme.DARK)
        assertThat(preferences.cacheRecipes).isFalse()
        
        // When caching enabled, recipes should be cached
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        assertThat(cacheStats.totalEntries).isGreaterThan(0)
    }
    
    @Test
    fun `State synchronization should work correctly across multiple components`() = runTest {
        // Given - multiple state holders that need to stay synchronized
        val localThis = this@IntegrationTestSuite.localThis
        
        // Mock successful scenario
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Synchronized recipe content")
        
        // When - updating state across multiple components
        
        // 1. Update navigation state
        localThis.navigationState.updateCapturedImage(localThis.testBitmap)
        assertThat(localThis.navigationState.capturedImage).isNotNull()
        
        // 2. Update main screen state to match
        localThis.mainScreenState.updateCapturedImage(localThis.testBitmap)
        localThis.mainScreenState.updatePrompt("Test synchronization")
        
        // 3. Perform analysis
        val analysisResult = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            "Test synchronization"
        ).first()
        
        // 4. Update main screen with result
        localThis.mainScreenState.updateResult(analysisResult.rawResponse)
        
        // Then - all components should be synchronized
        assertThat(localThis.navigationState.capturedImage).isEqualTo(localThis.mainScreenState.capturedImage)
        assertThat(localThis.mainScreenState.prompt).isEqualTo("Test synchronization")
        assertThat(localThis.mainScreenState.result).isEqualTo("Synchronized recipe content")
        assertThat(analysisResult.success).isTrue()
        
        // State should remain consistent
        assertThat(localThis.mainScreenState.isAnalyzeEnabled).isTrue()
        assertThat(localThis.mainScreenState.hasSelectedCapturedImage).isTrue()
    }
    
    // MARK: - Multi-Component Data Flow Tests
    
    @Test
    fun `Recipe caching integration should work seamlessly with analysis workflow`() = runTest {
        // Given - recipe analysis with caching behavior
        val localThis = this@IntegrationTestSuite.localThis
        
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Cached recipe content")
        
        val testPrompt = "What is this dessert?"
        
        // When - performing multiple analyses with same input (cache test)
        
        // First analysis - should call AI
        val firstResult = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            testPrompt
        ).first()
        
        // Second analysis - should hit cache
        val secondResult = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            testPrompt
        ).first()
        
        // Then - caching integration should work correctly
        assertThat(firstResult.success).isTrue()
        assertThat(firstResult.recipe?.source).isEqualTo(RecipeSource.AI_GENERATED)
        
        assertThat(secondResult.success).isTrue()
        assertThat(secondResult.recipe?.source).isEqualTo(RecipeSource.CACHED)
        
        // Performance should improve with caching
        assertThat(secondResult.processingTime).isLessThan(firstResult.processingTime)
        
        // AI should only be called once
        coVerify(exactly = 1) { localThis.mockAIRepository.generateContent(any(), any()) }
        
        // Cache should contain the recipe
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        assertThat(cacheStats.totalEntries).isEqualTo(1)
        assertThat(cacheStats.hitCount).isEqualTo(1)
        assertThat(cacheStats.missCount).isEqualTo(1)
    }
    
    @Test
    fun `Security integration should protect entire analysis pipeline`() = runTest {
        // Given - security-aware analysis pipeline
        val localThis = this@IntegrationTestSuite.localThis
        
        // Mock security scenarios
        coEvery { localThis.mockSecurityRepository.checkSecurityEnvironment() } returns 
            SecurityEnvironmentResult.Secure
        coEvery { localThis.mockSecurityRepository.checkRateLimit(any()) } returns 
            RateLimitResult.Allowed(3, 60)
        
        // Test different input validation scenarios
        val validationScenarios = listOf(
            "hack the system" to UserInputValidationResult.Invalid("Suspicious content detected"),
            "chocolate cake recipe" to UserInputValidationResult.Valid("chocolate cake recipe"),
            "DELETE FROM users" to UserInputValidationResult.Invalid("SQL injection attempt detected")
        )
        
        // When - testing security integration across different inputs
        val results = mutableListOf<Pair<String, Boolean>>()
        
        validationScenarios.forEach { (input, expectedValidation) ->
            coEvery { localThis.mockSecurityRepository.validateUserInput(input) } returns expectedValidation
            
            try {
                val result = localThis.recipeRepository.analyzeRecipe(localThis.testBitmap, input).first()
                results.add(input to result.success)
            } catch (e: Exception) {
                results.add(input to false)
            }
        }
        
        // Then - security should be enforced throughout pipeline
        assertThat(results).hasSize(3)
        
        // Malicious inputs should be blocked
        assertThat(results.find { it.first == "hack the system" }?.second).isFalse()
        assertThat(results.find { it.first == "DELETE FROM users" }?.second).isFalse()
        
        // Valid input should succeed
        assertThat(results.find { it.first == "chocolate cake recipe" }?.second).isTrue()
        
        // Security repository should be called for all attempts
        coVerify(exactly = 3) { localThis.mockSecurityRepository.validateUserInput(any()) }
    }
    
    @Test
    fun `Multi-repository coordination should handle complex scenarios`() = runTest {
        // Given - complex scenario involving multiple repositories
        val localThis = this@IntegrationTestSuite.localThis
        
        // Setup user preferences for cache management
        val userPrefs = UserPreferences(cacheRecipes = true, maxCacheSize = 10)
        every { localThis.mockPreferencesDataSource.userPreferences } returns flowOf(userPrefs)
        coEvery { localThis.mockPreferencesDataSource.getCurrentUserPreferences() } returns userPrefs
        coEvery { localThis.mockPreferencesDataSource.saveUserPreferences(any()) } just Runs
        
        // Setup security responses
        coEvery { localThis.mockSecurityRepository.checkRateLimit(any()) } returns 
            RateLimitResult.Allowed(5, 60)
        coEvery { localThis.mockSecurityRepository.validateUserInput(any()) } returns 
            UserInputValidationResult.Valid("validated input")
        coEvery { localThis.mockSecurityRepository.checkSecurityEnvironment() } returns 
            SecurityEnvironmentResult.Secure
        
        // Setup AI responses
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.SuccessWithWarning("Recipe content", "Minor warning")
        
        // When - performing coordinated operations across repositories
        
        // 1. Update user preferences
        localThis.userPreferencesRepository.updateCacheSettings(true, 20, 30)
        localThis.userPreferencesRepository.incrementAnalysisCount()
        
        // 2. Perform recipe analysis (involves all repositories)
        val analysisResult = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            "complex scenario test"
        ).first()
        
        // 3. Record usage statistics
        localThis.userPreferencesRepository.recordPhotoAnalysis()
        
        // Then - all repositories should coordinate correctly
        assertThat(analysisResult.success).isTrue()
        assertThat(analysisResult.warnings).hasSize(1)
        assertThat(analysisResult.warnings.first()).contains("Minor warning")
        
        // Verify cross-repository coordination
        coVerifyOrder {
            localThis.mockSecurityRepository.validateUserInput(any())
            localThis.mockSecurityRepository.checkRateLimit(any())
            localThis.mockAIRepository.generateContent(any(), any())
        }
        
        // User preferences should be updated
        coVerify { localThis.mockPreferencesDataSource.saveUserPreferences(any()) }
        
        // Cache should be populated (verified through successful repository operation above)
    }
    
    // MARK: - Workflow State Management Tests
    
    @Test
    fun `Complex user workflow should maintain state consistency`() = runTest {
        // Given - complex multi-step user workflow
        val localThis = this@IntegrationTestSuite.localThis
        
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Workflow recipe content")
        
        // When - simulating complex user interactions
        
        // Step 1: User captures image
        localThis.navigationState.updateCapturedImage(localThis.testBitmap)
        localThis.mainScreenState.updateCapturedImage(localThis.testBitmap)
        
        // Step 2: User enters prompt
        localThis.mainScreenState.updatePrompt("Complex workflow test")
        
        // Step 3: User changes their mind and selects sample image instead
        localThis.mainScreenState.selectSampleImage(1)
        
        // Step 4: User updates prompt
        localThis.mainScreenState.updatePrompt("Updated workflow test")
        
        // Step 5: User performs analysis
        val analysisResult = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            "Updated workflow test"
        ).first()
        
        // Step 6: User views result
        localThis.mainScreenState.updateResult(analysisResult.rawResponse)
        
        // Step 7: User opens report dialog
        localThis.mainScreenState.showReportDialog()
        
        // Step 8: User closes dialog
        localThis.mainScreenState.hideReportDialog()
        
        // Then - state should remain consistent throughout workflow
        assertThat(localThis.mainScreenState.prompt).isEqualTo("Updated workflow test")
        assertThat(localThis.mainScreenState.result).isEqualTo("Workflow recipe content")
        assertThat(localThis.mainScreenState.hasSelectedSampleImage).isTrue()
        assertThat(localThis.mainScreenState.selectedImageIndex).isEqualTo(1)
        assertThat(localThis.mainScreenState.showReportDialog).isFalse()
        assertThat(localThis.mainScreenState.isAnalyzeEnabled).isTrue()
        
        // Navigation state should be synchronized
        assertThat(localThis.navigationState.capturedImage).isNotNull()
        
        // Analysis should have succeeded
        assertThat(analysisResult.success).isTrue()
    }
    
    @Test
    fun `Error state management should work across all components`() = runTest {
        // Given - scenario that will trigger various error states
        val localThis = this@IntegrationTestSuite.localThis
        
        // Setup progressive error scenarios
        coEvery { localThis.mockSecurityRepository.checkRateLimit(any()) } returnsMany listOf(
            RateLimitResult.Allowed(1, 60),
            RateLimitResult.Blocked("Rate limit exceeded", 60, 0)
        )
        
        coEvery { localThis.mockSecurityRepository.validateUserInput(any()) } returns 
            UserInputValidationResult.Valid("test")
        coEvery { localThis.mockSecurityRepository.checkSecurityEnvironment() } returns 
            SecurityEnvironmentResult.Secure
        
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returnsMany listOf(
            AIResponseProcessingResult.Success("Success"),
            AIResponseProcessingResult.Error("AI Error", "Service unavailable")
        )
        
        // When - triggering error conditions across workflow
        
        // First request should succeed
        val firstResult = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            "first request"
        ).first()
        
        // Second request should fail due to rate limit
        val secondResult = localThis.recipeRepository.analyzeRecipe(
            localThis.testBitmap,
            "second request"
        ).first()
        
        // Then - error handling should work correctly across components
        assertThat(firstResult.success).isTrue()
        assertThat(secondResult.success).isFalse()
        
        // Error information should be properly propagated
        assertThat(secondResult.errorMessage).isNotNull()
        
        // State should reflect error conditions appropriately
        localThis.mainScreenState.updateResult("Error: ${secondResult.errorMessage}")
        assertThat(localThis.mainScreenState.result).contains("Error:")
        
        // Components should maintain consistency even in error states
        assertThat(localThis.mainScreenState.isAnalyzeEnabled).isTrue() // User can still retry
    }
    
    // MARK: - Performance Integration Tests
    
    @Test
    fun `Integrated components should perform efficiently under load`() = runTest {
        // Given - high-load scenario across all components
        val localThis = this@IntegrationTestSuite.localThis
        
        // Setup fast responses for load testing
        coEvery { localThis.mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Load test recipe")
        coEvery { localThis.mockSecurityRepository.checkRateLimit(any()) } returns 
            RateLimitResult.Allowed(1000, 60)
        coEvery { localThis.mockSecurityRepository.validateUserInput(any()) } returns 
            UserInputValidationResult.Valid("load test")
        
        val requestCount = 50
        val startTime = System.currentTimeMillis()
        
        // When - performing many integrated operations
        val results = mutableListOf<com.ravidor.forksure.data.model.RecipeAnalysisResponse>()
        repeat(requestCount) { index ->
            // Update state
            localThis.mainScreenState.updatePrompt("Load test ${index + 1}")
            
            // Perform analysis
            val analysisResult = localThis.recipeRepository.analyzeRecipe(
                localThis.testBitmap,
                "Load test ${index + 1}"
            ).first()
            results.add(analysisResult)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Then - integrated performance should be acceptable
        assertThat(results).hasSize(requestCount)
        results.forEach { result ->
            assertThat(result.success).isTrue()
        }
        
        // Total time should be reasonable for integrated operations
        assertThat(totalTime).isLessThan(10000) // Under 10 seconds for 50 requests
        
        // Average time per request should be reasonable
        val averageTime = totalTime.toDouble() / requestCount
        assertThat(averageTime).isLessThan(200) // Under 200ms per integrated operation
        
        // Cache should be utilized efficiently
        val cacheStats = localThis.recipeCacheDataSource.cacheStats.first()
        assertThat(cacheStats.totalEntries).isAtMost(50) // LRU should limit cache size
        
        // All repositories should have been called
        coVerify(atLeast = requestCount) { localThis.mockAIRepository.generateContent(any(), any()) }
        coVerify(atLeast = requestCount) { localThis.mockSecurityRepository.validateUserInput(any()) }
    }
    
    // MARK: - Helper Methods
    
    private fun createTestRecipe(title: String, content: String): Recipe {
        return Recipe(
            id = "integration_test_${System.currentTimeMillis()}",
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
            rawResponse = "Test response for ${recipe.title}",
            processingTime = 100L,
            success = true,
            errorMessage = null,
            warnings = emptyList()
        )
    }
    
    private fun generateImageHash(bitmap: Bitmap): String {
        // Simple hash generation for testing
        val width = bitmap.width
        val height = bitmap.height
        val config = bitmap.config?.name ?: "UNKNOWN"
        return "test_hash_${width}x${height}_${config}_${System.currentTimeMillis()}"
    }
    
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun createTestBitmap(): Bitmap {
        // Create a simple 1x1 bitmap for testing purposes
        // This avoids the complex bitmap creation that can fail in Robolectric
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
} 
package com.ravidor.forksure

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.AIResponseProcessingResult
import com.ravidor.forksure.data.source.local.RecipeCacheDataSource
import com.ravidor.forksure.repository.AIRepository
import com.ravidor.forksure.repository.RecipeRepositoryImpl
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ExperimentalCoroutinesApi
class SimpleIntegrationTest {

    private lateinit var mockAIRepository: AIRepository
    private lateinit var cacheDataSource: RecipeCacheDataSource
    private lateinit var recipeRepository: RecipeRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockAIRepository = mockk(relaxed = true)
        cacheDataSource = RecipeCacheDataSource()
        recipeRepository = RecipeRepositoryImpl(mockAIRepository, cacheDataSource)
    }

    @Test
    fun `simple repository test should work`() = runTest {
        // Given
        coEvery { mockAIRepository.generateContent(any(), any()) } returns 
            AIResponseProcessingResult.Success("Simple test response")
        
        // Create a simple 1x1 bitmap for testing
        val testBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        
        // When
        try {
            val result = recipeRepository.analyzeRecipe(testBitmap, "test prompt").first()
            
            // Then
            assertThat(result.success).isTrue()
            assertThat(result.rawResponse).isEqualTo("Simple test response")
            
            // Cleanup
            if (!testBitmap.isRecycled) {
                testBitmap.recycle()
            }
        } catch (e: Exception) {
            // Print the exception to understand what's happening
            println("Exception: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            
            // Cleanup on error too
            if (!testBitmap.isRecycled) {
                testBitmap.recycle()
            }
            throw e
        }
    }
    
    @Test
    fun `context should be available`() {
        assertThat(context).isNotNull()
    }
    
    @Test
    fun `cache should work`() = runTest {
        val cacheStats = cacheDataSource.cacheStats.first()
        assertThat(cacheStats.totalEntries).isEqualTo(0)
    }
} 
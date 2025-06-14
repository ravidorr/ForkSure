package com.ravidor.forksure.repository

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.ravidor.forksure.AIResponseProcessingResult
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeSource
import com.ravidor.forksure.data.source.local.RecipeCacheDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

/**
 * Unit tests for RecipeRepository
 * Tests recipe analysis, caching, and search functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecipeRepositoryTest {

    private lateinit var mockAIRepository: AIRepository
    private lateinit var mockCacheDataSource: RecipeCacheDataSource
    private lateinit var repository: RecipeRepositoryImpl
    private val mockBitmap = mockk<Bitmap>(relaxed = true)

    @Before
    fun setup() {
        mockAIRepository = mockk(relaxed = true)
        mockCacheDataSource = mockk(relaxed = true)
        repository = RecipeRepositoryImpl(mockAIRepository, mockCacheDataSource)
    }

    @Test
    fun `getCachedRecipes should return all cached recipes`() = runTest {
        // Given
        val recipes = listOf(
            Recipe(
                id = "1", title = "Recipe 1", description = "Desc 1",
                ingredients = listOf("ingredient1"), instructions = listOf("step1"),
                source = RecipeSource.AI_GENERATED, createdAt = Date()
            ),
            Recipe(
                id = "2", title = "Recipe 2", description = "Desc 2",
                ingredients = listOf("ingredient2"), instructions = listOf("step2"),
                source = RecipeSource.AI_GENERATED, createdAt = Date()
            )
        )
        coEvery { mockCacheDataSource.getAllCachedRecipes() } returns recipes

        // When
        val result = repository.getCachedRecipes()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("Recipe 1")
        assertThat(result[1].title).isEqualTo("Recipe 2")
    }

    @Test
    fun `searchRecipes should filter by title`() = runTest {
        // Given
        val recipes = listOf(
            Recipe(
                id = "1", title = "Chocolate Cake", description = "Sweet dessert",
                ingredients = listOf("chocolate"), instructions = listOf("bake"),
                source = RecipeSource.AI_GENERATED, createdAt = Date()
            ),
            Recipe(
                id = "2", title = "Vanilla Cookies", description = "Crispy cookies",
                ingredients = listOf("vanilla"), instructions = listOf("mix"),
                source = RecipeSource.AI_GENERATED, createdAt = Date()
            )
        )
        coEvery { mockCacheDataSource.getAllCachedRecipes() } returns recipes

        // When
        val result = repository.searchRecipes("chocolate")

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Chocolate Cake")
    }

    @Test
    fun `searchRecipes should filter by ingredients`() = runTest {
        // Given
        val recipes = listOf(
            Recipe(
                id = "1", title = "Cake", description = "Sweet",
                ingredients = listOf("flour", "sugar", "chocolate"),
                instructions = listOf("mix"), source = RecipeSource.AI_GENERATED, createdAt = Date()
            ),
            Recipe(
                id = "2", title = "Bread", description = "Savory",
                ingredients = listOf("flour", "yeast"), instructions = listOf("knead"),
                source = RecipeSource.AI_GENERATED, createdAt = Date()
            )
        )
        coEvery { mockCacheDataSource.getAllCachedRecipes() } returns recipes

        // When
        val result = repository.searchRecipes("chocolate")

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Cake")
    }

    @Test
    fun `clearCache should call cache data source`() = runTest {
        // Given
        coEvery { mockCacheDataSource.clearAllRecipes() } returns Unit

        // When
        repository.clearCache()

        // Then
        coVerify { mockCacheDataSource.clearAllRecipes() }
    }

    @Test
    fun `getRecentRecipes should call cache data source`() = runTest {
        // Given
        val recipes = listOf(
            Recipe(
                id = "1", title = "Recent Recipe", description = "Desc",
                ingredients = listOf("ingredient"), instructions = listOf("step"),
                source = RecipeSource.AI_GENERATED, createdAt = Date()
            )
        )
        coEvery { mockCacheDataSource.getRecentlyAccessedRecipes(10) } returns recipes

        // When
        val result = repository.getRecentRecipes(10)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Recent Recipe")
        coVerify { mockCacheDataSource.getRecentlyAccessedRecipes(10) }
    }

    @Test
    fun `getFavoriteRecipes should filter by favorite IDs`() = runTest {
        // Given
        val allRecipes = listOf(
            Recipe(
                id = "1", title = "Recipe 1", description = "Desc 1",
                ingredients = listOf("ingredient1"), instructions = listOf("step1"),
                source = RecipeSource.AI_GENERATED, createdAt = Date()
            ),
            Recipe(
                id = "2", title = "Recipe 2", description = "Desc 2",
                ingredients = listOf("ingredient2"), instructions = listOf("step2"),
                source = RecipeSource.AI_GENERATED, createdAt = Date()
            )
        )
        val favoriteIds = setOf("1")
        coEvery { mockCacheDataSource.getAllCachedRecipes() } returns allRecipes

        // When
        val result = repository.getFavoriteRecipes(favoriteIds)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("1")
        assertThat(result[0].title).isEqualTo("Recipe 1")
    }
} 
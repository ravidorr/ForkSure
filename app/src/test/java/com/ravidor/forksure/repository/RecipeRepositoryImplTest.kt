package com.ravidor.forksure.repository

import android.graphics.Bitmap
import com.ravidor.forksure.AIResponseProcessingResult
import com.ravidor.forksure.data.model.DifficultyLevel
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeAnalysisResponse
import com.ravidor.forksure.data.model.RecipeSource
import com.ravidor.forksure.data.source.local.RecipeCacheDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRepositoryImplTest {

    private fun sampleRecipe(): Recipe = Recipe(
        id = "id-1",
        title = "Chocolate Cake Recipe",
        description = "A delicious chocolate cake",
        ingredients = listOf("1 cup sugar", "2 eggs", "1 cup flour"),
        instructions = listOf("Preheat oven", "Mix ingredients", "Bake 30 minutes"),
        prepTime = "10 min",
        cookTime = "30 min",
        servings = "8 servings",
        difficulty = DifficultyLevel.BEGINNER,
        tags = listOf("sweet", "chocolate"),
        source = RecipeSource.AI_GENERATED,
        confidence = 0.9f,
        createdAt = Date(),
        imageHash = "hash"
    )

    @Test
    fun analyzeRecipe_returnsCached_whenPresent() = runTest {
        val aiRepo = mockk<AIRepository>(relaxed = true)
        val cache = mockk<RecipeCacheDataSource>()
        val repo = RecipeRepositoryImpl(aiRepo, cache)

        val cached = sampleRecipe().copy(source = RecipeSource.CACHED)
        coEvery { cache.getCachedRecipe(any()) } returns cached

        val bmp = mockk<Bitmap>(relaxed = true)
        val res = repo.analyzeRecipe(bmp, "prompt").first()

        assertTrue(res.success)
        assertEquals(RecipeSource.CACHED, res.recipe?.source)
        coVerify(exactly = 0) { aiRepo.generateContent(any(), any()) }
    }

    @Test
    fun analyzeRecipe_success_parsesAndCaches() = runTest {
        val aiRepo = mockk<AIRepository>()
        val cache = mockk<RecipeCacheDataSource>(relaxed = true)
        val repo = RecipeRepositoryImpl(aiRepo, cache)

        coEvery { cache.getCachedRecipe(any()) } returns null
        val aiResponse = """
            Chocolate Chip Cookies Recipe
            1 cup sugar
            2 cups flour
            step 1: mix ingredients
            step 2: bake for 12 minutes
            serves 4 people
            easy and simple
            sweet chocolate
        """.trimIndent()
        coEvery { aiRepo.generateContent(any(), any()) } returns AIResponseProcessingResult.Success(aiResponse)

        val bmp = mockk<Bitmap>(relaxed = true)
        val res: RecipeAnalysisResponse = repo.analyzeRecipe(bmp, "prompt").first()

        assertTrue(res.success)
        val recipe = res.recipe!!
        assertTrue(recipe.title.contains("Recipe", ignoreCase = true) || recipe.title.contains("Cookies", ignoreCase = true))
        assertTrue(recipe.ingredients.isNotEmpty())
        assertTrue(recipe.instructions.isNotEmpty())
        // confidence should be > 0 due to ingredients, instructions and title
assertTrue(recipe.confidence > 0f)
        coVerify { cache.cacheRecipe(any(), any()) }
    }

    @Test
    fun analyzeRecipe_error_emitsFailure() = runTest {
        val aiRepo = mockk<AIRepository>()
        val cache = mockk<RecipeCacheDataSource>(relaxed = true)
        val repo = RecipeRepositoryImpl(aiRepo, cache)

        coEvery { cache.getCachedRecipe(any()) } returns null
coEvery { aiRepo.generateContent(any(), any()) } returns AIResponseProcessingResult.Error("AI request failed", "boom")

        val bmp = mockk<Bitmap>(relaxed = true)
        val res = repo.analyzeRecipe(bmp, "prompt").first()

        assertEquals(false, res.success)
assertEquals(true, res.errorMessage?.contains("boom") ?: false)
    }

    @Test
    fun analyzeRecipe_blocked_emitsFailure() = runTest {
        val aiRepo = mockk<AIRepository>()
        val cache = mockk<RecipeCacheDataSource>(relaxed = true)
        val repo = RecipeRepositoryImpl(aiRepo, cache)

        coEvery { cache.getCachedRecipe(any()) } returns null
coEvery { aiRepo.generateContent(any(), any()) } returns AIResponseProcessingResult.Blocked("Content policy", "policy")

        val bmp = mockk<Bitmap>(relaxed = true)
        val res = repo.analyzeRecipe(bmp, "prompt").first()

        assertEquals(false, res.success)
assertEquals(true, res.errorMessage?.contains("blocked", ignoreCase = true) ?: false)
    }

    @Test
    fun searchRecipes_filtersAndSorts() = runTest {
        val aiRepo = mockk<AIRepository>(relaxed = true)
        val cache = mockk<RecipeCacheDataSource>()
        val repo = RecipeRepositoryImpl(aiRepo, cache)

        val items = listOf(
            sampleRecipe().copy(id = "1", title = "Chocolate Cake", description = "rich cake", tags = listOf("sweet")),
            sampleRecipe().copy(id = "2", title = "Vanilla Cookies", description = "crispy cookies", tags = listOf("sweet")),
            sampleRecipe().copy(id = "3", title = "Tomato Soup", description = "savory soup", tags = listOf("savory"))
        )
        coEvery { cache.getAllCachedRecipes() } returns items

val result = repo.searchRecipes("chocolate sweet")
        assertTrue(result.first().title.contains("Chocolate"))
        assertTrue(result.all { r -> r.tags.contains("sweet") || r.title.contains("chocolate", true) || r.description.contains("chocolate", true) || r.ingredients.any { s -> s.contains("chocolate", true) } })
    }
}

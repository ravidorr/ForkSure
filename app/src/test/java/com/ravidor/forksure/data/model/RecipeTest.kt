package com.ravidor.forksure.data.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Date

/**
 * Comprehensive unit tests for Recipe data model
 * Tests model creation, validation, and transformations
 */
class RecipeTest {

    @Test
    fun `Recipe should create with all required fields`() {
        // Given - localThis pattern
        val localThis = object {
            val id = "recipe_123"
            val title = "Chocolate Chip Cookies"
            val description = "Delicious homemade cookies"
            val ingredients = listOf("flour", "sugar", "butter", "eggs")
            val instructions = listOf("Mix ingredients", "Bake at 350°F")
            val source = RecipeSource.AI_GENERATED
        }

        // When
        val recipe = Recipe(
            id = localThis.id,
            title = localThis.title,
            description = localThis.description,
            ingredients = localThis.ingredients,
            instructions = localThis.instructions,
            source = localThis.source
        )

        // Then
        assertThat(recipe.id).isEqualTo(localThis.id)
        assertThat(recipe.title).isEqualTo(localThis.title)
        assertThat(recipe.description).isEqualTo(localThis.description)
        assertThat(recipe.ingredients).isEqualTo(localThis.ingredients)
        assertThat(recipe.instructions).isEqualTo(localThis.instructions)
        assertThat(recipe.source).isEqualTo(localThis.source)
    }

    @Test
    fun `Recipe should have proper default values`() {
        // Given - localThis pattern
        val localThis = object {
            val id = "recipe_123"
            val title = "Test Recipe"
            val description = "Test description"
            val ingredients = listOf("ingredient1")
            val instructions = listOf("instruction1")
            val source = RecipeSource.AI_GENERATED
        }

        // When
        val recipe = Recipe(
            id = localThis.id,
            title = localThis.title,
            description = localThis.description,
            ingredients = localThis.ingredients,
            instructions = localThis.instructions,
            source = localThis.source
        )

        // Then
        assertThat(recipe.prepTime).isNull()
        assertThat(recipe.cookTime).isNull()
        assertThat(recipe.servings).isNull()
        assertThat(recipe.difficulty).isEqualTo(DifficultyLevel.UNKNOWN)
        assertThat(recipe.tags).isEmpty()
        assertThat(recipe.nutritionInfo).isNull()
        assertThat(recipe.warnings).isEmpty()
        assertThat(recipe.confidence).isEqualTo(0.0f)
        assertThat(recipe.imageHash).isNull()
        assertThat(recipe.createdAt).isNotNull()
    }

    @Test
    fun `Recipe should handle optional fields correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val id = "recipe_123"
            val title = "Advanced Recipe"
            val description = "Complex recipe with all fields"
            val ingredients = listOf("flour", "sugar", "butter")
            val instructions = listOf("Mix", "Bake")
            val source = RecipeSource.AI_GENERATED
            val prepTime = "30 minutes"
            val cookTime = "45 minutes"
            val servings = "8 servings"
            val difficulty = DifficultyLevel.ADVANCED
            val tags = listOf("dessert", "cookies", "homemade")
            val nutritionInfo = NutritionInfo(
                calories = 250,
                protein = "3g",
                carbohydrates = "35g",
                fat = "12g"
            )
            val warnings = listOf("Contains nuts", "High in sugar")
            val confidence = 0.95f
            val imageHash = "hash123"
            val createdAt = Date(1234567890L)
        }

        // When
        val recipe = Recipe(
            id = localThis.id,
            title = localThis.title,
            description = localThis.description,
            ingredients = localThis.ingredients,
            instructions = localThis.instructions,
            source = localThis.source,
            prepTime = localThis.prepTime,
            cookTime = localThis.cookTime,
            servings = localThis.servings,
            difficulty = localThis.difficulty,
            tags = localThis.tags,
            nutritionInfo = localThis.nutritionInfo,
            warnings = localThis.warnings,
            confidence = localThis.confidence,
            imageHash = localThis.imageHash,
            createdAt = localThis.createdAt
        )

        // Then
        assertThat(recipe.prepTime).isEqualTo(localThis.prepTime)
        assertThat(recipe.cookTime).isEqualTo(localThis.cookTime)
        assertThat(recipe.servings).isEqualTo(localThis.servings)
        assertThat(recipe.difficulty).isEqualTo(localThis.difficulty)
        assertThat(recipe.tags).isEqualTo(localThis.tags)
        assertThat(recipe.nutritionInfo).isEqualTo(localThis.nutritionInfo)
        assertThat(recipe.warnings).isEqualTo(localThis.warnings)
        assertThat(recipe.confidence).isEqualTo(localThis.confidence)
        assertThat(recipe.imageHash).isEqualTo(localThis.imageHash)
        assertThat(recipe.createdAt).isEqualTo(localThis.createdAt)
    }

    @Test
    fun `Recipe should handle empty collections correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val id = "recipe_123"
            val title = "Empty Recipe"
            val description = "Recipe with empty collections"
            val ingredients = emptyList<String>()
            val instructions = emptyList<String>()
            val source = RecipeSource.AI_GENERATED
        }

        // When
        val recipe = Recipe(
            id = localThis.id,
            title = localThis.title,
            description = localThis.description,
            ingredients = localThis.ingredients,
            instructions = localThis.instructions,
            source = localThis.source
        )

        // Then
        assertThat(recipe.ingredients).isEmpty()
        assertThat(recipe.instructions).isEmpty()
        assertThat(recipe.tags).isEmpty()
        assertThat(recipe.warnings).isEmpty()
    }

    @Test
    fun `Recipe should handle confidence values correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val baseRecipe = Recipe(
                id = "recipe_123",
                title = "Test Recipe",
                description = "Test description",
                ingredients = listOf("ingredient1"),
                instructions = listOf("instruction1"),
                source = RecipeSource.AI_GENERATED
            )
            val confidenceValues = listOf(0.0f, 0.5f, 1.0f, 0.95f, 0.01f)
        }

        // When/Then
        localThis.confidenceValues.forEach { confidence ->
            val recipe = localThis.baseRecipe.copy(confidence = confidence)
            assertThat(recipe.confidence).isEqualTo(confidence)
            assertThat(recipe.confidence).isAtLeast(0.0f)
            assertThat(recipe.confidence).isAtMost(1.0f)
        }
    }

    @Test
    fun `DifficultyLevel should have all expected values`() {
        // Given - localThis pattern
        val localThis = object {
            val expectedLevels = listOf(
                DifficultyLevel.BEGINNER,
                DifficultyLevel.INTERMEDIATE,
                DifficultyLevel.ADVANCED,
                DifficultyLevel.UNKNOWN
            )
        }

        // When/Then
        localThis.expectedLevels.forEach { level ->
            assertThat(level).isNotNull()
            assertThat(level.name).isNotEmpty()
        }
    }

    @Test
    fun `RecipeSource should have all expected values`() {
        // Given - localThis pattern
        val localThis = object {
            val expectedSources = listOf(
                RecipeSource.AI_GENERATED,
                RecipeSource.USER_PROVIDED,
                RecipeSource.CACHED
            )
        }

        // When/Then
        localThis.expectedSources.forEach { source ->
            assertThat(source).isNotNull()
            assertThat(source.name).isNotEmpty()
        }
    }

    @Test
    fun `NutritionInfo should create with all fields`() {
        // Given - localThis pattern
        val localThis = object {
            val calories = 300
            val protein = "5g"
            val carbohydrates = "40g"
            val fat = "15g"
            val fiber = "3g"
            val sugar = "20g"
            val sodium = "200mg"
        }

        // When
        val nutritionInfo = NutritionInfo(
            calories = localThis.calories,
            protein = localThis.protein,
            carbohydrates = localThis.carbohydrates,
            fat = localThis.fat,
            fiber = localThis.fiber,
            sugar = localThis.sugar,
            sodium = localThis.sodium
        )

        // Then
        assertThat(nutritionInfo.calories).isEqualTo(localThis.calories)
        assertThat(nutritionInfo.protein).isEqualTo(localThis.protein)
        assertThat(nutritionInfo.carbohydrates).isEqualTo(localThis.carbohydrates)
        assertThat(nutritionInfo.fat).isEqualTo(localThis.fat)
        assertThat(nutritionInfo.fiber).isEqualTo(localThis.fiber)
        assertThat(nutritionInfo.sugar).isEqualTo(localThis.sugar)
        assertThat(nutritionInfo.sodium).isEqualTo(localThis.sodium)
    }

    @Test
    fun `NutritionInfo should have proper default values`() {
        // Given - localThis pattern
        val localThis = object {
            // No specific values needed for default test
        }

        // When
        val nutritionInfo = NutritionInfo()

        // Then
        assertThat(nutritionInfo.calories).isNull()
        assertThat(nutritionInfo.protein).isNull()
        assertThat(nutritionInfo.carbohydrates).isNull()
        assertThat(nutritionInfo.fat).isNull()
        assertThat(nutritionInfo.fiber).isNull()
        assertThat(nutritionInfo.sugar).isNull()
        assertThat(nutritionInfo.sodium).isNull()
    }

    @Test
    fun `RecipeAnalysisRequest should create correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val imageHash = "hash123"
            val prompt = "What is this baked good?"
            val timestamp = Date(1234567890L)
            val userId = "user123"
        }

        // When
        val request = RecipeAnalysisRequest(
            imageHash = localThis.imageHash,
            prompt = localThis.prompt,
            timestamp = localThis.timestamp,
            userId = localThis.userId
        )

        // Then
        assertThat(request.imageHash).isEqualTo(localThis.imageHash)
        assertThat(request.prompt).isEqualTo(localThis.prompt)
        assertThat(request.timestamp).isEqualTo(localThis.timestamp)
        assertThat(request.userId).isEqualTo(localThis.userId)
    }

    @Test
    fun `RecipeAnalysisResponse should create correctly`() {
        // Given - localThis pattern
        val localThis = object {
            val recipe = Recipe(
                id = "recipe_123",
                title = "Test Recipe",
                description = "Test description",
                ingredients = listOf("ingredient1"),
                instructions = listOf("instruction1"),
                source = RecipeSource.AI_GENERATED
            )
            val rawResponse = "Raw AI response"
            val processingTime = 1500L
            val success = true
            val errorMessage = null
            val warnings = listOf("Warning 1", "Warning 2")
        }

        // When
        val response = RecipeAnalysisResponse(
            recipe = localThis.recipe,
            rawResponse = localThis.rawResponse,
            processingTime = localThis.processingTime,
            success = localThis.success,
            errorMessage = localThis.errorMessage,
            warnings = localThis.warnings
        )

        // Then
        assertThat(response.recipe).isEqualTo(localThis.recipe)
        assertThat(response.rawResponse).isEqualTo(localThis.rawResponse)
        assertThat(response.processingTime).isEqualTo(localThis.processingTime)
        assertThat(response.success).isEqualTo(localThis.success)
        assertThat(response.errorMessage).isEqualTo(localThis.errorMessage)
        assertThat(response.warnings).isEqualTo(localThis.warnings)
    }

    @Test
    fun `RecipeAnalysisResponse should handle error cases`() {
        // Given - localThis pattern
        val localThis = object {
            val recipe = null
            val rawResponse = "Error response"
            val processingTime = 500L
            val success = false
            val errorMessage = "Analysis failed"
            val warnings = emptyList<String>()
        }

        // When
        val response = RecipeAnalysisResponse(
            recipe = localThis.recipe,
            rawResponse = localThis.rawResponse,
            processingTime = localThis.processingTime,
            success = localThis.success,
            errorMessage = localThis.errorMessage,
            warnings = localThis.warnings
        )

        // Then
        assertThat(response.recipe).isNull()
        assertThat(response.success).isFalse()
        assertThat(response.errorMessage).isEqualTo(localThis.errorMessage)
        assertThat(response.warnings).isEmpty()
    }

    @Test
    fun `Recipe should be immutable`() {
        // Given - localThis pattern
        val localThis = object {
            val originalRecipe = Recipe(
                id = "recipe_123",
                title = "Original Recipe",
                description = "Original description",
                ingredients = listOf("ingredient1"),
                instructions = listOf("instruction1"),
                source = RecipeSource.AI_GENERATED
            )
        }

        // When
        val copiedRecipe = localThis.originalRecipe.copy(title = "Modified Recipe")

        // Then
        assertThat(localThis.originalRecipe.title).isEqualTo("Original Recipe")
        assertThat(copiedRecipe.title).isEqualTo("Modified Recipe")
        assertThat(localThis.originalRecipe.id).isEqualTo(copiedRecipe.id)
    }
} 
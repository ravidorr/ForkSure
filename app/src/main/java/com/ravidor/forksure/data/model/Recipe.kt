package com.ravidor.forksure.data.model

import androidx.compose.runtime.Immutable
import java.util.Date

/**
 * Data model representing a recipe analysis result
 */
@Immutable
data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val prepTime: String? = null,
    val cookTime: String? = null,
    val servings: String? = null,
    val difficulty: DifficultyLevel = DifficultyLevel.UNKNOWN,
    val tags: List<String> = emptyList(),
    val nutritionInfo: NutritionInfo? = null,
    val warnings: List<String> = emptyList(),
    val source: RecipeSource,
    val confidence: Float = 0.0f,
    val createdAt: Date = Date(),
    val imageHash: String? = null
)

/**
 * Nutrition information for a recipe
 */
@Immutable
data class NutritionInfo(
    val calories: Int? = null,
    val protein: String? = null,
    val carbohydrates: String? = null,
    val fat: String? = null,
    val fiber: String? = null,
    val sugar: String? = null,
    val sodium: String? = null
)

/**
 * Recipe difficulty levels
 */
@Immutable
enum class DifficultyLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    UNKNOWN
}

/**
 * Source of the recipe analysis
 */
@Immutable
enum class RecipeSource {
    AI_GENERATED,
    USER_PROVIDED,
    CACHED
}

/**
 * Recipe analysis request
 */
@Immutable
data class RecipeAnalysisRequest(
    val imageHash: String,
    val prompt: String,
    val timestamp: Date = Date(),
    val userId: String? = null
)

/**
 * Recipe analysis response
 */
@Immutable
data class RecipeAnalysisResponse(
    val recipe: Recipe?,
    val rawResponse: String,
    val processingTime: Long,
    val success: Boolean,
    val errorMessage: String? = null,
    val warnings: List<String> = emptyList()
) 
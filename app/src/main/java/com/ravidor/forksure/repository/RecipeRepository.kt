package com.ravidor.forksure.repository

import android.graphics.Bitmap
import com.ravidor.forksure.AIResponseProcessingResult
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeAnalysisRequest
import com.ravidor.forksure.data.model.RecipeAnalysisResponse
import com.ravidor.forksure.data.model.RecipeSource
import com.ravidor.forksure.data.model.DifficultyLevel
import com.ravidor.forksure.data.source.local.RecipeCacheDataSource
import com.ravidor.forksure.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for recipe-related operations
 */
interface RecipeRepository {
    suspend fun analyzeRecipe(bitmap: Bitmap, prompt: String): Flow<RecipeAnalysisResponse>
    suspend fun getCachedRecipes(): List<Recipe>
    suspend fun getRecentRecipes(limit: Int = 10): List<Recipe>
    suspend fun getFavoriteRecipes(favoriteIds: Set<String>): List<Recipe>
    suspend fun searchRecipes(query: String): List<Recipe>
    suspend fun clearCache()
    suspend fun getCacheStatistics(): RecipeCacheDataSource.CacheStatistics
}

/**
 * Implementation of RecipeRepository combining AI analysis with local caching
 */
@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val aiRepository: AIRepository,
    private val cacheDataSource: RecipeCacheDataSource
) : RecipeRepository {

    override suspend fun analyzeRecipe(bitmap: Bitmap, prompt: String): Flow<RecipeAnalysisResponse> = flow {
        val startTime = System.currentTimeMillis()
        
        try {
            // Generate image hash for caching
            val imageHash = generateImageHash(bitmap)
            val request = RecipeAnalysisRequest(
                imageHash = imageHash,
                prompt = prompt
            )
            
            // Check cache first
            val cachedRecipe = cacheDataSource.getCachedRecipe(request)
            if (cachedRecipe != null) {
                val response = RecipeAnalysisResponse(
                    recipe = cachedRecipe.copy(source = RecipeSource.CACHED),
                    rawResponse = "Cached result",
                    processingTime = System.currentTimeMillis() - startTime,
                    success = true
                )
                emit(response)
                return@flow
            }
            
            // Perform AI analysis
            when (val aiResult = aiRepository.generateContent(bitmap, prompt)) {
                is AIResponseProcessingResult.Success -> {
                    val recipe = parseAIResponse(aiResult.response, imageHash)
                    val response = RecipeAnalysisResponse(
                        recipe = recipe,
                        rawResponse = aiResult.response,
                        processingTime = System.currentTimeMillis() - startTime,
                        success = true
                    )
                    
                    // Cache the result
                    cacheDataSource.cacheRecipe(request, response)
                    
                    emit(response)
                }
                
                is AIResponseProcessingResult.SuccessWithWarning -> {
                    val recipe = parseAIResponse(aiResult.response, imageHash)
                    val response = RecipeAnalysisResponse(
                        recipe = recipe,
                        rawResponse = aiResult.response,
                        processingTime = System.currentTimeMillis() - startTime,
                        success = true,
                        warnings = listOf(aiResult.warning)
                    )
                    
                    // Cache the result
                    cacheDataSource.cacheRecipe(request, response)
                    
                    emit(response)
                }
                
                is AIResponseProcessingResult.Warning -> {
                    val recipe = parseAIResponse(aiResult.response, imageHash)
                    val response = RecipeAnalysisResponse(
                        recipe = recipe,
                        rawResponse = aiResult.response,
                        processingTime = System.currentTimeMillis() - startTime,
                        success = true,
                        warnings = listOf(aiResult.warning)
                    )
                    
                    // Cache the result
                    cacheDataSource.cacheRecipe(request, response)
                    
                    emit(response)
                }
                
                is AIResponseProcessingResult.Error -> {
                    val response = RecipeAnalysisResponse(
                        recipe = null,
                        rawResponse = aiResult.message,
                        processingTime = System.currentTimeMillis() - startTime,
                        success = false,
                        errorMessage = aiResult.message
                    )
                    emit(response)
                }
                
                is AIResponseProcessingResult.Blocked -> {
                    val response = RecipeAnalysisResponse(
                        recipe = null,
                        rawResponse = aiResult.message,
                        processingTime = System.currentTimeMillis() - startTime,
                        success = false,
                        errorMessage = "Content blocked: ${aiResult.message}"
                    )
                    emit(response)
                }
            }
            
        } catch (e: Exception) {
            val response = RecipeAnalysisResponse(
                recipe = null,
                rawResponse = "",
                processingTime = System.currentTimeMillis() - startTime,
                success = false,
                errorMessage = "Analysis failed: ${e.message}"
            )
            emit(response)
        }
    }
    
    override suspend fun getCachedRecipes(): List<Recipe> {
        return cacheDataSource.getAllCachedRecipes()
    }
    
    override suspend fun getRecentRecipes(limit: Int): List<Recipe> {
        return cacheDataSource.getRecentlyAccessedRecipes(limit)
    }
    
    override suspend fun getFavoriteRecipes(favoriteIds: Set<String>): List<Recipe> {
        val allRecipes = cacheDataSource.getAllCachedRecipes()
        return allRecipes.filter { recipe -> favoriteIds.contains(recipe.id) }
    }
    
    override suspend fun searchRecipes(query: String): List<Recipe> = withContext(Dispatchers.IO) {
        val allRecipes = cacheDataSource.getAllCachedRecipes()
        val searchTerms = query.lowercase().split(" ").filter { it.isNotBlank() }
        
        allRecipes.filter { recipe ->
            searchTerms.any { term ->
                recipe.title.lowercase().contains(term) ||
                recipe.description.lowercase().contains(term) ||
                recipe.ingredients.any { ingredient -> ingredient.lowercase().contains(term) } ||
                recipe.tags.any { tag -> tag.lowercase().contains(term) }
            }
        }.sortedByDescending { recipe ->
            // Score based on relevance
            var score = 0
            searchTerms.forEach { term ->
                if (recipe.title.lowercase().contains(term)) score += 3
                if (recipe.description.lowercase().contains(term)) score += 2
                if (recipe.ingredients.any { it.lowercase().contains(term) }) score += 1
                if (recipe.tags.any { it.lowercase().contains(term) }) score += 1
            }
            score
        }
    }
    
    override suspend fun clearCache() {
        cacheDataSource.clearAllRecipes()
    }
    
    override suspend fun getCacheStatistics(): RecipeCacheDataSource.CacheStatistics {
        return cacheDataSource.getCurrentCacheStatistics()
    }
    
    /**
     * Generate a hash for the image to use as cache key
     */
    private suspend fun generateImageHash(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val bytes = bitmap.let { bmp ->
                val stream = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                stream.toByteArray()
            }
            
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(bytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback to timestamp-based hash
            System.currentTimeMillis().toString()
        }
    }
    
    /**
     * Parse AI response into structured Recipe object
     */
    private fun parseAIResponse(response: String, imageHash: String): Recipe {
        // This is a simplified parser - in a real app, you'd want more sophisticated parsing
        val lines = response.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        var title = "Recipe Analysis"
        var description = response
        val ingredients = mutableListOf<String>()
        val instructions = mutableListOf<String>()
        val tags = mutableListOf<String>()
        var prepTime: String? = null
        var cookTime: String? = null
        var servings: String? = null
        var difficulty = DifficultyLevel.UNKNOWN
        
        // Extract title (usually first line or line with "Recipe" or similar)
        lines.firstOrNull { line ->
            line.contains("recipe", ignoreCase = true) || 
            line.contains("cake", ignoreCase = true) ||
            line.contains("cookie", ignoreCase = true) ||
            line.contains("bread", ignoreCase = true) ||
            line.length < 50
        }?.let { title = it }
        
        // Extract ingredients (lines that start with numbers, bullets, or contain common ingredient words)
        lines.forEach { line ->
            when {
                line.matches(Regex("^\\d+.*")) || 
                line.startsWith("•") || 
                line.startsWith("-") ||
                line.contains("cup", ignoreCase = true) ||
                line.contains("tablespoon", ignoreCase = true) ||
                line.contains("teaspoon", ignoreCase = true) ||
                line.contains("gram", ignoreCase = true) ||
                line.contains("ounce", ignoreCase = true) -> {
                    ingredients.add(line)
                }
                
                line.contains("step", ignoreCase = true) ||
                line.contains("mix", ignoreCase = true) ||
                line.contains("bake", ignoreCase = true) ||
                line.contains("preheat", ignoreCase = true) -> {
                    instructions.add(line)
                }
                
                line.contains("minute", ignoreCase = true) -> {
                    when {
                        line.contains("prep", ignoreCase = true) -> prepTime = extractTime(line)
                        line.contains("cook", ignoreCase = true) || line.contains("bake", ignoreCase = true) -> cookTime = extractTime(line)
                    }
                }
                
                line.contains("serve", ignoreCase = true) -> {
                    servings = extractServings(line)
                }
            }
        }
        
        // Determine difficulty based on content
        difficulty = when {
            response.contains("easy", ignoreCase = true) || 
            response.contains("simple", ignoreCase = true) ||
            response.contains("beginner", ignoreCase = true) -> DifficultyLevel.BEGINNER
            
            response.contains("intermediate", ignoreCase = true) ||
            response.contains("moderate", ignoreCase = true) -> DifficultyLevel.INTERMEDIATE
            
            response.contains("advanced", ignoreCase = true) ||
            response.contains("difficult", ignoreCase = true) ||
            response.contains("complex", ignoreCase = true) -> DifficultyLevel.ADVANCED
            
            else -> DifficultyLevel.UNKNOWN
        }
        
        // Extract tags based on content
        val commonTags = listOf("sweet", "savory", "chocolate", "vanilla", "fruit", "nuts", "gluten-free", "vegan", "vegetarian")
        commonTags.forEach { tag ->
            if (response.contains(tag, ignoreCase = true)) {
                tags.add(tag)
            }
        }
        
        return Recipe(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            ingredients = ingredients.ifEmpty { listOf("Ingredients not clearly identified") },
            instructions = instructions.ifEmpty { listOf("Instructions not clearly identified") },
            prepTime = prepTime,
            cookTime = cookTime,
            servings = servings,
            difficulty = difficulty,
            tags = tags,
            source = RecipeSource.AI_GENERATED,
            confidence = calculateConfidence(ingredients.size, instructions.size, title != "Recipe Analysis"),
            createdAt = Date(),
            imageHash = imageHash
        )
    }
    
    /**
     * Extract time information from text
     */
    private fun extractTime(text: String): String? {
        val timeRegex = Regex("(\\d+)\\s*(minute|min|hour|hr)", RegexOption.IGNORE_CASE)
        return timeRegex.find(text)?.value
    }
    
    /**
     * Extract serving information from text
     */
    private fun extractServings(text: String): String? {
        val servingRegex = Regex("(\\d+)\\s*(serving|portion|people)", RegexOption.IGNORE_CASE)
        return servingRegex.find(text)?.value
    }
    
    /**
     * Calculate confidence score based on parsed content quality
     */
    private fun calculateConfidence(ingredientCount: Int, instructionCount: Int, hasTitle: Boolean): Float {
        var confidence = 0.0f
        
        // Base confidence from having content
        if (ingredientCount > 0) confidence += 0.3f
        if (instructionCount > 0) confidence += 0.3f
        if (hasTitle) confidence += 0.2f
        
        // Bonus for detailed content
        if (ingredientCount >= 3) confidence += 0.1f
        if (instructionCount >= 2) confidence += 0.1f
        
        return confidence.coerceIn(0.0f, 1.0f)
    }
} 
package com.ravidor.forksure.data.source.local

import android.util.LruCache
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeAnalysisRequest
import com.ravidor.forksure.data.model.RecipeAnalysisResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for recipe caching using LRU cache
 * Provides fast access to recently analyzed recipes
 */
@Singleton
class RecipeCacheDataSource @Inject constructor() {
    
    // LRU cache for recipes (max 50 entries by default)
    private val recipeCache = LruCache<String, CachedRecipe>(50)
    
    // Request history for analytics
    private val requestHistory = ConcurrentHashMap<String, RecipeAnalysisRequest>()
    
    // Cache statistics
    private val _cacheStats = MutableStateFlow(CacheStatistics())
    val cacheStats: Flow<CacheStatistics> = _cacheStats.asStateFlow()
    
    // Mutex for thread-safe operations
    private val cacheMutex = Mutex()
    
    /**
     * Cached recipe with metadata
     */
    data class CachedRecipe(
        val recipe: Recipe,
        val originalRequest: RecipeAnalysisRequest,
        val cachedAt: Date = Date(),
        val accessCount: Int = 1,
        val lastAccessed: Date = Date()
    )
    
    /**
     * Cache statistics
     */
    data class CacheStatistics(
        val totalEntries: Int = 0,
        val hitCount: Long = 0,
        val missCount: Long = 0,
        val evictionCount: Long = 0,
        val totalSize: Int = 0,
        val oldestEntry: Date? = null,
        val newestEntry: Date? = null
    ) {
        val hitRate: Float
            get() = if (hitCount + missCount > 0) {
                hitCount.toFloat() / (hitCount + missCount)
            } else 0f
    }
    
    /**
     * Cache a recipe analysis result
     */
    suspend fun cacheRecipe(
        request: RecipeAnalysisRequest,
        response: RecipeAnalysisResponse
    ) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            response.recipe?.let { recipe ->
                val cacheKey = generateCacheKey(request)
                val cachedRecipe = CachedRecipe(
                    recipe = recipe,
                    originalRequest = request
                )
                
                recipeCache.put(cacheKey, cachedRecipe)
                requestHistory[cacheKey] = request
                
                updateCacheStatistics()
            }
        }
    }
    
    /**
     * Retrieve a cached recipe
     */
    suspend fun getCachedRecipe(request: RecipeAnalysisRequest): Recipe? = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val cacheKey = generateCacheKey(request)
            val cachedRecipe = recipeCache.get(cacheKey)
            
            if (cachedRecipe != null) {
                // Update access statistics
                val updatedRecipe = cachedRecipe.copy(
                    accessCount = cachedRecipe.accessCount + 1,
                    lastAccessed = Date()
                )
                recipeCache.put(cacheKey, updatedRecipe)
                
                // Update cache statistics
                val currentStats = _cacheStats.value
                _cacheStats.value = currentStats.copy(hitCount = currentStats.hitCount + 1)
                
                cachedRecipe.recipe
            } else {
                // Update miss statistics
                val currentStats = _cacheStats.value
                _cacheStats.value = currentStats.copy(missCount = currentStats.missCount + 1)
                null
            }
        }
    }
    
    /**
     * Check if a recipe is cached
     */
    suspend fun isCached(request: RecipeAnalysisRequest): Boolean = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(request)
        recipeCache.get(cacheKey) != null
    }
    
    /**
     * Get all cached recipes
     */
    suspend fun getAllCachedRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val recipes = mutableListOf<Recipe>()
            recipeCache.snapshot().values.forEach { cachedRecipe ->
                recipes.add(cachedRecipe.recipe)
            }
            recipes.sortedByDescending { it.createdAt }
        }
    }
    
    /**
     * Get recently accessed recipes
     */
    suspend fun getRecentlyAccessedRecipes(limit: Int = 10): List<Recipe> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            recipeCache.snapshot().values
                .sortedByDescending { it.lastAccessed }
                .take(limit)
                .map { it.recipe }
        }
    }
    
    /**
     * Get most accessed recipes
     */
    suspend fun getMostAccessedRecipes(limit: Int = 10): List<Recipe> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            recipeCache.snapshot().values
                .sortedByDescending { it.accessCount }
                .take(limit)
                .map { it.recipe }
        }
    }
    
    /**
     * Remove a specific recipe from cache
     */
    suspend fun removeRecipe(recipeId: String) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val keysToRemove = mutableListOf<String>()
            recipeCache.snapshot().forEach { (key, cachedRecipe) ->
                if (cachedRecipe.recipe.id == recipeId) {
                    keysToRemove.add(key)
                }
            }
            keysToRemove.forEach { key ->
                recipeCache.remove(key)
                requestHistory.remove(key)
            }
            updateCacheStatistics()
        }
    }
    
    /**
     * Clear old entries based on retention policy
     */
    suspend fun clearOldEntries(retentionDays: Int) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val cutoffDate = Date(System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L))
            val keysToRemove = mutableListOf<String>()
            
            recipeCache.snapshot().forEach { (key, cachedRecipe) ->
                if (cachedRecipe.cachedAt.before(cutoffDate)) {
                    keysToRemove.add(key)
                }
            }
            
            keysToRemove.forEach { key ->
                recipeCache.remove(key)
                requestHistory.remove(key)
            }
            
            updateCacheStatistics()
        }
    }
    
    /**
     * Clear all cached recipes
     */
    suspend fun clearAllRecipes() = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            recipeCache.evictAll()
            requestHistory.clear()
            updateCacheStatistics()
        }
    }
    
    /**
     * Resize cache
     */
    suspend fun resizeCache(newSize: Int) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            recipeCache.resize(newSize)
            updateCacheStatistics()
        }
    }
    
    /**
     * Get cache usage information
     */
    suspend fun getCacheUsage(): CacheUsage = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val snapshot = recipeCache.snapshot()
            val totalEntries = snapshot.size
            val maxSize = recipeCache.maxSize()
            val usagePercentage = if (maxSize > 0) (totalEntries.toFloat() / maxSize) * 100 else 0f
            
            val oldestEntry = snapshot.values.minByOrNull { it.cachedAt }?.cachedAt
            val newestEntry = snapshot.values.maxByOrNull { it.cachedAt }?.cachedAt
            
            CacheUsage(
                currentEntries = totalEntries,
                maxEntries = maxSize,
                usagePercentage = usagePercentage,
                oldestEntry = oldestEntry,
                newestEntry = newestEntry
            )
        }
    }
    
    /**
     * Cache usage information
     */
    data class CacheUsage(
        val currentEntries: Int,
        val maxEntries: Int,
        val usagePercentage: Float,
        val oldestEntry: Date?,
        val newestEntry: Date?
    )
    
    /**
     * Generate cache key from request
     */
    private fun generateCacheKey(request: RecipeAnalysisRequest): String {
        return "${request.imageHash}_${request.prompt.hashCode()}"
    }
    
    /**
     * Update cache statistics
     */
    private fun updateCacheStatistics() {
        val snapshot = recipeCache.snapshot()
        val oldestEntry = snapshot.values.minByOrNull { it.cachedAt }?.cachedAt
        val newestEntry = snapshot.values.maxByOrNull { it.cachedAt }?.cachedAt
        
        val currentStats = _cacheStats.value
        _cacheStats.value = currentStats.copy(
            totalEntries = snapshot.size,
            totalSize = snapshot.size,
            oldestEntry = oldestEntry,
            newestEntry = newestEntry
        )
    }
    
    /**
     * Get current cache statistics
     */
    suspend fun getCurrentCacheStatistics(): CacheStatistics = withContext(Dispatchers.IO) {
        return@withContext _cacheStats.value
    }
    
    /**
     * Get cache performance metrics
     */
    suspend fun getCacheMetrics(): CacheMetrics = withContext(Dispatchers.IO) {
        val stats = _cacheStats.value
        val usage = getCacheUsage()
        
        CacheMetrics(
            hitRate = stats.hitRate,
            totalRequests = stats.hitCount + stats.missCount,
            cacheEfficiency = if (usage.maxEntries > 0) usage.usagePercentage / 100f else 0f,
            averageAccessCount = recipeCache.snapshot().values
                .map { it.accessCount }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0
        )
    }
    
    /**
     * Cache performance metrics
     */
    data class CacheMetrics(
        val hitRate: Float,
        val totalRequests: Long,
        val cacheEfficiency: Float,
        val averageAccessCount: Double
    )
} 
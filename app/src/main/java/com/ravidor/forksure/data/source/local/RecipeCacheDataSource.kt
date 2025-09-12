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
import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.security.MessageDigest

/**
 * Local data source for recipe caching using LRU cache
 * Provides fast access to recently analyzed recipes
 */
@Singleton
class RecipeCacheDataSource @Inject constructor(
    private val context: Context
) {
    
    // LRU cache for recipes (max 50 entries by default)
    private val recipeCache = object : LruCache<String, CachedRecipe>(50) {
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: CachedRecipe, newValue: CachedRecipe?) {
            if (evicted) {
                // Keep mirrors in sync on eviction
                entries.remove(key)
                requestHistory.remove(key)
                val currentStats = _cacheStats.value
                _cacheStats.value = currentStats.copy(
                    evictionCount = currentStats.evictionCount + 1,
                    totalEntries = entries.size,
                    totalSize = entries.size
                )
            }
        }
    }
    
    // Mirror of cache entries for reliable iteration in tests and runtime
    private val entries = ConcurrentHashMap<String, CachedRecipe>()
    
    // Request history for analytics
    private val requestHistory = ConcurrentHashMap<String, RecipeAnalysisRequest>()
    
    // Cache statistics
    private val _cacheStats = MutableStateFlow(CacheStatistics())
    val cacheStats: Flow<CacheStatistics> = _cacheStats.asStateFlow()
    
    // Mutex for thread-safe operations
    private val cacheMutex = Mutex()
    
    private val gson = Gson()
    private val cacheFile: File by lazy { File(context.filesDir, "recipe_cache.json") }
    private val CACHE_VERSION = 1

    init {
        loadCacheFromDisk()
    }

    private fun loadCacheFromDisk() {
        if (!cacheFile.exists()) return
        try {
            val json = cacheFile.readText()
            val persisted = gson.fromJson(json, PersistedCache::class.java)
            if (persisted.version != CACHE_VERSION) return
            val calculatedChecksum = calculateChecksum(persisted.dataJson)
            if (persisted.checksum != calculatedChecksum) return
            val data = gson.fromJson(persisted.dataJson, CacheData::class.java)
            data.entries.forEach { (key, cachedRecipe) ->
                recipeCache.put(key, cachedRecipe)
                entries[key] = cachedRecipe
            }
            requestHistory.putAll(data.requestHistory)
        } catch (_: Throwable) {
            // Ignore any issues reading/parsing cache; fall back to empty in-memory cache
        }
    }

    private fun persistCacheToDisk() {
        try {
            val data = CacheData(
                entries = HashMap(entries),
                requestHistory = HashMap(requestHistory)
            )
            val dataJson = gson.toJson(data)
            val checksum = calculateChecksum(dataJson)
            val persisted = PersistedCache(
                version = CACHE_VERSION,
                checksum = checksum,
                dataJson = dataJson
            )
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(gson.toJson(persisted))
        } catch (_: Throwable) {
            // Ignore disk persistence issues in favor of in-memory cache reliability
        }
    }

    private fun calculateChecksum(data: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

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
                entries[cacheKey] = cachedRecipe
                requestHistory[cacheKey] = request
                
                updateCacheStatistics()
                persistCacheToDisk()
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
                entries[cacheKey] = updatedRecipe
                
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
        cacheMutex.withLock {
            val cacheKey = generateCacheKey(request)
            recipeCache.get(cacheKey) != null
        }
    }
    
    /**
     * Get all cached recipes
     */
    suspend fun getAllCachedRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            try {
                val list = entries.values.toList()
                list.map { it.recipe }
                    .sortedByDescending { it.createdAt }
            } catch (_: Throwable) { emptyList() }
        }
    }
    
    /**
     * Get recently accessed recipes
     */
    suspend fun getRecentlyAccessedRecipes(limit: Int = 10): List<Recipe> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            try {
                entries.values
                    .sortedByDescending { it.lastAccessed }
                    .take(limit)
                    .map { it.recipe }
            } catch (_: Throwable) { emptyList() }
        }
    }
    
    /**
     * Get most accessed recipes
     */
    suspend fun getMostAccessedRecipes(limit: Int = 10): List<Recipe> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            try {
                entries.values
                    .sortedByDescending { it.accessCount }
                    .take(limit)
                    .map { it.recipe }
            } catch (_: Throwable) { emptyList() }
        }
    }
    
    /**
     * Remove a specific recipe from cache
     */
    suspend fun removeRecipe(recipeId: String) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            try {
                val keysToRemove = entries.filterValues { it.recipe.id == recipeId }.keys
                keysToRemove.forEach { key ->
                    try { recipeCache.remove(key) } catch (_: Throwable) { }
                    try { entries.remove(key) } catch (_: Throwable) { }
                    try { requestHistory.remove(key) } catch (_: Throwable) { }
                }
                updateCacheStatistics()
                persistCacheToDisk()
            } catch (_: Throwable) { /* ignore */ }
        }
    }
    
    /**
     * Clear old entries based on retention policy
     */
    suspend fun clearOldEntries(retentionDays: Int) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val cutoffDate = Date(System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L))
            val keysToRemove = mutableListOf<String>()
            try {
                entries.forEach { (key, cachedRecipe) ->
                    if (cachedRecipe.cachedAt.before(cutoffDate)) {
                        keysToRemove.add(key)
                    }
                }
            } catch (_: Throwable) { /* ignore */ }
            
            keysToRemove.forEach { key ->
                entries.remove(key)
                recipeCache.remove(key)
                requestHistory.remove(key)
            }
            
            updateCacheStatistics()
            persistCacheToDisk()
        }
    }
    
    /**
     * Clear all cached recipes
     */
    suspend fun clearAllRecipes() = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            recipeCache.evictAll()
            entries.clear()
            requestHistory.clear()
            updateCacheStatistics()
            persistCacheToDisk()
        }
    }
    
    /**
     * Resize cache
     */
    suspend fun resizeCache(newSize: Int) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            recipeCache.resize(newSize)
            updateCacheStatistics()
            persistCacheToDisk()
        }
    }
    
    /**
     * Get cache usage information
     */
    suspend fun getCacheUsage(): CacheUsage = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            try {
                val totalEntries = entries.size
                val maxSize = recipeCache.maxSize()
                val usagePercentage = if (maxSize > 0) (totalEntries.toFloat() / maxSize) * 100 else 0f
                
                val oldestEntry = entries.values.minByOrNull { it.cachedAt }?.cachedAt
                val newestEntry = entries.values.maxByOrNull { it.cachedAt }?.cachedAt
                
                CacheUsage(
                    currentEntries = totalEntries,
                    maxEntries = maxSize,
                    usagePercentage = usagePercentage,
                    oldestEntry = oldestEntry,
                    newestEntry = newestEntry
                )
            } catch (_: Throwable) {
                CacheUsage(0, recipeCache.maxSize(), 0f, null, null)
            }
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
        // Use a stable, collision-resistant key derived from full prompt rather than hashCode
        // This avoids rare collisions and any environment-specific behavior with hash codes.
        return "${request.imageHash}_${request.prompt}"
    }
    
    /**
     * Update cache statistics
     */
    private fun updateCacheStatistics() {
        val values = entries.values.toList()
        val oldestEntry = values.minByOrNull { it.cachedAt }?.cachedAt
        val newestEntry = values.maxByOrNull { it.cachedAt }?.cachedAt
        
        val currentStats = _cacheStats.value
        _cacheStats.value = currentStats.copy(
            totalEntries = values.size,
            totalSize = values.size,
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
        val avg = try {
            val v = entries.values.map { it.accessCount }.average()
            if (v.isNaN()) 0.0 else v
        } catch (_: Throwable) { 0.0 }
        
        CacheMetrics(
            hitRate = stats.hitRate,
            totalRequests = stats.hitCount + stats.missCount,
            cacheEfficiency = if (usage.maxEntries > 0) usage.usagePercentage / 100f else 0f,
            averageAccessCount = avg
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

    data class PersistedCache(
        @SerializedName("version") val version: Int,
        @SerializedName("checksum") val checksum: String,
        @SerializedName("dataJson") val dataJson: String
    )
    data class CacheData(
        @SerializedName("entries") val entries: Map<String, CachedRecipe>,
        @SerializedName("requestHistory") val requestHistory: Map<String, RecipeAnalysisRequest>
    )
} 
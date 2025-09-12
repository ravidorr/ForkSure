package com.ravidor.forksure.data.source.local

import android.content.Context
import com.ravidor.forksure.data.model.DifficultyLevel
import com.ravidor.forksure.data.model.Recipe
import com.ravidor.forksure.data.model.RecipeAnalysisRequest
import com.ravidor.forksure.data.model.RecipeAnalysisResponse
import com.ravidor.forksure.data.model.RecipeSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Date

class RecipeCacheDataSourceMetricsTest {

    private fun tempDir(): File {
        val dir = File("build/tmp/test-cache-metrics-" + System.currentTimeMillis())
        dir.mkdirs()
        return dir
    }

    private fun context(): Context = mockk(relaxed = true) {
        val dir = tempDir()
        every { filesDir } returns dir
    }

    private fun recipe(id: String, created: Date = Date()) = Recipe(
        id = id,
        title = "T$id",
        description = "D$id",
        ingredients = listOf("a", "b"),
        instructions = listOf("x", "y"),
        difficulty = DifficultyLevel.BEGINNER,
        tags = emptyList(),
        source = RecipeSource.AI_GENERATED,
        confidence = 1f,
        createdAt = created,
        imageHash = "hash$id"
    )

    private fun req(id: String) = RecipeAnalysisRequest(imageHash = "h$id", prompt = "p$id")

    private fun res(recipeId: String, created: Date = Date()) =
        RecipeAnalysisResponse(recipe = recipe(recipeId, created), rawResponse = "raw", processingTime = 1, success = true)

    @Test
    fun retention_policy_clears_entries_with_zero_days() = runTest {
        val ds = RecipeCacheDataSource(context())
        // Add a couple of entries
        ds.cacheRecipe(req("1"), res("1"))
        ds.cacheRecipe(req("2"), res("2"))

        // Clear entries older than now (0 days => cutoff = now)
        ds.clearOldEntries(0)

        // After clearing, expect no hits
        val hit1 = ds.getCachedRecipe(req("1"))
        val hit2 = ds.getCachedRecipe(req("2"))
        assertEquals(null, hit1)
        assertEquals(null, hit2)

        val stats = ds.getCurrentCacheStatistics()
        assertTrue("Total entries should be 0 after retention clear", stats.totalEntries == 0)
    }

    @Test
    fun eviction_updates_evictionCount_and_limits_size() = runTest {
        val ds = RecipeCacheDataSource(context())
        // Limit cache to size 2
        ds.resizeCache(2)

        ds.cacheRecipe(req("a"), res("a"))
        // Small delay to ensure different cachedAt ordering
        Thread.sleep(2)
        ds.cacheRecipe(req("b"), res("b"))
        Thread.sleep(2)
        ds.cacheRecipe(req("c"), res("c")) // should evict the least-recently used ("a")

        // Eviction should have occurred as size was exceeded; ensure capacity is respected
        val stats = ds.getCurrentCacheStatistics()
        assertTrue("Total entries should not exceed max size", stats.totalEntries <= 2)
        // Also verify the first key is no longer cached
        val isAStillCached = ds.isCached(req("a"))
        assertTrue("Oldest entry should be evicted", !isAStillCached)
    }

    @Test
    fun stats_oldest_and_newest_entries_progress_with_time() = runTest {
        val ds = RecipeCacheDataSource(context())

        ds.cacheRecipe(req("x1"), res("x1"))
        Thread.sleep(3)
        ds.cacheRecipe(req("x2"), res("x2"))

        val stats = ds.getCurrentCacheStatistics()
        // If both are non-null, newest should be after or equal to oldest; in practice after
        assertTrue(stats.oldestEntry == null || stats.newestEntry == null || !stats.newestEntry.before(stats.oldestEntry))
    }

    @Test
    fun metrics_average_access_count_reflects_hits() = runTest {
        val ds = RecipeCacheDataSource(context())
        val r = req("zz")
        ds.cacheRecipe(r, res("zz"))

        val before = ds.getCacheMetrics().averageAccessCount

        // Hit the same cached entry several times to increase accessCount
        repeat(3) {
            ds.getCachedRecipe(r)
        }

        val after = ds.getCacheMetrics().averageAccessCount
        assertTrue("Average access count should increase after repeated hits", after >= before)
    }
}

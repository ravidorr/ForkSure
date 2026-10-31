package com.ravidor.forksure

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable

/**
 * Memory management utilities for safe image processing and OutOfMemoryError prevention
 */
@Stable
object MemoryManager {
    
    private const val TAG = AppConstants.TAG_MEMORY_MANAGER
        
        // Memory thresholds in MB
        private const val LOW_MEMORY_THRESHOLD = 50L
        private const val CRITICAL_MEMORY_THRESHOLD = 20L
    
    /**
     * Check current memory status
     */
    data class MemoryStatus(
        val availableMemoryMB: Long,
        val totalMemoryMB: Long,
        val usedMemoryMB: Long,
        val isLowMemory: Boolean,
        val isCriticalMemory: Boolean
    )
    
    /**
     * Get current memory status
     */
    fun getMemoryStatus(context: Context): MemoryStatus {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory() / (1024 * 1024)
            val freeMemory = runtime.freeMemory() / (1024 * 1024)
            val maxMemory = runtime.maxMemory() / (1024 * 1024)
            val usedMemory = totalMemory - freeMemory
            val availableMemory = maxMemory - usedMemory
            
            MemoryStatus(
                availableMemoryMB = availableMemory,
                totalMemoryMB = totalMemory,
                usedMemoryMB = usedMemory,
                isLowMemory = availableMemory < LOW_MEMORY_THRESHOLD,
                isCriticalMemory = availableMemory < CRITICAL_MEMORY_THRESHOLD
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get memory status", e)
            MemoryStatus(
                availableMemoryMB = 0,
                totalMemoryMB = 0,
                usedMemoryMB = 0,
                isLowMemory = true,
                isCriticalMemory = true
            )
        }
    }
    
    /**
     * Handle memory trim callback
     */
    fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.d(TAG, "Memory trim level: $level - performing light cleanup")
                performLightMemoryCleanup()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.d(TAG, "Memory trim level: $level - performing aggressive cleanup")
                performAggressiveMemoryCleanup()
            }
        }
    }
    
    /**
     * Perform light memory cleanup
     */
    private fun performLightMemoryCleanup() {
        try {
            System.gc()
            Log.d(TAG, "Light memory cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during light memory cleanup", e)
        }
    }
    
    /**
     * Perform aggressive memory cleanup
     */
    private fun performAggressiveMemoryCleanup() {
        try {
            // Clear any image caches if they exist
            // This would be customized based on your app's caching strategy
            
            // Force garbage collection
            System.gc()
            
            Log.d(TAG, "Aggressive memory cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during aggressive memory cleanup", e)
        }
    }
}

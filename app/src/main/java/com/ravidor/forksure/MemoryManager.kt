package com.ravidor.forksure

import android.app.ActivityManager
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
}

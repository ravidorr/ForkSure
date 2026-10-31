package com.ravidor.forksure

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Stable
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

/**
 * ANR (Application Not Responding) prevention system that monitors
 * the main thread for blocks and provides early warning detection.
 */
@Stable
class ANRWatchdog private constructor() {
    
    companion object {
        private const val TAG = AppConstants.TAG_ANR_WATCHDOG
        
        // ANR detection thresholds
        private const val ANR_WARNING_THRESHOLD_MS = 4000L // 4 seconds
        private const val ANR_CRITICAL_THRESHOLD_MS = 8000L // 8 seconds
        private const val WATCHDOG_INTERVAL_MS = 1000L // Check every second
        
        @Volatile
        private var INSTANCE: ANRWatchdog? = null
        
        fun getInstance(): ANRWatchdog {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ANRWatchdog().also { INSTANCE = it }
            }
        }
    }
    
    private val isWatching = AtomicBoolean(false)
    private val lastMainThreadResponse = AtomicLong(System.currentTimeMillis())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val watchdogScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var watchdogJob: Job? = null
    
    /**
     * Stop ANR monitoring
     */
    fun stopWatching() {
        if (isWatching.compareAndSet(true, false)) {
            Log.d(TAG, "Stopping ANR watchdog monitoring")
            
            watchdogJob?.cancel()
            watchdogJob = null
        }
    }
    
    /**
     * Main monitoring loop that checks for main thread responsiveness
     */
    private suspend fun monitorMainThread() {
        while (isWatching.get()) {
            try {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastResponse = currentTime - lastMainThreadResponse.get()
                
                if (timeSinceLastResponse > ANR_CRITICAL_THRESHOLD_MS) {
                    handleCriticalANR(timeSinceLastResponse)
                } else if (timeSinceLastResponse > ANR_WARNING_THRESHOLD_MS) {
                    handleANRWarning(timeSinceLastResponse)
                }
                
                // Post a task to main thread to check responsiveness
                pingMainThread()
                
                delay(WATCHDOG_INTERVAL_MS.milliseconds)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in ANR monitoring loop", e)
                delay(WATCHDOG_INTERVAL_MS.milliseconds)
            }
        }
    }
    
    /**
     * Send a ping to the main thread to test responsiveness
     */
    private fun pingMainThread() {
        try {
            mainHandler.post {
                // Update timestamp when main thread responds
                lastMainThreadResponse.set(System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ping main thread", e)
        }
    }
    
    /**
     * Handle ANR warning (main thread blocked for > 4 seconds)
     */
    private fun handleANRWarning(blockedTimeMs: Long) {
        Log.w(TAG, "ANR Warning: Main thread blocked for ${blockedTimeMs}ms")
        
        try {
            // Capture stack trace of main thread
            val mainThread = Looper.getMainLooper().thread
            val stackTrace = mainThread.stackTrace
            val stackTraceString = stackTrace.joinToString("\n") { "  at $it" }
            
            // Log detailed information
            Log.w(TAG, "Main thread stack trace:\\n$stackTraceString")
            
            // Send non-fatal exception to Crashlytics
            val anrException = ANRException(
                "ANR Warning: Main thread blocked for ${blockedTimeMs}ms",
                stackTrace
            )
            
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("anr_blocked_time_ms", blockedTimeMs)
                setCustomKey("anr_severity", "WARNING")
                recordException(anrException)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling ANR warning", e)
        }
    }
    
    /**
     * Handle critical ANR (main thread blocked for > 8 seconds)
     */
    private fun handleCriticalANR(blockedTimeMs: Long) {
        Log.e(TAG, "Critical ANR: Main thread blocked for ${blockedTimeMs}ms")
        
        try {
            // Capture detailed system state
            val mainThread = Looper.getMainLooper().thread
            val stackTrace = mainThread.stackTrace
            val stackTraceString = stackTrace.joinToString("\n") { "  at $it" }
            
            // Log critical information
            Log.e(TAG, "Critical ANR - Main thread stack trace:\\n$stackTraceString")
            Log.e(TAG, "Critical ANR - System info: ${getSystemInfo()}")
            
            // Send high-priority exception to Crashlytics
            val criticalANRException = ANRException(
                "Critical ANR: Main thread blocked for ${blockedTimeMs}ms",
                stackTrace
            )
            
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("anr_blocked_time_ms", blockedTimeMs)
                setCustomKey("anr_severity", "CRITICAL")
                setCustomKey("memory_info", getMemoryInfo())
                setCustomKey("thread_count", Thread.activeCount())
                recordException(criticalANRException)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling critical ANR", e)
        }
    }
    
    /**
     * Get basic system information for ANR reporting
     */
    private fun getSystemInfo(): String {
        return try {
            "Available processors: ${Runtime.getRuntime().availableProcessors()}, " +
            "Active threads: ${Thread.activeCount()}"
        } catch (e: Exception) {
            "System info unavailable"
        }
    }
    
    /**
     * Get memory information for ANR reporting
     */
    private fun getMemoryInfo(): String {
        return try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory() / (1024 * 1024)
            val freeMemory = runtime.freeMemory() / (1024 * 1024)
            val maxMemory = runtime.maxMemory() / (1024 * 1024)
            
            "Total: ${totalMemory}MB, Free: ${freeMemory}MB, Max: ${maxMemory}MB"
        } catch (e: Exception) {
            "Memory info unavailable"
        }
    }
    
    /**
     * Get current watching status
     */
    fun isWatching(): Boolean = isWatching.get()
}

/**
 * Custom exception for ANR detection
 */
class ANRException(
    message: String,
    private val mainThreadStackTrace: Array<StackTraceElement>
) : RuntimeException(message) {
    
    override fun getStackTrace(): Array<StackTraceElement> {
        return mainThreadStackTrace
    }
    
    override fun toString(): String {
        return "${javaClass.simpleName}: $message"
    }
}

package com.ravidor.forksure

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Stable
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

// Centralized constants imports
import com.ravidor.forksure.AppConstants

/**
 * Testing and validation utilities for the crash prevention system.
 * These should only be used in debug builds for testing purposes.
 */
@Stable
object StabilityTestUtils {
    
    private const val TAG = AppConstants.TAG_STABILITY_TEST
        
        // Test parameters
        private const val MEMORY_PRESSURE_SIZE = 50 * 1024 * 1024 // 50MB
        private const val ANR_TEST_DURATION = 6000L // 6 seconds
        private const val NETWORK_TIMEOUT_TEST_DURATION = 15000L // 15 seconds
    
    private val testCounter = AtomicInteger(0)
    
    /**
     * Test the crash handler by triggering a controlled exception
     */
    fun testCrashHandler(testType: CrashTestType = CrashTestType.NULL_POINTER) {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "Crash handler tests only available in debug builds")
            return
        }
        
        Log.d(TAG, "Testing crash handler with type: $testType")
        
        // Add test metadata to Crashlytics
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("test_crash", true)
                setCustomKey("test_type", testType.name)
                setCustomKey("test_number", testCounter.incrementAndGet())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set test metadata", e)
        }
        
        // Trigger the specific crash type
        when (testType) {
            CrashTestType.NULL_POINTER -> {
                val nullString: String? = null
                @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
                nullString!!.length // This will throw NPE
            }
            CrashTestType.ARRAY_INDEX -> {
                val array = arrayOf(1, 2, 3)
                @Suppress("UNUSED_VARIABLE")
                val value = array[10] // This will throw ArrayIndexOutOfBoundsException
            }
            CrashTestType.STACK_OVERFLOW -> {
                recursiveFunction()
            }
            CrashTestType.OUT_OF_MEMORY -> {
                triggerOutOfMemory()
            }
            CrashTestType.ILLEGAL_STATE -> {
                throw IllegalStateException("Test illegal state exception")
            }
        }
    }
    
    /**
     * Test memory pressure scenarios
     */
    fun testMemoryPressure(context: Context): MemoryTestResult {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "Memory pressure tests only available in debug builds")
            return MemoryTestResult.NOT_AVAILABLE
        }
        
        Log.d(TAG, "Testing memory pressure scenarios")
        
        return try {
            val initialMemory = MemoryManager.getMemoryStatus(context)
            Log.d(TAG, "Initial memory: ${initialMemory.availableMemoryMB}MB available")
            
            // Allocate memory to create pressure
            val memoryBlocks = mutableListOf<ByteArray>()
            
            var shouldStop = false
            repeat(10) { iteration ->
                if (shouldStop) return@repeat
                
                try {
                    val block = ByteArray(MEMORY_PRESSURE_SIZE / 10)
                    memoryBlocks.add(block)
                    
                    val currentMemory = MemoryManager.getMemoryStatus(context)
                    Log.d(TAG, "After allocation $iteration: ${currentMemory.availableMemoryMB}MB available")
                    
                    if (currentMemory.isCriticalMemory) {
                        Log.w(TAG, "Critical memory reached at iteration $iteration")
                        shouldStop = true
                    }
                    
                } catch (e: OutOfMemoryError) {
                    Log.w(TAG, "OutOfMemoryError caught at iteration $iteration", e)
                    shouldStop = true
                }
            }
            
            // Clean up
            memoryBlocks.clear()
            System.gc()
            
            val finalMemory = MemoryManager.getMemoryStatus(context)
            Log.d(TAG, "Final memory: ${finalMemory.availableMemoryMB}MB available")
            
            MemoryTestResult.SUCCESS
            
        } catch (e: Exception) {
            Log.e(TAG, "Memory pressure test failed", e)
            MemoryTestResult.FAILED
        }
    }
    
    /**
     * Test ANR detection by blocking the main thread
     */
    fun testANRDetection() {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "ANR detection tests only available in debug builds")
            return
        }
        
        Log.d(TAG, "Testing ANR detection by blocking main thread")
        
        // This should trigger ANR warnings
        Handler(Looper.getMainLooper()).post {
            Log.d(TAG, "Starting main thread block for $ANR_TEST_DURATION ms")
            
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < ANR_TEST_DURATION) {
                // Busy wait to block main thread
                Thread.yield()
            }
            
            Log.d(TAG, "Main thread block completed")
        }
    }
    
    /**
     * Test network timeout scenarios
     */
    fun testNetworkTimeout() = runBlocking {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "Network timeout tests only available in debug builds")
            return@runBlocking
        }
        
        Log.d(TAG, "Testing network timeout scenarios")
        
        try {
            withTimeout(NETWORK_TIMEOUT_TEST_DURATION) {
                // Simulate long-running network operation
                delay(NETWORK_TIMEOUT_TEST_DURATION + 1000)
            }
        } catch (e: TimeoutCancellationException) {
            Log.d(TAG, "Network timeout test completed successfully")
            
            // Send to Crashlytics as non-fatal for testing
            FirebaseCrashlytics.getInstance().recordException(
                RuntimeException("Test network timeout", e)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Network timeout test failed", e)
        }
    }
    
    /**
     * Validate all stability systems are working
     */
    fun validateStabilitySystems(context: Context): StabilityValidationResult {
        Log.d(TAG, "Validating stability systems")
        
        val results = mutableListOf<String>()
        var allPassed = true
        
        // Check crash handler
        try {
            val crashHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (crashHandler is CrashHandler) {
                results.add("✓ Crash handler: Active")
            } else {
                results.add("✗ Crash handler: Not active")
                allPassed = false
            }
        } catch (e: Exception) {
            results.add("✗ Crash handler: Error checking")
            allPassed = false
        }
        
        // Check memory manager
        try {
            val memoryStatus = MemoryManager.getMemoryStatus(context)
            results.add("✓ Memory manager: Available ${memoryStatus.availableMemoryMB}MB")
        } catch (e: Exception) {
            results.add("✗ Memory manager: Error")
            allPassed = false
        }
        
        // Check ANR watchdog
        try {
            val anrWatchdog = ANRWatchdog.getInstance()
            if (anrWatchdog.isWatching()) {
                results.add("✓ ANR watchdog: Active")
            } else {
                results.add("✗ ANR watchdog: Not watching")
                allPassed = false
            }
        } catch (e: Exception) {
            results.add("✗ ANR watchdog: Error checking")
            allPassed = false
        }
        
        // Check Firebase Crashlytics
        try {
            FirebaseCrashlytics.getInstance().log("Stability validation check")
            results.add("✓ Firebase Crashlytics: Available")
        } catch (e: Exception) {
            results.add("✗ Firebase Crashlytics: Not available")
            allPassed = false
        }
        
        return StabilityValidationResult(
            allPassed = allPassed,
            results = results
        )
    }
    
    /**
     * Create sample bitmap for memory testing
     */
    fun createTestBitmap(width: Int = 2048, height: Int = 2048): android.graphics.Bitmap? {
        return try {
            android.graphics.Bitmap.createBitmap(
                width, height, 
                android.graphics.Bitmap.Config.ARGB_8888
            )
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Failed to create test bitmap due to memory constraints")
            null
        }
    }
    
    /**
     * Helper function to trigger stack overflow
     */
    private fun recursiveFunction(depth: Int = 0): Int {
        return if (depth < 1000000) { // This will cause stack overflow
            recursiveFunction(depth + 1)
        } else {
            depth
        }
    }
    
    /**
     * Helper function to trigger OutOfMemoryError
     */
    private fun triggerOutOfMemory() {
        val memoryList = mutableListOf<ByteArray>()
        while (true) {
            memoryList.add(ByteArray(1024 * 1024)) // 1MB chunks
        }
    }
}

/**
 * Types of crashes to test
 */
enum class CrashTestType {
    NULL_POINTER,
    ARRAY_INDEX,
    STACK_OVERFLOW,
    OUT_OF_MEMORY,
    ILLEGAL_STATE
}

/**
 * Result of memory testing
 */
enum class MemoryTestResult {
    SUCCESS,
    FAILED,
    NOT_AVAILABLE
}

/**
 * Result of stability system validation
 */
data class StabilityValidationResult(
    val allPassed: Boolean,
    val results: List<String>
) {
    fun getReport(): String {
        return "Stability Systems Validation\n" +
                "Status: ${if (allPassed) "PASS" else "FAIL"}\n\n" +
                results.joinToString("\n")
    }
}
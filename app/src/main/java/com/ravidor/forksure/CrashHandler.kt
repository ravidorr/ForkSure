package com.ravidor.forksure

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import android.os.Process
import android.util.Log
import androidx.compose.runtime.Stable
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

// Centralized constants imports
import com.ravidor.forksure.AppConstants

/**
 * Global exception handler that provides last-resort crash capture,
 * logging, and app recovery mechanisms.
 */
@Stable
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    
    companion object {
        private const val TAG = AppConstants.TAG_CRASH_HANDLER
        private const val PREF_CRASH_COUNT = "crash_count"
        private const val PREF_LAST_CRASH_TIME = "last_crash_time"
        private const val MAX_CRASHES_PER_HOUR = 3
        private const val CRASH_LOG_FILE = "crash_logs.txt"
        private const val HOUR_IN_MS = 60 * 60 * 1000L
    }
    
    private val crashPrefs: SharedPreferences = context.getSharedPreferences(
        "crash_handler_prefs", 
        Context.MODE_PRIVATE
    )
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", exception)
            
            // Check for crash loops
            if (isCrashLoop()) {
                Log.e(TAG, "Crash loop detected - terminating app without restart")
                handleCrashLoop()
                return
            }
            
            // Record crash for monitoring
            recordCrash(exception)
            
            // Log crash locally for offline analysis
            logCrashLocally(exception, thread)
            
            // Send to Firebase Crashlytics if available
            sendToCrashlytics(exception)
            
            // Clear volatile app state
            clearVolatileState()
            
            // Attempt graceful recovery
            attemptGracefulRecovery(exception)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in crash handler", e)
            // If crash handler crashes, pass to default handler
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    /**
     * Check if we're in a crash loop (too many crashes in short time)
     */
    private fun isCrashLoop(): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastCrashTime = crashPrefs.getLong(PREF_LAST_CRASH_TIME, 0)
        val crashCount = crashPrefs.getInt(PREF_CRASH_COUNT, 0)
        
        return if (currentTime - lastCrashTime < HOUR_IN_MS) {
            crashCount >= MAX_CRASHES_PER_HOUR
        } else {
            // Reset crash count if more than an hour has passed
            crashPrefs.edit {
                putInt(PREF_CRASH_COUNT, 0)
                putLong(PREF_LAST_CRASH_TIME, currentTime)
            }
            false
        }
    }
    
    /**
     * Handle crash loop situation - terminate without restart
     */
    private fun handleCrashLoop() {
        Log.e(TAG, "Terminating app due to crash loop")
        
        // Clear crash count for next app start
        crashPrefs.edit {
            putInt(PREF_CRASH_COUNT, 0)
            putLong(PREF_LAST_CRASH_TIME, 0)
        }
        
        // Force terminate
        Process.killProcess(Process.myPid())
        exitProcess(1)
    }
    
    /**
     * Record crash occurrence for monitoring
     */
    private fun recordCrash(exception: Throwable) {
        val currentTime = System.currentTimeMillis()
        val crashCount = crashPrefs.getInt(PREF_CRASH_COUNT, 0)
        
        crashPrefs.edit {
            putInt(PREF_CRASH_COUNT, crashCount + 1)
            putLong(PREF_LAST_CRASH_TIME, currentTime)
        }
        
        Log.d(TAG, "Recorded crash #${crashCount + 1}")
    }
    
    /**
     * Log crash details to local file for offline analysis
     */
    private fun logCrashLocally(exception: Throwable, thread: Thread) {
        try {
            val logFile = File(context.filesDir, CRASH_LOG_FILE)
            val fileWriter = FileWriter(logFile, true) // Append mode
            val printWriter = PrintWriter(fileWriter)
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
            
            printWriter.println("=== CRASH REPORT ===")
            printWriter.println("Timestamp: $timestamp")
            printWriter.println("Thread: ${thread.name}")
            printWriter.println("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            printWriter.println("Exception: ${exception.javaClass.simpleName}")
            printWriter.println("Message: ${exception.message}")
            printWriter.println("Stack Trace:")
            exception.printStackTrace(printWriter)
            printWriter.println("=====================")
            printWriter.println()
            
            printWriter.close()
            fileWriter.close()
            
            Log.d(TAG, "Crash logged to local file: ${logFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log crash locally", e)
        }
    }
    
    /**
     * Send crash details to Firebase Crashlytics
     */
    private fun sendToCrashlytics(exception: Throwable) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            // Add custom keys for better crash analysis
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
            crashlytics.setCustomKey("thread_name", Thread.currentThread().name)
            crashlytics.setCustomKey("available_memory", getAvailableMemory())
            
            // Record the exception
            crashlytics.recordException(exception)
            
            // Force send immediately
            crashlytics.sendUnsentReports()
            
            Log.d(TAG, "Crash sent to Firebase Crashlytics")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send crash to Crashlytics", e)
        }
    }
    
    /**
     * Clear volatile application state to prevent corruption
     */
    private fun clearVolatileState() {
        try {
            // Clear any temporary files
            val tempDir = File(context.cacheDir, "temp")
            if (tempDir.exists() && tempDir.isDirectory) {
                tempDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
            
            // Clear any in-memory caches (if any global cache managers exist)
            // This would be customized based on your app's architecture
            
            Log.d(TAG, "Volatile state cleared")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear volatile state", e)
        }
    }
    
    /**
     * Attempt graceful recovery by restarting the app
     */
    private fun attemptGracefulRecovery(exception: Throwable) {
        try {
            Log.d(TAG, "Attempting graceful recovery")
            
            // Create restart intent
            val restartIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("crashed", true)
                    putExtra("crash_reason", exception.message ?: "Unknown error")
                }
            
            if (restartIntent != null) {
                context.startActivity(restartIntent)
                Log.d(TAG, "App restart initiated")
            }
            
            // Terminate current process
            Process.killProcess(Process.myPid())
            exitProcess(0)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed graceful recovery", e)
            // Fall back to default handler
            defaultHandler?.uncaughtException(Thread.currentThread(), exception)
        }
    }
    
    /**
     * Get available memory in MB for crash reporting
     */
    private fun getAvailableMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            (runtime.freeMemory() / (1024 * 1024)) // Convert to MB
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Get crash logs for debugging (if needed)
     */
    fun getCrashLogs(): String? {
        return try {
            val logFile = File(context.filesDir, CRASH_LOG_FILE)
            if (logFile.exists()) {
                logFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read crash logs", e)
            null
        }
    }
    
    /**
     * Clear crash logs (for maintenance)
     */
    fun clearCrashLogs() {
        try {
            val logFile = File(context.filesDir, CRASH_LOG_FILE)
            if (logFile.exists()) {
                logFile.delete()
                Log.d(TAG, "Crash logs cleared")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash logs", e)
        }
    }
}
package com.ravidor.forksure

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Helper class for testing Firebase Crashlytics integration.
 * Provides utilities to verify that crash reporting is working correctly.
 */
@Stable
object CrashlyticsTestHelper {
    
    private const val TAG = "CrashlyticsTestHelper"
    
    /**
     * Test Firebase Crashlytics connectivity and configuration
     */
//    fun testCrashlyticsConnectivity(): CrashlyticsTestResult {
//        return try {
//            val crashlytics = FirebaseCrashlytics.getInstance()
//
//            // Note: isCrashlyticsCollectionEnabled is not always available
//            // We'll proceed with the test and let any errors surface
//
//            // Send a test log message
//            crashlytics.log("Crashlytics connectivity test - ${System.currentTimeMillis()}")
//
//            // Send a non-fatal test exception
//            val testException = RuntimeException("Crashlytics connectivity test - non-fatal")
//            crashlytics.recordException(testException)
//
//            // Force send any pending reports
//            crashlytics.sendUnsentReports()
//
//            Log.d(TAG, "Crashlytics test completed successfully")
//            CrashlyticsTestResult.SUCCESS
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Crashlytics test failed", e)
//            CrashlyticsTestResult.ERROR
//        }
//    }
    
    /**
     * Send a test non-fatal crash to verify reporting is working
     */
    fun sendTestNonFatalCrash(testType: String = "Manual Test") {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            // Add test metadata
            crashlytics.setCustomKey("test_type", testType)
            crashlytics.setCustomKey("test_timestamp", System.currentTimeMillis())
            crashlytics.setCustomKey("test_manual", true)
            
            // Create and send test exception
            val testException = RuntimeException("TEST CRASH - $testType - ${System.currentTimeMillis()}")
            crashlytics.recordException(testException)
            
            // Force immediate sending
            crashlytics.sendUnsentReports()
            
            Log.d(TAG, "Test non-fatal crash sent: $testType")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send test crash", e)
        }
    }
    
    /**
     * Test fatal crash (this will actually crash the app)
     * Use with caution - only for testing crash reporting
     */
//    fun sendTestFatalCrash(testType: String = "Fatal Test") {
//        Log.w(TAG, "Sending FATAL test crash: $testType")
//
//        try {
//            val crashlytics = FirebaseCrashlytics.getInstance()
//
//            // Add test metadata before crashing
//            crashlytics.setCustomKey("test_type", testType)
//            crashlytics.setCustomKey("test_timestamp", System.currentTimeMillis())
//            crashlytics.setCustomKey("test_fatal", true)
//            crashlytics.log("About to send fatal test crash: $testType")
//
//            // Force immediate sending of pending data
//            crashlytics.sendUnsentReports()
//
//            // Wait a moment for the data to be queued
//            Thread.sleep(100)
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error preparing fatal crash test", e)
//        }
//
//        // Now cause the actual crash
//        throw RuntimeException("TEST FATAL CRASH - $testType - ${System.currentTimeMillis()}")
//    }
    
    /**
     * Send comprehensive diagnostic information to Crashlytics
     */
    fun sendDiagnosticInfo(context: Context) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            // App information
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            crashlytics.setCustomKey("debug_build", BuildConfig.DEBUG)
            
            // Device information
            crashlytics.setCustomKey("android_version", android.os.Build.VERSION.RELEASE)
            crashlytics.setCustomKey("sdk_int", android.os.Build.VERSION.SDK_INT)
            crashlytics.setCustomKey("device_model", android.os.Build.MODEL)
            crashlytics.setCustomKey("device_manufacturer", android.os.Build.MANUFACTURER)
            
            // Memory information
            val memoryStatus = MemoryManager.getMemoryStatus(context)
            crashlytics.setCustomKey("available_memory_mb", memoryStatus.availableMemoryMB)
            crashlytics.setCustomKey("total_memory_mb", memoryStatus.totalMemoryMB)
            crashlytics.setCustomKey("used_memory_mb", memoryStatus.usedMemoryMB)
            crashlytics.setCustomKey("is_low_memory", memoryStatus.isLowMemory)
            crashlytics.setCustomKey("is_critical_memory", memoryStatus.isCriticalMemory)
            
            // Log diagnostic message
            crashlytics.log("Diagnostic information sent at ${System.currentTimeMillis()}")
            
            // Send as non-fatal for verification
            val diagnosticException = RuntimeException("Diagnostic information collection")
            crashlytics.recordException(diagnosticException)
            
            Log.d(TAG, "Diagnostic information sent to Crashlytics")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send diagnostic information", e)
        }
    }
    
    /**
     * Test crash reporting with a delayed crash
     * This gives time for Crashlytics to initialize properly
     */
    fun sendDelayedTestCrash(delayMs: Long = 2000, testType: String = "Delayed Test") {
        Log.d(TAG, "Scheduling delayed test crash in ${delayMs}ms")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(delayMs)
                sendTestNonFatalCrash(testType)
                Log.d(TAG, "Delayed test crash sent")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send delayed test crash", e)
            }
        }
    }
    
    /**
     * Verify Crashlytics configuration and provide troubleshooting info
     */
    fun verifyCrashlyticsSetup(context: Context): String {
        val report = StringBuilder()
        
        try {
            FirebaseCrashlytics.getInstance()
            
            report.appendLine("=== Crashlytics Configuration Report ===")
            report.appendLine("Crashlytics Instance: Available")
            report.appendLine("Build Type: ${BuildConfig.BUILD_TYPE}")
            report.appendLine("Debug Build: ${BuildConfig.DEBUG}")
            report.appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            
            // Check if google-services.json exists
            val googleServicesExists = try {
                context.assets.open("google-services.json").use { true }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            report.appendLine("Google Services Config: ${if (googleServicesExists) "Found" else "Missing"}")
            
            // Check Firebase App initialization
            val firebaseInitialized = try {
                com.google.firebase.FirebaseApp.getInstance()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            report.appendLine("Firebase Initialized: $firebaseInitialized")
            
            report.appendLine("Package Name: ${context.packageName}")
            report.appendLine("=== End Report ===")
            
        } catch (e: Exception) {
            report.appendLine("Error generating report: ${e.message}")
        }
        
        val reportString = report.toString()
        Log.d(TAG, reportString)
        return reportString
    }
}

/**
 * Result of Crashlytics connectivity test
 */
//enum class CrashlyticsTestResult {
//    SUCCESS,
//    DISABLED,
//    ERROR
//}
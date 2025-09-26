package com.ravidor.forksure

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Quick and simple way to test Crashlytics without UI.
 * Call this from anywhere in your app to send a test crash.
 */
object CrashlyticsQuickTest {
    
    private const val TAG = "CrashlyticsQuickTest"
    
    /**
     * Send a quick test crash to Firebase Crashlytics.
     * This will appear in the "Non-fatal" section of Firebase Console.
     */
    fun sendTestCrash() {
        try {
            Log.d(TAG, "Sending test crash to Firebase Crashlytics...")
            
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            // Add test metadata
            crashlytics.setCustomKey("quick_test", true)
            crashlytics.setCustomKey("test_timestamp", System.currentTimeMillis())
            crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            
            // Send a log message
            crashlytics.log("Quick test crash triggered at ${System.currentTimeMillis()}")
            
            // Create and send test exception
            val testException = RuntimeException("QUICK TEST CRASH - ${System.currentTimeMillis()}")
            crashlytics.recordException(testException)
            
            // Force immediate sending
            crashlytics.sendUnsentReports()
            
            Log.d(TAG, "Test crash sent successfully! Check Firebase Console in 2-5 minutes.")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send test crash", e)
        }
    }
}
package com.ravidor.forksure

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
// FIX: Import the non-deprecated constants with an alias to avoid naming conflicts.
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE as TRIM_COMPLETE
import android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE as TRIM_MODERATE

/**
 * Application class for ForkSure app with Hilt dependency injection
 */
@HiltAndroidApp
class ForkSureApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
        // ... (rest of onCreate) ...
        Log.d(AppConstants.TAG_APPLICATION, "ForkSure application initialized with Hilt DI and crash monitoring")
    }

    /**
     * Initialize Firebase services
     */
    private fun initializeFirebase() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            crashlytics.setUserId("user_${System.currentTimeMillis().toString().takeLast(8)}")

            Log.d(AppConstants.TAG_APPLICATION, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_APPLICATION, "Failed to initialize Firebase", e)
        }
    }

    // ... (other initialization methods) ...

    /**
     * Handle memory trim callbacks from the system.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(AppConstants.TAG_APPLICATION, "Memory trim callback: level $level")

        // FIX: Use the new, non-conflicting aliased constant. This will not be deprecated.
        if (level >= TRIM_MODERATE) {
            try {
                FirebaseCrashlytics.getInstance().log("Memory trim event: level $level")
            } catch (e: Exception) {
                Log.e(AppConstants.TAG_APPLICATION, "Failed to log memory trim to Crashlytics", e)
            }
        }
    }

    /**
     * Custom exception for better reporting in Crashlytics.
     */
    class LowMemoryException : RuntimeException("Low memory condition detected by onLowMemory() callback.")

    /**
     * Handle low memory callbacks.
     */
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(AppConstants.TAG_APPLICATION, "onLowMemory callback received. Delegating to onTrimMemory.")

        // FIX: Use the new, non-conflicting aliased constant. This will not be deprecated.
        onTrimMemory(TRIM_COMPLETE)

        try {
            FirebaseCrashlytics.getInstance().recordException(LowMemoryException())
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_APPLICATION, "Failed to log low memory to Crashlytics", e)
        }
    }
}

package com.ravidor.forksure

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.StrictMode
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Application class for ForkSure app with Hilt dependency injection
 */
@HiltAndroidApp
class ForkSureApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        initializeFirebase()
        
        // Initialize crash handler
        initializeCrashHandler()
        
        // Initialize StrictMode for debug builds
        initializeStrictMode()
        
        // Initialize ANR monitoring
        initializeANRMonitoring()
        
        // Initialize accessibility logging for the entire app
        AccessibilityTestHelper.logAccessibilityInfo(this, "ForkSure")
        
        // Send a quick test crash on startup (only in debug builds)
        if (BuildConfig.DEBUG) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                CrashlyticsQuickTest.sendTestCrash()
            }, 3000) // Wait 3 seconds after app startup
        }
        
        // Log app initialization
        Log.d(AppConstants.TAG_APPLICATION, "ForkSure application initialized with Hilt DI and crash monitoring")
    }
    
    /**
     * Initialize Firebase services
     */
    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            
            // Configure Crashlytics
            val crashlytics = FirebaseCrashlytics.getInstance()
            // Enable Crashlytics in debug builds for testing (disable in production)
            crashlytics.setCrashlyticsCollectionEnabled(true)
            
            // Set user identifier for crash reports (anonymized)
            crashlytics.setUserId("user_${System.currentTimeMillis().toString().takeLast(8)}")
            
            Log.d(AppConstants.TAG_APPLICATION, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_APPLICATION, "Failed to initialize Firebase", e)
        }
    }
    
    /**
     * Initialize global crash handler
     */
    private fun initializeCrashHandler() {
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            val crashHandler = CrashHandler(this, defaultHandler)
            Thread.setDefaultUncaughtExceptionHandler(crashHandler)
            
            Log.d(AppConstants.TAG_APPLICATION, "Crash handler initialized")
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_APPLICATION, "Failed to initialize crash handler", e)
        }
    }
    
    /**
     * Initialize StrictMode for development builds to catch potential issues early
     */
    private fun initializeStrictMode() {
        if (BuildConfig.DEBUG) {
            try {
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork()
                        .detectCustomSlowCalls()
                        .penaltyLog()
                        .build()
                )
                
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .detectLeakedClosableObjects()
                        .detectLeakedRegistrationObjects()
                        .detectActivityLeaks()
                        .detectFileUriExposure()
                        .penaltyLog()
                        .build()
                )
                
                Log.d(AppConstants.TAG_APPLICATION, "StrictMode enabled for debug build")
            } catch (e: Exception) {
                Log.e(AppConstants.TAG_APPLICATION, "Failed to initialize StrictMode", e)
            }
        }
    }
    
    /**
     * Initialize ANR monitoring system
     */
    private fun initializeANRMonitoring() {
        try {
            val anrWatchdog = ANRWatchdog.getInstance()
            anrWatchdog.startWatching()
            
            Log.d(AppConstants.TAG_APPLICATION, "ANR monitoring initialized")
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_APPLICATION, "Failed to initialize ANR monitoring", e)
        }
    }
    
    /**
     * Handle memory trim callbacks from the system
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        Log.d(AppConstants.TAG_APPLICATION, "Memory trim callback: level $level")
        MemoryManager.onTrimMemory(level)
        
        // Also send to Crashlytics as non-fatal for monitoring
        try {
            FirebaseCrashlytics.getInstance().log("Memory trim: level $level")
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_APPLICATION, "Failed to log memory trim to Crashlytics", e)
        }
    }
    
    /**
     * Handle low memory callbacks
     */
    override fun onLowMemory() {
        super.onLowMemory()
        
        Log.w(AppConstants.TAG_APPLICATION, "Low memory callback received")
        MemoryManager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        
        // Log to Crashlytics as non-fatal
        try {
            FirebaseCrashlytics.getInstance().recordException(
                RuntimeException("Low memory condition detected")
            )
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_APPLICATION, "Failed to log low memory to Crashlytics", e)
        }
    }
}

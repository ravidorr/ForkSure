package com.ravidor.forksure

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for ForkSure app with Hilt dependency injection
 */
@HiltAndroidApp
class ForkSureApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize accessibility logging for the entire app
        AccessibilityTestHelper.logAccessibilityInfo(this, "ForkSure")
        
        // Log app initialization
        android.util.Log.d(AppConstants.TAG_APPLICATION, "ForkSure application initialized with Hilt DI")
    }
} 
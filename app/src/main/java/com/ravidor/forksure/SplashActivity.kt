package com.ravidor.forksure

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Activity for ForkSure
 * Handles splash screen functionality across all Android versions
 * 
 * For Android 12+: Uses the new Splash Screen API
 * For older versions: Uses custom theme-based splash screen
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
            
            // Optional: Keep splash screen visible longer
            splashScreen.setKeepOnScreenCondition { false }
        }
        
        super.onCreate(savedInstanceState)
        
        // For older Android versions, we use the theme-based approach
        // The splash screen background is defined in the theme
        
        // Short delay to show splash screen
        lifecycleScope.launch {
            delay(SPLASH_DISPLAY_LENGTH)
            
            // Start main activity
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    companion object {
        private const val SPLASH_DISPLAY_LENGTH = 1500L // 1.5 seconds
    }
} 
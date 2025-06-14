package com.ravidor.forksure

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash Activity for ForkSure
 * Handles splash screen functionality with theme-aware backgrounds
 * The splash screen theming is handled by the activity's theme configuration
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // The splash screen background is defined in the theme
        // No need to set any content - the theme handles the display
        
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
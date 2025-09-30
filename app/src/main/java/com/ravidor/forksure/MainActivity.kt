package com.ravidor.forksure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.ravidor.forksure.navigation.ForkSureNavigation
import com.ravidor.forksure.ui.theme.ForkSureTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var isLoading = true // Example flag for background work
        // 1. Install the splash screen
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // 1.5 Enable modern edge-to-edge behavior (back-compat via Activity 1.8+)
        enableEdgeToEdge()

        // 2. (Optional) Keep the splash screen visible while loading
        splashScreen.setKeepOnScreenCondition {
            isLoading
        }

        // Simulate a network call or data loading
        lifecycleScope.launch {
            isLoading = false // Mark loading as complete
        }
        
        // Initialize accessibility logging
        AccessibilityTestHelper.logAccessibilityInfo(this, "ForkSure")
        
        setContent {
            val context = LocalContext.current
            
            // Log accessibility status when app starts
            LaunchedEffect(Unit) {
                if (AccessibilityTestHelper.isScreenReaderEnabled(context)) {
                    android.util.Log.d("ForkSure", "Screen reader detected - enhanced accessibility features active")
                }
                
                // Test our crash prevention system (DEBUG ONLY)
//                if (BuildConfig.DEBUG) {
//                    try {
//                        // Validate all stability systems are working
//                        val validation = StabilityTestUtils.validateStabilitySystems(context)
//                        android.util.Log.d("StabilityTest", validation.getReport())
//                        // Test Firebase Crashlytics integration
//                        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
//                            log("ForkSure crash prevention system initialized")
//                            setCustomKey("stability_systems_active", validation.allPassed)
//                            setCustomKey("app_version", BuildConfig.VERSION_NAME)
//                        }
//                        android.util.Log.d("Firebase", "Crashlytics integration test completed")
//                    } catch (e: Exception) {
//                        android.util.Log.e("StabilityTest", "Error testing crash prevention system", e)
//                    }
//                }
            }
            
            ForkSureTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .semantics {
                            contentDescription = "ForkSure - AI-powered baking assistant application"
                        },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ForkSureNavigation()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check accessibility status when app resumes
        if (AccessibilityTestHelper.isAccessibilityEnabled(this)) {
            android.util.Log.d("ForkSure", "Accessibility services are active")
        }
    }
}
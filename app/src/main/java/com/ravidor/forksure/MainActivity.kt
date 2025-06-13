package com.ravidor.forksure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ravidor.forksure.navigation.ForkSureNavigation
import com.ravidor.forksure.ui.theme.ForkSureTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize accessibility logging
        AccessibilityTestHelper.logAccessibilityInfo(this, "ForkSure")
        
        setContent {
            val context = LocalContext.current
            
            // Log accessibility status when app starts
            LaunchedEffect(Unit) {
                if (AccessibilityTestHelper.isScreenReaderEnabled(context)) {
                    android.util.Log.d("ForkSure", "Screen reader detected - enhanced accessibility features active")
                }
            }
            
            ForkSureTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
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
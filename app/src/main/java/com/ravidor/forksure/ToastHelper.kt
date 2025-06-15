package com.ravidor.forksure

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Stable

/**
 * Simple Toast helper for components that don't have access to the MessageContainer system
 * Used primarily for camera capture feedback and other navigation-level messaging
 */
@Stable
object ToastHelper {
    
    /**
     * Show a success toast message
     */
    fun showSuccess(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
        
        // Announce for accessibility if screen reader is enabled
        if (AccessibilityHelper.isScreenReaderEnabled(context)) {
            AccessibilityHelper.announceForAccessibility(context, message)
        }
    }
    
    /**
     * Show an error toast message
     */
    fun showError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
        
        // Announce for accessibility if screen reader is enabled
        if (AccessibilityHelper.isScreenReaderEnabled(context)) {
            AccessibilityHelper.announceForAccessibility(context, message)
        }
    }
    
    /**
     * Show an info toast message
     */
    fun showInfo(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
        
        // Announce for accessibility if screen reader is enabled
        if (AccessibilityHelper.isScreenReaderEnabled(context)) {
            AccessibilityHelper.announceForAccessibility(context, message)
        }
    }
} // CI/CD Test: Verifying messaging system works with automated testing

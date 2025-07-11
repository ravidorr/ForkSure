package com.ravidor.forksure

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Stable
import androidx.core.content.getSystemService

@Stable
object AccessibilityHelper {
    
    /**
     * Provides haptic feedback for accessibility actions
     */
    fun provideHapticFeedback(context: Context, type: HapticFeedbackType = HapticFeedbackType.CLICK) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)!!
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)!!
        }
        
        val effect = when (type) {
            HapticFeedbackType.CLICK -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            HapticFeedbackType.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
            HapticFeedbackType.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 200), -1)
            HapticFeedbackType.LONG_PRESS -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }
    
    /**
     * Checks if screen reader (TalkBack) is enabled
     */
    fun isScreenReaderEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)!!
        return accessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Announces text for accessibility services using modern View-based approach
     * This is the recommended way to make accessibility announcements
     */
    fun announceForAccessibility(context: Context, message: String) {
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)!!
        if (accessibilityManager.isEnabled) {
            // Truncate message if too long for accessibility
            val truncatedMessage = if (message.length > 200) {
                "${message.take(197)}..."
            } else {
                message
            }
            
            // Use the modern View-based approach for accessibility announcements
            // This avoids all deprecated AccessibilityEvent APIs
            try {
                val view = View(context)
                view.announceForAccessibility(truncatedMessage)
            } catch (e: Exception) {
                // Fallback: If View creation fails, we can still use the deprecated API with suppression
                // This ensures accessibility always works even in edge cases
                @Suppress("DEPRECATION")
                val event = android.view.accessibility.AccessibilityEvent.obtain().apply {
                    eventType = android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                    text.add(truncatedMessage)
                }
                accessibilityManager.sendAccessibilityEvent(event)
                @Suppress("DEPRECATION")
                event.recycle()
            }
        }
    }
    
    /**
     * Gets the user's preferred font scale for accessibility
     */
    fun getFontScale(context: Context): Float {
        val configuration = context.resources.configuration
        return configuration.fontScale
    }
    
    /**
     * Checks if large text is enabled for accessibility
     */
    fun isLargeTextEnabled(context: Context): Boolean {
        return getFontScale(context) > 1.0f
    }
}

@Stable
enum class HapticFeedbackType {
    CLICK,
    SUCCESS,
    ERROR,
    LONG_PRESS
} 
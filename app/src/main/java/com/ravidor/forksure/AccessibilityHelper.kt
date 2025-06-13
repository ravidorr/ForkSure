package com.ravidor.forksure

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AccessibilityHelper {
    
    /**
     * Provides haptic feedback for accessibility actions
     */
    fun provideHapticFeedback(context: Context, type: HapticFeedbackType = HapticFeedbackType.CLICK) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (type) {
                HapticFeedbackType.CLICK -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticFeedbackType.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                HapticFeedbackType.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 200), -1)
                HapticFeedbackType.LONG_PRESS -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            when (type) {
                HapticFeedbackType.CLICK -> vibrator.vibrate(50)
                HapticFeedbackType.SUCCESS -> vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
                HapticFeedbackType.ERROR -> vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 200), -1)
                HapticFeedbackType.LONG_PRESS -> vibrator.vibrate(100)
            }
        }
    }
}

enum class HapticFeedbackType {
    CLICK,
    SUCCESS,
    ERROR,
    LONG_PRESS
} 
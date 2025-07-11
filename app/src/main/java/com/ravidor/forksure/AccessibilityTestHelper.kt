package com.ravidor.forksure

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.content.getSystemService

/**
 * Comprehensive accessibility testing helper for ForkSure app
 * Provides tools for testing and validating accessibility features
 */
@Stable
object AccessibilityTestHelper {
    
    /**
     * Checks if accessibility services are enabled
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)!!
        return accessibilityManager.isEnabled
    }
    
    /**
     * Checks if TalkBack or other screen readers are enabled
     */
    fun isScreenReaderEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)!!
        return accessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Gets the minimum touch target size for accessibility compliance
     */
    fun getMinimumTouchTargetSize(): Dp = 48.dp
    
    /**
     * Gets the recommended touch target size for better accessibility
     */
    fun getRecommendedTouchTargetSize(): Dp = 56.dp
    
    /**
     * Validates if a touch target meets accessibility requirements
     */
    fun validateTouchTargetSize(size: Dp): AccessibilityValidationResult {
        return when {
            size < 48.dp -> AccessibilityValidationResult.FAIL
            size < 56.dp -> AccessibilityValidationResult.WARNING
            else -> AccessibilityValidationResult.PASS
        }
    }
    
    /**
     * Creates a modifier that ensures minimum touch target size
     */
    @Composable
    fun Modifier.ensureMinimumTouchTarget(
        currentSize: Dp = 0.dp
    ): Modifier {
        val minimumSize = getMinimumTouchTargetSize()
        return if (currentSize < minimumSize) {
            this.size(minimumSize)
        } else {
            this
        }
    }
    
    /**
     * Validates content description for accessibility
     */
    fun validateContentDescription(contentDescription: String?): AccessibilityValidationResult {
        return when {
            contentDescription.isNullOrBlank() -> AccessibilityValidationResult.FAIL
            contentDescription.length < 3 -> AccessibilityValidationResult.WARNING
            contentDescription.length > 500 -> AccessibilityValidationResult.WARNING
            else -> AccessibilityValidationResult.PASS
        }
    }
    
    /**
     * Generates accessibility report for debugging
     */
    fun generateAccessibilityReport(context: Context): AccessibilityReport {
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)!!
        
        return AccessibilityReport(
            isAccessibilityEnabled = accessibilityManager.isEnabled,
            isScreenReaderEnabled = accessibilityManager.isTouchExplorationEnabled,
            enabledServices = try {
                accessibilityManager.getEnabledAccessibilityServiceList(
                    -1 // All feedback types
                ).map { it.resolveInfo.serviceInfo.name }
            } catch (e: Exception) {
                emptyList()
            },
            recommendations = generateRecommendations(context)
        )
    }
    
    private fun generateRecommendations(context: Context): List<String> {
        val recommendations = mutableListOf<String>()
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)!!
        
        if (!accessibilityManager.isEnabled) {
            recommendations.add("Consider testing with accessibility services enabled")
        }
        
        if (!accessibilityManager.isTouchExplorationEnabled) {
            recommendations.add("Test with TalkBack or other screen readers for better validation")
        }
        
        recommendations.add("Ensure all interactive elements have meaningful content descriptions")
        recommendations.add("Test with different font sizes and display settings")
        recommendations.add("Verify color contrast meets WCAG guidelines")
        recommendations.add("Test navigation using only keyboard or switch controls")
        
        return recommendations
    }
    
    /**
     * Logs accessibility information for debugging
     */
    fun logAccessibilityInfo(context: Context, tag: String = "AccessibilityTest") {
        val report = generateAccessibilityReport(context)
        android.util.Log.d(tag, "Accessibility Report:")
        android.util.Log.d(tag, "- Accessibility Enabled: ${report.isAccessibilityEnabled}")
        android.util.Log.d(tag, "- Screen Reader Enabled: ${report.isScreenReaderEnabled}")
        android.util.Log.d(tag, "- Enabled Services: ${report.enabledServices.joinToString(", ")}")
        android.util.Log.d(tag, "- Recommendations:")
        report.recommendations.forEach { recommendation ->
            android.util.Log.d(tag, "  * $recommendation")
        }
    }
}

@Stable
enum class AccessibilityValidationResult {
    PASS,
    WARNING,
    FAIL,
    NOT_APPLICABLE
}

@Immutable
data class AccessibilityReport(
    val isAccessibilityEnabled: Boolean,
    val isScreenReaderEnabled: Boolean,
    val enabledServices: List<String>,
    val recommendations: List<String>
)

@Immutable
object AccessibilityConstants {
    // WCAG 2.1 AA compliance
    const val MIN_CONTRAST_RATIO_NORMAL = 4.5
    const val MIN_CONTRAST_RATIO_LARGE = 3.0
    
    // Touch target sizes
    val MIN_TOUCH_TARGET_SIZE = 48.dp
    val RECOMMENDED_TOUCH_TARGET_SIZE = 56.dp
    
    // Text sizes
    val MIN_TEXT_SIZE = 14.dp
    val LARGE_TEXT_SIZE = 18.dp
    
    // Animation durations (reduced for accessibility)
    const val SHORT_ANIMATION_DURATION = 200L
    const val MEDIUM_ANIMATION_DURATION = 300L
    const val LONG_ANIMATION_DURATION = 500L
    
    // Content description guidelines
    const val MIN_CONTENT_DESCRIPTION_LENGTH = 3
    const val MAX_CONTENT_DESCRIPTION_LENGTH = 500
} 
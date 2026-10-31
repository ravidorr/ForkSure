package com.ravidor.forksure

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

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

@Immutable
data class AccessibilityReport(
    val isAccessibilityEnabled: Boolean,
    val isScreenReaderEnabled: Boolean,
    val enabledServices: List<String>,
    val recommendations: List<String>
)

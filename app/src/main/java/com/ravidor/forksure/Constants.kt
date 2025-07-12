package com.ravidor.forksure

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Centralized constants for the ForkSure application
 * This file contains all configuration values, dimensions, colors, and other constants
 * to improve maintainability and consistency across the app.
 */

/**
 * Application-wide configuration constants
 */
@Immutable
object AppConstants {
    const val APP_NAME = "ForkSure"
    const val DEVELOPER_EMAIL = "support@avidor.org" // Use generic support email instead of personal
    
    // Logging tags
    const val TAG_APPLICATION = "ForkSureApplication"
    const val TAG_BAKING_VIEW_MODEL = "BakingViewModel"
    const val TAG_SECURITY_MANAGER = "SecurityManager"
    const val TAG_ERROR_HANDLER = "ErrorHandler"
    const val TAG_ENHANCED_ERROR_HANDLER = "EnhancedErrorHandler"
    const val TAG_CONTENT_REPORTING = "ContentReporting"
    const val TAG_SECURITY_STATUS = "SecurityStatusIndicator"
}

/**
 * UI dimension constants for consistent spacing and sizing
 */
@Immutable
object Dimensions {
    // Standard padding values
    val PADDING_EXTRA_SMALL: Dp = 4.dp
    val PADDING_SMALL: Dp = 8.dp
    val PADDING_MEDIUM: Dp = 12.dp
    val PADDING_STANDARD: Dp = 16.dp
    val PADDING_LARGE: Dp = 24.dp
    val PADDING_EXTRA_LARGE: Dp = 32.dp
    
    // Specific UI element sizes
    val BORDER_WIDTH_STANDARD: Dp = 4.dp
    val CORNER_RADIUS_SMALL: Dp = 6.dp
    val CORNER_RADIUS_STANDARD: Dp = 8.dp
    val CORNER_RADIUS_LARGE: Dp = 12.dp
    
    // Image and component sizes
    val SAMPLE_IMAGE_SIZE: Dp = 120.dp
    val CAPTURED_IMAGE_HEIGHT: Dp = 200.dp
    val CAMERA_BUTTON_SIZE: Dp = 64.dp
    val ICON_SIZE_SMALL: Dp = 12.dp
    val ICON_SIZE_STANDARD: Dp = 16.dp
    val ICON_SIZE_LARGE: Dp = 24.dp
    val STATUS_DOT_SIZE: Dp = 6.dp
    
    // Accessibility dimensions
    val MIN_TOUCH_TARGET: Dp = 48.dp
    val RECOMMENDED_TOUCH_TARGET: Dp = 56.dp
    val MIN_TEXT_SIZE: Dp = 14.dp
    val LARGE_TEXT_SIZE: Dp = 18.dp
}

/**
 * Color constants for consistent theming
 */
@Immutable
object AppColors {
    // Status colors (work well in both light and dark themes)
    val SUCCESS_COLOR = Color(0xFF4CAF50) // Green
    val WARNING_COLOR = Color(0xFFFF9800) // Orange  
    val ERROR_COLOR = Color(0xFFF44336) // Red
    val INFO_COLOR = Color(0xFF2196F3) // Blue
    
    // ForkSure Brand Colors - Light Theme
    val FORKSURE_PRIMARY_LIGHT = Color(0xFF6650a4) // Deep purple
    val FORKSURE_SECONDARY_LIGHT = Color(0xFF625b71) // Purple grey
    val FORKSURE_TERTIARY_LIGHT = Color(0xFF7D5260) // Pink
    val FORKSURE_SURFACE_LIGHT = Color(0xFFFFFBFE) // Off-white
    val FORKSURE_BACKGROUND_LIGHT = Color(0xFFFFFBFE) // Off-white
    val FORKSURE_ON_PRIMARY_LIGHT = Color(0xFFFFFFFF) // White
    val FORKSURE_ON_SURFACE_LIGHT = Color(0xFF1C1B1F) // Dark grey
    val FORKSURE_ON_BACKGROUND_LIGHT = Color(0xFF1C1B1F) // Dark grey
    
    // ForkSure Brand Colors - Dark Theme
    val FORKSURE_PRIMARY_DARK = Color(0xFFD0BCFF) // Light purple
    val FORKSURE_SECONDARY_DARK = Color(0xFFCCC2DC) // Light purple grey
    val FORKSURE_TERTIARY_DARK = Color(0xFFEFB8C8) // Light pink
    val FORKSURE_SURFACE_DARK = Color(0xFF1C1B1F) // Dark grey
    val FORKSURE_BACKGROUND_DARK = Color(0xFF121212) // True dark
    val FORKSURE_ON_PRIMARY_DARK = Color(0xFF371E73) // Dark purple
    val FORKSURE_ON_SURFACE_DARK = Color(0xFFE6E1E5) // Light grey
    val FORKSURE_ON_BACKGROUND_DARK = Color(0xFFE6E1E5) // Light grey
    
    // Legacy Material 3 colors (for backward compatibility)
    val PURPLE_80 = FORKSURE_PRIMARY_DARK
    val PURPLE_GREY_80 = FORKSURE_SECONDARY_DARK
    val PINK_80 = FORKSURE_TERTIARY_DARK
    val PURPLE_40 = FORKSURE_PRIMARY_LIGHT
    val PURPLE_GREY_40 = FORKSURE_SECONDARY_LIGHT
    val PINK_40 = FORKSURE_TERTIARY_LIGHT
    
    // Legacy background colors (for backward compatibility)
    val SURFACE_LIGHT = FORKSURE_SURFACE_LIGHT
    val ON_BACKGROUND_LIGHT = FORKSURE_ON_BACKGROUND_LIGHT
    val ON_SURFACE_LIGHT = FORKSURE_ON_SURFACE_LIGHT
    
    // Dark theme status colors (adjusted for better visibility)
    val SUCCESS_COLOR_DARK = Color(0xFF66BB6A) // Lighter green
    val WARNING_COLOR_DARK = Color(0xFFFFB74D) // Lighter orange
    val ERROR_COLOR_DARK = Color(0xFFEF5350) // Lighter red
    val INFO_COLOR_DARK = Color(0xFF42A5F5) // Lighter blue
    
    // Transparency levels
    const val ALPHA_DISABLED = 0.38f
    const val ALPHA_MEDIUM = 0.7f
    const val ALPHA_LOW = 0.1f
}

// ===== Security Constants =====
object SecurityConstants {
    const val PREFS_NAME = "forksure_security_prefs"
    const val MAX_REQUESTS_PER_MINUTE = 10
    const val MAX_REQUESTS_PER_HOUR = 50
    const val MAX_REQUESTS_PER_DAY = 200
    const val MAX_PROMPT_LENGTH = 1000
    const val MAX_RESPONSE_LENGTH = 5000
    
    // Status update intervals
    const val UPDATE_INTERVAL_MS = 5000L // 5 seconds
    const val INITIAL_DELAY_MS = 100L // Small initial delay
    const val LOW_REQUESTS_THRESHOLD = 3 // Show warning when requests < 3
}

/**
 * Animation and timing constants
 */
@Immutable
object AnimationConstants {
    // Duration constants
    const val SHORT_ANIMATION_DURATION = 200L
    const val MEDIUM_ANIMATION_DURATION = 300L
    const val LONG_ANIMATION_DURATION = 500L
    
    // Delay constants
    const val HAPTIC_FEEDBACK_DELAY = 50L
    const val UI_UPDATE_DELAY = 100L
}

// Accessibility constants are defined in AccessibilityTestHelper.kt

/**
 * Network and API constants
 */
@Immutable
object NetworkConstants {
    // Timeouts
    const val CONNECTION_TIMEOUT_MS = 30000L // 30 seconds
    const val READ_TIMEOUT_MS = 60000L // 60 seconds
    
    // Retry configuration
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 1000L
    const val EXPONENTIAL_BACKOFF_MULTIPLIER = 2.0
}

/**
 * Content reporting constants
 */
@Immutable
object ContentReportingConstants {
    // Report types
    const val REPORT_TYPE_INAPPROPRIATE = "inappropriate"
    const val REPORT_TYPE_OFFENSIVE = "offensive"
    const val REPORT_TYPE_HARMFUL = "harmful"
    const val REPORT_TYPE_OTHER = "other"
    
    // Validation
    const val MAX_ADDITIONAL_DETAILS_LENGTH = 1000
    const val MIN_REPORT_REASON_LENGTH = 1
}

/**
 * Sample data constants
 */
@Immutable
object SampleDataConstants {
    // Sample images resource IDs
    val SAMPLE_IMAGES = arrayOf(
        R.drawable.baked_goods_1, // Cupcake
        R.drawable.baked_goods_2, // Cookies
        R.drawable.baked_goods_3, // Cake
    )
    
    // Sample image descriptions resource IDs
    val IMAGE_DESCRIPTIONS = arrayOf(
        R.string.image1_description,
        R.string.image2_description,
        R.string.image3_description,
    )
    
    // Sample image names for logging/debugging
    val IMAGE_NAMES = arrayOf(
        "Cupcake",
        "Cookies", 
        "Cake"
    )
}

/**
 * Navigation constants
 */
@Immutable
object NavigationConstants {
    // Route definitions
    const val ROUTE_MAIN = "main"
    const val ROUTE_CAMERA = "camera"
    const val ROUTE_RESULTS = "results"
    
    // Navigation accessibility descriptions
    const val ACCESSIBILITY_NAVIGATION_TO_CAMERA = "Navigating to camera screen"
    const val ACCESSIBILITY_NAVIGATION_TO_MAIN = "Navigating to main screen"
    const val ACCESSIBILITY_NAVIGATION_TO_RESULTS = "Navigating to results screen"
}

/**
 * Theme-aware color utilities for dynamic light/dark mode support
 */
@Immutable
object ThemeColors {
    /**
     * Get theme-aware success color
     */
    @Composable
    fun successColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (isDarkTheme) AppColors.SUCCESS_COLOR_DARK else AppColors.SUCCESS_COLOR
    }
    
    /**
     * Get theme-aware warning color
     */
    @Composable
    fun warningColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (isDarkTheme) AppColors.WARNING_COLOR_DARK else AppColors.WARNING_COLOR
    }
    
    /**
     * Get theme-aware error color
     */
    @Composable
    fun errorColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (isDarkTheme) AppColors.ERROR_COLOR_DARK else AppColors.ERROR_COLOR
    }
    
    /**
     * Get theme-aware info color
     */
    @Composable
    fun infoColor(isDarkTheme: Boolean = isSystemInDarkTheme()): Color {
        return if (isDarkTheme) AppColors.INFO_COLOR_DARK else AppColors.INFO_COLOR
    }
} 

package com.ravidor.forksure.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ravidor.forksure.AppColors

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.FORKSURE_PRIMARY_DARK,
    secondary = AppColors.FORKSURE_SECONDARY_DARK,
    tertiary = AppColors.FORKSURE_TERTIARY_DARK,
    background = AppColors.FORKSURE_BACKGROUND_DARK,
    surface = AppColors.FORKSURE_SURFACE_DARK,
    onPrimary = AppColors.FORKSURE_ON_PRIMARY_DARK,
    onSecondary = AppColors.FORKSURE_ON_SURFACE_DARK,
    onTertiary = AppColors.FORKSURE_ON_SURFACE_DARK,
    onBackground = AppColors.FORKSURE_ON_BACKGROUND_DARK,
    onSurface = AppColors.FORKSURE_ON_SURFACE_DARK,
    error = AppColors.ERROR_COLOR_DARK,
    onError = AppColors.FORKSURE_ON_PRIMARY_DARK
)

private val LightColorScheme = lightColorScheme(
    primary = AppColors.FORKSURE_PRIMARY_LIGHT,
    secondary = AppColors.FORKSURE_SECONDARY_LIGHT,
    tertiary = AppColors.FORKSURE_TERTIARY_LIGHT,
    background = AppColors.FORKSURE_BACKGROUND_LIGHT,
    surface = AppColors.FORKSURE_SURFACE_LIGHT,
    onPrimary = AppColors.FORKSURE_ON_PRIMARY_LIGHT,
    onSecondary = AppColors.FORKSURE_ON_SURFACE_LIGHT,
    onTertiary = AppColors.FORKSURE_ON_SURFACE_LIGHT,
    onBackground = AppColors.FORKSURE_ON_BACKGROUND_LIGHT,
    onSurface = AppColors.FORKSURE_ON_SURFACE_LIGHT,
    error = AppColors.ERROR_COLOR,
    onError = AppColors.FORKSURE_ON_PRIMARY_LIGHT
)

@Composable
fun ForkSureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Set status bar appearance based on theme (this is the modern way)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            
            // Use edge-to-edge display for modern Android experience
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Set transparent status bar to let the app content show through
            // This is the modern approach that avoids the deprecated statusBarColor
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
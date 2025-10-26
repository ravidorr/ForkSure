package com.ravidor.forksure.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

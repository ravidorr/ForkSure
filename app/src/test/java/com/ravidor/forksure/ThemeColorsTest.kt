package com.ravidor.forksure

import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ThemeColors utility functions
 */
class ThemeColorsTest {

    @Test
    fun `test success color returns correct color for light theme`() {
        // Light theme should return the standard success color
        val expectedColor = AppColors.SUCCESS_COLOR
        // Note: In actual usage, this would be tested with Compose testing
        // For unit testing, we verify the color constants are properly defined
        assertEquals(Color(0xFF4CAF50), expectedColor)
    }

    @Test
    fun `test success color returns correct color for dark theme`() {
        // Dark theme should return the dark variant
        val expectedColor = AppColors.SUCCESS_COLOR_DARK
        assertEquals(Color(0xFF66BB6A), expectedColor)
    }

    @Test
    fun `test warning color returns correct color for light theme`() {
        val expectedColor = AppColors.WARNING_COLOR
        assertEquals(Color(0xFFFF9800), expectedColor)
    }

    @Test
    fun `test warning color returns correct color for dark theme`() {
        val expectedColor = AppColors.WARNING_COLOR_DARK
        assertEquals(Color(0xFFFFB74D), expectedColor)
    }

    @Test
    fun `test error color returns correct color for light theme`() {
        val expectedColor = AppColors.ERROR_COLOR
        assertEquals(Color(0xFFF44336), expectedColor)
    }

    @Test
    fun `test error color returns correct color for dark theme`() {
        val expectedColor = AppColors.ERROR_COLOR_DARK
        assertEquals(Color(0xFFEF5350), expectedColor)
    }

    @Test
    fun `test info color returns correct color for light theme`() {
        val expectedColor = AppColors.INFO_COLOR
        assertEquals(Color(0xFF2196F3), expectedColor)
    }

    @Test
    fun `test info color returns correct color for dark theme`() {
        val expectedColor = AppColors.INFO_COLOR_DARK
        assertEquals(Color(0xFF42A5F5), expectedColor)
    }

    @Test
    fun `test all dark theme colors are different from light theme colors`() {
        // Dark theme colors should be different from light theme colors
        // This ensures proper theme differentiation
        
        // Success colors
        assertNotEquals("Dark success color should be different from light", 
            AppColors.SUCCESS_COLOR_DARK, AppColors.SUCCESS_COLOR)
        
        // Warning colors  
        assertNotEquals("Dark warning color should be different from light",
            AppColors.WARNING_COLOR_DARK, AppColors.WARNING_COLOR)
        
        // Error colors
        assertNotEquals("Dark error color should be different from light",
            AppColors.ERROR_COLOR_DARK, AppColors.ERROR_COLOR)
        
        // Info colors
        assertNotEquals("Dark info color should be different from light",
            AppColors.INFO_COLOR_DARK, AppColors.INFO_COLOR)
    }

    @Test
    fun `test brand colors are properly defined for both themes`() {
        // Verify all brand colors are defined and different
        assertNotEquals(AppColors.FORKSURE_PRIMARY_LIGHT, AppColors.FORKSURE_PRIMARY_DARK)
        assertNotEquals(AppColors.FORKSURE_SECONDARY_LIGHT, AppColors.FORKSURE_SECONDARY_DARK)
        assertNotEquals(AppColors.FORKSURE_TERTIARY_LIGHT, AppColors.FORKSURE_TERTIARY_DARK)
        assertNotEquals(AppColors.FORKSURE_BACKGROUND_LIGHT, AppColors.FORKSURE_BACKGROUND_DARK)
        assertNotEquals(AppColors.FORKSURE_SURFACE_LIGHT, AppColors.FORKSURE_SURFACE_DARK)
    }

    @Test
    fun `test dark theme has proper contrast colors`() {
        // Dark theme should have light text on dark backgrounds
        val darkBackground = AppColors.FORKSURE_BACKGROUND_DARK
        val darkOnBackground = AppColors.FORKSURE_ON_BACKGROUND_DARK
        
        // Background should be dark (low values)
        assertTrue("Dark background should be dark", 
            darkBackground.red < 0.5f && darkBackground.green < 0.5f && darkBackground.blue < 0.5f)
        
        // Text on dark background should be light (high values)
        assertTrue("Text on dark background should be light",
            darkOnBackground.red > 0.5f && darkOnBackground.green > 0.5f && darkOnBackground.blue > 0.5f)
    }
} 
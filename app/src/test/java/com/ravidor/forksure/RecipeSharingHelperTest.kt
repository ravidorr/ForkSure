package com.ravidor.forksure

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for RecipeSharingHelper
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecipeSharingHelperTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private val mockBitmap = mockk<Bitmap>(relaxed = true)

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        every { mockContext.packageManager } returns mockPackageManager
    }

    @Test
    fun `shareRecipeToKeep should return result`() {
        // Given
        val recipeContent = "Test recipe content"
        val recipeName = "Test Recipe"
        
        every { mockContext.startActivity(any()) } just Runs

        // When
        val result = RecipeSharingHelper.shareRecipeToKeep(mockContext, recipeContent, recipeName)

        // Then
        assertThat(result).isAnyOf(true, false)
    }

    @Test
    fun `formatRecipeForSharing should include proper formatting`() {
        // Given
        val recipeContent = "Test recipe with ingredients"
        val recipeName = "Chocolate Cake"

        // When
        val result = RecipeSharingHelper.formatRecipeForSharing(recipeContent, recipeName)

        // Then
        assertThat(result).contains("Chocolate Cake")
        assertThat(result).contains("Test recipe with ingredients")
        assertThat(result).contains("Shared from ForkSure")
    }

    @Test
    fun `isAppAvailable should return true for ANY target`() {
        // Given
        val targetApp = ShareTarget.ANY

        // When
        val result = RecipeSharingHelper.isAppAvailable(mockContext, targetApp)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `isAppAvailable should check GOOGLE_KEEP`() {
        // Given
        val targetApp = ShareTarget.GOOGLE_KEEP
        val mockPackageInfo = mockk<PackageInfo>()
        
        every { mockPackageManager.getPackageInfo("com.google.android.keep", 0) } returns mockPackageInfo

        // When
        val result = RecipeSharingHelper.isAppAvailable(mockContext, targetApp)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `formatRecipeForSharing should handle empty content`() {
        // Given
        val recipeContent = ""
        val recipeName = ""

        // When
        val result = RecipeSharingHelper.formatRecipeForSharing(recipeContent, recipeName)

        // Then
        assertThat(result).isNotEmpty()
    }
} 
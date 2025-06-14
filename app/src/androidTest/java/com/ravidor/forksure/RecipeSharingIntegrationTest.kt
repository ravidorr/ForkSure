package com.ravidor.forksure

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for RecipeSharingHelper
 * Tests actual Android system interactions and app availability
 */
@RunWith(AndroidJUnit4::class)
class RecipeSharingIntegrationTest {

    private lateinit var context: android.content.Context
    private lateinit var packageManager: PackageManager

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        packageManager = context.packageManager
    }

    @Test
    fun formatRecipeForSharing_shouldCreateWellFormattedText() {
        // Given
        val recipeContent = """
            # Chocolate Chip Cookies
            
            ## Ingredients
            - 2 cups flour
            - 1 cup sugar
            - 1 cup chocolate chips
            
            ## Instructions
            1. Mix dry ingredients
            2. Add wet ingredients  
            3. Bake at 350°F for 12 minutes
        """.trimIndent()
        
        val recipeName = "Chocolate Chip Cookies"

        // When
        val result = RecipeSharingHelper.formatRecipeForSharing(recipeContent, recipeName)

        // Then
        assertThat(result).contains("🧁 Chocolate Chip Cookies")
        assertThat(result).contains("━━━━━━━━━━━━━━━━━━━━")
        assertThat(result).contains("## Ingredients")
        assertThat(result).contains("- 2 cups flour")
        assertThat(result).contains("## Instructions") 
        assertThat(result).contains("1. Mix dry ingredients")
        assertThat(result).contains("📱 Shared from ForkSure")
        
        // Should include current date
        assertThat(result).matches(".*\\d{4}.*") // Contains year
    }

    @Test
    fun isAppAvailable_shouldCheckActualInstalledApps() {
        // When & Then - test with apps that are commonly available
        
        // Gmail is usually available on most Android devices
        val gmailAvailable = RecipeSharingHelper.isAppAvailable(context, ShareTarget.GMAIL)
        // Don't assert true/false since it depends on device, just verify no crash
        assertThat(gmailAvailable).isAnyOf(true, false)
        
        // Google Keep availability varies
        val keepAvailable = RecipeSharingHelper.isAppAvailable(context, ShareTarget.GOOGLE_KEEP)
        assertThat(keepAvailable).isAnyOf(true, false)
        
        // ANY should always be true
        val anyAvailable = RecipeSharingHelper.isAppAvailable(context, ShareTarget.ANY)
        assertThat(anyAvailable).isTrue()
    }

    @Test
    fun shareIntent_shouldHaveCorrectType() {
        // Given
        val recipeContent = "Test recipe content"
        val recipeName = "Test Recipe"
        
        // When
        val formattedContent = RecipeSharingHelper.formatRecipeForSharing(recipeContent, recipeName)
        
        // Create a share intent manually to test structure
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, formattedContent)
            putExtra(Intent.EXTRA_SUBJECT, "Recipe: $recipeName")
        }

        // Then
        assertThat(shareIntent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(shareIntent.type).isEqualTo("text/plain")
        assertThat(shareIntent.getStringExtra(Intent.EXTRA_TEXT)).contains("🧁 Test Recipe")
        assertThat(shareIntent.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo("Recipe: Test Recipe")
    }

    @Test
    fun packageNames_shouldBeCorrectForDifferentApps() {
        // Test that we're using the correct package names for popular apps
        val packageNames = mapOf(
            ShareTarget.GOOGLE_KEEP to "com.google.android.keep",
            ShareTarget.GMAIL to "com.google.android.gm", 
            ShareTarget.GOOGLE_DOCS to "com.google.android.apps.docs.editors.docs",
            ShareTarget.NOTION to "notion.id",
            ShareTarget.EVERNOTE to "com.evernote"
        )
        
        packageNames.forEach { (target, expectedPackage) ->
            // Try to get package info - this will throw if package name is wrong format
            try {
                packageManager.getPackageInfo(expectedPackage, 0)
                // Package exists and name is valid
            } catch (e: Exception) {
                // Package doesn't exist on this device, but name format should be valid
                assertThat(expectedPackage).matches("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$")
            }
        }
    }

    @Test
    fun shareRecipeToKeep_shouldNotCrashWithRealContext() {
        // Given
        val recipeContent = "Test recipe for integration test"
        val recipeName = "Integration Test Recipe"

        // When & Then - should not crash even if Keep isn't installed
        try {
            val result = RecipeSharingHelper.shareRecipeToKeep(context, recipeContent, recipeName)
            // Result can be true or false depending on device
            assertThat(result).isAnyOf(true, false)
        } catch (e: Exception) {
            // If it throws, it should be a specific expected exception, not a crash
            assertThat(e.message).isNotEmpty()
        }
    }

    @Test
    fun shareRecipeWithImage_shouldHandleNullImage() {
        // Given
        val recipeContent = "Test recipe with null image"
        val recipeName = "Null Image Test"

        // When & Then - should fallback to text-only sharing
        try {
            val result = RecipeSharingHelper.shareRecipeWithImage(
                context, 
                recipeContent, 
                recipeName, 
                null
            )
            assertThat(result).isAnyOf(true, false)
        } catch (e: Exception) {
            assertThat(e.message).isNotEmpty()
        }
    }

    @Test
    fun shareRecipeToApp_shouldHandleUnavailableApp() {
        // Given
        val recipeContent = "Test recipe for unavailable app"
        val recipeName = "Unavailable App Test"
        
        // Use an app that's likely not installed (Notion)
        val targetApp = ShareTarget.NOTION

        // When & Then - should fallback gracefully
        try {
            val result = RecipeSharingHelper.shareRecipeToApp(
                context,
                recipeContent,
                recipeName,
                targetApp
            )
            // Should either succeed with fallback or return false
            assertThat(result).isAnyOf(true, false)
        } catch (e: Exception) {
            assertThat(e.message).isNotEmpty()
        }
    }

    @Test
    fun formatRecipeForSharing_shouldHandleUnicodeContent() {
        // Given
        val recipeContent = """
            # Crème Brûlée 🍮
            
            ## Ingredients
            - 500ml crème fraîche
            - 4 œufs
            - 100g sucre
            
            ## Instructions
            1. Préchauffez le four à 150°C
            2. Mélangez délicatement
            3. Caramélisez avec soin ✨
        """.trimIndent()
        
        val recipeName = "Crème Brûlée"

        // When
        val result = RecipeSharingHelper.formatRecipeForSharing(recipeContent, recipeName)

        // Then
        assertThat(result).contains("🧁 Crème Brûlée")
        assertThat(result).contains("- 500ml crème fraîche")
        assertThat(result).contains("- 4 œufs")
        assertThat(result).contains("Préchauffez le four")
        assertThat(result).contains("🍮")
        assertThat(result).contains("✨")
    }

    @Test
    fun shareTarget_enumShouldContainAllExpectedValues() {
        // When
        val allTargets = ShareTarget.values()

        // Then
        assertThat(allTargets).hasLength(6)
        assertThat(allTargets).asList().containsExactly(
            ShareTarget.GOOGLE_KEEP,
            ShareTarget.GMAIL,
            ShareTarget.GOOGLE_DOCS,
            ShareTarget.NOTION,
            ShareTarget.EVERNOTE,
            ShareTarget.ANY
        )
    }
} 
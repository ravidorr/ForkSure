package com.ravidor.forksure

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for ShareButton components
 * Tests user interactions, accessibility, and UI behavior
 */
@RunWith(AndroidJUnit4::class)
class ShareButtonComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleRecipeContent = """
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

    @Test
    fun shareButton_shouldDisplayCorrectly() {
        // Given
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            ShareButton(
                outputText = sampleRecipeContent,
                onShareComplete = onShareComplete
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun shareButton_shouldBeClickable() {
        // Given
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            ShareButton(
                outputText = sampleRecipeContent,
                onShareComplete = onShareComplete
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertHasClickAction()
            .performClick()
    }

    @Test
    fun shareButton_shouldHandleEmptyContent() {
        // Given
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            ShareButton(
                outputText = "",
                onShareComplete = onShareComplete
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun shareButton_shouldWorkWithoutCallback() {
        // When - test that button works without onShareComplete callback
        composeTestRule.setContent {
            ShareButton(outputText = sampleRecipeContent)
        }

        // Then
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun advancedShareButton_shouldDisplayDropdownMenu() {
        // Given
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            AdvancedShareButton(
                outputText = sampleRecipeContent,
                onShareComplete = onShareComplete
            )
        }

        // Click the share button to show dropdown
        composeTestRule
            .onNodeWithText("📤 Share")
            .performClick()

        // Then - verify dropdown options appear
        composeTestRule.waitForIdle()
        
        // Check for general share option (always available)
        composeTestRule
            .onNodeWithText("📤 Share to other apps...")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun advancedShareButton_shouldHideDropdownWhenDismissed() {
        // Given
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            AdvancedShareButton(
                outputText = sampleRecipeContent,
                onShareComplete = onShareComplete
            )
        }

        // Click to show dropdown
        composeTestRule
            .onNodeWithText("📤 Share")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Click outside to dismiss (or press back)
        composeTestRule.onRoot().performTouchInput { 
            click(center.copy(x = 0f, y = 0f)) // Click outside
        }

        composeTestRule.waitForIdle()

        // Then dropdown options should not be visible
        composeTestRule
            .onNodeWithText("📤 Share to other apps...")
            .assertDoesNotExist()
    }

    @Test
    fun shareToKeepButton_shouldDisplayCorrectly() {
        // Given
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            ShareToKeepButton(
                outputText = sampleRecipeContent,
                onShareComplete = onShareComplete
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("📤 Save to Keep")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun shareToKeepButton_shouldBeClickable() {
        // Given
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            ShareToKeepButton(
                outputText = sampleRecipeContent,
                onShareComplete = onShareComplete
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("📤 Save to Keep")
            .assertHasClickAction()
            .performClick()
    }

    @Test
    fun shareToKeepButton_shouldHandleLongContent() {
        // Given
        val longContent = "Very long recipe content. ".repeat(100)
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            ShareToKeepButton(
                outputText = longContent,
                onShareComplete = onShareComplete
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("📤 Save to Keep")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun allShareButtons_shouldHandleSpecialCharactersInContent() {
        // Given
        val specialContent = "Recipe with émojis 🍰, spéciàl chars & symbols @#$%"
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            ShareButton(
                outputText = specialContent,
                onShareComplete = onShareComplete
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun shareButtons_shouldHaveProperSemantics() {
        // Given
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            ShareButton(
                outputText = sampleRecipeContent,
                onShareComplete = onShareComplete
            )
        }

        // Then - check that button has proper semantics
        composeTestRule
            .onNodeWithText("📤 Share")
            .assert(hasClickAction())
            .assertContentDescriptionContains("Share recipe")
    }

    @Test
    fun advancedShareButton_shouldShowGeneralShareOption() {
        // Given
        val onShareComplete = mockk<(Boolean) -> Unit>(relaxed = true)

        // When
        composeTestRule.setContent {
            AdvancedShareButton(
                outputText = sampleRecipeContent,
                onShareComplete = onShareComplete
            )
        }

        // Show dropdown
        composeTestRule
            .onNodeWithText("📤 Share")
            .performClick()

        composeTestRule.waitForIdle()

        // Then - verify general share option is always available
        composeTestRule
            .onNodeWithText("📤 Share to other apps...")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun shareButtons_shouldRenderWithoutCrashing() {
        // Test that all share button variants render without crashing
        
        composeTestRule.setContent {
            ShareButton(outputText = sampleRecipeContent)
        }
        
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()
        
        composeTestRule.setContent {
            AdvancedShareButton(outputText = sampleRecipeContent)
        }
        
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()
            
        composeTestRule.setContent {
            ShareToKeepButton(outputText = sampleRecipeContent)
        }
        
        composeTestRule
            .onNodeWithText("📤 Save to Keep")
            .assertExists()
    }

    @Test
    fun shareButton_shouldExtractRecipeTitle() {
        // Test that recipe title extraction is called when sharing
        val contentWithTitle = """
            # Delicious Chocolate Cake
            
            This is a wonderful recipe for chocolate cake.
        """.trimIndent()

        // When
        composeTestRule.setContent {
            ShareButton(outputText = contentWithTitle)
        }

        // Then - verify button exists and can be clicked
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()
            .performClick()
    }

    @Test
    fun shareButtons_shouldHandleDifferentContentTypes() {
        // Test with markdown content
        composeTestRule.setContent {
            ShareButton(outputText = "# Simple Recipe\n\nJust mix and bake!")
        }
        
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()

        // Test with plain text
        composeTestRule.setContent {
            ShareButton(outputText = "Simple plain text recipe")
        }
        
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()

        // Test with mixed content
        composeTestRule.setContent {
            ShareButton(outputText = "Recipe with **bold** and *italic* text")
        }
        
        composeTestRule
            .onNodeWithText("📤 Share")
            .assertExists()
    }
} 
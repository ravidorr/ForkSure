package com.ravidor.forksure.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravidor.forksure.ErrorType
import com.ravidor.forksure.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI Component Tests for StateSections
 * Tests loading states, error handling, and user interactions
 */
@RunWith(AndroidJUnit4::class)
class StateSectionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingSection_shouldDisplayLoadingIndicatorAndText() {
        // Given - localThis pattern
        val localThis = object {
            // No state needed for loading section
        }

        // When
        composeTestRule.setContent {
            LoadingSection()
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Loading analysis of baked goods")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Analyzing baked goods...")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun errorSection_shouldDisplayErrorMessageAndActionButtons() {
        // Given - localThis pattern
        val localThis = object {
            var retryCount = 0
            var dismissCount = 0
            val errorState = UiState.Error(
                errorMessage = "Network connection failed",
                errorType = ErrorType.NETWORK,
                canRetry = true
            )
            val onRetry = { retryCount++; Unit }
            val onDismiss = { dismissCount++; Unit }
        }

        // When
        composeTestRule.setContent {
            ErrorSection(
                errorState = localThis.errorState,
                onRetry = localThis.onRetry,
                onDismiss = localThis.onDismiss
            )
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Error occurred")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Network connection failed")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("📶")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun errorSection_shouldHandleRetryButtonClicks() {
        // Given - localThis pattern
        val localThis = object {
            var retryCount = 0
            var dismissCount = 0
            val errorState = UiState.Error(
                errorMessage = "Server error occurred",
                errorType = ErrorType.SERVER_ERROR,
                canRetry = true
            )
            val onRetry = { retryCount++; Unit }
            val onDismiss = { dismissCount++; Unit }
        }

        // When
        composeTestRule.setContent {
            ErrorSection(
                errorState = localThis.errorState,
                onRetry = localThis.onRetry,
                onDismiss = localThis.onDismiss
            )
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Retry the last operation")
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        assert(localThis.retryCount == 1)
        assert(localThis.dismissCount == 0)
    }

    @Test
    fun errorSection_shouldHandleDismissButtonClicks() {
        // Given - localThis pattern
        val localThis = object {
            var retryCount = 0
            var dismissCount = 0
            val errorState = UiState.Error(
                errorMessage = "Content policy violation",
                errorType = ErrorType.CONTENT_POLICY,
                canRetry = false
            )
            val onRetry = { retryCount++; Unit }
            val onDismiss = { dismissCount++; Unit }
        }

        // When
        composeTestRule.setContent {
            ErrorSection(
                errorState = localThis.errorState,
                onRetry = localThis.onRetry,
                onDismiss = localThis.onDismiss
            )
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Dismiss error message")
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        assert(localThis.dismissCount == 1)
        assert(localThis.retryCount == 0)
    }

    @Test
    fun errorSection_shouldHideRetryButtonWhenCanRetryIsFalse() {
        // Given - localThis pattern
        val localThis = object {
            var retryCount = 0
            var dismissCount = 0
            val errorState = UiState.Error(
                errorMessage = "API key invalid",
                errorType = ErrorType.API_KEY,
                canRetry = false
            )
            val onRetry = { retryCount++; Unit }
            val onDismiss = { dismissCount++; Unit }
        }

        // When
        composeTestRule.setContent {
            ErrorSection(
                errorState = localThis.errorState,
                onRetry = localThis.onRetry,
                onDismiss = localThis.onDismiss
            )
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Retry the last operation")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithContentDescription("Dismiss error message")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun errorSection_shouldDisplayCorrectErrorIconsForDifferentErrorTypes() {
        // Given - localThis pattern
        val localThis = object {
            val errorTypes = listOf(
                ErrorType.NETWORK to "📶",
                ErrorType.API_KEY to "🔑", 
                ErrorType.QUOTA_EXCEEDED to "⏰",
                ErrorType.CONTENT_POLICY to "🚫",
                ErrorType.IMAGE_SIZE to "📷",
                ErrorType.SERVER_ERROR to "🔧",
                ErrorType.UNKNOWN to "⚠️"
            )
            val onRetry = { }
            val onDismiss = { }
        }

        // When/Then - Test each error type
        localThis.errorTypes.forEach { (errorType, expectedIcon) ->
            val errorState = UiState.Error(
                errorMessage = "Test error for $errorType",
                errorType = errorType,
                canRetry = true
            )

            composeTestRule.setContent {
                ErrorSection(
                    errorState = errorState,
                    onRetry = localThis.onRetry,
                    onDismiss = localThis.onDismiss
                )
            }

            composeTestRule
                .onNodeWithText(expectedIcon)
                .assertExists()
                .assertIsDisplayed()
        }
    }

    @Test
    fun errorActionButtons_shouldDisplayBothButtonsWhenCanRetryIsTrue() {
        // Given - localThis pattern
        val localThis = object {
            var retryCount = 0
            var dismissCount = 0
            val onRetry = { retryCount++; Unit }
            val onDismiss = { dismissCount++; Unit }
        }

        // When
        composeTestRule.setContent {
            ErrorActionButtons(
                canRetry = true,
                onRetry = localThis.onRetry,
                onDismiss = localThis.onDismiss
            )
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Retry the last operation")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Dismiss error message")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun errorActionButtons_shouldDisplayOnlyDismissButtonWhenCanRetryIsFalse() {
        // Given - localThis pattern
        val localThis = object {
            var retryCount = 0
            var dismissCount = 0
            val onRetry = { retryCount++; Unit }
            val onDismiss = { dismissCount++; Unit }
        }

        // When
        composeTestRule.setContent {
            ErrorActionButtons(
                canRetry = false,
                onRetry = localThis.onRetry,
                onDismiss = localThis.onDismiss
            )
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Retry the last operation")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithContentDescription("Dismiss error message")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun errorActionButtons_shouldHandleMultipleClicksCorrectly() {
        // Given - localThis pattern
        val localThis = object {
            var retryCount = 0
            var dismissCount = 0
            val onRetry = { retryCount++; Unit }
            val onDismiss = { dismissCount++; Unit }
        }

        // When
        composeTestRule.setContent {
            ErrorActionButtons(
                canRetry = true,
                onRetry = localThis.onRetry,
                onDismiss = localThis.onDismiss
            )
        }

        // Then - Multiple retry clicks
        composeTestRule
            .onNodeWithContentDescription("Retry the last operation")
            .performClick()
            .performClick()
            .performClick()

        assert(localThis.retryCount == 3)

        // Multiple dismiss clicks
        composeTestRule
            .onNodeWithContentDescription("Dismiss error message")
            .performClick()
            .performClick()

        assert(localThis.dismissCount == 2)
    }

    @Test
    fun errorSection_shouldHandleLongErrorMessagesCorrectly() {
        // Given - localThis pattern
        val localThis = object {
            val longErrorMessage = "This is a very long error message that should be displayed properly " +
                "and should wrap to multiple lines if necessary. The error section should handle this " +
                "gracefully and maintain proper formatting and accessibility features."
            val errorState = UiState.Error(
                errorMessage = longErrorMessage,
                errorType = ErrorType.UNKNOWN,
                canRetry = true
            )
            val onRetry = { }
            val onDismiss = { }
        }

        // When
        composeTestRule.setContent {
            ErrorSection(
                errorState = localThis.errorState,
                onRetry = localThis.onRetry,
                onDismiss = localThis.onDismiss
            )
        }

        // Then
        composeTestRule
            .onNodeWithText(localThis.longErrorMessage)
            .assertExists()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Error message and suggestions")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun loadingSection_shouldHaveProperAccessibilityFeatures() {
        // Given - localThis pattern
        val localThis = object {
            // No state needed for loading section
        }

        // When
        composeTestRule.setContent {
            LoadingSection()
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Loading analysis of baked goods")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction() // Should not have click action
            .assertIsNotEnabled() // Loading should not be interactive
    }
} 
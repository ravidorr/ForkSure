package com.ravidor.forksure

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravidor.forksure.repository.FakeAIRepository
import com.ravidor.forksure.repository.FakeSecurityRepository
import com.ravidor.forksure.screens.MainScreen
import com.ravidor.forksure.state.rememberMainScreenState
import com.ravidor.forksure.ui.theme.ForkSureTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for MainScreen
 * Tests the complete user flow and UI interactions
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Inject
    lateinit var fakeAIRepository: FakeAIRepository

    @Inject
    lateinit var fakeSecurityRepository: FakeSecurityRepository

    private lateinit var bakingViewModel: BakingViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Reset fake repositories
        fakeAIRepository.reset()
        fakeSecurityRepository.reset()
        
        bakingViewModel = BakingViewModel(fakeAIRepository, fakeSecurityRepository)
    }

    @Test
    fun mainScreen_initialState_displaysCorrectly() {
        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Verify main components are displayed
        composeTestRule.onNodeWithText("ForkSure - Baking with AI").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Take photo button. Opens camera to capture baked goods").assertIsDisplayed()
        composeTestRule.onNodeWithText("Or choose a sample:").assertIsDisplayed()
        
        // Verify analyze button is initially disabled
        composeTestRule.onNodeWithContentDescription("Analyze button. Disabled. Select an image and enter a prompt to enable").assertIsNotEnabled()
    }

    @Test
    fun mainScreen_enterPromptAndSelectImage_enablesAnalyzeButton() {
        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter a prompt
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("What type of cake is this?")

        // Select a sample image (first one should be selected by default)
        // Analyze button should now be enabled
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .assertIsEnabled()
    }

    @Test
    fun mainScreen_analyzeWithSuccess_showsResults() {
        // Setup successful AI response
        fakeAIRepository.setSuccessResponse("This is a delicious chocolate cake with rich frosting.")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("What is this?")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        // Wait for and verify results
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("This is a delicious chocolate cake with rich frosting.").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Report content button. Report inappropriate AI-generated content").assertIsDisplayed()
    }

    @Test
    fun mainScreen_analyzeWithError_showsErrorMessage() {
        // Setup error response
        fakeAIRepository.setErrorResponse("Network connection failed")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("What is this?")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        // Wait for and verify error
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Error occurred during analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("🔧").assertIsDisplayed() // Server error icon
        composeTestRule.onNodeWithContentDescription("Retry analysis button. Try the AI analysis again").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dismiss error button. Clear the error message").assertIsDisplayed()
    }

    @Test
    fun mainScreen_analyzeWithRateLimit_showsRateLimitError() {
        // Setup rate limit
        fakeSecurityRepository.setRateLimited("Too many requests. Please wait.")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("What is this?")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        // Wait for and verify rate limit error
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Error occurred during analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("⏰").assertIsDisplayed() // Quota exceeded icon
        composeTestRule.onNodeWithText("Too many requests. Please wait.").assertIsDisplayed()
    }

    @Test
    fun mainScreen_analyzeWithInvalidInput_showsInputError() {
        // Setup invalid input
        fakeSecurityRepository.setInputInvalid("Input contains prohibited content")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("Invalid content")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        // Wait for and verify input validation error
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Error occurred during analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("🚫").assertIsDisplayed() // Content policy icon
        composeTestRule.onNodeWithText("Input contains prohibited content").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showLoadingState_displaysProgressIndicator() {
        // Setup delayed response
        fakeAIRepository.setDelay(1000L)
        fakeAIRepository.setSuccessResponse("Test response")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("What is this?")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        // Verify loading state
        composeTestRule.onNodeWithContentDescription("Loading AI analysis results").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analyzing your baked goods...").assertIsDisplayed()
    }

    @Test
    fun mainScreen_retryAfterError_worksCorrectly() {
        // Setup initial error then success
        fakeAIRepository.setErrorResponse("Initial error")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze (will fail)
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("What is this?")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        composeTestRule.waitForIdle()

        // Setup success for retry
        fakeAIRepository.setSuccessResponse("Retry successful!")

        // Click retry
        composeTestRule.onNodeWithContentDescription("Retry analysis button. Try the AI analysis again")
            .performClick()

        // Verify success
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Retry successful!").assertIsDisplayed()
    }

    @Test
    fun mainScreen_dismissError_clearsErrorState() {
        // Setup error
        fakeAIRepository.setErrorResponse("Test error")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Trigger error
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("What is this?")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        composeTestRule.waitForIdle()

        // Dismiss error
        composeTestRule.onNodeWithContentDescription("Dismiss error button. Clear the error message")
            .performClick()

        // Verify error is cleared
        composeTestRule.onNodeWithContentDescription("Error occurred during analysis").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_successfulAnalysis_showsPrintButton() {
        // Setup successful AI response
        fakeAIRepository.setSuccessResponse("# Chocolate Chip Cookies\n\nDelicious homemade cookies with chocolate chips.")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("What is this recipe?")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        // Wait for results and verify print button is displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Print recipe button. Print the AI-generated recipe").assertIsDisplayed()
        composeTestRule.onNodeWithText("🖨️ Print").assertIsDisplayed()
    }

    @Test
    fun mainScreen_printAndReportButtons_displayedTogether() {
        // Setup successful AI response
        fakeAIRepository.setSuccessResponse("Recipe content for testing print and report buttons")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("Analyze this recipe")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        // Wait for results and verify both buttons are displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Print recipe button. Print the AI-generated recipe").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Report content button. Report inappropriate AI-generated content").assertIsDisplayed()
        composeTestRule.onNodeWithText("🖨️ Print").assertIsDisplayed()
        composeTestRule.onNodeWithText("🚩 Report").assertIsDisplayed()
    }

    @Test
    fun mainScreen_printButton_isClickable() {
        // Setup successful AI response
        fakeAIRepository.setSuccessResponse("Test recipe for print functionality")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("Test recipe")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        // Wait for results and verify print button is clickable
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Print recipe button. Print the AI-generated recipe")
            .assertIsEnabled()
            .performClick() // This should not crash the test
    }

    @Test
    fun mainScreen_noResults_noPrintButton() {
        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Verify print button is not displayed in initial state
        composeTestRule.onNodeWithContentDescription("Print recipe button. Print the AI-generated recipe")
            .assertDoesNotExist()
    }

    @Test
    fun mainScreen_errorState_noPrintButton() {
        // Setup error response
        fakeAIRepository.setErrorResponse("Analysis failed")

        composeTestRule.setContent {
            ForkSureTheme {
                val selectedImage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
                MainScreen(
                    bakingViewModel = bakingViewModel,
                    capturedImage = null,
                    selectedImage = selectedImage,
                    onNavigateToCamera = {},
                    onCapturedImageUpdated = {}
                )
            }
        }

        // Enter prompt and analyze (will fail)
        composeTestRule.onNodeWithContentDescription("Prompt input field. Enter your question about the baked goods")
            .performTextInput("What is this?")
        
        composeTestRule.onNodeWithContentDescription("Analyze button. Start AI analysis of selected image with your prompt")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify print button is not displayed in error state
        composeTestRule.onNodeWithContentDescription("Print recipe button. Print the AI-generated recipe")
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Error occurred during analysis").assertIsDisplayed()
    }
} 
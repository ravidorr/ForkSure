package com.ravidor.forksure.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenStateTest {

    @Test
    fun mainScreenState_initialValues_areCorrect() {
        val state = MainScreenState(
            initialPrompt = "Initial Prompt",
            initialResult = "Initial Result",
            initialSelectedImageIndex = 1
        )

        assertEquals("Initial Prompt", state.prompt)
        assertEquals("Initial Result", state.result)
        assertEquals(1, state.selectedImageIndex)
        assertNull(state.capturedImage)
        assertFalse(state.showReportDialog)
        assertTrue(state.isAnalyzeEnabled)
    }

    @Test
    fun mainScreenState_stateUpdates_workAsExpected() {
        val state = MainScreenState()

        state.updatePrompt("New Prompt")
        assertEquals("New Prompt", state.prompt)

        state.updateResult("New Result")
        assertEquals("New Result", state.result)

        state.selectSampleImage(2)
        assertEquals(2, state.selectedImageIndex)
        assertTrue(state.hasSelectedSampleImage)

        state.showReportDialog()
        assertTrue(state.showReportDialog)

        state.hideReportDialog()
        assertFalse(state.showReportDialog)

        state.resetToInitialState()
        assertEquals("", state.prompt)
        assertEquals("", state.result)
        assertEquals(-2, state.selectedImageIndex)
    }

    @Test
    fun defaultMainScreenActions_onDismissError_invokesCallbackWithoutInfiniteRecursion() {
        val state = MainScreenState()
        var errorDismissed = false
        var retryCalled = false
        var clearStateCalled = false

        val actions = DefaultMainScreenActions(
            state = state,
            navigateToCamera = {},
            onAnalyze = { _, _ -> },
            onSubmitReport = {},
            onRetry = { retryCalled = true },
            onDismissErrorAction = { errorDismissed = true },
            onClearState = { clearStateCalled = true }
        )

        actions.onDismissError()
        assertTrue("onDismissError should invoke onDismissErrorAction callback", errorDismissed)

        actions.onRetryAnalysis()
        assertTrue(retryCalled)

        actions.onBackToMainScreen()
        assertTrue(clearStateCalled)
    }
}

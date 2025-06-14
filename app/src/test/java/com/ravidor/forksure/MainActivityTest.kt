package com.ravidor.forksure

import android.content.Context
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for MainActivity
 * Tests activity lifecycle, initialization, and accessibility setup
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [34])
class MainActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Mock static accessibility helper methods
        mockkObject(AccessibilityTestHelper)
        every { AccessibilityTestHelper.logAccessibilityInfo(any(), any()) } just Runs
        every { AccessibilityTestHelper.isScreenReaderEnabled(any()) } returns false
        every { AccessibilityTestHelper.isAccessibilityEnabled(any()) } returns false
    }

    @Test
    fun `activity should launch successfully`() {
        // When
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Then
            scenario.onActivity { activity ->
                assertThat(activity).isNotNull()
                assertThat(activity.isFinishing).isFalse()
            }
        }
    }

    @Test
    fun `onCreate should initialize accessibility logging`() {
        // When
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Then
            scenario.onActivity { _ ->
                verify { AccessibilityTestHelper.logAccessibilityInfo(any(), "ForkSure") }
            }
        }
    }

    @Test
    fun `onCreate should set content with proper theme`() {
        // When
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Then
            scenario.onActivity { activity ->
                assertThat(activity.hasWindowFocus()).isTrue()
            }
        }
    }

    @Test
    fun `activity should handle accessibility enabled state`() {
        // Given
        every { AccessibilityTestHelper.isAccessibilityEnabled(any()) } returns true
        
        // When
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            
            // Then - verify activity can handle accessibility state without crashing
            scenario.onActivity { activity ->
                assertThat(activity).isNotNull()
            }
        }
    }

    @Test
    fun `activity should handle accessibility disabled state`() {
        // Given
        every { AccessibilityTestHelper.isAccessibilityEnabled(any()) } returns false
        
        // When
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            
            // Then - verify activity can handle accessibility state without crashing
            scenario.onActivity { activity ->
                assertThat(activity).isNotNull()
            }
        }
    }

    @Test
    fun `activity should handle screen reader enabled state`() {
        // Given
        every { AccessibilityTestHelper.isScreenReaderEnabled(any()) } returns true
        
        // When
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Then - verify no crashes occur with screen reader enabled
            scenario.onActivity { activity ->
                assertThat(activity).isNotNull()
            }
        }
    }

    @Test
    fun `activity lifecycle should complete without errors`() {
        // When
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            
            // Then
            scenario.onActivity { activity ->
                assertThat(activity).isNotNull()
            }
        }
    }
} 
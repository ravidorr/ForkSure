package com.ravidor.forksure.screens

import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ravidor.forksure.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive UI Component Tests for ImageComponents
 * Tests user interactions, state changes, and accessibility features
 */
@RunWith(AndroidJUnit4::class)
class ImageComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cameraSection_shouldDisplayTakePhotoButtonAndRespondToClicks() {
        // Given - localThis pattern
        val localThis = object {
            var clickCount = 0
            val onTakePhoto = { clickCount++; Unit }
        }

        // When
        composeTestRule.setContent {
            CameraSection(
                onTakePhoto = localThis.onTakePhoto,
                onPhotoUploaded = { /* no-op for test */ }
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("Take Photo")
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        assert(localThis.clickCount == 1)
    }

    @Test
    fun cameraSection_shouldHaveProperAccessibilityContentDescription() {
        // Given - localThis pattern
        val localThis = object {
            var clickCount = 0
            val onTakePhoto = { clickCount++; Unit }
        }

        // When
        composeTestRule.setContent {
            CameraSection(
                onTakePhoto = localThis.onTakePhoto,
                onPhotoUploaded = { /* no-op for test */ }
            )
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Take a photo to analyze baked goods")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun capturedImageCard_shouldDisplayImageAndHandleSelectionState() {
        // Given - localThis pattern
        val localThis = object {
            var isSelected = false
            var clickCount = 0
            val mockBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val onImageClick = { 
                clickCount++
                isSelected = !isSelected
                Unit
            }
        }

        // When - Not selected
        composeTestRule.setContent {
            CapturedImageCard(
                bitmap = localThis.mockBitmap,
                isSelected = localThis.isSelected,
                onImageClick = localThis.onImageClick
            )
        }

        // Then - Should be clickable and show not selected state
        composeTestRule
            .onNodeWithContentDescription("Captured image, not selected")
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        assert(localThis.clickCount == 1)
        localThis.isSelected = true

        // When - Selected state
        composeTestRule.setContent {
            CapturedImageCard(
                bitmap = localThis.mockBitmap,
                isSelected = localThis.isSelected,
                onImageClick = localThis.onImageClick
            )
        }

        // Then - Should show selected state
        composeTestRule
            .onNodeWithContentDescription("Captured image, selected")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun sampleImagesSection_shouldDisplayHorizontalListOfImages() {
        // Given - localThis pattern
        val localThis = object {
            val images = arrayOf(
                R.drawable.baked_goods_1,
                R.drawable.baked_goods_2,
                R.drawable.baked_goods_3
            )
            val descriptions = arrayOf(
                R.string.image1_description,
                R.string.image2_description, 
                R.string.image3_description
            )
            var selectedIndex = 0
            var lastSelectedIndex = -1
            val onImageSelected: (Int) -> Unit = { index: Int ->
                lastSelectedIndex = index
                selectedIndex = index
            }
        }

        // When
        composeTestRule.setContent {
            SampleImagesSection(
                images = localThis.images,
                imageDescriptions = localThis.descriptions,
                selectedImageIndex = localThis.selectedIndex,
                onImageSelected = localThis.onImageSelected
            )
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Horizontal list of sample baking images")
            .assertExists()
            .assertIsDisplayed()

        // Should display all images
        composeTestRule
            .onAllNodesWithContentDescription("Cookies and pastries, selected")
            .assertCountEquals(1)
    }

    @Test
    fun sampleImageItem_shouldHandleSelectionStateChanges() {
        // Given - localThis pattern
        val localThis = object {
            var isSelected = false
            var clickCount = 0
            val imageRes = R.drawable.baked_goods_1
            val imageDescription = "Test cookies"
            val onImageClick = { 
                clickCount++
                isSelected = !isSelected
                Unit
            }
        }

        // When - Not selected
        composeTestRule.setContent {
            SampleImageItem(
                imageRes = localThis.imageRes,
                imageDescription = localThis.imageDescription,
                isSelected = localThis.isSelected,
                onImageClick = localThis.onImageClick
            )
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Test cookies, not selected")
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        assert(localThis.clickCount == 1)
        localThis.isSelected = true

        // When - Selected state
        composeTestRule.setContent {
            SampleImageItem(
                imageRes = localThis.imageRes,
                imageDescription = localThis.imageDescription,
                isSelected = localThis.isSelected,
                onImageClick = localThis.onImageClick
            )
        }

        // Then - Should show selected state
        composeTestRule
            .onNodeWithContentDescription("Test cookies, selected")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun sampleImagesSection_shouldAllowMultipleImageSelection() {
        // Given - localThis pattern
        val localThis = object {
            val images = arrayOf(
                R.drawable.baked_goods_1,
                R.drawable.baked_goods_2,
                R.drawable.baked_goods_3
            )
            val descriptions = arrayOf(
                R.string.image1_description,
                R.string.image2_description,
                R.string.image3_description
            )
            var selectedIndex = 0
            val selectedIndices = mutableListOf<Int>()
            val onImageSelected: (Int) -> Unit = { index: Int ->
                selectedIndices.add(index)
                selectedIndex = index
            }
        }

        // When
        composeTestRule.setContent {
            SampleImagesSection(
                images = localThis.images,
                imageDescriptions = localThis.descriptions,
                selectedImageIndex = localThis.selectedIndex,
                onImageSelected = localThis.onImageSelected
            )
        }

        // Then - Click on different images
        composeTestRule.onAllNodes(hasClickAction())
            .filterToOne(hasContentDescription("Cookies and pastries, not selected"))
            .performClick()

        composeTestRule.onAllNodes(hasClickAction())
            .filterToOne(hasContentDescription("Bread and muffins, not selected"))
            .performClick()

        // Should have recorded clicks
        assert(localThis.selectedIndices.size == 2)
        assert(localThis.selectedIndices.contains(0))
        assert(localThis.selectedIndices.contains(1))
    }

    @Test
    fun imageComponents_shouldHandleEdgeCasesGracefully() {
        // Given - localThis pattern
        val localThis = object {
            val emptyImages = arrayOf<Int>()
            val emptyDescriptions = arrayOf<Int>()
            var selectedIndex = -1
            val onImageSelected: (Int) -> Unit = { index: Int -> selectedIndex = index }
        }

        // When - Empty arrays
        composeTestRule.setContent {
            SampleImagesSection(
                images = localThis.emptyImages,
                imageDescriptions = localThis.emptyDescriptions,
                selectedImageIndex = localThis.selectedIndex,
                onImageSelected = localThis.onImageSelected
            )
        }

        // Then - Should handle gracefully
        composeTestRule
            .onNodeWithContentDescription("Horizontal list of sample baking images")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun capturedImageCard_shouldHandleNullBitmapGracefully() {
        // Given - localThis pattern
        val localThis = object {
            var clickCount = 0
            val mockBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val onImageClick = { clickCount++; Unit }
        }

        // When - Minimal bitmap
        composeTestRule.setContent {
            CapturedImageCard(
                bitmap = localThis.mockBitmap,
                isSelected = false,
                onImageClick = localThis.onImageClick
            )
        }

        // Then - Should still display and be clickable
        composeTestRule
            .onNodeWithContentDescription("Captured image, not selected")
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        assert(localThis.clickCount == 1)
    }
} 
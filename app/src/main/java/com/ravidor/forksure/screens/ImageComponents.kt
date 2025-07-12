package com.ravidor.forksure.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ravidor.forksure.AccessibilityHelper
import com.ravidor.forksure.Dimensions
import com.ravidor.forksure.HapticFeedbackType
import com.ravidor.forksure.R
import com.ravidor.forksure.ToastHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Image-related UI components for the main screen
 * Contains camera, upload, captured image, and sample image components
 */

@Composable
fun CameraSection(
    onTakePhoto: () -> Unit,
    onPhotoUploaded: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val takePhotoDescription = stringResource(R.string.accessibility_take_photo_description)
    val uploadPhotoDescription = stringResource(R.string.accessibility_upload_photo_description)
    val takePhotoText = stringResource(R.string.take_photo)
    val uploadPhotoText = stringResource(R.string.upload_photo)
    
    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (bitmap != null) {
                    onPhotoUploaded(bitmap)
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                    ToastHelper.showSuccess(context, context.getString(R.string.success_photo_uploaded))
                } else {
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
                    ToastHelper.showError(context, context.getString(R.string.error_invalid_image_format))
                }
            } catch (e: Exception) {
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
                ToastHelper.showError(context, context.getString(R.string.error_photo_upload_failed))
            }
        } ?: run {
            ToastHelper.showError(context, context.getString(R.string.error_no_photo_selected))
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.PADDING_STANDARD),
        horizontalArrangement = Arrangement.Center
    ) {
        // Take Photo Button
        Button(
            onClick = { 
                onTakePhoto()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .padding(bottom = Dimensions.PADDING_SMALL)
                .semantics {
                    contentDescription = takePhotoDescription
                }
        ) {
            Text(
                text = takePhotoText,
                style = MaterialTheme.typography.headlineSmall
            )
        }
        
        Spacer(modifier = Modifier.width(Dimensions.PADDING_STANDARD))
        
        // Upload Photo Button
        OutlinedButton(
            onClick = { 
                photoPickerLauncher.launch("image/*")
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .padding(bottom = Dimensions.PADDING_SMALL)
                .semantics {
                    contentDescription = uploadPhotoDescription
                }
        ) {
            Text(
                text = uploadPhotoText,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun CapturedImageCard(
    bitmap: Bitmap,
    isSelected: Boolean,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDescription = stringResource(R.string.accessibility_sample_image_selected, "Captured")
    val notSelectedDescription = stringResource(R.string.accessibility_sample_image_not_selected, "Captured")
    
    Card(
        modifier = modifier
            .padding(Dimensions.PADDING_STANDARD)
            .fillMaxWidth()
            .height(Dimensions.CAPTURED_IMAGE_HEIGHT)
            .clickable { 
                onImageClick()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            }
            .then(
                if (isSelected) {
                    Modifier.border(BorderStroke(Dimensions.BORDER_WIDTH_STANDARD, MaterialTheme.colorScheme.primary))
                } else Modifier
            )
            .semantics {
                contentDescription = if (isSelected) {
                    selectedDescription
                } else {
                    notSelectedDescription
                }
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SampleImagesSection(
    images: Array<Int>,
    imageDescriptions: Array<Int>,
    selectedImageIndex: Int,
    onImageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.PADDING_STANDARD)
            .semantics {
                contentDescription = "Horizontal list of sample baking images"
            }
    ) {
        itemsIndexed(images) { index, image ->
            SampleImageItem(
                imageRes = image,
                imageDescription = stringResource(imageDescriptions[index]),
                isSelected = index == selectedImageIndex,
                onImageClick = { onImageSelected(index) }
            )
        }
    }
}

@Composable
fun SampleImageItem(
    imageRes: Int,
    imageDescription: String,
    isSelected: Boolean,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    val selectedDescription = stringResource(R.string.accessibility_sample_image_selected, imageDescription)
    val notSelectedDescription = stringResource(R.string.accessibility_sample_image_not_selected, imageDescription)
    
    var imageModifier = Modifier
        .padding(start = Dimensions.PADDING_SMALL, end = Dimensions.PADDING_SMALL)
        .requiredSize(Dimensions.SAMPLE_IMAGE_SIZE)
        .clickable { 
            onImageClick()
            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
        }
        .semantics {
            contentDescription = if (isSelected) {
                selectedDescription
            } else {
                notSelectedDescription
            }
        }
        
    if (isSelected) {
        imageModifier = imageModifier.border(BorderStroke(Dimensions.BORDER_WIDTH_STANDARD, MaterialTheme.colorScheme.primary))
    }
    
    Image(
        painter = painterResource(imageRes),
        contentDescription = "",
        modifier = imageModifier
    )
} 
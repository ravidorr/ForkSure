package com.ravidor.forksure

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.core.graphics.createBitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import com.ravidor.forksure.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCapture(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit,
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Request permission only once when camera screen loads, if not already granted
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    val cameraScreenDescription = stringResource(R.string.accessibility_camera_screen)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = cameraScreenDescription
            }
    ) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onImageCaptured = onImageCaptured,
                onError = onError,
                onBackPressed = onBackPressed
            )
        } else {
            // Permission not granted state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .semantics {
                        contentDescription = cameraScreenDescription
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📷",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .semantics {
                            contentDescription = "Camera icon"
                        }
                )
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.camera_permission_settings_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Back button
                Button(
                    onClick = {
                        onBackPressed()
                        AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                    },
                    modifier = Modifier
                        .semantics {
                            contentDescription = "Go back to main screen button"
                        }
                ) {
                    Text(
                        text = stringResource(R.string.back_to_main_screen),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit,
    onBackPressed: () -> Unit = {}
) {
    val cameraViewfinderDescription = stringResource(R.string.accessibility_camera_viewfinder)
    val captureButtonDescription = stringResource(R.string.accessibility_capture_button_ready)
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var isCameraReady by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(cameraProviderFuture) {
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            isCameraReady = true
        } catch (exc: Exception) {
            onError(context.getString(R.string.camera_bind_error, exc.message ?: "Unknown error"))
        }
    }

    val backFromCameraDesc = stringResource(R.string.accessibility_back_from_camera)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = cameraViewfinderDescription
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Camera preview showing live view from camera"
                }
        )

        // Back button in top-left corner
        Button(
            onClick = {
                onBackPressed()
                AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .semantics {
                    contentDescription = backFromCameraDesc
                }
        ) {
            Text(
                text = stringResource(R.string.back_to_main_screen),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Camera status indicator
        if (!isCameraReady) {
            val initializingText = stringResource(R.string.camera_initializing)
            Text(
                text = initializingText,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .semantics {
                        contentDescription = "Camera status: $initializingText"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        FloatingActionButton(
            onClick = {
                if (isCameraReady && !isCapturing) {
                    isCapturing = true
                    AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.CLICK)
                    captureImage(
                        imageCapture = imageCapture,
                        executor = executor,
                        context = context,
                        onImageCaptured = { bitmap ->
                            isCapturing = false
                            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.SUCCESS)
                            onImageCaptured(bitmap)
                        },
                        onError = { error ->
                            isCapturing = false
                            AccessibilityHelper.provideHapticFeedback(context, HapticFeedbackType.ERROR)
                            onError(error)
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .size(64.dp)
                .semantics {
                    contentDescription = captureButtonDescription
                }
        ) {
            Text(
                text = "📷",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics {
                    contentDescription = "" // Handled by parent
                }
            )
        }

        // Capture status
        if (isCapturing) {
            val capturingText = stringResource(R.string.camera_capturing)
            Text(
                text = capturingText,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .semantics {
                        contentDescription = "Status: $capturingText"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun captureImage(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    context: Context,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = imageProxyToBitmap(image)
                    onImageCaptured(bitmap)
                } catch (e: Exception) {
                    onError(context.getString(R.string.camera_process_error, e.message ?: "Unknown error"))
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(context.getString(R.string.camera_capture_error, exception.message ?: "Unknown error"))
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    return when (image.format) {
        ImageFormat.JPEG -> {
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        ImageFormat.YUV_420_888 -> {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            // Copy Y plane
            yBuffer.get(nv21, 0, ySize)
            // Copy U and V planes (interleaved for NV21)
            val uvPixelStride = image.planes[1].pixelStride
            if (uvPixelStride == 1) {
                // Tightly packed UV data
                uBuffer.get(nv21, ySize, uSize)
                vBuffer.get(nv21, ySize + uSize, vSize)
            } else {
                // UV data is not tightly packed - need to extract manually
                val uvBuffer = ByteArray(uSize)
                uBuffer.get(uvBuffer)
                var uvIndex = ySize
                for (i in 0 until uSize step uvPixelStride) {
                    nv21[uvIndex++] = uvBuffer[i]
                }
                
                val vvBuffer = ByteArray(vSize)
                vBuffer.get(vvBuffer)
                for (i in 0 until vSize step uvPixelStride) {
                    nv21[uvIndex++] = vvBuffer[i]
                }
            }
            
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
        else -> {
            // Fallback for unsupported formats - create a placeholder bitmap
            val width = image.width.coerceAtLeast(1)
            val height = image.height.coerceAtLeast(1)
            createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }
} 
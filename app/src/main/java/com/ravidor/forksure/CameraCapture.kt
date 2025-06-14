package com.ravidor.forksure

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCapture(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var permissionRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted && !permissionRequested) {
            permissionRequested = true
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Camera capture screen"
            }
    ) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onImageCaptured = onImageCaptured,
                onError = onError
            )
        } else {
            // Permission not granted state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .semantics {
                        contentDescription = "Camera permission required"
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
                    text = "Camera permission is required to take photos of your baked goods.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics {
                        contentDescription = "Camera permission explanation"
                    }
                )
                if (permissionRequested) {
                    Text(
                        text = "Please grant camera permission in your device settings if the permission dialog didn't appear.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .semantics {
                                contentDescription = "Permission instruction"
                            }
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
    onError: (String) -> Unit
) {
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
            onError("Failed to bind camera: ${exc.message}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Camera viewfinder"
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

        // Camera status indicator
        if (!isCameraReady) {
            Text(
                text = "Initializing camera...",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .semantics {
                        contentDescription = "Camera status: Initializing camera"
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
                    contentDescription = when {
                        !isCameraReady -> "Capture photo button. Camera is initializing, please wait"
                        isCapturing -> "Capture photo button. Currently capturing photo, please wait"
                        else -> "Capture photo button. Tap to take a photo of your baked goods"
                    }
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
            Text(
                text = "Capturing photo...",
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics {
                        contentDescription = "Status: Capturing photo"
                    },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun captureImage(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
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
                    onError("Failed to process captured image: ${e.message}")
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError("Image capture failed: ${exception.message}")
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} 
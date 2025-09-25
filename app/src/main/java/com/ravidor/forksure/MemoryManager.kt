package com.ravidor.forksure

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Stable
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.min
import kotlin.math.sqrt

// Centralized constants imports
import com.ravidor.forksure.AppConstants

/**
 * Memory management utilities for safe image processing and OutOfMemoryError prevention
 */
@Stable
object MemoryManager {
    
    private const val TAG = AppConstants.TAG_MEMORY_MANAGER
        
        // Memory thresholds in MB
        private const val LOW_MEMORY_THRESHOLD = 50L
        private const val CRITICAL_MEMORY_THRESHOLD = 20L
        
        // Image processing constants
        private const val MAX_IMAGE_SIZE_MB = 2L // 2MB max for processing
        private const val MAX_DIMENSION = 2048 // Max width/height in pixels
        private const val JPEG_QUALITY_HIGH = 90
        private const val JPEG_QUALITY_MEDIUM = 70
        private const val JPEG_QUALITY_LOW = 50
        
        // Sample size calculation constants
        private const val TARGET_SIZE_BYTES = 1024 * 1024 // 1MB target size
    
    /**
     * Check current memory status
     */
    data class MemoryStatus(
        val availableMemoryMB: Long,
        val totalMemoryMB: Long,
        val usedMemoryMB: Long,
        val isLowMemory: Boolean,
        val isCriticalMemory: Boolean
    )
    
    /**
     * Get current memory status
     */
    fun getMemoryStatus(context: Context): MemoryStatus {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory() / (1024 * 1024)
            val freeMemory = runtime.freeMemory() / (1024 * 1024)
            val maxMemory = runtime.maxMemory() / (1024 * 1024)
            val usedMemory = totalMemory - freeMemory
            val availableMemory = maxMemory - usedMemory
            
            MemoryStatus(
                availableMemoryMB = availableMemory,
                totalMemoryMB = totalMemory,
                usedMemoryMB = usedMemory,
                isLowMemory = availableMemory < LOW_MEMORY_THRESHOLD,
                isCriticalMemory = availableMemory < CRITICAL_MEMORY_THRESHOLD
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get memory status", e)
            MemoryStatus(0, 0, 0, true, true)
        }
    }
    
    /**
     * Handle memory trim callback
     */
    fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.d(TAG, "Memory trim level: $level - performing light cleanup")
                performLightMemoryCleanup()
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.d(TAG, "Memory trim level: $level - performing aggressive cleanup")
                performAggressiveMemoryCleanup()
            }
        }
    }
    
    /**
     * Perform light memory cleanup
     */
    private fun performLightMemoryCleanup() {
        try {
            System.gc()
            Log.d(TAG, "Light memory cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during light memory cleanup", e)
        }
    }
    
    /**
     * Perform aggressive memory cleanup
     */
    private fun performAggressiveMemoryCleanup() {
        try {
            // Clear any image caches if they exist
            // This would be customized based on your app's caching strategy
            
            // Force garbage collection
            System.gc()
            
            Log.d(TAG, "Aggressive memory cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during aggressive memory cleanup", e)
        }
    }
    
    /**
     * Safely decode bitmap with memory constraints
     */
    fun decodeBitmapSafely(
        context: Context,
        uri: Uri,
        maxWidth: Int = MAX_DIMENSION,
        maxHeight: Int = MAX_DIMENSION
    ): Result<Bitmap> {
        return try {
            val memoryStatus = getMemoryStatus(context)
            
            if (memoryStatus.isCriticalMemory) {
                return Result.failure(OutOfMemoryError("Insufficient memory available for image processing"))
            }
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // First pass: get image dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                
                BitmapFactory.decodeStream(inputStream, null, options)
                
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    return Result.failure(IllegalArgumentException("Invalid image dimensions"))
                }
                
                // Calculate sample size to reduce memory usage
                val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxWidth, maxHeight)
                
                Log.d(TAG, "Original: ${options.outWidth}x${options.outHeight}, Sample size: $sampleSize")
                
                // Second pass: decode with sample size and memory-efficient settings
                context.contentResolver.openInputStream(uri)?.use { secondInputStream ->
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = if (memoryStatus.isLowMemory) {
                            Bitmap.Config.RGB_565 // Use less memory
                        } else {
                            Bitmap.Config.ARGB_8888 // Better quality
                        }
                        inDither = false
                        inPurgeable = true
                        inInputShareable = true
                    }
                    
                    val bitmap = BitmapFactory.decodeStream(secondInputStream, null, decodeOptions)
                    
                    if (bitmap != null) {
                        Log.d(TAG, "Decoded bitmap: ${bitmap.width}x${bitmap.height}, Size: ${bitmap.byteCount / 1024}KB")
                        Result.success(bitmap)
                    } else {
                        Result.failure(IllegalStateException("Failed to decode bitmap"))
                    }
                } ?: Result.failure(IllegalStateException("Failed to open input stream for decoding"))
                
            } ?: Result.failure(IllegalStateException("Failed to open input stream"))
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError during bitmap decoding", e)
            performAggressiveMemoryCleanup()
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during safe bitmap decoding", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calculate appropriate sample size for image decoding
     */
    private fun calculateSampleSize(
        originalWidth: Int, 
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        var sampleSize = 1
        
        if (originalWidth > maxWidth || originalHeight > maxHeight) {
            val halfWidth = originalWidth / 2
            val halfHeight = originalHeight / 2
            
            while ((halfWidth / sampleSize) >= maxWidth && (halfHeight / sampleSize) >= maxHeight) {
                sampleSize *= 2
            }
        }
        
        return sampleSize
    }
    
    /**
     * Compress bitmap to target size with quality adjustment
     */
    fun compressBitmap(
        bitmap: Bitmap,
        maxSizeBytes: Long = TARGET_SIZE_BYTES.toLong(),
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): Result<ByteArray> {
        return try {
            if (bitmap.isRecycled) {
                return Result.failure(IllegalStateException("Bitmap is recycled"))
            }
            
            // Start with high quality and reduce if needed
            var quality = JPEG_QUALITY_HIGH
            var outputStream: ByteArrayOutputStream
            
            do {
                outputStream = ByteArrayOutputStream()
                bitmap.compress(format, quality, outputStream)
                
                if (outputStream.size() <= maxSizeBytes || quality <= 10) {
                    break
                }
                
                quality -= 10
                outputStream.reset()
                
            } while (quality > 0)
            
            val compressedData = outputStream.toByteArray()
            Log.d(TAG, "Compressed bitmap to ${compressedData.size / 1024}KB with quality $quality")
            
            Result.success(compressedData)
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError during bitmap compression", e)
            performAggressiveMemoryCleanup()
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during bitmap compression", e)
            Result.failure(e)
        }
    }
    
    /**
     * Scale bitmap safely to target dimensions
     */
    fun scaleBitmapSafely(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Result<Bitmap> {
        return try {
            if (bitmap.isRecycled) {
                return Result.failure(IllegalStateException("Bitmap is recycled"))
            }
            
            // Check if scaling is needed
            if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
                return Result.success(bitmap)
            }
            
            val scaleX = targetWidth.toFloat() / bitmap.width
            val scaleY = targetHeight.toFloat() / bitmap.height
            
            val matrix = Matrix().apply {
                setScale(scaleX, scaleY)
            }
            
            val scaledBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, 
                bitmap.width, bitmap.height,
                matrix, true
            )
            
            Log.d(TAG, "Scaled bitmap from ${bitmap.width}x${bitmap.height} to ${scaledBitmap.width}x${scaledBitmap.height}")
            
            Result.success(scaledBitmap)
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError during bitmap scaling", e)
            performAggressiveMemoryCleanup()
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during bitmap scaling", e)
            Result.failure(e)
        }
    }
    
    /**
     * Safely recycle bitmap if not already recycled
     */
    fun recycleBitmapSafely(bitmap: Bitmap?) {
        try {
            bitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                    Log.d(TAG, "Bitmap recycled successfully")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recycling bitmap", e)
        }
    }
    
    /**
     * Execute memory-intensive operation with monitoring
     */
    internal inline fun <T> executeWithMemoryMonitoring(
        context: Context,
        operation: () -> T
    ): Result<T> {
        return try {
            val initialMemory = getMemoryStatus(context)
            Log.d(TAG, "Starting operation with ${initialMemory.availableMemoryMB}MB available")
            
            if (initialMemory.isCriticalMemory) {
                performAggressiveMemoryCleanup()
                val postCleanupMemory = getMemoryStatus(context)
                if (postCleanupMemory.isCriticalMemory) {
                    return Result.failure(OutOfMemoryError("Insufficient memory for operation"))
                }
            }
            
            val result = operation()
            
            val finalMemory = getMemoryStatus(context)
            Log.d(TAG, "Operation completed. Memory: ${finalMemory.availableMemoryMB}MB available")
            
            Result.success(result)
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError during monitored operation", e)
            performAggressiveMemoryCleanup()
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during monitored operation", e)
            Result.failure(e)
        }
    }
}
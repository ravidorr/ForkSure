package com.ravidor.forksure

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.Stable
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for sharing recipes to external apps like Google Keep, Notes, etc.
 * Uses Android's share intent system for maximum compatibility
 */
@Stable
object RecipeSharingHelper {
    private const val TAG = "RecipeSharingHelper"
    
    /**
     * Share recipe content to Google Keep or other note-taking apps
     */
    fun shareRecipeToKeep(
        context: Context,
        recipeContent: String,
        recipeName: String = "ForkSure Recipe"
    ): Boolean {
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, recipeName)
                putExtra(Intent.EXTRA_TEXT, formatRecipeForSharing(recipeContent, recipeName))
                
                // Optional: Try to target Google Keep specifically first
                setPackage("com.google.android.keep")
            }
            
            // Create chooser with fallback to other apps if Keep isn't available
            val chooserIntent = Intent.createChooser(shareIntent, "Save recipe to...")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(
                createFallbackShareIntent(context, recipeContent, recipeName)
            ))
            
            context.startActivity(chooserIntent)
            Log.d(TAG, "Recipe sharing intent launched successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing recipe to Keep", e)
            false
        }
    }
    
    /**
     * Share recipe with image to apps that support both text and images
     */
    fun shareRecipeWithImage(
        context: Context,
        recipeContent: String,
        recipeName: String,
        image: Bitmap?
    ): Boolean {
        return try {
            if (image == null) {
                return shareRecipeToKeep(context, recipeContent, recipeName)
            }
            
            // Save image to temporary URI
            val imageUri = saveImageToTempUri(context, image, recipeName)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_SUBJECT, recipeName)
                putExtra(Intent.EXTRA_TEXT, formatRecipeForSharing(recipeContent, recipeName))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Share recipe with photo...")
            context.startActivity(chooserIntent)
            
            Log.d(TAG, "Recipe with image sharing intent launched successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing recipe with image", e)
            shareRecipeToKeep(context, recipeContent, recipeName) // Fallback to text-only
        }
    }
    
    /**
     * Share recipe to specific app types
     */
    fun shareRecipeToApp(
        context: Context,
        recipeContent: String,
        recipeName: String,
        targetApp: ShareTarget
    ): Boolean {
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, recipeName)
                putExtra(Intent.EXTRA_TEXT, formatRecipeForSharing(recipeContent, recipeName))
                
                when (targetApp) {
                    ShareTarget.GOOGLE_KEEP -> setPackage("com.google.android.keep")
                    ShareTarget.GMAIL -> setPackage("com.google.android.gm")
                    ShareTarget.GOOGLE_DOCS -> setPackage("com.google.android.apps.docs.editors.docs")
                    ShareTarget.NOTION -> setPackage("notion.id")
                    ShareTarget.EVERNOTE -> setPackage("com.evernote")
                    ShareTarget.ANY -> { /* No specific package */ }
                }
            }
            
            context.startActivity(shareIntent)
            Log.d(TAG, "Recipe shared to ${targetApp.name} successfully")
            true
            
        } catch (e: Exception) {
            Log.w(TAG, "Specific app not available, falling back to general share", e)
            // Fallback to general share
            shareRecipeToKeep(context, recipeContent, recipeName)
        }
    }
    
    /**
     * Format recipe content for sharing with proper structure
     */
    private fun formatRecipeForSharing(
        recipeContent: String,
        recipeName: String
    ): String {
        val timestamp = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        
        return buildString {
            appendLine("🧁 $recipeName")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine(recipeContent)
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("📱 Shared from ForkSure on $timestamp")
        }
    }
    
    /**
     * Create fallback share intent for when specific apps aren't available
     */
    private fun createFallbackShareIntent(
        context: Context,
        recipeContent: String,
        recipeName: String
    ): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, recipeName)
            putExtra(Intent.EXTRA_TEXT, formatRecipeForSharing(recipeContent, recipeName))
        }
    }
    
    /**
     * Save image to temporary URI for sharing
     */
    private fun saveImageToTempUri(
        context: Context,
        bitmap: Bitmap,
        recipeName: String
    ): Uri? {
        return try {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bytes)
            
            val path = MediaStore.Images.Media.insertImage(
                context.contentResolver,
                bitmap,
                "ForkSure_${recipeName.replace(" ", "_")}",
                "Recipe photo from ForkSure app"
            )
            
            Uri.parse(path)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image for sharing", e)
            null
        }
    }
    
    /**
     * Check if specific sharing apps are available
     */
    fun isAppAvailable(context: Context, targetApp: ShareTarget): Boolean {
        return try {
            val packageName = when (targetApp) {
                ShareTarget.GOOGLE_KEEP -> "com.google.android.keep"
                ShareTarget.GMAIL -> "com.google.android.gm"
                ShareTarget.GOOGLE_DOCS -> "com.google.android.apps.docs.editors.docs"
                ShareTarget.NOTION -> "notion.id"
                ShareTarget.EVERNOTE -> "com.evernote"
                ShareTarget.ANY -> return true
            }
            
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Available sharing targets
 */
enum class ShareTarget {
    GOOGLE_KEEP,
    GMAIL,
    GOOGLE_DOCS,
    NOTION,
    EVERNOTE,
    ANY
} 
package com.ravidor.forksure.repository

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.ravidor.forksure.AIResponseProcessingResult
import com.ravidor.forksure.EnhancedErrorHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for AI-related operations
 */
interface AIRepository {
    suspend fun generateContent(bitmap: Bitmap, prompt: String): AIResponseProcessingResult
}

/**
 * Implementation of AIRepository using Google's Generative AI
 */
@Singleton
class AIRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generativeModel: GenerativeModel
) : AIRepository {

    override suspend fun generateContent(bitmap: Bitmap, prompt: String): AIResponseProcessingResult {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AIRepository", "Starting AI request with prompt: ${prompt.take(100)}...")
                android.util.Log.d("AIRepository", "Bitmap size: ${bitmap.width}x${bitmap.height}")
                
                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                android.util.Log.d("AIRepository", "AI response received: ${response.text?.take(200) ?: "null"}")
                
                response.text?.let { outputContent ->
                    // Validate AI response for safety
                    android.util.Log.d("AIRepository", "Processing AI response for safety")
                    EnhancedErrorHandler.processAIResponse(context, outputContent)
                } ?: AIResponseProcessingResult.Error(
                    "No response received",
                    "No response received from AI service. Please try again."
                ).also {
                    android.util.Log.e("AIRepository", "No response text received from AI service")
                }
            } catch (e: Exception) {
                android.util.Log.e("AIRepository", "AI request failed", e)
                AIResponseProcessingResult.Error(
                    "AI request failed",
                    e.message ?: "Unknown error occurred during AI request"
                )
            }
        }
    }
} 
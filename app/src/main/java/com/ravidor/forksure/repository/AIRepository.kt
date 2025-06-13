package com.ravidor.forksure.repository

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.ravidor.forksure.AIResponseProcessingResult
import com.ravidor.forksure.EnhancedErrorHandler
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
    private val generativeModel: GenerativeModel
) : AIRepository {

    override suspend fun generateContent(bitmap: Bitmap, prompt: String): AIResponseProcessingResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                response.text?.let { outputContent ->
                    // Validate AI response for safety
                    EnhancedErrorHandler.processAIResponse(outputContent)
                } ?: AIResponseProcessingResult.Error(
                    "No response received",
                    "No response received from AI service. Please try again."
                )
            } catch (e: Exception) {
                AIResponseProcessingResult.Error(
                    "AI request failed",
                    e.message ?: "Unknown error occurred during AI request"
                )
            }
        }
    }
} 
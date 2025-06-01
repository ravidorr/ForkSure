package com.ravidor.forksure

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BakingViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey
    )

    // Store last request for retry functionality
    private var lastBitmap: Bitmap? = null
    private var lastPrompt: String? = null
    private var lastContext: Context? = null

    fun sendPrompt(
        bitmap: Bitmap,
        prompt: String,
        context: Context
    ) {
        // Store for potential retry
        lastBitmap = bitmap
        lastPrompt = prompt
        lastContext = context
        
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                } ?: run {
                    _uiState.value = ErrorHandler.handleError(
                        Exception("No response received from AI"),
                        context
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ErrorHandler.handleError(e, context)
            }
        }
    }

    fun retryLastRequest() {
        val bitmap = lastBitmap
        val prompt = lastPrompt
        val context = lastContext
        
        if (bitmap != null && prompt != null && context != null) {
            sendPrompt(bitmap, prompt, context)
        }
    }

    fun clearError() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Initial
        }
    }
}
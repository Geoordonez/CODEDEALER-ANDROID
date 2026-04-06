package com.example.codedealer

import androidx.lifecycle.ViewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GeminiViewModel : ViewModel() {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "AIzaSyAsL8Z-bWTbtH-D4iAeVkAao8VJHGN0Y1g"
    )

    private val _respuestaIA = MutableStateFlow("Escribe algo para que Gemini te ayude...")
    val respuestaIA: StateFlow<String> = _respuestaIA

    private val _estaCargando = MutableStateFlow(false)
    val estaCargando: StateFlow<Boolean> = _estaCargando

    fun preguntarAGemini(prompt: String) {
        viewModelScope.launch {
            _estaCargando.value = true
            try {
                val response = generativeModel.generateContent(prompt)
                _respuestaIA.value = response.text ?: "Gemini no supo qué responder."
            } catch (e: Exception) {
                _respuestaIA.value = "Error de conexión: ${e.localizedMessage}"
            } finally {
                _estaCargando.value = false
            }
        }
    }
}
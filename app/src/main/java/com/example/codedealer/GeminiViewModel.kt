package com.example.codedealer

import androidx.lifecycle.ViewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.type.content
import com.google.firebase.database.FirebaseDatabase
class GeminiViewModel : ViewModel() {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "",
        systemInstruction = content {
            text(
                """
                Eres 'CodeDealer AI', un asistente virtual experto en programación y desarrollo de software.
                Tus respuestas deben ser amables, directas y en español.
                Si te preguntan cosas que no tienen nada que ver con programación o tecnología, 
                responde cortésmente que tu especialidad es el código y no puedes ayudar con otros temas.
                adicionalmente tu trabajo principal, las personas o programadores van a intentar 
                buscar propuestas, tu vas a ser la herramienta que acturara como un buscador entre las propuetas
                es decir, si yo te digo que quiero programadores que sepa python, tu buscaras las propuestas 
                existentes y me las recomendaras.
                """.trimIndent()
            )
        }
    )

    private val _respuestaIA = MutableStateFlow("Escribe algo para que Gemini te ayude...")
    val respuestaIA: StateFlow<String> = _respuestaIA

    private val _estaCargando = MutableStateFlow(false)
    val estaCargando: StateFlow<Boolean> = _estaCargando

    fun preguntarAGeminiConContexto(promptUsuario: String) {
        viewModelScope.launch {
            _estaCargando.value = true
            try {
                // 1. Obtenemos las propuestas de Firebase primero
                val database = FirebaseDatabase.getInstance().getReference("Propuestas")
                database.get().addOnSuccessListener { snapshot ->
                    val listaPropuestas = mutableListOf<String>()

                    for (hijo in snapshot.children) {
                        val titulo = hijo.child("titulo").value.toString()
                        val desc = hijo.child("descripcion").value.toString()
                        listaPropuestas.add("- Tarea: $titulo. Detalle: $desc")
                    }

                    // 2. Creamos un super-prompt con los datos
                    val contextoData =
                        "Aquí tienes las propuestas actuales de la base de datos:\n" +
                                listaPropuestas.joinToString("\n")

                    val promptFinal = """
                    $contextoData
                    
                    Instrucción del usuario: $promptUsuario
                    
                    Si el usuario menciona palabras clave, busca en la lista anterior y 
                    recomienda las tareas que coincidan.
                """.trimIndent()

                    // 3. Se lo mandamos a Gemini
                    viewModelScope.launch {
                        val response = generativeModel.generateContent(promptFinal)
                        _respuestaIA.value =
                            response.text ?: "No encontré propuestas con esas palabras clave."
                        _estaCargando.value = false
                    }
                }.addOnFailureListener {
                    _respuestaIA.value = "Error al leer la base de datos."
                    _estaCargando.value = false
                }
            } catch (e: Exception) {
                _respuestaIA.value = "Error: ${e.localizedMessage}"
                _estaCargando.value = false
            }
        }
    }
}

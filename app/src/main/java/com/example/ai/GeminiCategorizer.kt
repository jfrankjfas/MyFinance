package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiCategorizedResult(
    val title: String,
    val amount: Double,
    val category: String,
    val type: String, // "EXPENSE" or "INCOME"
    val note: String
)

object GeminiCategorizer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun categorizeTransactionInput(userPrompt: String): Result<AiCategorizedResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    Exception("Llave GEMINI_API_KEY no configurada. Agrega tu clave en el panel de Secretos.")
                )
            }

            val systemInstructions = """
                Eres un asistente experto en finanzas personales para la aplicación 'Finanzas Clara'.
                Tu tarea es analizar el texto introducido por el usuario y extraer los detalles de la transacción.
                Debes responder EXCLUSIVAMENTE con un objeto JSON sin formato markdown extra con estas llaves exactas:
                - "title": Título corto y descriptivo de la transacción (ej. "Uber al Trabajo", "Supermercado Coto").
                - "amount": Número decimal con el monto positivo (ej. 24.50). Si no se especifica monto, usa 0.0.
                - "category": Selecciona la categoría más apropiada de esta lista exacta:
                  ["Alimentación", "Transporte", "Entretenimiento", "Servicios", "Salud", "Educación", "Compras", "Sueldo", "Inversión", "Otro"].
                - "type": "EXPENSE" para gastos o "INCOME" para ingresos/sueldos/ventas.
                - "note": Breve nota explicativa o comercio/lugar (opcional).
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemInstructions\n\nEntrada del usuario: \"$userPrompt\""))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Error en servidor IA (${response.code})"))
            }

            val responseObj = JSONObject(responseBodyString)
            val candidates = responseObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No se pudo obtener respuesta de la IA"))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val parts = firstCandidate.getJSONObject("content").getJSONArray("parts")
            val textResult = parts.getJSONObject(0).getString("text").trim()

            // Clean json backticks if present
            val cleanedJson = textResult.replace("```json", "").replace("```", "").trim()
            val parsedResult = JSONObject(cleanedJson)

            val title = parsedResult.optString("title", "Transacción IA")
            val amount = parsedResult.optDouble("amount", 0.0)
            val category = parsedResult.optString("category", "Otro")
            val type = parsedResult.optString("type", "EXPENSE").uppercase()
            val note = parsedResult.optString("note", "Categorizado automáticamente por Gemini IA")

            Result.success(
                AiCategorizedResult(
                    title = title,
                    amount = amount,
                    category = category,
                    type = if (type == "INCOME") "INCOME" else "EXPENSE",
                    note = note
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

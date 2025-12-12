package com.example.travelbuddy.helpers

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiApiHelper {
    suspend fun generateContent(apiKey: String, prompt: String): String? = withContext(Dispatchers.IO) {
        val modelsToTry = listOf("gemini-2.0-flash", "gemini-2.5-flash", "gemini-flash-latest", "gemini-2.5-pro")
        
        for (modelName in modelsToTry) {
            for (apiVersion in listOf("v1", "v1beta")) {
                try {
                    val url = URL("https://generativelanguage.googleapis.com/$apiVersion/models/$modelName:generateContent?key=$apiKey")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 30000
                    connection.readTimeout = 30000
                    
                    val requestBody = JSONObject().apply {
                        put("contents", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", org.json.JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.7)
                            put("topK", 40)
                            put("topP", 0.95)
                            put("maxOutputTokens", 4096)
                        })
                    }
                    
                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(requestBody.toString())
                        writer.flush()
                    }
                    
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonResponse = JSONObject(response)
                        val candidates = jsonResponse.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            if (parts.length() > 0) {
                                return@withContext parts.getJSONObject(0).getString("text")
                            }
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }
        null
    }
    
    fun getErrorMessage(e: Exception, apiKey: String): String {
        return when {
            e.message?.contains("403") == true || e.message?.contains("PERMISSION_DENIED") == true -> 
                "Błąd 403: Brak uprawnień. Sprawdź Generative Language API w Google Cloud Console."
            e.message?.contains("401") == true || e.message?.contains("UNAUTHENTICATED") == true ->
                "Błąd 401: Nieprawidłowy klucz API."
            e.message?.contains("not found") == true || e.message?.contains("not supported") == true ->
                "Błąd: Model niedostępny. Sprawdź Google Cloud Console."
            else -> "Błąd: ${e.message}"
        }
    }
}


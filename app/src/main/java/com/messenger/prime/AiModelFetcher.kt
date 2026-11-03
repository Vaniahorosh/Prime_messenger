package com.messenger.prime

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiModelFetcher {

    fun fetchModels(apiKey: String, provider: String, customUrl: String, callback: (List<String>) -> Unit) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            callback(getDefaultModelsForProvider(provider))
            return
        }

        Thread {
            val models = mutableListOf<String>()
            try {
                when (provider) {
                    "Gemini" -> {
                        val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$trimmedKey")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.connectTimeout = 8000
                        conn.readTimeout = 8000
                        if (conn.responseCode in 200..299) {
                            val resStr = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(resStr)
                            val arr = json.optJSONArray("models")
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    val mObj = arr.getJSONObject(i)
                                    val name = mObj.optString("name", "") // e.g. "models/gemini-1.5-flash"
                                    val cleanName = name.removePrefix("models/")
                                    if (cleanName.isNotBlank() && !models.contains(cleanName)) {
                                        models.add(cleanName)
                                    }
                                }
                            }
                        }
                    }
                    "OpenAI", "OpenRouter", "DeepSeek" -> {
                        val endpoint = when (provider) {
                            "OpenAI" -> "https://api.openai.com/v1/models"
                            "OpenRouter" -> "https://openrouter.ai/api/v1/models"
                            else -> {
                                if (customUrl.isNotBlank()) {
                                    if (customUrl.contains("chat/completions")) customUrl.replace("/chat/completions", "/models")
                                    else if (customUrl.endsWith("/")) "${customUrl}models" else "$customUrl/models"
                                } else "https://api.deepseek.com/v1/models"
                            }
                        }
                        val url = URL(endpoint)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.setRequestProperty("Authorization", "Bearer $trimmedKey")
                        if (provider == "OpenRouter") {
                            conn.setRequestProperty("HTTP-Referer", "https://prime.messenger")
                        }
                        conn.connectTimeout = 8000
                        conn.readTimeout = 8000
                        if (conn.responseCode in 200..299) {
                            val resStr = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(resStr)
                            val arr = json.optJSONArray("data")
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    val mObj = arr.getJSONObject(i)
                                    val modelId = mObj.optString("id", "")
                                    if (modelId.isNotBlank() && !models.contains(modelId)) {
                                        models.add(modelId)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // If empty or failed, fallback to provider defaults
            if (models.isEmpty()) {
                models.addAll(getDefaultModelsForProvider(provider))
            }

            Handler(Looper.getMainLooper()).post {
                callback(models)
            }
        }.start()
    }

    private fun getDefaultModelsForProvider(provider: String): List<String> {
        return when (provider) {
            "Gemini" -> listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp")
            "OpenAI" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")
            "Anthropic" -> listOf("claude-3-5-sonnet-20241022", "claude-3-haiku-20240307")
            "OpenRouter" -> listOf("google/gemini-2.0-flash-exp:free", "deepseek/deepseek-chat", "openai/gpt-4o-mini")
            "DeepSeek" -> listOf("deepseek-chat", "deepseek-reasoner")
            else -> listOf("gemini-1.5-flash")
        }
    }
}

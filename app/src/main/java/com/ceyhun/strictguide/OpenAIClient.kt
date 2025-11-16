1package com.ceyhun.strictguide

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.Executors

class OpenAIClient {

   2private val client = OkHttpClient()
    private val executor = Executors.newSingleThreadExecutor()

    fun ask(userText: String, scene: String, onAnswer: (String) -> Unit) {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank()) {
            onAnswer("Ключ OpenAI не задан")
            return
        }

        3val systemPrompt = """
            Ты голосовой и навигационный ассистент для слепого человека.
            Камера видит: $scene
            Если сцена непонятная — попроси повернуть телефон медленно.
            Не отвечай “я не могу”.
            Отвечай коротко, по шагам.
        """.trimIndent()

        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userText)
                })
            })
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        executor.execute {
            try {
                client.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string()
                    if (resp.isSuccessful && !respBody.isNullOrBlank()) {
                        val root = JSONObject(respBody)
                        val choices = root.getJSONArray("choices")
                        val first = choices.getJSONObject(0)
                        val message = first.getJSONObject("message")
                        val answer = message.getString("content")
                        onAnswer(answer)
                    } else {
                        onAnswer("Не получилось получить ответ")
                    }
                }
            } catch (e: Exception) {
                onAnswer("Ошибка сети")
            }
        }
    }
}

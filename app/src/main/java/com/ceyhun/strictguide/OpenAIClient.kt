package com.ceyhun.strictguide

class OpenAIClient {

    fun ask(userText: String, scene: String, onAnswer: (String) -> Unit) {
        val systemPrompt = """
            Ты голосовой и навигационный ассистент для слепого человека.
            Камера видит: $scene
            Если сцена непонятная — попроси повернуть телефон медленно.
            Не отвечай “я не могу”.
            Отвечай коротко, по шагам.
        """.trimIndent()

        // Простой ответ на основе userText и scene
        val answer = when {
            userText.contains("что я вижу", ignoreCase = true) -> "Вы видите: $scene."
            userText.contains("помоги", ignoreCase = true) -> "Пожалуйста, уточните, как я могу помочь."
            else -> "Я не уверен, как ответить на это. Пожалуйста, уточните."
        }

        onAnswer(answer)
    }
}
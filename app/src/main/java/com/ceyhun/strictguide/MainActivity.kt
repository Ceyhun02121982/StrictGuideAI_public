// 📄 Файл: app/src/main/java/com/ceyhun/strictguide/MainActivity.kt
package com.ceyhun.strictguide

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // ---- UI -------------------------------------------------------------------
    private lateinit var tvStatus: TextView
    private lateinit var tvHeard: TextView
    private lateinit var btnMic: Button
    private lateinit var btnTest: Button

    // ---- Speech ---------------------------------------------------------------
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isListening = false

    // ---- HTTP -----------------------------------------------------------------
    private val client by lazy { OkHttpClient() }

    // ---- API key из build.gradle (buildConfigField) ---------------------------
    private val apiKey: String = BuildConfig.OPENAI_API_KEY


    // ---- Permissions ----------------------------------------------------------
    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val micOk = grants[Manifest.permission.RECORD_AUDIO] == true
        tvStatus.text = if (micOk) "Разрешение на микрофон получено." else "Нужно разрешение на микрофон."
    }

    // optional launcher (не используется сейчас, оставил на будущее)
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                tvHeard.text = "Вы сказали: $text"
                askChat(text)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvHeard  = findViewById(R.id.tvHeard)
        btnMic   = findViewById(R.id.btnMic)
        btnTest  = findViewById(R.id.btnTest)

        // Проверка ключа
        tvStatus.text = if (apiKey.isBlank()) {
            "Ключ не найден. Укажи OPENAI_API_KEY в build.gradle / переменных окружения."
        } else {
            "Готово."
        }

        // Инициализация TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale("ru", "RU")
        }

        // Микрофон
        btnMic.setOnClickListener {
            ensurePermissionsThen {
                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    tvStatus.text = "Распознавание речи недоступно на устройстве."
                    return@ensurePermissionsThen
                }
                if (isListening) stopListening() else startListening()
            }
        }

        // Тестовый запрос
        btnTest.setOnClickListener {
            ensurePermissionsThen {
                askChat("Привет! Скажи коротко, что ты меня слышишь.")
            }
        }
    }

    // ===== Speech-to-Text ======================================================

    private fun startListening() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { tvStatus.text = "Говорите…" }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { tvStatus.text = "Обработка…" }
                    override fun onError(error: Int) {
                        isListening = false
                        tvStatus.text = "Ошибка распознавания ($error)"
                        btnMic.text = "🎤 Нажмите и говорите"
                    }
                    override fun onResults(results: Bundle) {
                        isListening = false
                        btnMic.text = "🎤 Нажмите и говорите"
                        val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        tvHeard.text = "Вы сказали: $text"
                        if (text.isNotBlank()) askChat(text) else tvStatus.text = "Не удалось распознать текст."
                    }
                    override fun onPartialResults(partialResults: Bundle) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.startListening(intent)
        isListening = true
        tvStatus.text = "Слушаю…"
        btnMic.text = "⏹️ Остановить"
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        tvStatus.text = "Остановлено."
        btnMic.text = "🎤 Нажмите и говорите"
    }

    // ===== ChatGPT call (исправлено: messages = JSONArray) =====================

    private fun askChat(userText: String) {
        if (apiKey.isBlank()) {
            tvStatus.text = "Ключ API не настроен."
            return
        }

        tvStatus.text = "Отправляю запрос…"

        // ✅ messages как массив объектов
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", "Ты отвечаешь кратко и ясно."))
            .put(JSONObject().put("role", "user").put("content", userText))

        val body = JSONObject()
            .put("model", "gpt-4o-mini")
            .put("messages", messages)
            .put("temperature", 0.3)
            .put("max_tokens", 160)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { tvStatus.text = "Сеть: ${e.message}" }
            }
            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string().orElse("")
                if (!response.isSuccessful) {
                    runOnUiThread { tvStatus.text = "HTTP ${response.code}: ${raw.take(140)}" }
                    return
                }
                val reply = try {
                    val root = JSONObject(raw)
                    root.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } catch (t: Throwable) {
                    "Не удалось распарсить ответ: ${t.message}"
                }
                runOnUiThread {
                    tvStatus.text = "Ответ: $reply"
                    speak(reply)
                }
            }
        })
    }

    // ===== Permissions =========================================================

    private fun ensurePermissionsThen(block: () -> Unit) {
        val need = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) need += Manifest.permission.RECORD_AUDIO
        if (need.isEmpty()) block() else requestPerms.launch(need.toTypedArray())
    }

    // ===== Text-to-Speech ======================================================

    private fun speak(text: String) {
        if (text.isBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "replyId")
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    // ===== Cleanup =============================================================

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    // small helper
    private fun String?.orElse(fallback: String) = this ?: fallback
}

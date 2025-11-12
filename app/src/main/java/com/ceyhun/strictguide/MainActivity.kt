package com.ceyhun.strictguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // UI
    private lateinit var previewView: PreviewView
    private lateinit var tvStatus: TextView
    private lateinit var btnMic: Button

    // voice
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    // camera / mlkit
    private lateinit var objectDetector: ObjectDetector
    private var lastSpoken: Long = 0L
    private var lastLabel: String? = null

    // http
    private val httpClient = OkHttpClient()
    private val executor = Executors.newSingleThreadExecutor()

    // permissions
    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else tvStatus.text = "Нет разрешения на камеру"
    }

    private val requestMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* просто запросили */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.cameraPreview)
        tvStatus = findViewById(R.id.tvStatus)
        btnMic = findViewById(R.id.btnMic)

        // TTS
        tts = TextToSpeech(this, this)

        // ML Kit
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()
        objectDetector = ObjectDetection.getClient(options)

        // permissions
        askPermissions()

        // speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        btnMic.setOnClickListener {
            startListening()
        }
    }

    private fun askPermissions() {
        val camGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val micGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!camGranted) requestCamera.launch(Manifest.permission.CAMERA)
        else startCamera()

        if (!micGranted) requestMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    // ===== CAMERA =====
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val selector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                selector,
                preview,
                analyzer
            )

            tvStatus.text = "Камера работает"
            speak("Камера работает")

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        objectDetector.process(image)
            .addOnSuccessListener { detected ->
                if (detected.isNotEmpty()) {
                    val obj = detected[0]
                    val label = obj.labels.firstOrNull()?.text

                    if (!label.isNullOrBlank()) {
                        tvStatus.text = "Я вижу: $label"
                        val now = System.currentTimeMillis()
                        val shouldSpeak = (now - lastSpoken > 4000) && (label != lastLabel)
                        if (shouldSpeak) {
                            speak("Я вижу $label")
                            lastSpoken = now
                            lastLabel = label
                        }
                    }
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                imageProxy.close()
            }
    }

    // ===== SPEECH TO TEXT =====
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите…")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = "Слушаю…"
            }

            override fun onResults(results: Bundle?) {
                val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val userText = texts?.firstOrNull()
                if (!userText.isNullOrBlank()) {
                    tvStatus.text = "Ты сказал: $userText"
                    // отправляем в OpenAI
                    callOpenAI(userText)
                }
            }

            override fun onError(error: Int) {
                tvStatus.text = "Не услышал"
            }

            // остальные методы можно пустыми
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })

        speechRecognizer?.startListening(intent)
    }

    // ===== OPENAI =====
    private fun callOpenAI(userText: String) {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isNullOrBlank()) {
            speak("Ключ OpenAI не задан")
            return
        }

        // простой запрос chat/completions совместимый со многими прокси
        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "Ты ассистент для слепого пользователя. Отвечай кратко и вслух.")
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
                httpClient.newCall(request).execute().use { resp ->
                    val respBody = resp.body?.string()
                    if (resp.isSuccessful && !respBody.isNullOrBlank()) {
                        val root = JSONObject(respBody)
                        val choices = root.getJSONArray("choices")
                        val first = choices.getJSONObject(0)
                        val message = first.getJSONObject("message")
                        val answer = message.getString("content")
                        runOnUiThread {
                            tvStatus.text = answer
                            speak(answer)
                        }
                    } else {
                        runOnUiThread {
                            speak("Не получилось получить ответ")
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    speak("Ошибка сети")
                }
            }
        }
    }

    // ===== TTS =====
    private fun speak(text: String) {
        if (text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ru", "RU")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}

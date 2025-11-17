package com.ceyhun.strictguide

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var tvStatus: TextView
    private lateinit var btnMic: Button

    // наши “модули”
    private lateinit var visionManager: VisionManager
    private lateinit var voiceManager: VoiceManager
    private lateinit var aiClient: OpenAIClient
    private lateinit var sceneStore: SceneStore

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) visionManager.startCamera()
        else tvStatus.text = "Нет разрешения на камеру"
    }

    private val requestMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) tvStatus.text = "Нет разрешения на микрофон"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.cameraPreview)
        tvStatus = findViewById(R.id.tvStatus)
        btnMic = findViewById(R.id.btnMic)

        // хранилище “что видит камера”
        sceneStore = SceneStore()

        // голос
        voiceManager = VoiceManager(this) { text ->
            // когда распознали речь — вызываем ИИ
            val scene = sceneStore.getScene()
            aiClient.ask(text, scene) { answer ->
                runOnUiThread {
                    tvStatus.text = answer
                    voiceManager.speak(answer)
                }
            }
        }

        // ИИ
        aiClient = OpenAIClient()

        // камера + анализ
        visionManager = VisionManager(
            activity = this,
            previewView = previewView,
            sceneStore = sceneStore,
            onSpeak = { msg -> voiceManager.speak(msg) },
            onStatus = { msg -> runOnUiThread { tvStatus.text = msg } }
        )

        askPermissions()

        btnMic.setOnClickListener {
            voiceManager.startListening()
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

        if (!camGranted) {
            requestCamera.launch(Manifest.permission.CAMERA)
        } else {
            visionManager.startCamera()
        }

        if (!micGranted) {
            requestMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onDestroy() {
        voiceManager.destroy()
        super.onDestroy()
    }
}
package com.ceyhun.strictguide

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class VisionManager(
    private val activity: AppCompatActivity,
    private val previewView: PreviewView,
    private val sceneStore: SceneStore,
    private val onSpeak: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {

    private val detector: ObjectDetector
    private var lastSpoken: Long = 0L
    private var lastLabel: String? = null

    init {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        detector = ObjectDetection.getClient(options)
    }

    @SuppressLint("RestrictedApi")
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        ContextCompat.getMainExecutor(activity)
                    ) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val selector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                activity,
                selector,
                preview,
                analyzer
            )

            onStatus("Камера работает")
            onSpeak("Камера работает")

        }, ContextCompat.getMainExecutor(activity))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close(); return
        }
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(image)
            .addOnSuccessListener { detected ->
                if (detected.isNotEmpty()) {
                    val names = mutableListOf<String>()
                    for (obj in detected) {
                        val lbl = obj.labels.firstOrNull()?.text
                        if (!lbl.isNullOrBlank()
                            && lbl.lowercase() != "unknown"
                            && lbl.lowercase() != "null"
                        ) {
                            names.add(lbl)
                        }
                    }

                    if (names.isNotEmpty()) {
                        val scene = "камера видит: " + names.joinToString(", ")
                        sceneStore.update(scene)
                        onStatus(scene)

                        val first = names.first()
                        val now = System.currentTimeMillis()
                        if (now - lastSpoken > 4000 && first != lastLabel) {
                            onSpeak("Я вижу $first")
                            lastSpoken = now
                            lastLabel = first
                        }
                    } else {
                        sceneStore.update("камера видит объекты, но без понятных названий")
                    }
                } else {
                    sceneStore.update("камера ничего не распознала")
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                sceneStore.update("ошибка распознавания")
                imageProxy.close()
            }
    }
}

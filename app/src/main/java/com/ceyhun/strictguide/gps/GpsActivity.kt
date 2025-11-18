package com.ceyhun.strictguide.gps

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ceyhun.strictguide.R
import java.util.Locale

/**
 * Экран GPS для незрячего пользователя.
 *
 * - Показывает статус и координаты на экране.
 * - Озвучивает важные состояния голосом:
 *   * просьбу выдать разрешение;
 *   * сообщение об отсутствии разрешения;
 *   * подсказку что делать;
 *   * успешное получение координат;
 *   * общий статус «поиск координат».
 *
 * Работает поверх GpsManager, не меняя его API.
 */
class GpsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var gpsManager: GpsManager

    private lateinit var tvGpsCoords: TextView
    private lateinit var tvGpsStatus: TextView
    private lateinit var btnGpsStart: Button

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gps)

        // Вьюхи из activity_gps.xml
        tvGpsCoords = findViewById(R.id.tvGpsCoords)
        tvGpsStatus = findViewById(R.id.tvGpsStatus)
        btnGpsStart = findViewById(R.id.btnGpsStart)

        // Менеджер GPS
        gpsManager = GpsManager(this)

        // Инициализация системы озвучки
        tts = TextToSpeech(this, this)

        // Начальный текст/подсказка
        updateStatus(
            getString(R.string.gps_accessibility_hint),
            speak = true
        )

        btnGpsStart.setOnClickListener {
            startGpsFlow()
        }

        Log.d("GpsActivity", "GpsActivity создан")
    }

    // ================== TextToSpeech ==================

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ru")) // русская озвучка
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

            if (!ttsReady) {
                Log.w("GpsActivity", "Язык TTS не поддерживается или нет данных.")
            } else {
                speakOnce(getString(R.string.gps_accessibility_hint))
            }
        } else {
            Log.e("GpsActivity", "Ошибка инициализации TTS: status=$status")
            ttsReady = false
        }
    }

    private fun speakOnce(text: String) {
        if (!ttsReady || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "gps_speak")
    }

    // ================== ЛОГИКА GPS ==================

    private fun startGpsFlow() {
        Log.d("GpsActivity", "Кнопка старта GPS нажата")

        // 1. Проверяем разрешения через GpsManager
        if (!gpsManager.hasLocationPermission()) {
            // Строки уже есть в strings.xml
            val msg = getString(R.string.gps_permission_request)
            updateStatus(msg, speak = true)

            // Здесь можно добавить реальный запрос разрешений через ActivityCompat.requestPermissions
            // или вынести в общий PermissionsManager.
            Log.w("GpsActivity", "Разрешение на доступ к местоположению не предоставлено.")
            return
        }

        // 2. Говорим, что начинаем поиск координат
        updateStatus(getString(R.string.status_text), speak = true)

        // 3. Запускаем обновления координат
        gpsManager.startLocationUpdates { location ->
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                val coordsText = "Широта: $lat\nДолгота: $lon"

                updateLocation(coordsText, speak = true)
            } else {
                // Если координаты не получены — даём подсказку из strings.xml
                val hint = getString(R.string.gps_permission_hint)
                updateStatus(hint, speak = true)
                Log.w("GpsActivity", "Координаты не получены.")
            }
        }
    }

    // ================== ОБНОВЛЕНИЕ UI + ГОЛОС ==================

    private fun updateStatus(status: String, speak: Boolean = false) {
        tvGpsStatus.text = status
        Log.d("GpsActivity", "Статус GPS обновлён: $status")
        if (speak) speakOnce(status)
    }

    private fun updateLocation(coordsText: String, speak: Boolean = false) {
        tvGpsCoords.text = coordsText
        Log.d("GpsActivity", "Координаты обновлены: $coordsText")
        if (speak) speakOnce(coordsText)
    }

    override fun onDestroy() {
        super.onDestroy()
        gpsManager.stopLocationUpdates()
        tts?.stop()
        tts?.shutdown()
        Log.d("GpsActivity", "GpsActivity уничтожен, остановка обновлений GPS и TTS.")
    }
}
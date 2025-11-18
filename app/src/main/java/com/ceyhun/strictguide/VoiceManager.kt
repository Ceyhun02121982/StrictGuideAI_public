package com.ceyhun.strictguide

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

const val ERROR_USER_CANCELED = 0

class VoiceManager(
    private val activity: AppCompatActivity,
    private val onUserSaid: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(activity, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
    }

    fun startListening() {
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
                speak("Слушаю")
            }

            override fun onResults(results: Bundle?) {
                val texts =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val userText = texts?.firstOrNull()
                if (!userText.isNullOrBlank()) {
                    onUserSaid(userText)
                } else {
                    speak("Не услышал, повтори")
                }
            }

            override fun onError(error: Int) {
                // отдельный случай — пользователь сам отменил
                if (error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_USER_CANCELED
                ) {
                    speak("Вы отменили голосовой ввод.")
                } else {
                    speak("Повтори ещё раз")
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ru", "RU")
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }

    fun recognizeShortCommands() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                speak("Говорите команду")
            }

            override fun onResults(results: Bundle?) {
                val texts =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val userText = texts?.firstOrNull()
                if (!userText.isNullOrBlank()) {

                    val normalized = userText
                        .trim()
                        .lowercase(Locale.getDefault())

                    when (normalized) {
                        "где я" -> onUserSaid("Вы находитесь в текущем местоположении.")
                        "что вокруг" -> onUserSaid("Вокруг вас находятся здания и деревья.")
                        "прочитай текст" -> onUserSaid("Текст для чтения.")
                        else -> speak("Команда не распознана, повторите.")
                    }
                } else {
                    speak("Не услышал, повторите команду.")
                }
            }

            override fun onError(error: Int) {
                speak("Ошибка распознавания, повторите команду.")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
}
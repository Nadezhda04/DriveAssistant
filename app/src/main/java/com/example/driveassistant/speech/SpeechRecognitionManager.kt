package com.example.driveassistant.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognitionManager(
    private val context: Context,
    private val onStatusChanged: (String) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit
) {

    private val speechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    init {
        speechRecognizer.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    onStatusChanged("Слушам...")
                }

                override fun onBeginningOfSpeech() {
                    onStatusChanged("Разпознавам реч...")
                }

                override fun onRmsChanged(rmsdB: Float) {
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                }

                override fun onEndOfSpeech() {
                    onStatusChanged("Обработвам...")
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO ->
                            "Проблем с микрофона"

                        SpeechRecognizer.ERROR_CLIENT ->
                            "Разпознаването беше прекъснато"

                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Няма разрешение за микрофона"

                        SpeechRecognizer.ERROR_NETWORK ->
                            "Проблем с интернет връзката"

                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Връзката изтече"

                        SpeechRecognizer.ERROR_NO_MATCH ->
                            "Не успях да разбера казаното"

                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                            "Разпознаването е заето"

                        SpeechRecognizer.ERROR_SERVER ->
                            "Грешка в услугата за разпознаване"

                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "Не беше засечена реч"

                        else ->
                            "Грешка при разпознаването: $error"
                    }

                    onStatusChanged(message)
                }

                override fun onResults(results: Bundle?) {
                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val result =
                        matches?.firstOrNull()
                            ?: "Няма разпознат текст"

                    onFinalResult(result)
                    onStatusChanged("Готов")
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                    val matches =
                        partialResults?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    matches?.firstOrNull()?.let {
                        onPartialResult(it)
                    }
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )
    }

    fun startListening() {

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "bg-BG"
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    true
                )

                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
                )
            }

        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}
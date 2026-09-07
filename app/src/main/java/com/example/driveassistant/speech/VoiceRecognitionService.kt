package com.example.driveassistant.speech

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.driveassistant.data.AppDatabase
import com.example.driveassistant.data.VoiceNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class VoiceRecognitionService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        startForeground(
            2001,
            createNotification()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        startRecognition()

        return START_NOT_STICKY
    }

    private fun startRecognition() {

        speechRecognizer?.destroy()

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(
                        "VoiceRecognitionService",
                        "Слушам..."
                    )
                }

                override fun onBeginningOfSpeech() {
                    Log.d(
                        "VoiceRecognitionService",
                        "Разпознавам..."
                    )
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(
                        "VoiceRecognitionService",
                        "Обработвам..."
                    )
                }

                override fun onError(error: Int) {
                    Log.e(
                        "VoiceRecognitionService",
                        "Speech error: $error"
                    )

                    stopSelf()
                }

                override fun onResults(results: Bundle?) {

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val text =
                        matches?.firstOrNull()

                    Log.d(
                        "VoiceRecognitionService",
                        "Result: $text"
                    )

                    if (!text.isNullOrBlank()) {
                        saveNote(text)
                    } else {
                        stopSelf()
                    }
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                    val matches =
                        partialResults?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    Log.d(
                        "VoiceRecognitionService",
                        "Partial: ${matches?.firstOrNull()}"
                    )
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        val recognizerIntent =
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
            }

        speechRecognizer?.startListening(
            recognizerIntent
        )
    }

    private fun saveNote(text: String) {

        serviceScope.launch {

            val database =
                AppDatabase.getInstance(
                    applicationContext
                )

            val trip =
                database
                    .tripDao()
                    .getActiveTrip()

            database
                .voiceNoteDao()
                .insert(
                    VoiceNote(
                        text = text,
                        tripId = trip?.id
                    )
                )

            stopSelf()
        }
    }

    private fun createNotification(): Notification {

        val channelId =
            "voice_recognition"

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    channelId,
                    "Voice recognition",
                    NotificationManager.IMPORTANCE_LOW
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }

        return Notification.Builder(
            this,
            channelId
        )
            .setContentTitle("Drive Assistant")
            .setContentText("Слушам за гласова бележка...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    override fun onDestroy() {

        speechRecognizer?.destroy()
        speechRecognizer = null

        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? =
        null
}
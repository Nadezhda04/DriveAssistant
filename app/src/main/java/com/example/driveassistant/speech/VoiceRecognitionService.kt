package com.example.driveassistant.speech

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.CalendarContract
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
import java.util.Calendar
import java.util.Locale
import android.app.PendingIntent

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

                        val calendarEvent =
                            parseCalendarEvent(text)

                        saveNote(
                            text = text,
                            calendarEvent = calendarEvent
                        )

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

    private fun saveNote(
        text: String,
        calendarEvent: CalendarEvent?
    ) {

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

            if (calendarEvent != null) {

                Log.d(
                    "VoiceRecognitionService",
                    "Calendar event detected: " +
                            "${calendarEvent.title} - " +
                            "${calendarEvent.startTime}"
                )

                showCalendarNotification(calendarEvent)
            }

            stopSelf()
        }
    }

    private fun parseCalendarEvent(
        originalText: String
    ): CalendarEvent? {

        val text =
            originalText
                .lowercase(Locale("bg", "BG"))
                .trim()

        val calendar =
            Calendar.getInstance()

        var dateFound = false

        /*
         * ДНЕС
         */
        if (
            Regex("\\bднес\\b")
                .containsMatchIn(text)
        ) {
            dateFound = true
        }

        /*
         * УТРЕ
         */
        if (
            Regex("\\bутре\\b")
                .containsMatchIn(text)
        ) {

            calendar.add(
                Calendar.DAY_OF_MONTH,
                1
            )

            dateFound = true
        }

        /*
         * НА 12 СЕПТЕМВРИ
         */
        val months =
            mapOf(
                "януари" to Calendar.JANUARY,
                "февруари" to Calendar.FEBRUARY,
                "март" to Calendar.MARCH,
                "април" to Calendar.APRIL,
                "май" to Calendar.MAY,
                "юни" to Calendar.JUNE,
                "юли" to Calendar.JULY,
                "август" to Calendar.AUGUST,
                "септември" to Calendar.SEPTEMBER,
                "октомври" to Calendar.OCTOBER,
                "ноември" to Calendar.NOVEMBER,
                "декември" to Calendar.DECEMBER
            )

        val dateRegex =
            Regex(
                """(?:на\s+)?(\d{1,2})\s+(януари|февруари|март|април|май|юни|юли|август|септември|октомври|ноември|декември)"""
            )

        val dateMatch =
            dateRegex.find(text)

        if (dateMatch != null) {

            val day =
                dateMatch
                    .groupValues[1]
                    .toIntOrNull()

            val monthName =
                dateMatch
                    .groupValues[2]

            val month =
                months[monthName]

            if (
                day != null &&
                month != null
            ) {

                val now =
                    Calendar.getInstance()

                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    day
                )

                calendar.set(
                    Calendar.MONTH,
                    month
                )

                /*
                 * Ако датата за тази година вече е минала,
                 * приемаме следващата година.
                 */
                val testDate =
                    calendar.clone() as Calendar

                testDate.set(
                    Calendar.HOUR_OF_DAY,
                    23
                )

                testDate.set(
                    Calendar.MINUTE,
                    59
                )

                if (testDate.before(now)) {

                    calendar.add(
                        Calendar.YEAR,
                        1
                    )
                }

                dateFound = true
            }
        }

        /*
         * ЧАС:
         *
         * "в 15:30"
         * "15:30"
         * "в 9 часа"
         */

        var hour: Int? = null
        var minute: Int? = null

        val timeWithColon =
            Regex(
                """(?:\bв\s+)?(\d{1,2}):(\d{2})\b"""
            ).find(text)

        if (timeWithColon != null) {

            hour =
                timeWithColon
                    .groupValues[1]
                    .toIntOrNull()

            minute =
                timeWithColon
                    .groupValues[2]
                    .toIntOrNull()
        }

        if (hour == null) {

            val hourRegex =
                Regex(
                    """\bв\s+(\d{1,2})\s*(?:часа|час)?\b"""
                )

            val hourMatch =
                hourRegex.find(text)

            if (hourMatch != null) {

                hour =
                    hourMatch
                        .groupValues[1]
                        .toIntOrNull()

                minute = 0
            }
        }

        if (
            !dateFound ||
            hour == null ||
            minute == null
        ) {

            Log.d(
                "VoiceRecognitionService",
                "No complete calendar date/time detected"
            )

            return null
        }

        if (
            hour !in 0..23 ||
            minute !in 0..59
        ) {
            return null
        }

        calendar.set(
            Calendar.HOUR_OF_DAY,
            hour
        )

        calendar.set(
            Calendar.MINUTE,
            minute
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        val title =
            cleanEventTitle(text)

        return CalendarEvent(
            title =
                title.ifBlank {
                    "Drive Assistant"
                },
            startTime =
                calendar.timeInMillis
        )
    }

    private fun cleanEventTitle(
        text: String
    ): String {

        return text
            .replace(
                Regex("\\bднес\\b"),
                ""
            )
            .replace(
                Regex("\\bутре\\b"),
                ""
            )
            .replace(
                Regex(
                    """(?:на\s+)?\d{1,2}\s+(януари|февруари|март|април|май|юни|юли|август|септември|октомври|ноември|декември)"""
                ),
                ""
            )
            .replace(
                Regex(
                    """(?:\bв\s+)?\d{1,2}:\d{2}\b"""
                ),
                ""
            )
            .replace(
                Regex(
                    """\bв\s+\d{1,2}\s*(?:часа|час)?\b"""
                ),
                ""
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    private fun showCalendarNotification(
        event: CalendarEvent
    ) {

        val endTime =
            event.startTime + 60 * 60 * 1000

        val calendarIntent =
            Intent(Intent.ACTION_INSERT).apply {

                data =
                    CalendarContract.Events.CONTENT_URI

                putExtra(
                    CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                    event.startTime
                )

                putExtra(
                    CalendarContract.EXTRA_EVENT_END_TIME,
                    endTime
                )

                putExtra(
                    CalendarContract.Events.TITLE,
                    event.title
                )
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                event.startTime.hashCode(),
                calendarIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val channelId =
            "calendar_suggestions"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    channelId,
                    "Calendar suggestions",
                    NotificationManager.IMPORTANCE_HIGH
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }

        val notification =
            Notification.Builder(
                this,
                channelId
            )
                .setSmallIcon(
                    android.R.drawable.ic_menu_my_calendar
                )
                .setContentTitle(
                    "Добави в календар"
                )
                .setContentText(
                    event.title
                )
                .setContentIntent(
                    pendingIntent
                )
                .setAutoCancel(true)
                .build()

        getSystemService(
            NotificationManager::class.java
        ).notify(
            event.startTime.hashCode(),
            notification
        )
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
            .setContentTitle(
                "Drive Assistant"
            )
            .setContentText(
                "Слушам за гласова бележка..."
            )
            .setSmallIcon(
                android.R.drawable.ic_btn_speak_now
            )
            .build()
    }

    override fun onDestroy() {

        speechRecognizer?.destroy()
        speechRecognizer = null

        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null

    private data class CalendarEvent(
        val title: String,
        val startTime: Long
    )
}
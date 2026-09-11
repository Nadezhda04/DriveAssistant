package com.example.driveassistant.speech

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Calendar
import java.util.Locale

class VoiceRecognitionService : Service() {

    companion object {
        val isListening = MutableStateFlow(false)
    }

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
                    isListening.value = true
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
                    isListening.value = false
                    Log.d(
                        "VoiceRecognitionService",
                        "Обработвам..."
                    )
                }

                override fun onError(error: Int) {
                    isListening.value = false
                    Log.e(
                        "VoiceRecognitionService",
                        "Speech error: $error"
                    )

                    stopSelf()
                }

                override fun onResults(results: Bundle?) {
                    isListening.value = false
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

                showCalendarNotification(
                    calendarEvent
                )
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

        // ДНЕС
        if (
            Regex("\\bднес\\b")
                .containsMatchIn(text)
        ) {
            dateFound = true
        }

        // УТРЕ
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

        // СЛЕДВАЩИЯ ПОНЕДЕЛНИК / ВТОРНИК...
        val weekdays =
            mapOf(
                "понеделник" to Calendar.MONDAY,
                "вторник" to Calendar.TUESDAY,
                "сряда" to Calendar.WEDNESDAY,
                "четвъртък" to Calendar.THURSDAY,
                "петък" to Calendar.FRIDAY,
                "събота" to Calendar.SATURDAY,
                "неделя" to Calendar.SUNDAY
            )

        val weekdayRegex =
            Regex(
                """следващ(?:ия|ият|ата|ото)\s+(понеделник|вторник|сряда|четвъртък|петък|събота|неделя)"""
            )

        val weekdayMatch =
            weekdayRegex.find(text)

        if (weekdayMatch != null) {

            val weekdayName =
                weekdayMatch.groupValues[1]

            val targetDay =
                weekdays[weekdayName]

            if (targetDay != null) {

                val currentDay =
                    calendar.get(
                        Calendar.DAY_OF_WEEK
                    )

                var daysToAdd =
                    targetDay - currentDay

                if (daysToAdd <= 0) {
                    daysToAdd += 7
                }

                calendar.add(
                    Calendar.DAY_OF_MONTH,
                    daysToAdd
                )

                dateFound = true
            }
        }

        // В ПОНЕДЕЛНИК / ВЪВ ВТОРНИК...
        if (!dateFound) {

            val simpleWeekdayRegex =
                Regex(
                    """\b(?:в|във)\s+(понеделник|вторник|сряда|четвъртък|петък|събота|неделя)\b"""
                )

            val simpleWeekdayMatch =
                simpleWeekdayRegex.find(text)

            if (simpleWeekdayMatch != null) {

                val weekdayName =
                    simpleWeekdayMatch.groupValues[1]

                val targetDay =
                    weekdays[weekdayName]

                if (targetDay != null) {

                    val currentDay =
                        calendar.get(Calendar.DAY_OF_WEEK)

                    var daysToAdd =
                        targetDay - currentDay

                    if (daysToAdd <= 0) {
                        daysToAdd += 7
                    }

                    calendar.add(
                        Calendar.DAY_OF_MONTH,
                        daysToAdd
                    )

                    dateFound = true
                }
            }
        }

        // НА 12 СЕПТЕМВРИ
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

            val month =
                months[
                    dateMatch.groupValues[2]
                ]

            if (
                day != null &&
                month != null
            ) {

                val now =
                    Calendar.getInstance()

                calendar.set(
                    Calendar.MONTH,
                    month
                )

                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    day
                )

                if (
                    calendar.before(now)
                ) {
                    calendar.add(
                        Calendar.YEAR,
                        1
                    )
                }

                dateFound = true
            }
        }

        // НА 12-ТИ
        val dayOnlyRegex =
            Regex(
                """\bна\s+(\d{1,2})(?:-?ти|-?ри|-?ви|-?ми)?\b"""
            )

        val dayOnlyMatch =
            dayOnlyRegex.find(text)

        if (
            dayOnlyMatch != null &&
            dateMatch == null
        ) {

            val day =
                dayOnlyMatch
                    .groupValues[1]
                    .toIntOrNull()

            if (
                day != null &&
                day in 1..31
            ) {

                val now =
                    Calendar.getInstance()

                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    day
                )

                if (
                    calendar.before(now)
                ) {
                    calendar.add(
                        Calendar.MONTH,
                        1
                    )

                    calendar.set(
                        Calendar.DAY_OF_MONTH,
                        day
                    )
                }

                dateFound = true
            }
        }

        var hour: Int? = null
        var minute: Int? = null

        // 15:30
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

        // В 9 ЧАСА
        if (hour == null) {

            val numericHourMatch =
                Regex(
                    """\bв\s+(\d{1,2})\s*(?:часа|час)?\b"""
                ).find(text)

            if (numericHourMatch != null) {

                hour =
                    numericHourMatch
                        .groupValues[1]
                        .toIntOrNull()

                minute = 0
            }
        }

        // В ПЕТ И ПОЛОВИНА
        if (hour == null) {

            val wordHours =
                mapOf(
                    "един" to 1,
                    "едно" to 1,
                    "два" to 2,
                    "две" to 2,
                    "три" to 3,
                    "четири" to 4,
                    "пет" to 5,
                    "шест" to 6,
                    "седем" to 7,
                    "осем" to 8,
                    "девет" to 9,
                    "десет" to 10,
                    "единадесет" to 11,
                    "дванадесет" to 12,
                    "тринадесет" to 13,
                    "четиринадесет" to 14,
                    "петнадесет" to 15,
                    "шестнадесет" to 16,
                    "седемнадесет" to 17,
                    "осемнадесет" to 18,
                    "деветнадесет" to 19,
                    "двадесет" to 20
                )

            val halfPastRegex =
                Regex(
                    """\bв\s+([а-я]+)\s+и\s+половина\b"""
                )

            val halfPastMatch =
                halfPastRegex.find(text)

            if (halfPastMatch != null) {

                val hourWord =
                    halfPastMatch
                        .groupValues[1]

                val parsedHour =
                    wordHours[hourWord]

                if (parsedHour != null) {

                    hour = parsedHour
                    minute = 30
                }
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
                    """следващ(?:ия|ият|ата|ото)\s+(понеделник|вторник|сряда|четвъртък|петък|събота|неделя)"""
                ),
                ""
            )

            .replace(
                Regex(
                    """\b(?:в|във)\s+(понеделник|вторник|сряда|четвъртък|петък|събота|неделя)\b"""
                ),
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
                    """\bна\s+\d{1,2}(?:-?ти|-?ри|-?ви|-?ми)?\b"""
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
                Regex(
                    """\bв\s+[а-я]+\s+и\s+половина\b"""
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
            event.startTime +
                    60 * 60 * 1000

        val calendarIntent =
            Intent(
                Intent.ACTION_INSERT
            ).apply {

                data =
                    CalendarContract
                        .Events
                        .CONTENT_URI

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

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    channelId,
                    "Calendar suggestions",
                    NotificationManager.IMPORTANCE_HIGH
                )

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(
                channel
            )
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
            ).createNotificationChannel(
                channel
            )
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
        isListening.value = false
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
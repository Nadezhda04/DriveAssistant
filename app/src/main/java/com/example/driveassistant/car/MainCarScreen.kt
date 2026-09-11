package com.example.driveassistant.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.driveassistant.data.AppDatabase
import com.example.driveassistant.data.VoiceNote
import com.example.driveassistant.speech.VoiceRecognitionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainCarScreen(
    carContext: CarContext
) : Screen(carContext) {

    private val database =
        AppDatabase.getInstance(carContext)

    private val voiceNoteDao =
        database.voiceNoteDao()

    private val screenScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main
        )

    private var notes: List<VoiceNote> =
        emptyList()

    private var isListening = false

    init {
        screenScope.launch {
            voiceNoteDao
                .getAllNotes()
                .collectLatest { newNotes ->

                    notes = newNotes
                    invalidate()
                }
        }
        screenScope.launch {

            VoiceRecognitionService
                .isListening
                .collectLatest { listening ->

                    isListening = listening
                    invalidate()
                }
        }
    }

    override fun onGetTemplate(): Template {

        val itemListBuilder =
            ItemList.Builder()

        notes
            .take(10)
            .forEach { note ->

                itemListBuilder.addItem(
                    Row.Builder()
                        .setTitle(note.text)
                        .addText(
                            formatDate(
                                note.createdAt
                            )
                        )
                        .build()
                )
            }

        if (notes.isEmpty()) {

            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(
                        "Няма запазени бележки"
                    )
                    .addText(
                        "Добавете бележка от автомобила."
                    )
                    .build()
            )
        }

        val microphoneAction =
            Action.Builder()
                .setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            android.R.drawable.ic_btn_speak_now
                        )
                    ).build()
                )
                .setOnClickListener {
                    startVoiceRecognition()
                }
                .build()

        return ListTemplate.Builder()
            .setTitle(
                if (isListening)
                    "🎤 Слушам..."
                else
                    "Drive Assistant"
            )
            .setSingleList(
                itemListBuilder.build()
            )
            .setHeaderAction(
                Action.APP_ICON
            )
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        microphoneAction
                    )
                    .build()
            )
            .build()
    }

    private fun startVoiceRecognition() {

        val intent =
            android.content.Intent(
                carContext,
                com.example.driveassistant.speech.VoiceRecognitionService::class.java
            )

        androidx.core.content.ContextCompat.startForegroundService(
            carContext,
            intent
        )
    }

    private fun formatDate(
        timestamp: Long
    ): String {

        val formatter =
            SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale("bg", "BG")
            )

        return formatter.format(
            Date(timestamp)
        )
    }
}
package com.example.driveassistant.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.Observer
import com.example.driveassistant.data.AppDatabase
import com.example.driveassistant.data.VoiceNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    private val voiceInputManager =
        CarVoiceInputManager(carContext)

    private var notes: List<VoiceNote> = emptyList()

    init {
        screenScope.launch {
            voiceNoteDao
                .getAllNotes()
                .collectLatest { newNotes ->
                    notes = newNotes
                    invalidate()
                }
        }
    }

    override fun onGetTemplate(): Template {

        val itemListBuilder =
            ItemList.Builder()

        notes.take(10).forEach { note ->

            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle(note.text)
                    .addText(
                        formatDate(note.createdAt)
                    )
                    .build()
            )
        }

        if (notes.isEmpty()) {
            itemListBuilder.addItem(
                Row.Builder()
                    .setTitle("Няма запазени бележки")
                    .addText(
                        "Добавете бележка от автомобила."
                    )
                    .build()
            )
        }

        val addTestNoteAction =
            Action.Builder()
                .setTitle("Микрофон")
                .setOnClickListener {
                    startVoiceTest()
                }.build()

        return ListTemplate.Builder()
            .setTitle("Drive Assistant")
            .setSingleList(
                itemListBuilder.build()
            )
            .setHeaderAction(
                Action.APP_ICON
            )
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(addTestNoteAction)
                    .build()
            )
            .build()
    }

    private fun createNote(text: String) {
        screenScope.launch(Dispatchers.IO) {

            val activeTrip =
                database.tripDao().getActiveTrip()

            if (activeTrip != null) {
                voiceNoteDao.insert(
                    VoiceNote(
                        text = text,
                        tripId = activeTrip.id
                    )
                )
            }
        }
    }

    private fun startVoiceTest() {

        var receivedBytes = 0

        voiceInputManager.startRecording(

            onStarted = {
                android.util.Log.d(
                    "CarVoiceInput",
                    "Recording started"
                )
            },

            onAudioData = { data ->

                receivedBytes += data.size

                android.util.Log.d(
                    "CarVoiceInput",
                    "Received audio bytes: $receivedBytes"
                )

                // Засега спираме след приблизително
                // достатъчно аудио за кратък тест.
                if (receivedBytes >= 32000) {
                    voiceInputManager.stopRecording()

                    createNote(
                        "Получен е аудио запис от автомобила"
                    )
                }
            },

            onError = { error ->

                android.util.Log.e(
                    "CarVoiceInput",
                    error
                )
            }
        )
    }

    private fun formatDate(
        timestamp: Long
    ): String {

        val formatter =
            java.text.SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                java.util.Locale(
                    "bg",
                    "BG"
                )
            )

        return formatter.format(
            java.util.Date(timestamp)
        )
    }
}
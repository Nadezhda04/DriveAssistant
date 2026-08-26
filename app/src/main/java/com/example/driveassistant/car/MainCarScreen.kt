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
                .setTitle("Тестова бележка")
                .setOnClickListener {
                    addTestNote()
                }
                .build()

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

    private fun addTestNote() {

        screenScope.launch(
            Dispatchers.IO
        ) {
            voiceNoteDao.insert(
                VoiceNote(
                    text = "Тестова бележка от автомобила"
                )
            )
        }
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
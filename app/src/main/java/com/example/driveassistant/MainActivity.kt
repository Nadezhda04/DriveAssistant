package com.example.driveassistant

import android.os.Bundle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.driveassistant.data.AppDatabase
import com.example.driveassistant.ui.theme.DriveAssistantTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import com.example.driveassistant.trip.TripMonitoringService
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tripServiceIntent =
            Intent(
                this,
                TripMonitoringService::class.java
            )

        ContextCompat.startForegroundService(
            this,
            tripServiceIntent
        )

        setContent {
            DriveAssistantTheme {

                val database = remember {
                    AppDatabase.getInstance(this)
                }

                val voiceNoteDao = remember {
                    database.voiceNoteDao()
                }

                val notes by voiceNoteDao
                    .getAllNotes()
                    .collectAsState(initial = emptyList())

                val coroutineScope = rememberCoroutineScope()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Drive Assistant",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "Запазени бележки",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    LazyColumn {

                        items(
                            items = notes,
                            key = { note -> note.id }
                        ) { note ->
                            val formattedDate = remember(note.createdAt) {
                                SimpleDateFormat(
                                    "dd.MM.yyyy HH:mm",
                                    Locale("bg", "BG")
                                ).format(Date(note.createdAt))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = note.text,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            voiceNoteDao.delete(note)
                                        }
                                    }
                                ) {
                                    Text("Изтрий")
                                }


                            }
                        }
                    }
                }
            }
        }
    }
}
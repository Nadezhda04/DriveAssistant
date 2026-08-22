package com.example.driveassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.rememberCoroutineScope
import com.example.driveassistant.data.AppDatabase
import com.example.driveassistant.data.VoiceNote
import com.example.driveassistant.speech.SpeechRecognitionManager
import com.example.driveassistant.ui.theme.DriveAssistantTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            DriveAssistantTheme {

                var recognizedText by remember {
                    mutableStateOf("Няма разпознат текст")
                }

                var statusText by remember {
                    mutableStateOf("Готов")
                }

                var isListening by remember {
                    mutableStateOf(false)
                }

                val coroutineScope = rememberCoroutineScope()

                val database = remember {
                    AppDatabase.getInstance(this)
                }

                val voiceNoteDao = remember {
                    database.voiceNoteDao()
                }

                val notes by voiceNoteDao
                    .getAllNotes()
                    .collectAsState(initial = emptyList())

                val speechManager = remember {
                    SpeechRecognitionManager(
                        context = this,
                        onStatusChanged = { status ->
                            statusText = status

                            isListening = status == "Слушам..." ||
                                    status == "Разпознавам реч..." ||
                                    status == "Обработвам..."
                        },
                        onPartialResult = { partialText ->
                            recognizedText = partialText
                        },
                        onFinalResult = { finalText ->
                            recognizedText = finalText
                            isListening = false

                            if (finalText.isNotBlank()) {
                                coroutineScope.launch {
                                    voiceNoteDao.insert(
                                        VoiceNote(
                                            text = finalText
                                        )
                                    )
                                }
                            }
                        }
                    )
                }

                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { granted ->

                        if (granted) {
                            statusText = "Готов"
                        } else {
                            statusText = "Няма разрешение за микрофона"
                        }
                    }

                DisposableEffect(Unit) {
                    onDispose {
                        speechManager.destroy()
                    }
                }

                fun startListening() {

                    val permissionGranted =
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                    if (!permissionGranted) {
                        permissionLauncher.launch(
                            Manifest.permission.RECORD_AUDIO
                        )
                        return
                    }

                    recognizedText = ""
                    statusText = "Стартирам..."

                    speechManager.startListening()
                }

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
                        modifier = Modifier.padding(top = 24.dp),
                        text = statusText
                    )

                    Text(
                        modifier = Modifier.padding(vertical = 24.dp),
                        text = if (recognizedText.isBlank()) {
                            "..."
                        } else {
                            recognizedText
                        }
                    )

                    Button(
                        onClick = {
                            if (isListening) {
                                speechManager.stopListening()
                            } else {
                                startListening()
                            }
                        }
                    ) {
                        Text(
                            if (isListening) {
                                "⏹ Спри"
                            } else {
                                "🎤 Говори"
                            }
                        )
                    }
                    Spacer(
                        modifier = Modifier.height(32.dp)
                    )

                    Text(
                        text = "Запазени бележки",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    LazyColumn {
                        items(
                            items = notes,
                            key = { note -> note.id }
                        ) { note ->

                            Text(
                                text = note.text,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
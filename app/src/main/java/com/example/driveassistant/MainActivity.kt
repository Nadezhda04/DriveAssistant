package com.example.driveassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.driveassistant.data.AppDatabase
import com.example.driveassistant.data.VoiceNote
import com.example.driveassistant.speech.SpeechRecognitionManager
import com.example.driveassistant.ui.theme.DriveAssistantTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.livedata.observeAsState
import androidx.car.app.connection.CarConnection
import com.example.driveassistant.carconnection.CarConnectionManager
import androidx.compose.runtime.LaunchedEffect
import com.example.driveassistant.notification.NotificationHelper
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

                val carConnectionManager = remember {
                    CarConnectionManager(this)
                }

                val notificationHelper = remember {
                    NotificationHelper(this)
                }

                val notificationPermissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) {
                    }

                LaunchedEffect(Unit) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                        val permissionGranted =
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                        if (!permissionGranted) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                }



                val connectionType by carConnectionManager
                    .connectionType
                    .observeAsState(
                        initial = CarConnection.CONNECTION_TYPE_NOT_CONNECTED
                    )

                val carConnectionText = when (connectionType) {
                    CarConnection.CONNECTION_TYPE_PROJECTION ->
                        "Android Auto: свързан"

                    CarConnection.CONNECTION_TYPE_NATIVE ->
                        "Android Automotive: свързан"

                    else ->
                        "Автомобил: няма връзка"
                }

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
                        text = carConnectionText,
                        modifier = Modifier.padding(top = 8.dp)
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

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                voiceNoteDao.insert(
                                    VoiceNote(
                                        text = "Тестова бележка от emulator"
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Добави тестова бележка")
                    }

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
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    Text(
                                        text = formattedDate,
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
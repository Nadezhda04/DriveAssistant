package com.example.driveassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.driveassistant.ui.theme.DriveAssistantTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DriveAssistantTheme {

                var recognizedText by remember {
                    mutableStateOf("Няма разпознат текст")
                }

                val speechLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->

                        if (result.resultCode == RESULT_OK) {
                            val results =
                                result.data?.getStringArrayListExtra(
                                    RecognizerIntent.EXTRA_RESULTS
                                )

                            recognizedText =
                                results?.firstOrNull()
                                    ?: "Не беше разпознат текст"
                        }
                    }

                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { granted ->

                        if (!granted) {
                            recognizedText =
                                "Нужно е разрешение за микрофона"
                        }
                    }

                fun startSpeechRecognition() {

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

                    val intent =
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {

                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )

                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE,
                                "bg-BG"
                            )

                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                "Кажете бележката"
                            )
                        }

                    speechLauncher.launch(intent)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Drive Assistant",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        modifier = Modifier.padding(vertical = 24.dp),
                        text = recognizedText
                    )

                    Button(
                        onClick = {
                            startSpeechRecognition()
                        }
                    ) {
                        Text("🎤 Говори")
                    }
                }
            }
        }
    }
}
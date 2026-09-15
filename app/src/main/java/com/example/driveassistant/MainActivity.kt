package com.example.driveassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.driveassistant.data.AppDatabase
import com.example.driveassistant.data.Trip
import com.example.driveassistant.data.VoiceNote
import com.example.driveassistant.trip.TripMonitoringService
import com.example.driveassistant.ui.theme.DriveAssistantTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextCompat.startForegroundService(
            this,
            Intent(
                this,
                TripMonitoringService::class.java
            )
        )

        setContent {

            DriveAssistantTheme {

                val database = remember {
                    AppDatabase.getInstance(this)
                }

                val voiceNoteDao = remember {
                    database.voiceNoteDao()
                }

                val tripDao = remember {
                    database.tripDao()
                }

                val notes by voiceNoteDao
                    .getAllNotes()
                    .collectAsState(initial = emptyList())

                val trips by tripDao
                    .getAllTrips()
                    .collectAsState(initial = emptyList())

                val coroutineScope =
                    rememberCoroutineScope()

                var selectedTab by remember {
                    mutableIntStateOf(0)
                }

                var selectedTrip by remember {
                    mutableStateOf<Trip?>(null)
                }

                var noteToEdit by remember {
                    mutableStateOf<VoiceNote?>(null)
                }

                var editedText by remember {
                    mutableStateOf("")
                }

                var noteToDelete by remember {
                    mutableStateOf<VoiceNote?>(null)
                }

                var showHome by remember {
                    mutableStateOf(true)
                }

                val tripColors = listOf(
                    Color(0xFFE995B7),
                    Color(0xFFB165B3),
                    Color(0xFF5551AD),
                    Color(0xFF8C6BC1),
                    Color(0xFFE09A67)
                )

                if (selectedTrip != null) {

                    val trip = selectedTrip!!

                    val tripNotes =
                        notes.filter {
                            it.tripId == trip.id
                        }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {

                        TextButton(
                            onClick = {
                                selectedTrip = null
                            }
                        ) {
                            Text("← Назад")
                        }

                        // Header
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 28.dp,
                                bottomStart = 28.dp,
                                bottomEnd = 28.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF4B183)
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {

                                Text(
                                    text = "Пътуване",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )

                                Text(
                                    text = SimpleDateFormat(
                                        "dd.MM.yyyy",
                                        Locale("bg", "BG")
                                    ).format(
                                        Date(trip.startedAt)
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFFFF7F0)
                                )
                            }
                        }

                        // Информация за пътуването
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF3EBDD)
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text = "Начало",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Text(
                                            text = formatTime(trip.startedAt),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text = "Край",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Text(
                                            text =
                                                if (trip.endedAt != null)
                                                    formatTime(trip.endedAt)
                                                else
                                                    "В процес",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }

                                Text(
                                    text = "Продължителност",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 16.dp)
                                )

                                Text(
                                    text =
                                        if (trip.endedAt != null)
                                            formatDuration(
                                                trip.startedAt,
                                                trip.endedAt
                                            )
                                        else
                                            "—",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "${tripNotes.size} бележки",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }

                        Text(
                            text = "Бележки",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn {

                            items(
                                items = tripNotes,
                                key = { it.id }
                            ) { note ->

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {

                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {

                                        Text(
                                            text = note.text,
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        Text(
                                            text = formatDate(
                                                note.createdAt
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                } else {
                    if (showHome) {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp),
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 28.dp,
                                    bottomStart = 28.dp,
                                    bottomEnd = 28.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF4B183)
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {

                                    Text(
                                        text = "Drive Assistant",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White
                                    )

                                    Text(
                                        text = "Вашите бележки от пътуванията",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFFF7F0)
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        selectedTab = 0
                                        showHome = false
                                    },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF3EBDD)
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(24.dp)
                                ) {

                                    Text(
                                        text = "📝 Бележки",
                                        style = MaterialTheme.typography.titleLarge
                                    )

                                    Text(
                                        text = "${notes.size} запазени бележки",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        selectedTab = 1
                                        showHome = false
                                    },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFCDEBDD)
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(24.dp)
                                ) {

                                    Text(
                                        text = "🚗 Пътувания",
                                        style = MaterialTheme.typography.titleLarge
                                    )

                                    Text(
                                        text =
                                            "${
                                                trips.count { trip ->
                                                    notes.any {
                                                        it.tripId == trip.id
                                                    }
                                                }
                                            } пътувания",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }

                    } else {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    showHome = true
                                }
                            ) {
                                Text("← Начало")
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp),
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 28.dp,
                                    bottomStart = 28.dp,
                                    bottomEnd = 28.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF4B183)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Text(
                                        text = "Drive Assistant",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White
                                    )

                                    Text(
                                        text = "Вашите бележки от пътуванията",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFFF7F0),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 18.dp),
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedTab = 0
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (selectedTab == 0)
                                                Color(0xFFF4B183)   // apricot
                                            else
                                                Color(0xFFF3EBDD),  // light beige

                                        contentColor = Color(0xFF4A443C)
                                    )
                                ) {
                                    Text("Бележки")
                                }
                                Button(
                                    onClick = {
                                        selectedTab = 1
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (selectedTab == 1)
                                                Color(0xFFF4B183)   // apricot
                                            else
                                                Color(0xFFF3EBDD),  // light beige

                                        contentColor = Color(0xFF4A443C)
                                    )
                                ) {
                                    Text("Пътувания")
                                }
                            }

                            if (selectedTab == 0) {

                                LazyColumn {

                                    items(
                                        items = notes,
                                        key = { it.id }
                                    ) { note ->

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    vertical = 6.dp
                                                ),
                                            shape =
                                                RoundedCornerShape(
                                                    20.dp
                                                )
                                        ) {

                                            Column(
                                                modifier =
                                                    Modifier.padding(
                                                        16.dp
                                                    )
                                            ) {

                                                Text(
                                                    text =
                                                        note.text,
                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .bodyLarge
                                                )

                                                Text(
                                                    text =
                                                        formatDate(
                                                            note.createdAt
                                                        )
                                                )

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {

                                                    IconButton(
                                                        onClick = {
                                                            noteToEdit = note
                                                            editedText = note.text
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Редактирай"
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            noteToDelete = note
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Изтрий"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            } else {

                                LazyColumn {

                                    items(
                                        items = trips.filter { trip ->
                                            notes.any { note ->
                                                note.tripId == trip.id
                                            }
                                        },
                                        key = { it.id }
                                    ) { trip ->

                                        val tripNotes =
                                            notes.filter {
                                                it.tripId ==
                                                        trip.id
                                            }

                                        val color =
                                            tripColors[
                                                (trip.id %
                                                        tripColors.size)
                                                    .toInt()
                                            ]

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    vertical = 8.dp
                                                )
                                                .clickable {
                                                    selectedTrip =
                                                        trip
                                                },
                                            shape =
                                                RoundedCornerShape(
                                                    28.dp
                                                ),
                                            colors =
                                                CardDefaults
                                                    .cardColors(
                                                        containerColor =
                                                            color
                                                    )
                                        ) {

                                            Column(
                                                modifier =
                                                    Modifier.padding(
                                                        20.dp
                                                    )
                                            ) {

                                                Text(
                                                    text =
                                                        formatDate(
                                                            trip.startedAt
                                                        ),
                                                    color =
                                                        Color.White,
                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .titleLarge
                                                )

                                                Text(
                                                    text =
                                                        "${tripNotes.size} бележки",
                                                    color =
                                                        Color.White
                                                )

                                                tripNotes
                                                    .firstOrNull()
                                                    ?.let { note ->

                                                        Text(
                                                            text =
                                                                note.text,
                                                            color =
                                                                Color.White,
                                                            modifier =
                                                                Modifier.padding(
                                                                    top = 8.dp
                                                                )
                                                        )
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (noteToEdit != null) {

                    AlertDialog(
                        onDismissRequest = {
                            noteToEdit = null
                        },

                        title = {
                            Text(
                                "Редактиране на бележка"
                            )
                        },

                        text = {

                            OutlinedTextField(
                                value = editedText,
                                onValueChange = {
                                    editedText = it
                                }
                            )
                        },

                        confirmButton = {

                            TextButton(
                                onClick = {

                                    val note =
                                        noteToEdit

                                    if (
                                        note != null &&
                                        editedText
                                            .isNotBlank()
                                    ) {

                                        coroutineScope.launch {

                                            voiceNoteDao.update(
                                                note.copy(
                                                    text =
                                                        editedText
                                                            .trim()
                                                )
                                            )

                                            noteToEdit =
                                                null
                                        }
                                    }
                                }
                            ) {
                                Text("Запази")
                            }
                        },

                        dismissButton = {

                            TextButton(
                                onClick = {
                                    noteToEdit = null
                                }
                            ) {
                                Text("Отказ")
                            }
                        }
                    )
                }
                if (noteToDelete != null) {

                    AlertDialog(
                        onDismissRequest = {
                            noteToDelete = null
                        },

                        title = {
                            Text("Изтриване на бележка")
                        },

                        text = {
                            Text("Сигурни ли сте, че искате да изтриете тази бележка?")
                        },

                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val note = noteToDelete

                                    if (note != null) {
                                        coroutineScope.launch {
                                            voiceNoteDao.delete(note)
                                            noteToDelete = null
                                        }
                                    }
                                }
                            ) {
                                Text("Изтрий")
                            }
                        },

                        dismissButton = {
                            TextButton(
                                onClick = {
                                    noteToDelete = null
                                }
                            ) {
                                Text("Отказ")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun formatDate(
        timestamp: Long
    ): String {

        return SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale("bg", "BG")
        ).format(
            Date(timestamp)
        )
    }

    private fun formatTime(
        timestamp: Long
    ): String {

        return SimpleDateFormat(
            "HH:mm",
            Locale("bg", "BG")
        ).format(
            Date(timestamp)
        )
    }

    private fun formatDuration(
        startedAt: Long,
        endedAt: Long
    ): String {

        val duration =
            endedAt - startedAt

        val totalMinutes =
            duration / 60000

        val hours =
            totalMinutes / 60

        val minutes =
            totalMinutes % 60

        return if (hours > 0) {
            "${hours} ч. ${minutes} мин."
        } else {
            "${minutes} мин."
        }
    }
}
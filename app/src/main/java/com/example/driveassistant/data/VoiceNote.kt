package com.example.driveassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val text: String,

    val createdAt: Long = System.currentTimeMillis()
)
package com.example.driveassistant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {

    @Insert
    suspend fun insert(note: VoiceNote)

    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<VoiceNote>>

    @Delete
    suspend fun delete(note: VoiceNote)
}
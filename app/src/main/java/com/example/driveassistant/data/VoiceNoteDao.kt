package com.example.driveassistant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNoteDao {

    @Insert
    suspend fun insert(note: VoiceNote)

    @Update
    suspend fun update(note: VoiceNote)

    @Delete
    suspend fun delete(note: VoiceNote)

    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<VoiceNote>>

    @Query("SELECT COUNT(*) FROM voice_notes WHERE tripId = :tripId")
    suspend fun countNotesForTrip(tripId: Long): Int

    @Query("SELECT * FROM voice_notes WHERE tripId = :tripId ORDER BY createdAt DESC")
    suspend fun getNotesForTrip(tripId: Long): List<VoiceNote>
}


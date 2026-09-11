package com.example.driveassistant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Query("SELECT * FROM trips WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveTrip(): Trip?

    @Query("SELECT * FROM trips ORDER BY startedAt DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM voice_notes WHERE tripId = :tripId ORDER BY createdAt DESC")
    fun getNotesForTrip(tripId: Long): Flow<List<VoiceNote>>
}
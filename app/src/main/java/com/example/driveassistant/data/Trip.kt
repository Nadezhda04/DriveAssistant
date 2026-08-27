package com.example.driveassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val startedAt: Long = System.currentTimeMillis(),

    val endedAt: Long? = null
)
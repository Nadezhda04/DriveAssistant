package com.example.driveassistant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VoiceNote::class,
        Trip::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun voiceNoteDao(): VoiceNoteDao

    abstract fun tripDao(): TripDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "drive_assistant_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// This class sets up the actual SQLite database file on the phone.
// It lists the 4 tables (entities) we want to store: Teams, Players, Matches, and Deliveries.
@Database(
    entities = [
        TeamEntity::class,
        PlayerEntity::class,
        MatchEntity::class,
        DeliveryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CricketDatabase : RoomDatabase() {

    // Gives us access to our SQL commands in CricketDao
    abstract fun cricketDao(): CricketDao

    companion object {
        // We only ever want ONE copy of the database open at a time so nothing gets mixed up.
        @Volatile
        private var INSTANCE: CricketDatabase? = null

        fun getDatabase(context: Context): CricketDatabase {
            // If the database is already open, return it. Otherwise, create it on the phone's storage.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketDatabase::class.java,
                    "ef101_cricket_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

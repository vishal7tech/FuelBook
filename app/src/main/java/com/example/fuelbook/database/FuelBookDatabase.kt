package com.example.fuelbook.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DailyFuelEntry::class,
        DailyCashEntry::class,
        DailyUpiEntry::class,
        ReportHistory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FuelBookDatabase : RoomDatabase() {
    abstract fun dailyFuelEntryDao(): DailyFuelEntryDao
    abstract fun dailyCashEntryDao(): DailyCashEntryDao
    abstract fun dailyUpiEntryDao(): DailyUpiEntryDao
    abstract fun reportHistoryDao(): ReportHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: FuelBookDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FuelBookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuelBookDatabase::class.java,
                    "fuelbook_database"
                )
                .addCallback(DatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    // Database is created, no initial data needed
                }
            }
        }
    }
}

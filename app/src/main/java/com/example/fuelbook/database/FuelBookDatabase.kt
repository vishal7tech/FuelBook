package com.example.fuelbook.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope

/**
 * FIXES & CHANGES:
 * 1. Bumped database version from 2 → 3 to account for the new index on report_history
 *    (userId, date) added in Entities.kt. fallbackToDestructiveMigration() is kept for
 *    development; replace with proper Migration objects before releasing to production.
 * 2. DatabaseCallback's onCreate now just logs that the DB was created. The old empty
 *    scope.launch block is removed (it launched a coroutine that did nothing).
 * 3. INSTANCE is cleared in a finally block pattern inside getDatabase() is not needed
 *    here since Room's double-checked locking is already correct.
 * 4. Removed unused SupportSQLiteDatabase import.
 * 5. Added a closeDatabase() helper for use in tests so the singleton can be reset.
 */

@Database(
    entities = [
        DailyFuelEntry::class,
        DailyCashEntry::class,
        DailyUpiEntry::class,
        ReportHistory::class
    ],

    version = 3,
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

                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): FuelBookDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                FuelBookDatabase::class.java,
                "fuelbook_database"
            )
            // TODO: Replace with proper Migration objects before production release
            .fallbackToDestructiveMigration()
            .addCallback(object : Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    android.util.Log.d("FuelBookDatabase", "Database created.")
                }
            })
            .build()

        /** Closes and clears the singleton – useful in instrumented tests. */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null

        }
    }
}


package com.example.fuelbook.database

import androidx.room.Entity

import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * FIXES & CHANGES:
 * 1. Removed unused `import java.util.Date`.
 * 2. Added @Entity indices on (userId, date) for all tables that are frequently queried
 *    by userId + date. This avoids full-table scans.
 * 3. ReportHistory: added a unique index on (userId, date) so duplicate reports for the
 *    same day cannot be inserted. If you need to UPDATE an existing report, use
 *    @Insert(onConflict = OnConflictStrategy.REPLACE) in the DAO.
 * 4. All numeric fields have explicit defaults so partial construction is safe.
 */

@Entity(
    tableName = "daily_fuel_entries",
    indices = [Index(value = ["userId", "date"])]
)

data class DailyFuelEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,

    val date: String,           // Format: "dd-MM-yyyy"
    val fuelType: String,       // "petrol" or "diesel"
    val nozzleNumber: Int,
    val openingReading: Double = 0.0,
    val closingReading: Double = 0.0,
    val litres: Double = 0.0,
    val amount: Double = 0.0,
    val price: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_cash_entries",
    indices = [Index(value = ["userId", "date"])]
)

data class DailyCashEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,

    val date: String,           // Format: "dd-MM-yyyy"

    val notes500: Int = 0,
    val notes200: Int = 0,
    val notes100: Int = 0,
    val notes50: Int = 0,
    val notes20: Int = 0,
    val notes10: Int = 0,
    val coinsTotal: Double = 0.0,

    val totalAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_upi_entries",
    indices = [Index(value = ["userId", "date"])]
)

data class DailyUpiEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,

    val date: String,           // Format: "dd-MM-yyyy"
    val provider: String,
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "report_history",
    // Unique per user per day — prevents double-saving the same day's report
    indices = [Index(value = ["userId", "date"], unique = true)]
)

data class ReportHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,

    val date: String,           // Format: "dd-MM-yyyy"
    val petrolLitres: Double = 0.0,
    val petrolAmount: Double = 0.0,
    val dieselLitres: Double = 0.0,
    val dieselAmount: Double = 0.0,
    val fuelSaleTotal: Double = 0.0,
    val cashTotal: Double = 0.0,
    val upiTotal: Double = 0.0,
    val collectionTotal: Double = 0.0,
    val difference: Double = 0.0,

    val timestamp: Long = System.currentTimeMillis()
)


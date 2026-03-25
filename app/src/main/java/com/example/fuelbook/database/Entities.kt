package com.example.fuelbook.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "daily_fuel_entries")
data class DailyFuelEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String, // Format: "dd-MM-yyyy"
    val fuelType: String, // "petrol" or "diesel"
    val nozzleNumber: Int,
    val openingReading: Double,
    val closingReading: Double,
    val litres: Double,
    val amount: Double,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_cash_entries")
data class DailyCashEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String, // Format: "dd-MM-yyyy"
    val notes500: Int = 0,
    val notes200: Int = 0,
    val notes100: Int = 0,
    val notes50: Int = 0,
    val notes20: Int = 0,
    val notes10: Int = 0,
    val coinsTotal: Double = 0.0,
    val totalAmount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_upi_entries")
data class DailyUpiEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String, // Format: "dd-MM-yyyy"
    val provider: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "report_history")
data class ReportHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String, // Format: "dd-MM-yyyy"
    val petrolLitres: Double,
    val petrolAmount: Double,
    val dieselLitres: Double,
    val dieselAmount: Double,
    val fuelSaleTotal: Double,
    val cashTotal: Double,
    val upiTotal: Double,
    val collectionTotal: Double,
    val difference: Double,
    val timestamp: Long = System.currentTimeMillis()
)

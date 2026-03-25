package com.example.fuelbook.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class DataGenerator(private val database: FuelBookDatabase) {
    
    suspend fun generateSampleData() = withContext(Dispatchers.IO) {
        val today = "04-01-2026"
        
        // Generate sample fuel entries
        val petrolEntry1 = DailyFuelEntry(
            userId = "vishalchaudhari@example.com",
            date = today,
            fuelType = "petrol",
            nozzleNumber = 1,
            openingReading = 1000.0,
            closingReading = 754.5,
            litres = 245.5,
            amount = 24550.0,
            price = 104.0
        )
        database.dailyFuelEntryDao().insertFuelEntry(petrolEntry1)
        
        val dieselEntry1 = DailyFuelEntry(
            userId = "vishalchaudhari@example.com",
            date = today,
            fuelType = "diesel",
            nozzleNumber = 1,
            openingReading = 1000.0,
            closingReading = 619.25,
            litres = 380.75,
            amount = 38075.0,
            price = 89.0
        )
        database.dailyFuelEntryDao().insertFuelEntry(dieselEntry1)
        
        // Generate sample cash collection
        val cashCollection = DailyCashEntry(
            userId = "vishalchaudhari@example.com",
            date = today,
            notes500 = 50,
            notes200 = 30,
            notes100 = 50,
            notes50 = 20,
            notes20 = 25,
            notes10 = 50,
            coinsTotal = 500.0,
            totalAmount = 35000.0
        )
        database.dailyCashEntryDao().insertCashCollection(cashCollection)
        
        // Generate sample UPI collection
        val upiCollection1 = DailyUpiEntry(
            userId = "vishalchaudhari@example.com",
            date = today,
            provider = "Paytm",
            amount = 15000.0
        )
        database.dailyUpiEntryDao().insertUpiEntry(upiCollection1)
        
        val upiCollection2 = DailyUpiEntry(
            userId = "vishalchaudhari@example.com",
            date = today,
            provider = "Google Pay",
            amount = 12625.0
        )
        database.dailyUpiEntryDao().insertUpiEntry(upiCollection2)
    }
}

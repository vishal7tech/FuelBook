package com.example.fuelbook.database


import com.example.fuelbook.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FIXES & CHANGES:
 * 1. Hardcoded userId "vishalchaudhari@example.com" replaced with
 *    SharedPreferencesManager.DEFAULT_USER_ID constant so there is a single source of truth.
 * 2. Hardcoded date "04-01-2026" replaced with DateUtils.currentDate.
 * 3. BUG FIX – cashCollection.totalAmount was hardcoded to 35000.0 but should be computed
 *    from the denomination counts to ensure data consistency.
 * 4. Added a guard: generateSampleData() first checks whether data already exists for
 *    today so it won't insert duplicates if called more than once.
 * 5. Removed unused import java.util.*.
 */
class DataGenerator(private val database: FuelBookDatabase) {

    private val userId = com.example.fuelbook.utils.SharedPreferencesManager.DEFAULT_USER_ID

    /**
     * Generates sample data for the current test date.
     * Safe to call multiple times – skips insertion if data already exists.
     */
    suspend fun generateSampleData() = withContext(Dispatchers.IO) {
        val today = DateUtils.currentDate

        // Guard: skip if fuel entries already exist for today
        val existingEntries = database.dailyFuelEntryDao().getEntriesByDate(userId, today)
        if (existingEntries.isNotEmpty()) return@withContext

        // ── Petrol entry ──────────────────────────────────────────
        database.dailyFuelEntryDao().insertFuelEntry(
            DailyFuelEntry(
                userId         = userId,
                date           = today,
                fuelType       = "petrol",
                nozzleNumber   = 1,
                openingReading = 1000.0,
                closingReading = 754.5,
                litres         = 245.5,
                amount         = 245.5 * 104.0,
                price          = 104.0
            )
        )

        // ── Diesel entry ──────────────────────────────────────────
        database.dailyFuelEntryDao().insertFuelEntry(
            DailyFuelEntry(
                userId         = userId,
                date           = today,
                fuelType       = "diesel",
                nozzleNumber   = 1,
                openingReading = 1000.0,
                closingReading = 619.25,
                litres         = 380.75,
                amount         = 380.75 * 89.0,
                price          = 89.0
            )
        )

        // ── Cash collection ───────────────────────────────────────
        val notes500 = 50; val notes200 = 30; val notes100 = 50
        val notes50  = 20; val notes20  = 25; val notes10  = 50
        val coinsTotal = 500.0
        val computedCashTotal =
            (notes500 * 500) + (notes200 * 200) + (notes100 * 100) +
            (notes50  * 50 ) + (notes20  * 20 ) + (notes10  * 10 ) + coinsTotal

        database.dailyCashEntryDao().insertCashCollection(
            DailyCashEntry(
                userId      = userId,
                date        = today,
                notes500    = notes500,
                notes200    = notes200,
                notes100    = notes100,
                notes50     = notes50,
                notes20     = notes20,
                notes10     = notes10,
                coinsTotal  = coinsTotal,
                totalAmount = computedCashTotal
            )
        )

        // ── UPI collections ───────────────────────────────────────
        database.dailyUpiEntryDao().insertUpiEntry(
            DailyUpiEntry(userId = userId, date = today, provider = "Paytm",      amount = 15000.0)
        )
        database.dailyUpiEntryDao().insertUpiEntry(
            DailyUpiEntry(userId = userId, date = today, provider = "Google Pay",  amount = 12625.0)
        )

    }
}


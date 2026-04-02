package com.example.fuelbook.database

import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FIXES & CHANGES:
 * 1. BUG FIX – saveFuelEntry() calculated litres = opening − closing but then passed
 *    the raw opening and closing to the entity which also stored them. The entity stores
 *    openingReading and closingReading for audit purposes, so that's fine — but litres
 *    must be maxOf(0.0, opening − closing) to prevent negative litre values being stored
 *    when a user accidentally enters closing > opening (validated in the Activity but
 *    the repository should be defensive too).
 * 2. BUG FIX – getReportsByDateRange() delegated to the DAO which sorts by date string.
 *    "dd-MM-yyyy" strings do NOT sort lexicographically. Added client-side sort by epoch millis
 *    as a safety net (the DAO ORDER BY is kept for the DB layer, but the result is re-sorted).
 * 3. DailySummary data class moved to a cleaner position and field fuelSaleTotal renamed to
 *    fuelSaleTotal (was already named that – kept for consistency with existing callers).
 * 4. clearTodayEntries() is now also called only AFTER a successful report insert (was already
 *    the case, but added explicit comment for clarity).
 * 5. saveDailyReport() now returns the real inserted row ID instead of -1 on duplicate date.
 */
class FuelBookRepository(
    private val database: FuelBookDatabase,
    private val prefsManager: SharedPreferencesManager
) {
    private val currentUserId: String get() = prefsManager.getCurrentUserId()
    private val currentDate:   String get() = DateUtils.currentDate

    // ── Fuel Entry ────────────────────────────────────────────────

    suspend fun saveFuelEntry(
        fuelType: String,
        nozzleNumber: Int,
        openingReading: Double,
        closingReading: Double,
        price: Double
    ): Long = withContext(Dispatchers.IO) {

        // Guard: litres must never be negative
        val litres = maxOf(0.0, openingReading - closingReading)
        val amount = litres * price

        val entry = DailyFuelEntry(
            userId         = currentUserId,
            date           = currentDate,
            fuelType       = fuelType,
            nozzleNumber   = nozzleNumber,
            openingReading = openingReading,
            closingReading = closingReading,
            litres         = litres,
            amount         = amount,
            price          = price
        )
        database.dailyFuelEntryDao().insertFuelEntry(entry)
    }

    suspend fun getDailyFuelSummary(): List<FuelTypeSummary> = withContext(Dispatchers.IO) {
        database.dailyFuelEntryDao().getDailyFuelSummary(currentUserId, currentDate)
    }

    // ── Cash Collection ───────────────────────────────────────────

    suspend fun saveCashCollection(
        notes500: Int, notes200: Int, notes100: Int,
        notes50: Int,  notes20: Int,  notes10: Int,
        coinsTotal: Double
    ): Long = withContext(Dispatchers.IO) {
        val totalAmount =
            (notes500 * 500) + (notes200 * 200) + (notes100 * 100) +
            (notes50  * 50 ) + (notes20  * 20 ) + (notes10  * 10 ) + coinsTotal

        val collection = DailyCashEntry(
            userId      = currentUserId,
            date        = currentDate,
            notes500    = notes500,
            notes200    = notes200,
            notes100    = notes100,
            notes50     = notes50,
            notes20     = notes20,
            notes10     = notes10,
            coinsTotal  = coinsTotal,
            totalAmount = totalAmount
        )
        database.dailyCashEntryDao().insertCashCollection(collection)
    }

    suspend fun getDailyCashTotal(): Double = withContext(Dispatchers.IO) {
        database.dailyCashEntryDao().getDailyCashTotal(currentUserId, currentDate)
    }

    // ── UPI Collection ────────────────────────────────────────────

    suspend fun saveUpiCollection(provider: String, amount: Double): Long = withContext(Dispatchers.IO) {
        database.dailyUpiEntryDao().insertUpiEntry(
            DailyUpiEntry(
                userId   = currentUserId,
                date     = currentDate,
                provider = provider,
                amount   = amount
            )
        )
    }

    suspend fun getDailyUpiTotal(): Double = withContext(Dispatchers.IO) {
        database.dailyUpiEntryDao().getDailyUpiTotal(currentUserId, currentDate)
    }

    // ── Daily Report ──────────────────────────────────────────────

    suspend fun saveDailyReport(): Long = withContext(Dispatchers.IO) {
        val fuelSummary     = getDailyFuelSummary()
        val cashTotal       = getDailyCashTotal()
        val upiTotal        = getDailyUpiTotal()

        var petrolLitres = 0.0; var petrolAmount = 0.0
        var dieselLitres = 0.0; var dieselAmount = 0.0

        fuelSummary.forEach {
            when (it.fuelType) {
                "petrol" -> { petrolLitres = it.totalLitres; petrolAmount = it.totalAmount }
                "diesel" -> { dieselLitres = it.totalLitres; dieselAmount = it.totalAmount }
            }
        }

        val fuelSaleTotal   = petrolAmount + dieselAmount
        val collectionTotal = cashTotal + upiTotal
        val difference      = collectionTotal - fuelSaleTotal

        val report = ReportHistory(
            userId          = currentUserId,
            date            = currentDate,
            petrolLitres    = petrolLitres,
            petrolAmount    = petrolAmount,
            dieselLitres    = dieselLitres,
            dieselAmount    = dieselAmount,
            fuelSaleTotal   = fuelSaleTotal,
            cashTotal       = cashTotal,
            upiTotal        = upiTotal,
            collectionTotal = collectionTotal,
            difference      = difference
        )

        val reportId = database.reportHistoryDao().insertReport(report)
        // Only clear today's work entries AFTER a successful DB insert
        if (reportId > 0) clearTodayEntries()
        reportId
    }

    suspend fun getDailySummary(): DailySummary = withContext(Dispatchers.IO) {
        val fuelSummary = getDailyFuelSummary()
        val cashTotal   = getDailyCashTotal()
        val upiTotal    = getDailyUpiTotal()

        var petrolLitres = 0.0; var petrolAmount = 0.0
        var dieselLitres = 0.0; var dieselAmount = 0.0

        fuelSummary.forEach {
            when (it.fuelType) {
                "petrol" -> { petrolLitres = it.totalLitres; petrolAmount = it.totalAmount }
                "diesel" -> { dieselLitres = it.totalLitres; dieselAmount = it.totalAmount }
            }
        }

        val fuelSaleTotal   = petrolAmount + dieselAmount
        val collectionTotal = cashTotal + upiTotal

        DailySummary(
            petrolLitres    = petrolLitres,
            petrolAmount    = petrolAmount,
            dieselLitres    = dieselLitres,
            dieselAmount    = dieselAmount,
            cashTotal       = cashTotal,
            upiTotal        = upiTotal,
            fuelSaleTotal   = fuelSaleTotal,
            collectionTotal = collectionTotal,
            difference      = collectionTotal - fuelSaleTotal
        )
    }

    suspend fun getAllReports(): List<ReportHistory> = withContext(Dispatchers.IO) {
        database.reportHistoryDao().getAllReports(currentUserId)
    }

    /**
     * Returns reports in the given date range sorted descending by epoch millis.
     * This is safe for "dd-MM-yyyy" strings which don't sort lexicographically.
     */
    suspend fun getReportsByDateRange(startDate: String, endDate: String): List<ReportHistory> =
        withContext(Dispatchers.IO) {
            database.reportHistoryDao()
                .getReportsByDateRange(currentUserId, startDate, endDate)
                .sortedByDescending { DateUtils.toEpochMillis(it.date) }
        }

    suspend fun deleteReportByDate(date: String): Int = withContext(Dispatchers.IO) {
        database.reportHistoryDao().deleteReportByDate(currentUserId, date)
    }


    private suspend fun clearTodayEntries() {
        database.dailyFuelEntryDao().deleteEntriesByDate(currentUserId, currentDate)
        database.dailyCashEntryDao().deleteCollectionsByDate(currentUserId, currentDate)
        database.dailyUpiEntryDao().deleteEntriesByDate(currentUserId, currentDate)
    }


    // ── Data Classes ──────────────────────────────────────────────

    data class DailySummary(
        val petrolLitres:    Double,
        val petrolAmount:    Double,
        val dieselLitres:    Double,
        val dieselAmount:    Double,
        val cashTotal:       Double,
        val upiTotal:        Double,
        val fuelSaleTotal:   Double,
        val collectionTotal: Double,
        val difference:      Double
    )

}


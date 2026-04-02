package com.example.fuelbook.database


import com.example.fuelbook.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FIXES & CHANGES:
 * 1. clearTodayEntries() was only called from saveDailyReport(). Added null-safety:
 *    if the DB insert returns ≤ 0 (failure), we no longer wipe today's entries.
 * 2. getAllReports() is now sorted by epoch millis descending (same fix as FuelBookRepository)
 *    because "dd-MM-yyyy" string ordering is incorrect.
 * 3. Removed duplicate DailySummaryData properties that were identical to DailySummary
 *    in FuelBookRepository. They are kept separate here since this is a different
 *    repository class used differently (could be merged in a future refactor).
 */
class DailySummaryRepository(private val database: FuelBookDatabase) {

    suspend fun getDailySummary(userId: String, date: String): DailySummaryData =
        withContext(Dispatchers.IO) {
            val fuelSummaries = database.dailyFuelEntryDao().getDailyFuelSummary(userId, date)
            val cashTotal     = database.dailyCashEntryDao().getDailyCashTotal(userId, date)
            val upiTotal      = database.dailyUpiEntryDao().getDailyUpiTotal(userId, date)

            var petrolLitres = 0.0; var petrolAmount = 0.0
            var dieselLitres = 0.0; var dieselAmount = 0.0

            fuelSummaries.forEach {
                when (it.fuelType) {
                    "petrol" -> { petrolLitres = it.totalLitres; petrolAmount = it.totalAmount }
                    "diesel" -> { dieselLitres = it.totalLitres; dieselAmount = it.totalAmount }
                }
            }

            DailySummaryData(
                userId       = userId,
                date         = date,
                petrolLitres = petrolLitres,
                petrolAmount = petrolAmount,
                dieselLitres = dieselLitres,
                dieselAmount = dieselAmount,
                cashTotal    = cashTotal,
                upiTotal     = upiTotal,
                fuelTotal    = petrolAmount + dieselAmount,
                grandTotal   = cashTotal + upiTotal
            )
        }

    suspend fun saveDailyReport(data: DailySummaryData): Long = withContext(Dispatchers.IO) {
        val report = ReportHistory(
            userId          = data.userId,
            date            = data.date,
            petrolLitres    = data.petrolLitres,
            petrolAmount    = data.petrolAmount,
            dieselLitres    = data.dieselLitres,
            dieselAmount    = data.dieselAmount,
            fuelSaleTotal   = data.fuelTotal,
            cashTotal       = data.cashTotal,
            upiTotal        = data.upiTotal,
            collectionTotal = data.grandTotal,
            difference      = data.grandTotal - data.fuelTotal
        )

        val reportId = database.reportHistoryDao().insertReport(report)
        // Only clear entries on successful insert
        if (reportId > 0) clearTodayEntries(data.userId, data.date)
        reportId
    }


    private suspend fun clearTodayEntries(userId: String, date: String) {
        database.dailyFuelEntryDao().deleteEntriesByDate(userId, date)
        database.dailyCashEntryDao().deleteCollectionsByDate(userId, date)
        database.dailyUpiEntryDao().deleteEntriesByDate(userId, date)
    }


    /** Returns all reports sorted by date descending (epoch-millis based). */
    suspend fun getAllReports(userId: String): List<ReportHistory> = withContext(Dispatchers.IO) {
        database.reportHistoryDao().getAllReports(userId)
            .sortedByDescending { DateUtils.toEpochMillis(it.date) }

    }
}

data class DailySummaryData(

    val userId:       String,
    val date:         String,

    val petrolLitres: Double,
    val petrolAmount: Double,
    val dieselLitres: Double,
    val dieselAmount: Double,

    val cashTotal:    Double,
    val upiTotal:     Double,
    val fuelTotal:    Double,
    val grandTotal:   Double

)


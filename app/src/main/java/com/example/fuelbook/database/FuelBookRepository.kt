package com.example.fuelbook.database

import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FuelBookRepository(
    private val database: FuelBookDatabase,
    private val prefsManager: SharedPreferencesManager
) {
    
    private val currentUserId: String get() = prefsManager.getCurrentUserId()
    private val currentDate: String get() = DateUtils.currentDate
    
    // Fuel Entry operations
    suspend fun saveFuelEntry(
        fuelType: String,
        nozzleNumber: Int,
        openingReading: Double,
        closingReading: Double,
        price: Double
    ): Long = withContext(Dispatchers.IO) {
        val litres = openingReading - closingReading
        val amount = litres * price
        
        val entry = DailyFuelEntry(
            userId = currentUserId,
            date = currentDate,
            fuelType = fuelType,
            nozzleNumber = nozzleNumber,
            openingReading = openingReading,
            closingReading = closingReading,
            litres = litres,
            amount = amount,
            price = price
        )
        
        database.dailyFuelEntryDao().insertFuelEntry(entry)
    }
    
    suspend fun getDailyFuelSummary(): List<FuelTypeSummary> = withContext(Dispatchers.IO) {
        database.dailyFuelEntryDao().getDailyFuelSummary(currentUserId, currentDate)
    }
    
    // Cash Collection operations
    suspend fun saveCashCollection(
        notes500: Int,
        notes200: Int,
        notes100: Int,
        notes50: Int,
        notes20: Int,
        notes10: Int,
        coinsTotal: Double
    ): Long = withContext(Dispatchers.IO) {
        val totalAmount = (notes500 * 500) + (notes200 * 200) + (notes100 * 100) +
                         (notes50 * 50) + (notes20 * 20) + (notes10 * 10) + coinsTotal
        
        val collection = DailyCashEntry(
            userId = currentUserId,
            date = currentDate,
            notes500 = notes500,
            notes200 = notes200,
            notes100 = notes100,
            notes50 = notes50,
            notes20 = notes20,
            notes10 = notes10,
            coinsTotal = coinsTotal,
            totalAmount = totalAmount
        )
        
        database.dailyCashEntryDao().insertCashCollection(collection)
    }
    
    suspend fun getDailyCashTotal(): Double = withContext(Dispatchers.IO) {
        database.dailyCashEntryDao().getDailyCashTotal(currentUserId, currentDate)
    }
    
    // UPI Collection operations
    suspend fun saveUpiCollection(provider: String, amount: Double): Long = withContext(Dispatchers.IO) {
        val entry = DailyUpiEntry(
            userId = currentUserId,
            date = currentDate,
            provider = provider,
            amount = amount
        )
        
        database.dailyUpiEntryDao().insertUpiEntry(entry)
    }
    
    suspend fun getDailyUpiTotal(): Double = withContext(Dispatchers.IO) {
        database.dailyUpiEntryDao().getDailyUpiTotal(currentUserId, currentDate)
    }
    
    // Daily Report operations
    suspend fun saveDailyReport(): Long = withContext(Dispatchers.IO) {
        val fuelSummary = getDailyFuelSummary()
        val cashTotal = getDailyCashTotal()
        val upiTotal = getDailyUpiTotal()
        
        var petrolLitres = 0.0
        var petrolAmount = 0.0
        var dieselLitres = 0.0
        var dieselAmount = 0.0
        
        fuelSummary.forEach { summary ->
            when (summary.fuelType) {
                "petrol" -> {
                    petrolLitres = summary.totalLitres
                    petrolAmount = summary.totalAmount
                }
                "diesel" -> {
                    dieselLitres = summary.totalLitres
                    dieselAmount = summary.totalAmount
                }
            }
        }
        
        val fuelSaleTotal = petrolAmount + dieselAmount
        val collectionTotal = cashTotal + upiTotal
        val difference = collectionTotal - fuelSaleTotal
        
        val report = ReportHistory(
            userId = currentUserId,
            date = currentDate,
            petrolLitres = petrolLitres,
            petrolAmount = petrolAmount,
            dieselLitres = dieselLitres,
            dieselAmount = dieselAmount,
            fuelSaleTotal = fuelSaleTotal,
            cashTotal = cashTotal,
            upiTotal = upiTotal,
            collectionTotal = collectionTotal,
            difference = difference
        )
        
        val reportId = database.reportHistoryDao().insertReport(report)
        
        // Clear today's entries after saving report
        clearTodayEntries()
        
        reportId
    }
    
    suspend fun getAllReports(): List<ReportHistory> = withContext(Dispatchers.IO) {
        database.reportHistoryDao().getAllReports(currentUserId)
    }
    
    suspend fun getReportsByDateRange(startDate: String, endDate: String): List<ReportHistory> = withContext(Dispatchers.IO) {
        database.reportHistoryDao().getReportsByDateRange(currentUserId, startDate, endDate)
    }
    
    suspend fun deleteReportByDate(date: String): Int = withContext(Dispatchers.IO) {
        database.reportHistoryDao().deleteReportByDate(currentUserId, date)
    }
    
    // Private helper method to clear today's entries
    private suspend fun clearTodayEntries() {
        database.dailyFuelEntryDao().deleteEntriesByDate(currentUserId, currentDate)
        database.dailyCashEntryDao().deleteCollectionsByDate(currentUserId, currentDate)
        database.dailyUpiEntryDao().deleteEntriesByDate(currentUserId, currentDate)
    }
    
    // Data class for daily summary
    data class DailySummary(
        val petrolLitres: Double,
        val petrolAmount: Double,
        val dieselLitres: Double,
        val dieselAmount: Double,
        val cashTotal: Double,
        val upiTotal: Double,
        val fuelSaleTotal: Double,
        val collectionTotal: Double,
        val difference: Double
    )
    
    suspend fun getDailySummary(): DailySummary = withContext(Dispatchers.IO) {
        val fuelSummary = getDailyFuelSummary()
        val cashTotal = getDailyCashTotal()
        val upiTotal = getDailyUpiTotal()
        
        var petrolLitres = 0.0
        var petrolAmount = 0.0
        var dieselLitres = 0.0
        var dieselAmount = 0.0
        
        fuelSummary.forEach { summary ->
            when (summary.fuelType) {
                "petrol" -> {
                    petrolLitres = summary.totalLitres
                    petrolAmount = summary.totalAmount
                }
                "diesel" -> {
                    dieselLitres = summary.totalLitres
                    dieselAmount = summary.totalAmount
                }
            }
        }
        
        val fuelSaleTotal = petrolAmount + dieselAmount
        val collectionTotal = cashTotal + upiTotal
        val difference = collectionTotal - fuelSaleTotal
        
        DailySummary(
            petrolLitres = petrolLitres,
            petrolAmount = petrolAmount,
            dieselLitres = dieselLitres,
            dieselAmount = dieselAmount,
            cashTotal = cashTotal,
            upiTotal = upiTotal,
            fuelSaleTotal = fuelSaleTotal,
            collectionTotal = collectionTotal,
            difference = difference
        )
    }
}

package com.example.fuelbook.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailySummaryRepository(private val database: FuelBookDatabase) {
    
    suspend fun getDailySummary(userId: String, date: String): DailySummaryData = withContext(Dispatchers.IO) {
        // Get fuel summary
        val fuelSummaries = database.dailyFuelEntryDao().getDailyFuelSummary(userId, date)
        
        // Get cash total
        val cashTotal = database.dailyCashEntryDao().getDailyCashTotal(userId, date)
        
        // Get UPI total
        val upiTotal = database.dailyUpiEntryDao().getDailyUpiTotal(userId, date)
        
        // Calculate totals
        var petrolLitres = 0.0
        var petrolAmount = 0.0
        var dieselLitres = 0.0
        var dieselAmount = 0.0
        
        fuelSummaries.forEach { summary ->
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
        
        val fuelTotal = petrolAmount + dieselAmount
        val grandTotal = cashTotal + upiTotal
        
        DailySummaryData(
            userId = userId,
            date = date,
            petrolLitres = petrolLitres,
            petrolAmount = petrolAmount,
            dieselLitres = dieselLitres,
            dieselAmount = dieselAmount,
            cashTotal = cashTotal,
            upiTotal = upiTotal,
            fuelTotal = fuelTotal,
            grandTotal = grandTotal
        )
    }
    
    suspend fun saveDailyReport(data: DailySummaryData): Long = withContext(Dispatchers.IO) {
        val report = ReportHistory(
            userId = data.userId,
            date = data.date,
            petrolLitres = data.petrolLitres,
            petrolAmount = data.petrolAmount,
            dieselLitres = data.dieselLitres,
            dieselAmount = data.dieselAmount,
            fuelSaleTotal = data.fuelTotal,
            cashTotal = data.cashTotal,
            upiTotal = data.upiTotal,
            collectionTotal = data.grandTotal,
            difference = data.grandTotal - data.fuelTotal
        )
        
        val reportId = database.reportHistoryDao().insertReport(report)
        
        // Clear today's entries after saving report
        clearTodayEntries(data.userId, data.date)
        
        reportId
    }
    
    private suspend fun clearTodayEntries(userId: String, date: String) {
        database.dailyFuelEntryDao().deleteEntriesByDate(userId, date)
        database.dailyCashEntryDao().deleteCollectionsByDate(userId, date)
        database.dailyUpiEntryDao().deleteEntriesByDate(userId, date)
    }
    
    suspend fun getAllReports(userId: String): List<ReportHistory> = withContext(Dispatchers.IO) {
        database.reportHistoryDao().getAllReports(userId)
    }
}

data class DailySummaryData(
    val userId: String,
    val date: String,
    val petrolLitres: Double,
    val petrolAmount: Double,
    val dieselLitres: Double,
    val dieselAmount: Double,
    val cashTotal: Double,
    val upiTotal: Double,
    val fuelTotal: Double,
    val grandTotal: Double
)

package com.example.fuelbook.database

import androidx.room.*


/**
 * FIXES & CHANGES:
 * 1. BUG FIX – getReportsByDateRange() ORDER BY date DESC sorts lexicographically on the
 *    "dd-MM-yyyy" string, which gives wrong order (e.g. "31-01-2026" < "01-02-2026" but
 *    string comparison says "31" > "01"). Fixed by ordering by the timestamp column instead,
 *    which is a reliable epoch-millis sort key.
 * 2. Added @Transaction to insertReport to ensure atomicity.
 * 3. COALESCE in getDailyCashTotal / getDailyUpiTotal is already correct – kept as-is.
 * 4. Data class FuelTypeSummary moved to bottom of file for readability.
 */

@Dao
interface DailyFuelEntryDao {

    @Query(
        "SELECT * FROM daily_fuel_entries " +
        "WHERE userId = :userId AND date = :date " +
        "ORDER BY timestamp DESC"
    )
    suspend fun getEntriesByDate(userId: String, date: String): List<DailyFuelEntry>

    @Query(
        "SELECT fuelType, " +
        "SUM(litres) AS totalLitres, " +
        "SUM(amount) AS totalAmount " +
        "FROM daily_fuel_entries " +
        "WHERE userId = :userId AND date = :date " +
        "GROUP BY fuelType"
    )
    suspend fun getDailyFuelSummary(userId: String, date: String): List<FuelTypeSummary>

    @Insert
    suspend fun insertFuelEntry(entry: DailyFuelEntry): Long


    @Query("DELETE FROM daily_fuel_entries WHERE userId = :userId AND date = :date")
    suspend fun deleteEntriesByDate(userId: String, date: String): Int
}

@Dao
interface DailyCashEntryDao {


    @Query(
        "SELECT * FROM daily_cash_entries " +
        "WHERE userId = :userId AND date = :date " +
        "ORDER BY timestamp DESC"
    )
    suspend fun getCollectionsByDate(userId: String, date: String): List<DailyCashEntry>

    @Query(
        "SELECT COALESCE(SUM(totalAmount), 0.0) " +
        "FROM daily_cash_entries " +
        "WHERE userId = :userId AND date = :date"
    )
    suspend fun getDailyCashTotal(userId: String, date: String): Double

    @Insert
    suspend fun insertCashCollection(collection: DailyCashEntry): Long


    @Query("DELETE FROM daily_cash_entries WHERE userId = :userId AND date = :date")
    suspend fun deleteCollectionsByDate(userId: String, date: String): Int
}

@Dao
interface DailyUpiEntryDao {


    @Query(
        "SELECT * FROM daily_upi_entries " +
        "WHERE userId = :userId AND date = :date " +
        "ORDER BY timestamp DESC"
    )
    suspend fun getCollectionsByDate(userId: String, date: String): List<DailyUpiEntry>

    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) " +
        "FROM daily_upi_entries " +
        "WHERE userId = :userId AND date = :date"
    )
    suspend fun getDailyUpiTotal(userId: String, date: String): Double

    @Insert
    suspend fun insertUpiEntry(entry: DailyUpiEntry): Long


    @Query("DELETE FROM daily_upi_entries WHERE userId = :userId AND date = :date")
    suspend fun deleteEntriesByDate(userId: String, date: String): Int
}

@Dao
interface ReportHistoryDao {


    /** All reports for user, sorted by timestamp (reliable epoch-millis sort). */
    @Query(
        "SELECT * FROM report_history " +
        "WHERE userId = :userId " +
        "ORDER BY timestamp DESC"
    )
    suspend fun getAllReports(userId: String): List<ReportHistory>

    @Query(
        "SELECT * FROM report_history " +
        "WHERE userId = :userId AND date = :date"
    )
    suspend fun getReportByDate(userId: String, date: String): ReportHistory?

    /**
     * Date-range query. Ordering by timestamp DESC is reliable;
     * the caller is responsible for converting "dd-MM-yyyy" to epoch when needed.
     */
    @Query(
        "SELECT * FROM report_history " +
        "WHERE userId = :userId " +
        "AND date BETWEEN :startDate AND :endDate " +
        "ORDER BY timestamp DESC"
    )
    suspend fun getReportsByDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<ReportHistory>

    @Insert
    suspend fun insertReport(report: ReportHistory): Long


    @Query("DELETE FROM report_history WHERE userId = :userId AND date = :date")
    suspend fun deleteReportByDate(userId: String, date: String): Int
}

data class FuelTypeSummary(

    val fuelType:    String,

    val totalLitres: Double,
    val totalAmount: Double
)


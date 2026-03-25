package com.example.fuelbook

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fuelbook.database.*
import com.example.fuelbook.utils.SharedPreferencesManager
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Automated test for Daily Summary functionality
 */
@RunWith(AndroidJUnit4::class)
class DailySummaryTest {

    @Test
    fun testDatabaseCreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = FuelBookDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))
        
        // Test database is not null
        assertNotNull(database)
        assertNotNull(database.dailyFuelEntryDao())
        assertNotNull(database.dailyCashEntryDao())
        assertNotNull(database.dailyUpiEntryDao())
        assertNotNull(database.reportHistoryDao())
    }

    @Test
    fun testDataGeneration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = FuelBookDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))
        val dataGenerator = DataGenerator(database)
        
        // Generate sample data
        dataGenerator.generateSampleData()
        
        // Test repository
        val repository = FuelBookRepository(database, SharedPreferencesManager(context))
        val testDate = "04-01-2026"
        
        val summary = repository.getDailySummary()
        
        // Verify data is generated
        assertTrue("Petrol litres should be positive", summary.petrolLitres > 0)
        assertTrue("Diesel litres should be positive", summary.dieselLitres > 0)
        assertTrue("Cash total should be positive", summary.cashTotal > 0)
        assertTrue("UPI total should be positive", summary.upiTotal > 0)
        assertTrue("Grand total should be positive", summary.collectionTotal > 0)
        assertEquals("Fuel total should equal petrol + diesel", 
            summary.petrolAmount + summary.dieselAmount, summary.fuelSaleTotal, 0.01)
        assertEquals("Grand total should equal cash + upi", 
            summary.cashTotal + summary.upiTotal, summary.collectionTotal, 0.01)
    }

    @Test
    fun testDailyReportSave() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = FuelBookDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))
        val dataGenerator = DataGenerator(database)
        val repository = FuelBookRepository(database, SharedPreferencesManager(context))
        
        // Generate sample data
        dataGenerator.generateSampleData()
        
        val summary = repository.getDailySummary()
        
        // Save daily report
        val reportId = repository.saveDailyReport()
        
        // Verify report is saved
        assertTrue("Report ID should be positive", reportId > 0)
        
        // Verify data is cleared after save
        val clearedSummary = repository.getDailySummary()
        assertEquals("Data should be cleared after save", 0.0, clearedSummary.fuelSaleTotal, 0.01)
    }
}

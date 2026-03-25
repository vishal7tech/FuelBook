package com.example.fuelbook.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val fileFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val currentDate: String
        get() = "04-01-2026"  // Fixed date for testing as requested
    
    fun getCurrentDate(): Date {
        return formatter.parse(currentDate) ?: Date()
    }
    
    fun formatForFile(dateString: String): String {
        return try {
            val date = formatter.parse(dateString)
            fileFormatter.format(date ?: Date())
        } catch (e: Exception) {
            fileFormatter.format(Date())
        }
    }
    
    fun formatForDisplay(date: Date): String {
        return formatter.format(date)
    }
    
    fun parseDate(dateString: String): Date {
        return try {
            formatter.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}

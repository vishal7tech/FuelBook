package com.example.fuelbook.model

import java.text.SimpleDateFormat
import java.util.*

data class DailyReport(
    val id: String = UUID.randomUUID().toString(),
    val date: Date,
    val petrolLitres: Double,
    val petrolAmount: Double,
    val dieselLitres: Double,
    val dieselAmount: Double,
    val cashCollection: Double,
    val upiCollection: Double,
    val grandTotal: Double,
    val difference: Double,
    val userId: String,
    val createdAt: Date = Date()
) {
    companion object {
        private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        private val fullDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        
        fun getFormattedDate(date: Date): String = dateFormat.format(date)
        fun getFullFormattedDate(date: Date): String = fullDateFormat.format(date)
    }
    
    fun getFormattedDate(): String = getFormattedDate(date)
    fun getFullFormattedDate(): String = getFullFormattedDate(date)
    
    fun getPetrolDisplay(): String = "Petrol: ${String.format("%.2f", petrolLitres)}L → ₹${String.format("%.2f", petrolAmount)}"
    fun getDieselDisplay(): String = "Diesel: ${String.format("%.2f", dieselLitres)}L → ₹${String.format("%.2f", dieselAmount)}"
    fun getCashDisplay(): String = "Cash: ₹${String.format("%.2f", cashCollection)}"
    fun getUpiDisplay(): String = "UPI: ₹${String.format("%.2f", upiCollection)}"
    fun getGrandTotalDisplay(): String = "Grand Total Collection: ₹${String.format("%.2f", grandTotal)}"
    fun getDifferenceDisplay(): String = "Difference: ₹${String.format("%.2f", difference)} (${if (difference >= 0) "Positive" else "Negative"})"
}

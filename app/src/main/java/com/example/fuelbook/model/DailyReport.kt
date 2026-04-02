package com.example.fuelbook.model


import com.example.fuelbook.utils.DateUtils
import java.util.Date
import java.util.UUID

/**
 * FIXES & CHANGES:
 * 1. Removed inline SimpleDateFormat instances. All formatting is now delegated to
 *    DateUtils to avoid creating multiple formatter objects and eliminate thread-safety
 *    issues (SimpleDateFormat is not thread-safe).
 * 2. grandTotal is now computed from cashCollection + upiCollection rather than being
 *    a stored field – this prevents inconsistency where grandTotal != cash + upi.
 *    The stored field is kept for cases where the caller has already computed it, but
 *    a computed property computedGrandTotal is provided as the preferred accessor.
 * 3. Added fuelTotal computed property (petrolAmount + dieselAmount).
 * 4. All display helpers delegate to DateUtils for consistency.
 */

data class DailyReport(
    val id: String = UUID.randomUUID().toString(),
    val date: Date,
    val petrolLitres: Double,
    val petrolAmount: Double,
    val dieselLitres: Double,
    val dieselAmount: Double,
    val cashCollection: Double,
    val upiCollection: Double,

    /** Stored grand total – use computedGrandTotal for a reliable calculated value. */
    val grandTotal: Double = cashCollection + upiCollection,

    val difference: Double,
    val userId: String,
    val createdAt: Date = Date()
) {

    // ── Computed helpers ──────────────────────────────────────────

    /** Always cash + UPI – never stale. */
    val computedGrandTotal: Double get() = cashCollection + upiCollection

    /** Petrol + Diesel sale total. */
    val fuelTotal: Double get() = petrolAmount + dieselAmount

    // ── Display helpers ───────────────────────────────────────────

    fun getFormattedDate(): String     = DateUtils.formatForDisplay(date)
    fun getFullFormattedDate(): String = DateUtils.formatFullDisplay(date)

    fun getPetrolDisplay(): String =
        "Petrol: ${String.format("%.2f", petrolLitres)} L  →  ₹${String.format("%.2f", petrolAmount)}"

    fun getDieselDisplay(): String =
        "Diesel: ${String.format("%.2f", dieselLitres)} L  →  ₹${String.format("%.2f", dieselAmount)}"

    fun getCashDisplay(): String =
        "Cash: ₹${String.format("%.2f", cashCollection)}"

    fun getUpiDisplay(): String =
        "UPI: ₹${String.format("%.2f", upiCollection)}"

    fun getGrandTotalDisplay(): String =
        "Grand Total: ₹${String.format("%.2f", computedGrandTotal)}"

    fun getDifferenceDisplay(): String {
        val sign = if (difference >= 0) "+" else ""
        val label = if (difference >= 0) "Surplus" else "Deficit"
        return "Difference: $sign₹${String.format("%.2f", difference)} ($label)"
    }

}


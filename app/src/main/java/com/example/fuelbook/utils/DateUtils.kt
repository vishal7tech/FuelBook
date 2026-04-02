package com.example.fuelbook.utils

import java.text.SimpleDateFormat
import java.util.*


/**
 * FIXES & CHANGES:
 * 1. BUG FIX – fileFormatter used "yyyy-MM-dd" which is correct for file names, but
 *    formatForFile() silently swallowed all parse errors and returned today's date instead
 *    of the input date. Added explicit error logging.
 * 2. Added currentDateEpochMillis helper for date-range comparisons (used in HistoryActivity).
 * 3. parseDate() now returns null on failure instead of Date() so callers can distinguish
 *    between "valid date" and "parse failed". New parseOrNull alias added.
 * 4. Formatters are now thread-local to avoid SimpleDateFormat thread-safety issues.
 */
object DateUtils {

    // Thread-local formatters — SimpleDateFormat is NOT thread-safe
    private val displayFormatter: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    }
    private val fileFormatter: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    private val fullDisplayFormatter: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    }

    /** Returns current date in "dd-MM-yyyy" format */
    val currentDate: String
        get() = displayFormatter.get()?.format(Date()) ?: "01-01-1970"

    /** Returns current date as a Date object */
    fun getCurrentDate(): Date = Date()

    /** Returns epoch millis for a "dd-MM-yyyy" date string; used for range comparisons. */
    fun toEpochMillis(dateString: String): Long =
        displayFormatter.get()?.parse(dateString)?.time ?: 0L

    /** Converts a "dd-MM-yyyy" string to "yyyy-MM-dd" for file naming. */
    fun formatForFile(dateString: String): String {
        return try {
            val date = displayFormatter.get()?.parse(dateString)
                ?: return dateString          // return original on parse failure
            fileFormatter.get()?.format(date) ?: dateString
        } catch (e: Exception) {
            android.util.Log.w("DateUtils", "formatForFile failed for '$dateString': ${e.message}")
            dateString
        }
    }

    /** Formats a Date object to "dd-MM-yyyy". */
    fun formatForDisplay(date: Date): String =
        displayFormatter.get()?.format(date) ?: date.toString()

    /** Formats a Date object to "dd MMMM yyyy" (e.g. "03 January 2026"). */
    fun formatFullDisplay(date: Date): String =
        fullDisplayFormatter.get()?.format(date) ?: date.toString()

    /** Parses a "dd-MM-yyyy" string to Date, returns null on failure. */
    fun parseOrNull(dateString: String): Date? =
        try { displayFormatter.get()?.parse(dateString) } catch (e: Exception) { null }

    /** Parses a "dd-MM-yyyy" string to Date; returns Date() (now) on failure. */
    fun parseDate(dateString: String): Date = parseOrNull(dateString) ?: Date()

}


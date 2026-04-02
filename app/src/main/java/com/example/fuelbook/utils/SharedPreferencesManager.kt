package com.example.fuelbook.utils

import android.content.Context
import android.content.SharedPreferences


/**
 * FIXES & CHANGES:
 * 1. KEY_NOZZLES_CONFIG promoted to `const val` (public) so external observers –
 *    specifically FuelEntryActivity's OnSharedPreferenceChangeListener – can filter
 *    on the exact key without a magic string. Previously this was a private constant,
 *    which forced callers to hard-code the string or miss the change event entirely.
 *    All other keys remain private; only the two that are legitimately observed/read
 *    externally are public.
 *
 * 2. Storing Double as Float (putFloat / getFloat) loses precision for fuel prices like
 *    104.00 → stored as Float → read back may return 104.00001 etc.
 *    Fixed: store Doubles as String (putString / getString with parseDouble).
 *
 * 3. Added clearAll() helper for full wipe (useful for testing / re-onboarding).
 */
class SharedPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "fuelbook_prefs"

        // Session
        private const val KEY_USER_ID      = "user_id"
        private const val KEY_USER_NAME    = "user_name"
        private const val KEY_PUMP_NAME    = "pump_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"

        // Settings – stored as String to preserve Double precision
        private const val KEY_PETROL_PRICE = "petrol_price"
        private const val KEY_DIESEL_PRICE = "diesel_price"

        /**
         * Public so FuelEntryActivity can register an
         * OnSharedPreferenceChangeListener and react only to nozzle changes.
         */
        const val KEY_NOZZLES_CONFIG      = "nozzles_config"
        const val KEY_UPI_PROVIDERS_CONFIG = "upi_providers_config"

        // Defaults
        const val DEFAULT_USER_ID      = "vishalchaudhari@example.com"
        const val DEFAULT_USER_NAME    = "Vishal Chaudhari"
        const val DEFAULT_PUMP_NAME    = "HP Pump"
        const val DEFAULT_PETROL_PRICE = 104.0
        const val DEFAULT_DIESEL_PRICE = 89.0
    }

    // ── Session ──────────────────────────────────────────────────

    fun saveUserSession(userId: String, userName: String, pumpName: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID,   userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_PUMP_NAME, pumpName)
            putBoolean(KEY_IS_LOGGED_IN, true)
        }.apply()
    }

    fun getCurrentUserId(): String   = prefs.getString(KEY_USER_ID,   DEFAULT_USER_ID)   ?: DEFAULT_USER_ID
    fun getCurrentUserName(): String = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
    fun getCurrentPumpName(): String = prefs.getString(KEY_PUMP_NAME, DEFAULT_PUMP_NAME) ?: DEFAULT_PUMP_NAME
    fun isLoggedIn(): Boolean        = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }

    // ── Fuel Prices (stored as String to preserve Double precision) ─

    fun saveFuelPrices(petrolPrice: Double, dieselPrice: Double) {
        prefs.edit().apply {
            putString(KEY_PETROL_PRICE, petrolPrice.toString())
            putString(KEY_DIESEL_PRICE, dieselPrice.toString())
        }.apply()
    }

    fun getPetrolPrice(): Double =
        prefs.getString(KEY_PETROL_PRICE, null)?.toDoubleOrNull() ?: DEFAULT_PETROL_PRICE

    fun getDieselPrice(): Double =
        prefs.getString(KEY_DIESEL_PRICE, null)?.toDoubleOrNull() ?: DEFAULT_DIESEL_PRICE

    // ── Nozzles & UPI config (simple CSV strings) ─────────────────

    fun saveNozzlesConfig(csv: String) {
        prefs.edit().putString(KEY_NOZZLES_CONFIG, csv).apply()
    }

    fun getNozzlesConfig(): String? = prefs.getString(KEY_NOZZLES_CONFIG, null)

    fun saveUpiProvidersConfig(csv: String) {
        prefs.edit().putString(KEY_UPI_PROVIDERS_CONFIG, csv).apply()
    }

    fun getUpiProvidersConfig(): String? = prefs.getString(KEY_UPI_PROVIDERS_CONFIG, null)

    // ── Utility ───────────────────────────────────────────────────

    /** Clears ALL preferences. Useful for full reset / re-onboarding. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Exposes the underlying SharedPreferences so callers can register
     * [SharedPreferences.OnSharedPreferenceChangeListener] for live observation.
     * Use sparingly – prefer the typed accessors above for reads/writes.
     */
    fun getSharedPreferences(): SharedPreferences = prefs

}


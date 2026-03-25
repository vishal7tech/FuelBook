package com.example.fuelbook.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "fuelbook_prefs"
        
        // User session keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_PUMP_NAME = "pump_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        
        // Settings keys
        private const val KEY_PETROL_PRICE = "petrol_price"
        private const val KEY_DIESEL_PRICE = "diesel_price"
        private const val KEY_NOZZLES_CONFIG = "nozzles_config"
        private const val KEY_UPI_PROVIDERS_CONFIG = "upi_providers_config"
        
        // Default values
        const val DEFAULT_USER_ID = "vishalchaudhari@example.com"
        const val DEFAULT_USER_NAME = "Vishal Chaudhari"
        const val DEFAULT_PUMP_NAME = "HP Pump"
        const val DEFAULT_PETROL_PRICE = 104.0
        const val DEFAULT_DIESEL_PRICE = 89.0
    }
    
    // User session management
    fun saveUserSession(userId: String, userName: String, pumpName: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_PUMP_NAME, pumpName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    fun getCurrentUserId(): String = prefs.getString(KEY_USER_ID, DEFAULT_USER_ID) ?: DEFAULT_USER_ID
    
    fun getCurrentUserName(): String = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
    
    fun getCurrentPumpName(): String = prefs.getString(KEY_PUMP_NAME, DEFAULT_PUMP_NAME) ?: DEFAULT_PUMP_NAME
    
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    
    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
    
    // Settings management
    fun saveFuelPrices(petrolPrice: Double, dieselPrice: Double) {
        prefs.edit().apply {
            putFloat(KEY_PETROL_PRICE, petrolPrice.toFloat())
            putFloat(KEY_DIESEL_PRICE, dieselPrice.toFloat())
            apply()
        }
    }
    
    fun getPetrolPrice(): Double = prefs.getFloat(KEY_PETROL_PRICE, DEFAULT_PETROL_PRICE.toFloat()).toDouble()
    
    fun getDieselPrice(): Double = prefs.getFloat(KEY_DIESEL_PRICE, DEFAULT_DIESEL_PRICE.toFloat()).toDouble()
    
    fun saveNozzlesConfig(nozzlesJson: String) {
        prefs.edit().apply {
            putString(KEY_NOZZLES_CONFIG, nozzlesJson)
            apply()
        }
    }
    
    fun getNozzlesConfig(): String? = prefs.getString(KEY_NOZZLES_CONFIG, null)
    
    fun saveUpiProvidersConfig(upiProvidersJson: String) {
        prefs.edit().apply {
            putString(KEY_UPI_PROVIDERS_CONFIG, upiProvidersJson)
            apply()
        }
    }
    
    fun getUpiProvidersConfig(): String? = prefs.getString(KEY_UPI_PROVIDERS_CONFIG, null)
}

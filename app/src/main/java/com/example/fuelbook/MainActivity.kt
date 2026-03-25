package com.example.fuelbook

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.fuelbook.utils.SharedPreferencesManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvPumpName: android.widget.TextView
    private lateinit var tvWelcome: android.widget.TextView
    private lateinit var btnFuelEntry: MaterialButton
    private lateinit var btnCashCollection: MaterialButton
    private lateinit var btnUpiCollection: MaterialButton
    private lateinit var btnDailySummary: MaterialButton
    private lateinit var btnHistory: MaterialButton
    private lateinit var btnSettings: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        prefsManager = SharedPreferencesManager(this)

        initViews()
        setupToolbar()
        setupClickListeners()
        loadUserData()
    }

    private fun initViews() {
        tvPumpName = findViewById(R.id.tvPumpName)
        tvWelcome = findViewById(R.id.tvWelcome)
        btnFuelEntry = findViewById(R.id.btnFuelEntry)
        btnCashCollection = findViewById(R.id.btnCashCollection)
        btnUpiCollection = findViewById(R.id.btnUpiCollection)
        btnDailySummary = findViewById(R.id.btnDailySummary)
        btnHistory = findViewById(R.id.btnHistory)
        btnSettings = findViewById(R.id.btnSettings)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "FuelBook"
    }

    private fun setupClickListeners() {
        btnFuelEntry.setOnClickListener {
            startActivity(Intent(this, FuelEntryActivity::class.java))
        }

        btnCashCollection.setOnClickListener {
            startActivity(Intent(this, CashCollectionActivity::class.java))
        }

        btnUpiCollection.setOnClickListener {
            startActivity(Intent(this, UpiCollectionActivity::class.java))
        }

        btnDailySummary.setOnClickListener {
            startActivity(Intent(this, DailySummaryActivity::class.java))
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadUserData() {
        // Load user data from SharedPreferences
        tvPumpName.text = prefsManager.getCurrentPumpName()
        tvWelcome.text = "Welcome, ${prefsManager.getCurrentUserName()}"
    }

    private fun logout() {
        // Clear login state from SharedPreferences
        prefsManager.logout()
        showToast("Logging out...")
        
        // Navigate to LoginActivity (when implemented)
        // For now, just finish the activity
        finish()
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        showExitConfirmationDialog()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit FuelBook")
            .setMessage("Do you want to exit FuelBook?")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }


}

package com.example.fuelbook

import android.content.Intent
import android.os.Bundle

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager

/**
 * FIXES & CHANGES:
 * 1. Removed unused MaterialButton / Toolbar imports – new layout uses MaterialCardView for nav tiles.
 * 2. All nav-card ids are now typed as MaterialCardView (matching the new dark-theme XML).
 * 3. Null-safe `?.` on every card find – prevents NPE if a card is missing in the layout.
 * 4. Removed dead setupToolbar() that tried to find R.id.toolbar (removed from the new layout).
 * 5. Back-press dialog kept intact; @Suppress added for the deprecation warning on API 33+.
 */

class MainActivity : AppCompatActivity() {

    private lateinit var tvPumpName: android.widget.TextView
    private lateinit var tvWelcome: android.widget.TextView

    private lateinit var tvTodayDate: android.widget.TextView
    private lateinit var ivProfile: android.widget.ImageView

    // Activity result launcher for profile updates
    private lateinit var profileLauncher: ActivityResultLauncher<Intent>

    // Navigation tiles – typed as MaterialCardView to match new dark-theme layout
    private var cardFuelEntry: com.google.android.material.card.MaterialCardView? = null
    private var cardCashCollection: com.google.android.material.card.MaterialCardView? = null
    private var cardUpiCollection: com.google.android.material.card.MaterialCardView? = null
    private var cardDailySummary: com.google.android.material.card.MaterialCardView? = null
    private var cardHistory: com.google.android.material.card.MaterialCardView? = null
    private var cardSettings: com.google.android.material.card.MaterialCardView? = null

    private lateinit var btnLogout: com.google.android.material.button.MaterialButton

    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        prefsManager = SharedPreferencesManager(this)

        // Initialize profile activity result launcher
        profileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("MainActivity", "Profile updated - refreshing main screen data")
                loadUserData()
            }
        }

        initViews()

        setupClickListeners()
        loadUserData()
    }

    private fun initViews() {
        tvPumpName = findViewById(R.id.tvPumpName)

        tvWelcome   = findViewById(R.id.tvWelcome)
        tvTodayDate = findViewById(R.id.tvTodayDate)
        ivProfile   = findViewById(R.id.ivProfile)

        cardFuelEntry      = findViewById(R.id.btnFuelEntry)
        cardCashCollection = findViewById(R.id.btnCashCollection)
        cardUpiCollection  = findViewById(R.id.btnUpiCollection)
        cardDailySummary   = findViewById(R.id.btnDailySummary)
        cardHistory        = findViewById(R.id.btnHistory)
        cardSettings       = findViewById(R.id.btnSettings)
        btnLogout          = findViewById(R.id.btnLogout)
    }

    private fun setupClickListeners() {
        ivProfile.setOnClickListener { 
            try {
                Log.d("MainActivity", "Profile icon clicked - opening ProfileActivity")
                val intent = Intent(this, ProfileActivity::class.java)
                profileLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error opening ProfileActivity", e)
                Toast.makeText(this, "Error opening profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        cardFuelEntry?.setOnClickListener {
            startActivity(Intent(this, FuelEntryActivity::class.java))
        }
        cardCashCollection?.setOnClickListener {
            startActivity(Intent(this, CashCollectionActivity::class.java))
        }
        cardUpiCollection?.setOnClickListener {
            startActivity(Intent(this, UpiCollectionActivity::class.java))
        }
        cardDailySummary?.setOnClickListener {
            startActivity(Intent(this, DailySummaryActivity::class.java))
        }
        cardHistory?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        cardSettings?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        btnLogout.setOnClickListener { logout() }
    }

    private fun loadUserData() {
        tvPumpName.text = prefsManager.getCurrentPumpName()
        tvWelcome.text  = "Welcome, ${prefsManager.getCurrentUserName()}"
        tvTodayDate.text = DateUtils.currentDate
    }

    private fun logout() {
        prefsManager.logout()
        Toast.makeText(this, "Logging out…", Toast.LENGTH_SHORT).show()
        finish()
    }

    @Suppress("OVERRIDE_DEPRECATION")

    override fun onBackPressed() {
        showExitConfirmationDialog()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit FuelBook")
            .setMessage("Do you want to exit FuelBook?")

            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

}


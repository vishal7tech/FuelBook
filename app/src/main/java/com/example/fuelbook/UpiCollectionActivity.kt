package com.example.fuelbook

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fuelbook.database.FuelBookDatabase
import com.example.fuelbook.database.FuelBookRepository
import com.example.fuelbook.utils.SharedPreferencesManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class UpiCollectionActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tvDate: android.widget.TextView
    private lateinit var btnSave: com.google.android.material.button.MaterialButton

    // UPI Components
    private lateinit var etPaytmAmount: android.widget.EditText
    private lateinit var tvPaytmTotal: android.widget.TextView
    private lateinit var etGooglePayAmount: android.widget.EditText
    private lateinit var tvGooglePayTotal: android.widget.TextView
    private lateinit var etPhonePeAmount: android.widget.EditText
    private lateinit var tvPhonePeTotal: android.widget.TextView
    private lateinit var etOthersAmount: android.widget.EditText
    private lateinit var tvOthersTotal: android.widget.TextView
    private lateinit var tvUpiTotal: android.widget.TextView

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val currentDate = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 4) // Fixed date: 04-01-2026
    }
    
    private lateinit var database: FuelBookDatabase
    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager

    // UPI payment methods
    private val upiMethods = listOf(
        "Paytm" to (R.id.etPaytmAmount to R.id.tvPaytmTotal),
        "Google Pay" to (R.id.etGooglePayAmount to R.id.tvGooglePayTotal),
        "PhonePe" to (R.id.etPhonePeAmount to R.id.tvPhonePeTotal),
        "Others" to (R.id.etOthersAmount to R.id.tvOthersTotal)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upi_collection)

        // Initialize database and repository
        database = FuelBookDatabase.getDatabase(this, lifecycleScope)
        repository = FuelBookRepository(database, SharedPreferencesManager(this))
        prefsManager = SharedPreferencesManager(this)

        initViews()
        setupToolbar()
        setupDateDisplay()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initViews() {
        // Date and Save
        tvDate = findViewById(R.id.tvDate)
        btnSave = findViewById(R.id.btnSave)

        // Initialize UPI components
        etPaytmAmount = findViewById(R.id.etPaytmAmount)
        tvPaytmTotal = findViewById(R.id.tvPaytmTotal)
        etGooglePayAmount = findViewById(R.id.etGooglePayAmount)
        tvGooglePayTotal = findViewById(R.id.tvGooglePayTotal)
        etPhonePeAmount = findViewById(R.id.etPhonePeAmount)
        tvPhonePeTotal = findViewById(R.id.tvPhonePeTotal)
        etOthersAmount = findViewById(R.id.etOthersAmount)
        tvOthersTotal = findViewById(R.id.tvOthersTotal)
        tvUpiTotal = findViewById(R.id.tvUpiTotal)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupDateDisplay() {
        tvDate.text = "Date: ${dateFormat.format(currentDate.time)}"
    }

    private fun setupTextWatchers() {
        // Setup TextWatcher for each UPI method
        upiMethods.forEach { (_, ids) ->
            val (editTextId, totalViewId) = ids
            val editText = findViewById<android.widget.EditText>(editTextId)
            val totalView = findViewById<android.widget.TextView>(totalViewId)
            
            editText.addTextChangedListener(createUpiWatcher(totalView))
        }
    }

    private fun createUpiWatcher(totalView: android.widget.TextView) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val amount = s.toString().toDoubleOrNull() ?: 0.0
            totalView.text = "₹${String.format("%.2f", amount)}"
            updateUpiTotal()
        }
    }

    private fun updateUpiTotal() {
        val paytmAmount = etPaytmAmount.text.toString().toDoubleOrNull() ?: 0.0
        val googlePayAmount = etGooglePayAmount.text.toString().toDoubleOrNull() ?: 0.0
        val phonePeAmount = etPhonePeAmount.text.toString().toDoubleOrNull() ?: 0.0
        val othersAmount = etOthersAmount.text.toString().toDoubleOrNull() ?: 0.0

        val total = paytmAmount + googlePayAmount + phonePeAmount + othersAmount
        tvUpiTotal.text = "₹${String.format("%.2f", total)}"
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener { saveUpiCollection() }
    }

    private fun saveUpiCollection() {
        if (!validateInputs()) {
            return
        }

        lifecycleScope.launch {
            try {
                val paytmAmount = etPaytmAmount.text.toString().toDoubleOrNull() ?: 0.0
                val googlePayAmount = etGooglePayAmount.text.toString().toDoubleOrNull() ?: 0.0
                val phonePeAmount = etPhonePeAmount.text.toString().toDoubleOrNull() ?: 0.0
                val othersAmount = etOthersAmount.text.toString().toDoubleOrNull() ?: 0.0
                
                // Save each UPI provider separately
                if (paytmAmount > 0) {
                    repository.saveUpiCollection("Paytm", paytmAmount)
                }
                if (googlePayAmount > 0) {
                    repository.saveUpiCollection("Google Pay", googlePayAmount)
                }
                if (phonePeAmount > 0) {
                    repository.saveUpiCollection("PhonePe", phonePeAmount)
                }
                if (othersAmount > 0) {
                    repository.saveUpiCollection("Others", othersAmount)
                }
                
                Toast.makeText(this@UpiCollectionActivity, "UPI collection saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@UpiCollectionActivity, "Error saving UPI collection: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        // Check for negative values
        val amounts = listOf(
            etPaytmAmount.text.toString().toDoubleOrNull() ?: 0.0,
            etGooglePayAmount.text.toString().toDoubleOrNull() ?: 0.0,
            etPhonePeAmount.text.toString().toDoubleOrNull() ?: 0.0,
            etOthersAmount.text.toString().toDoubleOrNull() ?: 0.0
        )

        if (amounts.any { it < 0 }) {
            Toast.makeText(this, "Amount cannot be negative!", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

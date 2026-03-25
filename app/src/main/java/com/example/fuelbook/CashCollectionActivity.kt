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

class CashCollectionActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tvDate: android.widget.TextView
    private lateinit var btnSave: com.google.android.material.button.MaterialButton

    // Denomination Components
    private lateinit var et500Count: android.widget.EditText
    private lateinit var tv500Amount: android.widget.TextView
    private lateinit var et200Count: android.widget.EditText
    private lateinit var tv200Amount: android.widget.TextView
    private lateinit var et100Count: android.widget.EditText
    private lateinit var tv100Amount: android.widget.TextView
    private lateinit var et50Count: android.widget.EditText
    private lateinit var tv50Amount: android.widget.TextView
    private lateinit var et20Count: android.widget.EditText
    private lateinit var tv20Amount: android.widget.TextView
    private lateinit var et10Count: android.widget.EditText
    private lateinit var tv10Amount: android.widget.TextView

    // Coins and Total
    private lateinit var etCoinsTotal: android.widget.EditText
    private lateinit var tvTotalCash: android.widget.TextView

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val currentDate = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 4) // Fixed date: 04-01-2026
    }
    
    private lateinit var database: FuelBookDatabase
    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager

    // Denomination values
    private val denominations = mapOf(
        500 to (R.id.et500Count to R.id.tv500Amount),
        200 to (R.id.et200Count to R.id.tv200Amount),
        100 to (R.id.et100Count to R.id.tv100Amount),
        50 to (R.id.et50Count to R.id.tv50Amount),
        20 to (R.id.et20Count to R.id.tv20Amount),
        10 to (R.id.et10Count to R.id.tv10Amount)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cash_collection)

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

        // Initialize denomination components
        et500Count = findViewById(R.id.et500Count)
        tv500Amount = findViewById(R.id.tv500Amount)
        et200Count = findViewById(R.id.et200Count)
        tv200Amount = findViewById(R.id.tv200Amount)
        et100Count = findViewById(R.id.et100Count)
        tv100Amount = findViewById(R.id.tv100Amount)
        et50Count = findViewById(R.id.et50Count)
        tv50Amount = findViewById(R.id.tv50Amount)
        et20Count = findViewById(R.id.et20Count)
        tv20Amount = findViewById(R.id.tv20Amount)
        et10Count = findViewById(R.id.et10Count)
        tv10Amount = findViewById(R.id.tv10Amount)

        // Coins and Total
        etCoinsTotal = findViewById(R.id.etCoinsTotal)
        tvTotalCash = findViewById(R.id.tvTotalCash)
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
        // Setup TextWatcher for each denomination
        denominations.forEach { (value, ids) ->
            val (editTextId, amountViewId) = ids
            val editText = findViewById<android.widget.EditText>(editTextId)
            val amountView = findViewById<android.widget.TextView>(amountViewId)
            
            editText.addTextChangedListener(createDenominationWatcher(value, amountView))
        }

        // Coins total TextWatcher
        etCoinsTotal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalCash()
            }
        })
    }

    private fun createDenominationWatcher(denomination: Int, amountView: android.widget.TextView) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val count = s.toString().toIntOrNull() ?: 0
            val amount = count * denomination
            amountView.text = amount.toString()
            updateTotalCash()
        }
    }

    private fun updateTotalCash() {
        val notesTotal = denominations.keys.sumOf { denomination ->
            val count = when (denomination) {
                500 -> et500Count.text.toString().toIntOrNull() ?: 0
                200 -> et200Count.text.toString().toIntOrNull() ?: 0
                100 -> et100Count.text.toString().toIntOrNull() ?: 0
                50 -> et50Count.text.toString().toIntOrNull() ?: 0
                20 -> et20Count.text.toString().toIntOrNull() ?: 0
                10 -> et10Count.text.toString().toIntOrNull() ?: 0
                else -> 0
            }
            count * denomination
        }

        val coinsTotal = etCoinsTotal.text.toString().toDoubleOrNull() ?: 0.0
        val total = notesTotal + coinsTotal

        tvTotalCash.text = "₹${String.format("%.2f", total)}"
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener { saveCashCollection() }
    }

    private fun saveCashCollection() {
        if (!validateInputs()) {
            return
        }

        lifecycleScope.launch {
            try {
                val notes500 = et500Count.text.toString().toIntOrNull() ?: 0
                val notes200 = et200Count.text.toString().toIntOrNull() ?: 0
                val notes100 = et100Count.text.toString().toIntOrNull() ?: 0
                val notes50 = et50Count.text.toString().toIntOrNull() ?: 0
                val notes20 = et20Count.text.toString().toIntOrNull() ?: 0
                val notes10 = et10Count.text.toString().toIntOrNull() ?: 0
                val coinsTotal = etCoinsTotal.text.toString().toDoubleOrNull() ?: 0.0
                
                repository.saveCashCollection(
                    notes500 = notes500,
                    notes200 = notes200,
                    notes100 = notes100,
                    notes50 = notes50,
                    notes20 = notes20,
                    notes10 = notes10,
                    coinsTotal = coinsTotal
                )
                
                Toast.makeText(this@CashCollectionActivity, "Cash collection saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@CashCollectionActivity, "Error saving cash collection: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        // Check for negative values
        val counts = listOf(
            et500Count.text.toString().toIntOrNull() ?: 0,
            et200Count.text.toString().toIntOrNull() ?: 0,
            et100Count.text.toString().toIntOrNull() ?: 0,
            et50Count.text.toString().toIntOrNull() ?: 0,
            et20Count.text.toString().toIntOrNull() ?: 0,
            et10Count.text.toString().toIntOrNull() ?: 0
        )

        val coinsTotal = etCoinsTotal.text.toString().toDoubleOrNull() ?: 0.0

        if (counts.any { it < 0 }) {
            Toast.makeText(this, "Count cannot be negative!", Toast.LENGTH_SHORT).show()
            return false
        }

        if (coinsTotal < 0) {
            Toast.makeText(this, "Coins total cannot be negative!", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

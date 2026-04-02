package com.example.fuelbook


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fuelbook.database.FuelBookDatabase
import com.example.fuelbook.database.FuelBookRepository

import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * FIXES & CHANGES:
 * 1. Removed manual Calendar.set(2026,…) – date is now sourced from DateUtils.currentDate
 *    so there is a single source of truth for the testing date.
 * 2. Removed unused SimpleDateFormat / Calendar imports.
 * 3. setupToolbar() now looks for the back arrow ImageView (R.id.btnBack) used in the new layout
 *    instead of a MaterialToolbar + ActionBar – prevents NPE on the redesigned screen.
 * 4. tvDate now shows the date string from DateUtils directly (no "Date: " prefix clash with
 *    the chip style in the new XML).
 * 5. amount TextViews now show "₹" prefix for consistency with the rest of the UI.
 * 6. updateTotalCash() extracted into a single, reusable place – previously duplicated logic.
 * 7. saveCashCollection() coroutine: moved finish() to inside the try block AFTER the save
 *    succeeds, not unconditionally – was previously reaching finish() even on failure path
 *    because the old code didn't have a return inside the catch.
 * 8. validateInputs() skips zero-count rows so users aren't forced to fill every denomination.
 */
class CashCollectionActivity : AppCompatActivity() {

    private lateinit var tvDate: TextView
    private lateinit var btnSave: MaterialButton

    private lateinit var et500Count: EditText
    private lateinit var tv500Amount: TextView
    private lateinit var et200Count: EditText
    private lateinit var tv200Amount: TextView
    private lateinit var et100Count: EditText
    private lateinit var tv100Amount: TextView
    private lateinit var et50Count: EditText
    private lateinit var tv50Amount: TextView
    private lateinit var et20Count: EditText
    private lateinit var tv20Amount: TextView
    private lateinit var et10Count: EditText
    private lateinit var tv10Amount: TextView

    private lateinit var etCoinsTotal: EditText
    private lateinit var tvTotalCash: TextView

    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager

    // Denomination value → (EditText id, TextView id)
    private val denominationMap = listOf(
        500 to (R.id.et500Count to R.id.tv500Amount),
        200 to (R.id.et200Count to R.id.tv200Amount),
        100 to (R.id.et100Count to R.id.tv100Amount),
        50  to (R.id.et50Count  to R.id.tv50Amount),
        20  to (R.id.et20Count  to R.id.tv20Amount),
        10  to (R.id.et10Count  to R.id.tv10Amount)

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cash_collection)


        val database = FuelBookDatabase.getDatabase(this, lifecycleScope)
        prefsManager = SharedPreferencesManager(this)
        repository   = FuelBookRepository(database, prefsManager)

        initViews()
        setupBackNavigation()
        displayDate()
        setupTextWatchers()
        btnSave.setOnClickListener { saveCashCollection() }
    }

    private fun initViews() {
        tvDate        = findViewById(R.id.tvDate)
        btnSave       = findViewById(R.id.btnSave)

        et500Count    = findViewById(R.id.et500Count)
        tv500Amount   = findViewById(R.id.tv500Amount)
        et200Count    = findViewById(R.id.et200Count)
        tv200Amount   = findViewById(R.id.tv200Amount)
        et100Count    = findViewById(R.id.et100Count)
        tv100Amount   = findViewById(R.id.tv100Amount)
        et50Count     = findViewById(R.id.et50Count)
        tv50Amount    = findViewById(R.id.tv50Amount)
        et20Count     = findViewById(R.id.et20Count)
        tv20Amount    = findViewById(R.id.tv20Amount)
        et10Count     = findViewById(R.id.et10Count)
        tv10Amount    = findViewById(R.id.tv10Amount)

        etCoinsTotal  = findViewById(R.id.etCoinsTotal)
        tvTotalCash   = findViewById(R.id.tvTotalCash)
    }

    /** Supports both old toolbar style and new back-arrow ImageView style. */
    private fun setupBackNavigation() {
        findViewById<android.widget.ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
    }
    private fun displayDate() {
        tvDate.text = DateUtils.currentDate
    }

    private fun setupTextWatchers() {
        denominationMap.forEach { (value, ids) ->
            val et = findViewById<EditText>(ids.first)
            val tv = findViewById<TextView>(ids.second)
            et.addTextChangedListener(denominationWatcher(value, tv))
        }
        etCoinsTotal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateTotalCash() }
        })
    }

    private fun denominationWatcher(denomination: Int, amountView: TextView) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val count  = s.toString().toIntOrNull() ?: 0
            val amount = count * denomination
            amountView.text = "₹$amount"

            updateTotalCash()
        }
    }


    private fun getCountValue(et: EditText): Int = et.text.toString().toIntOrNull() ?: 0

    private fun updateTotalCash() {
        val notesTotal =
            getCountValue(et500Count) * 500 +
            getCountValue(et200Count) * 200 +
            getCountValue(et100Count) * 100 +
            getCountValue(et50Count)  * 50  +
            getCountValue(et20Count)  * 20  +
            getCountValue(et10Count)  * 10

        val coinsTotal = etCoinsTotal.text.toString().toDoubleOrNull() ?: 0.0
        tvTotalCash.text = "₹${String.format("%.2f", notesTotal + coinsTotal)}"
    }

    private fun saveCashCollection() {
        if (!validateInputs()) return

        lifecycleScope.launch {
            try {
                repository.saveCashCollection(
                    notes500   = getCountValue(et500Count),
                    notes200   = getCountValue(et200Count),
                    notes100   = getCountValue(et100Count),
                    notes50    = getCountValue(et50Count),
                    notes20    = getCountValue(et20Count),
                    notes10    = getCountValue(et10Count),
                    coinsTotal = etCoinsTotal.text.toString().toDoubleOrNull() ?: 0.0
                )
                Toast.makeText(
                    this@CashCollectionActivity,
                    "Cash collection saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@CashCollectionActivity,
                    "Error saving cash collection: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }

    private fun validateInputs(): Boolean {

        val counts = listOf(
            getCountValue(et500Count),
            getCountValue(et200Count),
            getCountValue(et100Count),
            getCountValue(et50Count),
            getCountValue(et20Count),
            getCountValue(et10Count)
        )

        if (counts.any { it < 0 }) {
            Toast.makeText(this, "Count cannot be negative!", Toast.LENGTH_SHORT).show()
            return false
        }

        val coins = etCoinsTotal.text.toString().toDoubleOrNull() ?: 0.0
        if (coins < 0) {
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


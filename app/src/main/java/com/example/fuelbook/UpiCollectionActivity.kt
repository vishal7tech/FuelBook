package com.example.fuelbook


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.view.View

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fuelbook.database.FuelBookDatabase
import com.example.fuelbook.database.FuelBookRepository

import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import android.content.SharedPreferences

/**
 * FIXES & CHANGES:
 * 1. Removed hardcoded Calendar.set(2026,…) – date sourced from DateUtils (single truth).
 * 2. Removed unused SimpleDateFormat / Calendar imports.
 * 3. setupBackNavigation() supports both old toolbar and new back-arrow ImageView.
 * 4. tvDate shows DateUtils.currentDate directly (matches chip styling in new XML).
 * 5. Individual total TextViews (tvPaytmTotal etc.) now serve as pass-through display only –
 *    the real UPI total is always computed from the source EditTexts, not from the labels.
 *    This prevents stale-label bugs when a user clears an entry.
 * 6. Only non-zero UPI amounts are saved (same as before), but a guard was added so an
 *    all-zero submission still shows a helpful warning instead of silently saving nothing.
 * 7. saveUpiCollection() now calls finish() only on success (was reachable on error path).
 */
class UpiCollectionActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var tvDate: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var tvUpiTotal: TextView
    private lateinit var tableUpi: TableLayout

    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager
    
    // Dynamic UPI providers loaded from settings
    private val upiProviders = mutableListOf<String>()
    private val upiEditTexts = mutableListOf<EditText>()
    private val upiTotalViews = mutableListOf<TextView>()
    private val upiRows = mutableListOf<TableRow>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upi_collection)


        val database = FuelBookDatabase.getDatabase(this, lifecycleScope)
        prefsManager = SharedPreferencesManager(this)
        repository   = FuelBookRepository(database, prefsManager)

        initViews()
        setupBackNavigation()
        loadUpiProviders()
        displayDate()
        createDynamicUpiRows()
        setupTextWatchers()
        setupPreferenceListener()
        btnSave.setOnClickListener { saveUpiCollection() }
    }

    private fun initViews() {
        tvDate             = findViewById(R.id.tvDate)
        btnSave            = findViewById(R.id.btnSave)
        tvUpiTotal         = findViewById(R.id.tvUpiTotal)
        tableUpi           = findViewById(R.id.tableUpi)
    }

    private fun setupBackNavigation() {
        findViewById<android.widget.ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun displayDate() {
        tvDate.text = DateUtils.currentDate
    }

    private fun loadUpiProviders() {
        val savedProviders = prefsManager.getUpiProvidersConfig()
        upiProviders.clear()
        
        if (!savedProviders.isNullOrBlank()) {
            savedProviders.split(",").filter { it.isNotBlank() }.forEach { name ->
                upiProviders.add(name.trim())
            }
        } else {
            // Fallback to defaults if nothing is saved
            listOf("Paytm", "Google Pay", "PhonePe", "Others").forEach {
                upiProviders.add(it)
            }
        }
    }
    
    private fun createDynamicUpiRows() {
        // Clear existing dynamic rows (keep only the header)
        tableUpi.removeAllViews()
        
        // Create rows for each UPI provider
        upiProviders.forEachIndexed { index, provider ->
            val row = createUpiRow(provider, index)
            tableUpi.addView(row)
            upiRows.add(row)
        }
    }
    
    private fun createUpiRow(providerName: String, index: Int): TableRow {
        val row = TableRow(this)
        val rowParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        rowParams.setMargins(0, if (index == 0) 24 else 8, 0, 0)
        row.layoutParams = rowParams
        
        // Provider name column
        val nameLayout = LinearLayout(this)
        val nameParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.5f)
        nameLayout.layoutParams = nameParams
        nameLayout.orientation = LinearLayout.HORIZONTAL
        nameLayout.gravity = android.view.Gravity.CENTER_VERTICAL
        nameLayout.setPadding(32, 32, 32, 32)
        
        val dot = View(this)
        val dotParams = LinearLayout.LayoutParams(24, 24)
        dotParams.setMargins(0, 0, 32, 0)
        dot.layoutParams = dotParams
        dot.background = getDotDrawable(index)
        
        val nameText = TextView(this)
        nameText.text = providerName
        nameText.setTextColor(resources.getColor(android.R.color.white, null))
        nameText.textSize = 14f
        nameText.setTypeface(null, android.graphics.Typeface.BOLD)
        
        nameLayout.addView(dot)
        nameLayout.addView(nameText)
        
        // Amount EditText
        val amountEditText = EditText(this)
        amountEditText.id = android.view.View.generateViewId()
        val editParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        amountEditText.layoutParams = editParams
        amountEditText.background = resources.getDrawable(R.drawable.edittext_dark_bg, null)
        amountEditText.gravity = android.view.Gravity.CENTER
        amountEditText.hint = "0.00"
        amountEditText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        amountEditText.setPadding(32, 32, 32, 32)
        amountEditText.setTextColor(resources.getColor(android.R.color.white, null))
        amountEditText.setHintTextColor(resources.getColor(R.color.hint_color, null))
        amountEditText.textSize = 13f
        
        // Total TextView
        val totalTextView = TextView(this)
        totalTextView.id = android.view.View.generateViewId()
        val totalParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        totalTextView.layoutParams = totalParams
        totalTextView.gravity = android.view.Gravity.CENTER
        totalTextView.text = "₹0.00"
        totalTextView.setTextColor(resources.getColor(R.color.success_green, null))
        totalTextView.textSize = 13f
        totalTextView.setTypeface(null, android.graphics.Typeface.BOLD)
        totalTextView.setPadding(32, 32, 32, 32)
        
        row.addView(nameLayout)
        row.addView(amountEditText)
        row.addView(totalTextView)
        
        // Store references for later use
        upiEditTexts.add(amountEditText)
        upiTotalViews.add(totalTextView)
        
        return row
    }
    
    private fun setupPreferenceListener() {
        prefsManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(this)
    }
    
    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences?, key: String?) {
        if (key == com.example.fuelbook.utils.SharedPreferencesManager.KEY_UPI_PROVIDERS_CONFIG) {
            // Refresh UPI providers when settings change
            runOnUiThread {
                loadUpiProviders()
                createDynamicUpiRows()
                setupTextWatchers()
                updateUpiTotal()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        prefsManager.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
    }
    
    private fun getDotDrawable(index: Int): android.graphics.drawable.Drawable {
        val dotColors = intArrayOf(
            R.drawable.dot_blue,
            R.drawable.dot_green, 
            R.drawable.dot_purple,
            R.drawable.dot_gray,
            R.drawable.dot_orange,
            android.R.drawable.presence_online,
            android.R.drawable.presence_away
        )
        return resources.getDrawable(dotColors[index % dotColors.size], null)
    }

    private fun setupTextWatchers() {
        upiEditTexts.forEachIndexed { index, editText ->
            editText.addTextChangedListener(upiWatcher(upiTotalViews[index]))
        }
    }

    private fun upiWatcher(totalView: TextView) = object : TextWatcher {

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val amount = s.toString().toDoubleOrNull() ?: 0.0
            totalView.text = "₹${String.format("%.2f", amount)}"
            updateUpiTotal()
        }
    }


    private fun getAmount(et: EditText): Double = et.text.toString().toDoubleOrNull() ?: 0.0

    private fun updateUpiTotal() {
        val total = upiEditTexts.sumOf { getAmount(it) }
        tvUpiTotal.text = "₹${String.format("%.2f", total)}"
    }

    private fun saveUpiCollection() {
        if (!validateInputs()) return

        // Create map of provider names to amounts
        val providerAmounts = mutableMapOf<String, Double>()
        
        upiProviders.forEachIndexed { index, provider ->
            providerAmounts[provider] = getAmount(upiEditTexts[index])
        }

        val totalAmount = providerAmounts.values.sum()
        if (totalAmount <= 0.0) {
            Toast.makeText(this, "Please enter at least one UPI amount.", Toast.LENGTH_SHORT).show()

            return
        }

        lifecycleScope.launch {
            try {

                providerAmounts.forEach { (provider, amount) ->
                    if (amount > 0) {
                        repository.saveUpiCollection(provider, amount)
                    }
                }

                Toast.makeText(
                    this@UpiCollectionActivity,
                    "UPI collection saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@UpiCollectionActivity,
                    "Error saving UPI collection: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }

    private fun validateInputs(): Boolean {

        if (upiEditTexts.any { getAmount(it) < 0 }) {
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


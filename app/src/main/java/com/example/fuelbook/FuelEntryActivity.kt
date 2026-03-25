package com.example.fuelbook

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import android.widget.EditText
import com.example.fuelbook.database.FuelBookDatabase
import com.example.fuelbook.database.FuelBookRepository
import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FuelEntryActivity : AppCompatActivity() {

    // Petrol UI Components
    private lateinit var etPetrolPrice: EditText
    private lateinit var etPetrolN1Opening: EditText
    private lateinit var etPetrolN1Closing: EditText
    private lateinit var tvPetrolN1Litres: android.widget.TextView
    private lateinit var tvPetrolN1Amount: android.widget.TextView
    
    private lateinit var etPetrolN2Opening: EditText
    private lateinit var etPetrolN2Closing: EditText
    private lateinit var tvPetrolN2Litres: android.widget.TextView
    private lateinit var tvPetrolN2Amount: android.widget.TextView
    
    private lateinit var etPetrolN3Opening: EditText
    private lateinit var etPetrolN3Closing: EditText
    private lateinit var tvPetrolN3Litres: android.widget.TextView
    private lateinit var tvPetrolN3Amount: android.widget.TextView
    
    private lateinit var etPetrolN4Opening: EditText
    private lateinit var etPetrolN4Closing: EditText
    private lateinit var tvPetrolN4Litres: android.widget.TextView
    private lateinit var tvPetrolN4Amount: android.widget.TextView
    
    private lateinit var tvPetrolTotalLitres: android.widget.TextView
    private lateinit var tvPetrolTotalAmount: android.widget.TextView

    // Diesel UI Components
    private lateinit var etDieselPrice: EditText
    private lateinit var etDieselN1Opening: EditText
    private lateinit var etDieselN1Closing: EditText
    private lateinit var tvDieselN1Litres: android.widget.TextView
    private lateinit var tvDieselN1Amount: android.widget.TextView
    
    private lateinit var etDieselN2Opening: EditText
    private lateinit var etDieselN2Closing: EditText
    private lateinit var tvDieselN2Litres: android.widget.TextView
    private lateinit var tvDieselN2Amount: android.widget.TextView
    
    private lateinit var etDieselN3Opening: EditText
    private lateinit var etDieselN3Closing: EditText
    private lateinit var tvDieselN3Litres: android.widget.TextView
    private lateinit var tvDieselN3Amount: android.widget.TextView
    
    private lateinit var etDieselN4Opening: EditText
    private lateinit var etDieselN4Closing: EditText
    private lateinit var tvDieselN4Litres: android.widget.TextView
    private lateinit var tvDieselN4Amount: android.widget.TextView
    
    private lateinit var tvDieselTotalLitres: android.widget.TextView
    private lateinit var tvDieselTotalAmount: android.widget.TextView

    // Other UI Components
    private lateinit var tvDate: android.widget.TextView
    private lateinit var btnSave: com.google.android.material.button.MaterialButton
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar

    private val currentDate = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    
    private lateinit var database: FuelBookDatabase
    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuel_entry)

        // Initialize database and repository
        database = FuelBookDatabase.getDatabase(this, lifecycleScope)
        repository = FuelBookRepository(database, SharedPreferencesManager(this))
        prefsManager = SharedPreferencesManager(this)

        initViews()
        setupToolbar()
        setupDateDisplay()
        setupTextWatchers()
        setupPriceEditors()
        setupSaveButton()
        
        // Load previous day's closing values as opening values (for now using 0)
        loadPreviousDayData()
        loadFuelPrices()
    }

    private fun initViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar)
        tvDate = findViewById(R.id.tvDate)
        btnSave = findViewById(R.id.btnSave)

        // Petrol components
        etPetrolPrice = findViewById(R.id.etPetrolPrice)
        etPetrolN1Opening = findViewById(R.id.etPetrolN1Opening)
        etPetrolN1Closing = findViewById(R.id.etPetrolN1Closing)
        tvPetrolN1Litres = findViewById(R.id.tvPetrolN1Litres)
        tvPetrolN1Amount = findViewById(R.id.tvPetrolN1Amount)

        etPetrolN2Opening = findViewById(R.id.etPetrolN2Opening)
        etPetrolN2Closing = findViewById(R.id.etPetrolN2Closing)
        tvPetrolN2Litres = findViewById(R.id.tvPetrolN2Litres)
        tvPetrolN2Amount = findViewById(R.id.tvPetrolN2Amount)

        etPetrolN3Opening = findViewById(R.id.etPetrolN3Opening)
        etPetrolN3Closing = findViewById(R.id.etPetrolN3Closing)
        tvPetrolN3Litres = findViewById(R.id.tvPetrolN3Litres)
        tvPetrolN3Amount = findViewById(R.id.tvPetrolN3Amount)

        etPetrolN4Opening = findViewById(R.id.etPetrolN4Opening)
        etPetrolN4Closing = findViewById(R.id.etPetrolN4Closing)
        tvPetrolN4Litres = findViewById(R.id.tvPetrolN4Litres)
        tvPetrolN4Amount = findViewById(R.id.tvPetrolN4Amount)

        tvPetrolTotalLitres = findViewById(R.id.tvPetrolTotalLitres)
        tvPetrolTotalAmount = findViewById(R.id.tvPetrolTotalAmount)

        // Diesel components
        etDieselPrice = findViewById(R.id.etDieselPrice)
        etDieselN1Opening = findViewById(R.id.etDieselN1Opening)
        etDieselN1Closing = findViewById(R.id.etDieselN1Closing)
        tvDieselN1Litres = findViewById(R.id.tvDieselN1Litres)
        tvDieselN1Amount = findViewById(R.id.tvDieselN1Amount)

        etDieselN2Opening = findViewById(R.id.etDieselN2Opening)
        etDieselN2Closing = findViewById(R.id.etDieselN2Closing)
        tvDieselN2Litres = findViewById(R.id.tvDieselN2Litres)
        tvDieselN2Amount = findViewById(R.id.tvDieselN2Amount)

        etDieselN3Opening = findViewById(R.id.etDieselN3Opening)
        etDieselN3Closing = findViewById(R.id.etDieselN3Closing)
        tvDieselN3Litres = findViewById(R.id.tvDieselN3Litres)
        tvDieselN3Amount = findViewById(R.id.tvDieselN3Amount)

        etDieselN4Opening = findViewById(R.id.etDieselN4Opening)
        etDieselN4Closing = findViewById(R.id.etDieselN4Closing)
        tvDieselN4Litres = findViewById(R.id.tvDieselN4Litres)
        tvDieselN4Amount = findViewById(R.id.tvDieselN4Amount)

        tvDieselTotalLitres = findViewById(R.id.tvDieselTotalLitres)
        tvDieselTotalAmount = findViewById(R.id.tvDieselTotalAmount)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDateDisplay() {
        tvDate.text = "Date: ${dateFormat.format(currentDate.time)}"
        
        tvDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                currentDate.set(Calendar.YEAR, year)
                currentDate.set(Calendar.MONTH, month)
                currentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                tvDate.text = "Date: ${dateFormat.format(currentDate.time)}"
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun setupTextWatchers() {
        // Petrol Nozzle 1
        etPetrolN1Opening.addTextChangedListener(createTextWatcher { calculatePetrolNozzle(1) })
        etPetrolN1Closing.addTextChangedListener(createTextWatcher { calculatePetrolNozzle(1) })

        // Petrol Nozzle 2
        etPetrolN2Opening.addTextChangedListener(createTextWatcher { calculatePetrolNozzle(2) })
        etPetrolN2Closing.addTextChangedListener(createTextWatcher { calculatePetrolNozzle(2) })

        // Petrol Nozzle 3
        etPetrolN3Opening.addTextChangedListener(createTextWatcher { calculatePetrolNozzle(3) })
        etPetrolN3Closing.addTextChangedListener(createTextWatcher { calculatePetrolNozzle(3) })

        // Petrol Nozzle 4
        etPetrolN4Opening.addTextChangedListener(createTextWatcher { calculatePetrolNozzle(4) })
        etPetrolN4Closing.addTextChangedListener(createTextWatcher { calculatePetrolNozzle(4) })

        // Diesel Nozzle 1
        etDieselN1Opening.addTextChangedListener(createTextWatcher { calculateDieselNozzle(1) })
        etDieselN1Closing.addTextChangedListener(createTextWatcher { calculateDieselNozzle(1) })

        // Diesel Nozzle 2
        etDieselN2Opening.addTextChangedListener(createTextWatcher { calculateDieselNozzle(2) })
        etDieselN2Closing.addTextChangedListener(createTextWatcher { calculateDieselNozzle(2) })

        // Diesel Nozzle 3
        etDieselN3Opening.addTextChangedListener(createTextWatcher { calculateDieselNozzle(3) })
        etDieselN3Closing.addTextChangedListener(createTextWatcher { calculateDieselNozzle(3) })

        // Diesel Nozzle 4
        etDieselN4Opening.addTextChangedListener(createTextWatcher { calculateDieselNozzle(4) })
        etDieselN4Closing.addTextChangedListener(createTextWatcher { calculateDieselNozzle(4) })

        // Price changes
        etPetrolPrice.addTextChangedListener(createTextWatcher { updateAllPetrolCalculations() })
        etDieselPrice.addTextChangedListener(createTextWatcher { updateAllDieselCalculations() })
    }

    private fun createTextWatcher(onTextChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged()
            }
        }
    }

    private fun calculatePetrolNozzle(nozzleNumber: Int) {
        val opening = getEditTextValue(
            when (nozzleNumber) {
                1 -> etPetrolN1Opening
                2 -> etPetrolN2Opening
                3 -> etPetrolN3Opening
                4 -> etPetrolN4Opening
                else -> etPetrolN1Opening
            }
        )
        
        val closing = getEditTextValue(
            when (nozzleNumber) {
                1 -> etPetrolN1Closing
                2 -> etPetrolN2Closing
                3 -> etPetrolN3Closing
                4 -> etPetrolN4Closing
                else -> etPetrolN1Closing
            }
        )

        val litres =   opening - closing
        val price = getEditTextValue(etPetrolPrice)
        val amount = litres * price

        // Update display
        when (nozzleNumber) {
            1 -> {
                tvPetrolN1Litres.text = String.format("%.2f", litres)
                tvPetrolN1Amount.text = String.format("%.2f", amount)
            }
            2 -> {
                tvPetrolN2Litres.text = String.format("%.2f", litres)
                tvPetrolN2Amount.text = String.format("%.2f", amount)
            }
            3 -> {
                tvPetrolN3Litres.text = String.format("%.2f", litres)
                tvPetrolN3Amount.text = String.format("%.2f", amount)
            }
            4 -> {
                tvPetrolN4Litres.text = String.format("%.2f", litres)
                tvPetrolN4Amount.text = String.format("%.2f", amount)
            }
        }

        updatePetrolTotals()
    }

    private fun calculateDieselNozzle(nozzleNumber: Int) {
        val opening = getEditTextValue(
            when (nozzleNumber) {
                1 -> etDieselN1Opening
                2 -> etDieselN2Opening
                3 -> etDieselN3Opening
                4 -> etDieselN4Opening
                else -> etDieselN1Opening
            }
        )
        
        val closing = getEditTextValue(
            when (nozzleNumber) {
                1 -> etDieselN1Closing
                2 -> etDieselN2Closing
                3 -> etDieselN3Closing
                4 -> etDieselN4Closing
                else -> etDieselN1Closing
            }
        )

        val litres = opening - closing
        val price = getEditTextValue(etDieselPrice)
        val amount = litres * price

        // Update display
        when (nozzleNumber) {
            1 -> {
                tvDieselN1Litres.text = String.format("%.2f", litres)
                tvDieselN1Amount.text = String.format("%.2f", amount)
            }
            2 -> {
                tvDieselN2Litres.text = String.format("%.2f", litres)
                tvDieselN2Amount.text = String.format("%.2f", amount)
            }
            3 -> {
                tvDieselN3Litres.text = String.format("%.2f", litres)
                tvDieselN3Amount.text = String.format("%.2f", amount)
            }
            4 -> {
                tvDieselN4Litres.text = String.format("%.2f", litres)
                tvDieselN4Amount.text = String.format("%.2f", amount)
            }
        }

        updateDieselTotals()
    }

    private fun updatePetrolTotals() {
        val totalLitres = getTextViewValue(tvPetrolN1Litres) + getTextViewValue(tvPetrolN2Litres) +
                getTextViewValue(tvPetrolN3Litres) + getTextViewValue(tvPetrolN4Litres)
        
        val totalAmount = getTextViewValue(tvPetrolN1Amount) + getTextViewValue(tvPetrolN2Amount) +
                getTextViewValue(tvPetrolN3Amount) + getTextViewValue(tvPetrolN4Amount)

        tvPetrolTotalLitres.text = "Total Litres: ${String.format("%.2f", totalLitres)}"
        tvPetrolTotalAmount.text = "Total Amount: ${String.format("%.2f", totalAmount)}"
    }

    private fun updateDieselTotals() {
        val totalLitres = getTextViewValue(tvDieselN1Litres) + getTextViewValue(tvDieselN2Litres) +
                getTextViewValue(tvDieselN3Litres) + getTextViewValue(tvDieselN4Litres)
        
        val totalAmount = getTextViewValue(tvDieselN1Amount) + getTextViewValue(tvDieselN2Amount) +
                getTextViewValue(tvDieselN3Amount) + getTextViewValue(tvDieselN4Amount)

        tvDieselTotalLitres.text = "Total Litres: ${String.format("%.2f", totalLitres)}"
        tvDieselTotalAmount.text = "Total Amount: ${String.format("%.2f", totalAmount)}"
    }

    private fun updateAllPetrolCalculations() {
        calculatePetrolNozzle(1)
        calculatePetrolNozzle(2)
        calculatePetrolNozzle(3)
        calculatePetrolNozzle(4)
    }

    private fun updateAllDieselCalculations() {
        calculateDieselNozzle(1)
        calculateDieselNozzle(2)
        calculateDieselNozzle(3)
        calculateDieselNozzle(4)
    }

    private fun setupPriceEditors() {
        // Make price fields editable on click
        etPetrolPrice.setOnClickListener {
            etPetrolPrice.requestFocus()
            etPetrolPrice.selectAll()
        }
        
        etDieselPrice.setOnClickListener {
            etDieselPrice.requestFocus()
            etDieselPrice.selectAll()
        }
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            if (!validateInputs()) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val petrolPrice = getEditTextValue(etPetrolPrice)
                    val dieselPrice = getEditTextValue(etDieselPrice)
                    
                    // Save petrol nozzles
                    savePetrolNozzles(petrolPrice)
                    
                    // Save diesel nozzles
                    saveDieselNozzles(dieselPrice)
                    
                    // Save fuel prices to preferences
                    prefsManager.saveFuelPrices(petrolPrice, dieselPrice)
                    
                    val petrolTotalLitres = getTextViewValue(tvPetrolN1Litres) + getTextViewValue(tvPetrolN2Litres) +
                            getTextViewValue(tvPetrolN3Litres) + getTextViewValue(tvPetrolN4Litres)
                    
                    val dieselTotalLitres = getTextViewValue(tvDieselN1Litres) + getTextViewValue(tvDieselN2Litres) +
                            getTextViewValue(tvDieselN3Litres) + getTextViewValue(tvDieselN4Litres)

                    val message = "Fuel Entry Saved Successfully!\n" +
                            "Petrol: ${String.format("%.2f", petrolTotalLitres)}L\n" +
                            "Diesel: ${String.format("%.2f", dieselTotalLitres)}L"

                    Toast.makeText(this@FuelEntryActivity, message, Toast.LENGTH_LONG).show()
                    
                    // Finish activity and return to main
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@FuelEntryActivity, "Error saving fuel entry: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private suspend fun savePetrolNozzles(price: Double) {
        val petrolNozzles = listOf(
            Pair(1, Pair(etPetrolN1Opening, etPetrolN1Closing)),
            Pair(2, Pair(etPetrolN2Opening, etPetrolN2Closing)),
            Pair(3, Pair(etPetrolN3Opening, etPetrolN3Closing)),
            Pair(4, Pair(etPetrolN4Opening, etPetrolN4Closing))
        )
        
        petrolNozzles.forEach { (nozzleNumber, pair) ->
            val (openingEt, closingEt) = pair
            val opening = getEditTextValue(openingEt)
            val closing = getEditTextValue(closingEt)
            
            if (opening > 0 || closing > 0) {
                repository.saveFuelEntry("petrol", nozzleNumber, opening, closing, price)
            }
        }
    }
    
    private suspend fun saveDieselNozzles(price: Double) {
        val dieselNozzles = listOf(
            Pair(1, Pair(etDieselN1Opening, etDieselN1Closing)),
            Pair(2, Pair(etDieselN2Opening, etDieselN2Closing)),
            Pair(3, Pair(etDieselN3Opening, etDieselN3Closing)),
            Pair(4, Pair(etDieselN4Opening, etDieselN4Closing))
        )
        
        dieselNozzles.forEach { (nozzleNumber, pair) ->
            val (openingEt, closingEt) = pair
            val opening = getEditTextValue(openingEt)
            val closing = getEditTextValue(closingEt)
            
            if (opening > 0 || closing > 0) {
                repository.saveFuelEntry("diesel", nozzleNumber, opening, closing, price)
            }
        }
    }

    private fun validateInputs(): Boolean {
        // Check for negative values
        val petrolNozzles = listOf(
            Pair(etPetrolN1Opening, etPetrolN1Closing),
            Pair(etPetrolN2Opening, etPetrolN2Closing),
            Pair(etPetrolN3Opening, etPetrolN3Closing),
            Pair(etPetrolN4Opening, etPetrolN4Closing)
        )

        val dieselNozzles = listOf(
            Pair(etDieselN1Opening, etDieselN1Closing),
            Pair(etDieselN2Opening, etDieselN2Closing),
            Pair(etDieselN3Opening, etDieselN3Closing),
            Pair(etDieselN4Opening, etDieselN4Closing)
        )

        for ((opening, closing) in petrolNozzles + dieselNozzles) {
            val openingValue = getEditTextValue(opening)
            val closingValue = getEditTextValue(closing)
            
            if (openingValue < 0 || closingValue < 0) {
                Toast.makeText(this, "Negative values are not allowed", Toast.LENGTH_SHORT).show()
                return false
            }
            
            if (closingValue > openingValue) {
                Toast.makeText(this, "Opening value cannot be less than closing value", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        val petrolPrice = getEditTextValue(etPetrolPrice)
        val dieselPrice = getEditTextValue(etDieselPrice)
        
        if (petrolPrice <= 0 || dieselPrice <= 0) {
            Toast.makeText(this, "Price must be greater than 0", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun loadPreviousDayData() {
        // For now, set opening values to 0
        // In future, this would load from database
        etPetrolN1Opening.setText("0")
        etPetrolN2Opening.setText("0")
        etPetrolN3Opening.setText("0")
        etPetrolN4Opening.setText("0")
        
        etDieselN1Opening.setText("0")
        etDieselN2Opening.setText("0")
        etDieselN3Opening.setText("0")
        etDieselN4Opening.setText("0")
    }
    
    private fun loadFuelPrices() {
        // Load saved fuel prices
        val petrolPrice = prefsManager.getPetrolPrice()
        val dieselPrice = prefsManager.getDieselPrice()
        
        etPetrolPrice.setText(String.format("%.2f", petrolPrice))
        etDieselPrice.setText(String.format("%.2f", dieselPrice))
    }

    private fun getEditTextValue(editText: EditText): Double {
        val text = editText.text.toString()
        return if (text.isEmpty()) 0.0 else text.toDoubleOrNull() ?: 0.0
    }

    private fun getTextViewValue(textView: android.widget.TextView): Double {
        val text = textView.text.toString()
        // Extract numeric value from strings like "Total Litres: 123.45" or "123.45"
        val regex = Regex("([0-9]*\\.?[0-9]+)")
        val match = regex.find(text)
        return match?.value?.toDoubleOrNull() ?: 0.0
    }
}
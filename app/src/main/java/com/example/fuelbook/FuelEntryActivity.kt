package com.example.fuelbook

import android.app.DatePickerDialog

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
import java.util.*

/**
 * FIXES & CHANGES (Nozzle Management Sync):
 *
 * 1. FIX – loadNozzles() is now called from onResume() instead of only onCreate().
 *    This guarantees that whenever the user navigates back from SettingsActivity, the
 *    nozzle list is re-read from SharedPreferences and the UI is refreshed immediately.
 *
 * 2. FIX – A SharedPreferences.OnSharedPreferenceChangeListener is registered in
 *    onStart() / unregistered in onStop(). This handles the edge case where Settings
 *    is still open in a split-screen or the prefs change from another entry point –
 *    the Fuel Entry screen reacts in real-time without needing a full resume cycle.
 *
 * 3. FIX – applyNozzleConfiguration() now:
 *      a) Updates the nozzle-label TextViews (tvPetrolN1Label … tvDieselN4Label) to
 *         show the actual user-defined names (e.g. "N5", "PUMP-A") instead of hardcoded
 *         "Nozzle 1 … Nozzle 4".
 *      b) Shows only the rows that correspond to configured nozzles and HIDES the rest,
 *         so a pump with 2 nozzles never sees dead rows N3/N4.
 *      c) Resets the state arrays (petrolLitres/Amounts, dieselLitres/Amounts) for hidden
 *         rows to 0 so totals are never inflated by stale data.
 *
 * 4. FIX – setupTextWatchers() is called once in onCreate(); it attaches watchers to all
 *    four fixed EditText pairs. The visibility changes in applyNozzleConfiguration() ensure
 *    hidden rows cannot be edited, so their watcher callbacks are harmless (they'll always
 *    compute 0 for invisible, cleared fields).
 *
 * 5. All other existing logic (price loading, validation, save) is preserved unchanged.
 */
class FuelEntryActivity : AppCompatActivity() {

    // ── Petrol UI ──────────────────────────────────────────────────
    private lateinit var etPetrolPrice: EditText
    private lateinit var etPetrolN1Opening: EditText; private lateinit var etPetrolN1Closing: EditText
    private lateinit var tvPetrolN1Litres: TextView;  private lateinit var tvPetrolN1Amount: TextView
    private lateinit var etPetrolN2Opening: EditText; private lateinit var etPetrolN2Closing: EditText
    private lateinit var tvPetrolN2Litres: TextView;  private lateinit var tvPetrolN2Amount: TextView
    private lateinit var etPetrolN3Opening: EditText; private lateinit var etPetrolN3Closing: EditText
    private lateinit var tvPetrolN3Litres: TextView;  private lateinit var tvPetrolN3Amount: TextView
    private lateinit var etPetrolN4Opening: EditText; private lateinit var etPetrolN4Closing: EditText
    private lateinit var tvPetrolN4Litres: TextView;  private lateinit var tvPetrolN4Amount: TextView
    private lateinit var tvPetrolTotalLitres: TextView; private lateinit var tvPetrolTotalAmount: TextView

    // Nozzle label TextViews – must exist in activity_fuel_entry.xml with these IDs.
    // Each row's parent container is used for show/hide so all child views toggle together.
    private lateinit var tvPetrolN1Label: TextView
    private lateinit var tvPetrolN2Label: TextView
    private lateinit var tvPetrolN3Label: TextView
    private lateinit var tvPetrolN4Label: TextView

    // Row containers (e.g. a LinearLayout / ConstraintLayout wrapping each nozzle row).
    // IDs expected: layoutPetrolN1Row … layoutPetrolN4Row, layoutDieselN1Row … layoutDieselN4Row
    private lateinit var layoutPetrolN1Row: View
    private lateinit var layoutPetrolN2Row: View
    private lateinit var layoutPetrolN3Row: View
    private lateinit var layoutPetrolN4Row: View

    // ── Diesel UI ──────────────────────────────────────────────────
    private lateinit var etDieselPrice: EditText
    private lateinit var etDieselN1Opening: EditText; private lateinit var etDieselN1Closing: EditText
    private lateinit var tvDieselN1Litres: TextView;  private lateinit var tvDieselN1Amount: TextView
    private lateinit var etDieselN2Opening: EditText; private lateinit var etDieselN2Closing: EditText
    private lateinit var tvDieselN2Litres: TextView;  private lateinit var tvDieselN2Amount: TextView
    private lateinit var etDieselN3Opening: EditText; private lateinit var etDieselN3Closing: EditText
    private lateinit var tvDieselN3Litres: TextView;  private lateinit var tvDieselN3Amount: TextView
    private lateinit var etDieselN4Opening: EditText; private lateinit var etDieselN4Closing: EditText
    private lateinit var tvDieselN4Litres: TextView;  private lateinit var tvDieselN4Amount: TextView
    private lateinit var tvDieselTotalLitres: TextView; private lateinit var tvDieselTotalAmount: TextView

    private lateinit var tvDieselN1Label: TextView
    private lateinit var tvDieselN2Label: TextView
    private lateinit var tvDieselN3Label: TextView
    private lateinit var tvDieselN4Label: TextView

    private lateinit var layoutDieselN1Row: View
    private lateinit var layoutDieselN2Row: View
    private lateinit var layoutDieselN3Row: View
    private lateinit var layoutDieselN4Row: View

    // ── Other UI ───────────────────────────────────────────────────
    private lateinit var tvDate: TextView
    private lateinit var btnSave: MaterialButton

    // ── State holders ──────────────────────────────────────────────
    private val petrolLitres  = DoubleArray(4)
    private val petrolAmounts = DoubleArray(4)
    private val dieselLitres  = DoubleArray(4)
    private val dieselAmounts = DoubleArray(4)

    // ── Nozzle config ──────────────────────────────────────────────
    /** Live list of nozzle names read from SharedPreferences. */
    private val availableNozzles = mutableListOf<String>()
    private val maxNozzles = 4

    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager

    /**
     * Listener registered while the activity is started.
     * Reloads nozzles whenever SettingsActivity (or any code) changes KEY_NOZZLES_CONFIG
     * in the shared preferences – even if this activity is already in the foreground.
     */
    private val prefsChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SharedPreferencesManager.KEY_NOZZLES_CONFIG) {
            loadNozzles()
            applyNozzleConfiguration()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuel_entry)


        val database = FuelBookDatabase.getDatabase(this, lifecycleScope)
        prefsManager = SharedPreferencesManager(this)
        repository   = FuelBookRepository(database, prefsManager)

        initViews()
        setupBackNavigation()
        setupDateDisplay()
        setupPriceEditors()
        setupTextWatchers()   // Attach once; visibility controls which rows are usable.
        setupSaveButton()
        loadFuelPrices()
        // Initial nozzle load + UI application happens in onResume.
    }

    /**
     * Called every time this activity becomes visible – including on back-navigation from
     * SettingsActivity. This is the primary sync point for nozzle changes.
     */
    override fun onResume() {
        super.onResume()
        loadNozzles()
        applyNozzleConfiguration()
    }

    /** Register the live-update listener. */
    override fun onStart() {
        super.onStart()
        prefsManager.getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(prefsChangeListener)
    }

    /** Unregister to avoid memory leaks. */
    override fun onStop() {
        super.onStop()
        prefsManager.getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(prefsChangeListener)
    }

    // ─────────────────────────────────────────────────────────────
    // View initialisation
    // ─────────────────────────────────────────────────────────────

    private fun initViews() {
        tvDate  = findViewById(R.id.tvDate)
        btnSave = findViewById(R.id.btnSave)

        etPetrolPrice       = findViewById(R.id.etPetrolPrice)
        etPetrolN1Opening   = findViewById(R.id.etPetrolN1Opening)
        etPetrolN1Closing   = findViewById(R.id.etPetrolN1Closing)
        tvPetrolN1Litres    = findViewById(R.id.tvPetrolN1Litres)
        tvPetrolN1Amount    = findViewById(R.id.tvPetrolN1Amount)
        etPetrolN2Opening   = findViewById(R.id.etPetrolN2Opening)
        etPetrolN2Closing   = findViewById(R.id.etPetrolN2Closing)
        tvPetrolN2Litres    = findViewById(R.id.tvPetrolN2Litres)
        tvPetrolN2Amount    = findViewById(R.id.tvPetrolN2Amount)
        etPetrolN3Opening   = findViewById(R.id.etPetrolN3Opening)
        etPetrolN3Closing   = findViewById(R.id.etPetrolN3Closing)
        tvPetrolN3Litres    = findViewById(R.id.tvPetrolN3Litres)
        tvPetrolN3Amount    = findViewById(R.id.tvPetrolN3Amount)
        etPetrolN4Opening   = findViewById(R.id.etPetrolN4Opening)
        etPetrolN4Closing   = findViewById(R.id.etPetrolN4Closing)
        tvPetrolN4Litres    = findViewById(R.id.tvPetrolN4Litres)
        tvPetrolN4Amount    = findViewById(R.id.tvPetrolN4Amount)
        tvPetrolTotalLitres = findViewById(R.id.tvPetrolTotalLitres)
        tvPetrolTotalAmount = findViewById(R.id.tvPetrolTotalAmount)

        tvPetrolN1Label = findViewById(R.id.tvPetrolN1Label)
        tvPetrolN2Label = findViewById(R.id.tvPetrolN2Label)
        tvPetrolN3Label = findViewById(R.id.tvPetrolN3Label)
        tvPetrolN4Label = findViewById(R.id.tvPetrolN4Label)

        layoutPetrolN1Row = findViewById(R.id.layoutPetrolN1Row)
        layoutPetrolN2Row = findViewById(R.id.layoutPetrolN2Row)
        layoutPetrolN3Row = findViewById(R.id.layoutPetrolN3Row)
        layoutPetrolN4Row = findViewById(R.id.layoutPetrolN4Row)

        etDieselPrice       = findViewById(R.id.etDieselPrice)
        etDieselN1Opening   = findViewById(R.id.etDieselN1Opening)
        etDieselN1Closing   = findViewById(R.id.etDieselN1Closing)
        tvDieselN1Litres    = findViewById(R.id.tvDieselN1Litres)
        tvDieselN1Amount    = findViewById(R.id.tvDieselN1Amount)
        etDieselN2Opening   = findViewById(R.id.etDieselN2Opening)
        etDieselN2Closing   = findViewById(R.id.etDieselN2Closing)
        tvDieselN2Litres    = findViewById(R.id.tvDieselN2Litres)
        tvDieselN2Amount    = findViewById(R.id.tvDieselN2Amount)
        etDieselN3Opening   = findViewById(R.id.etDieselN3Opening)
        etDieselN3Closing   = findViewById(R.id.etDieselN3Closing)
        tvDieselN3Litres    = findViewById(R.id.tvDieselN3Litres)
        tvDieselN3Amount    = findViewById(R.id.tvDieselN3Amount)
        etDieselN4Opening   = findViewById(R.id.etDieselN4Opening)
        etDieselN4Closing   = findViewById(R.id.etDieselN4Closing)
        tvDieselN4Litres    = findViewById(R.id.tvDieselN4Litres)
        tvDieselN4Amount    = findViewById(R.id.tvDieselN4Amount)
        tvDieselTotalLitres = findViewById(R.id.tvDieselTotalLitres)
        tvDieselTotalAmount = findViewById(R.id.tvDieselTotalAmount)

        tvDieselN1Label = findViewById(R.id.tvDieselN1Label)
        tvDieselN2Label = findViewById(R.id.tvDieselN2Label)
        tvDieselN3Label = findViewById(R.id.tvDieselN3Label)
        tvDieselN4Label = findViewById(R.id.tvDieselN4Label)

        layoutDieselN1Row = findViewById(R.id.layoutDieselN1Row)
        layoutDieselN2Row = findViewById(R.id.layoutDieselN2Row)
        layoutDieselN3Row = findViewById(R.id.layoutDieselN3Row)
        layoutDieselN4Row = findViewById(R.id.layoutDieselN4Row)
    }

    // ─────────────────────────────────────────────────────────────
    // Nozzle loading & UI sync  ← THE CORE FIX
    // ─────────────────────────────────────────────────────────────

    /**
     * Reads the current nozzle list from SharedPreferences.
     * Falls back to ["N1","N2","N3","N4"] on first run.
     */
    private fun loadNozzles() {
        val saved = prefsManager.getNozzlesConfig()
        availableNozzles.clear()
        if (!saved.isNullOrBlank()) {
            saved.split(",").filter { it.isNotBlank() }
                .take(maxNozzles)
                .forEach { availableNozzles.add(it.trim().uppercase()) }
        } else {
            listOf("N1", "N2", "N3", "N4").forEach { availableNozzles.add(it) }
        }
        android.util.Log.d("FuelEntryActivity", "Nozzles reloaded: $availableNozzles")
    }

    /**
     * Applies the current [availableNozzles] list to the UI:
     *  - Shows/hides row containers so only configured nozzles are visible.
     *  - Updates label TextViews to show the actual nozzle name.
     *  - Clears + resets state arrays for hidden rows so totals stay correct.
     */
    private fun applyNozzleConfiguration() {
        val petrolRows = listOf(
            Triple(layoutPetrolN1Row, tvPetrolN1Label,
                listOf(etPetrolN1Opening, etPetrolN1Closing)),
            Triple(layoutPetrolN2Row, tvPetrolN2Label,
                listOf(etPetrolN2Opening, etPetrolN2Closing)),
            Triple(layoutPetrolN3Row, tvPetrolN3Label,
                listOf(etPetrolN3Opening, etPetrolN3Closing)),
            Triple(layoutPetrolN4Row, tvPetrolN4Label,
                listOf(etPetrolN4Opening, etPetrolN4Closing))
        )
        val dieselRows = listOf(
            Triple(layoutDieselN1Row, tvDieselN1Label,
                listOf(etDieselN1Opening, etDieselN1Closing)),
            Triple(layoutDieselN2Row, tvDieselN2Label,
                listOf(etDieselN2Opening, etDieselN2Closing)),
            Triple(layoutDieselN3Row, tvDieselN3Label,
                listOf(etDieselN3Opening, etDieselN3Closing)),
            Triple(layoutDieselN4Row, tvDieselN4Label,
                listOf(etDieselN4Opening, etDieselN4Closing))
        )

        petrolRows.forEachIndexed { index, (row, label, inputs) ->
            val isActive = index < availableNozzles.size
            row.visibility = if (isActive) View.VISIBLE else View.GONE
            if (isActive) {
                label.text = availableNozzles[index]
            } else {
                // Clear inputs + reset state for hidden rows
                inputs.forEach { it.text?.clear() }
                petrolLitres[index]  = 0.0
                petrolAmounts[index] = 0.0
            }
        }

        dieselRows.forEachIndexed { index, (row, label, inputs) ->
            val isActive = index < availableNozzles.size
            row.visibility = if (isActive) View.VISIBLE else View.GONE
            if (isActive) {
                label.text = availableNozzles[index]
            } else {
                inputs.forEach { it.text?.clear() }
                dieselLitres[index]  = 0.0
                dieselAmounts[index] = 0.0
            }
        }

        // Refresh totals after potential state reset
        updatePetrolTotals()
        updateDieselTotals()
    }

    // ─────────────────────────────────────────────────────────────
    // Setup helpers
    // ─────────────────────────────────────────────────────────────

    private fun setupBackNavigation() {
        findViewById<android.widget.ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun setupDateDisplay() {
        tvDate.text = DateUtils.currentDate
        tvDate.setOnClickListener { showDatePicker() }
    }

    private fun loadFuelPrices() {
        etPetrolPrice.setText(String.format("%.2f", prefsManager.getPetrolPrice()))
        etDieselPrice.setText(String.format("%.2f", prefsManager.getDieselPrice()))
    }

    private fun setupPriceEditors() {
        etPetrolPrice.setOnClickListener { it.requestFocus(); (it as EditText).selectAll() }
        etDieselPrice.setOnClickListener { it.requestFocus(); (it as EditText).selectAll() }
    }

    private fun setupTextWatchers() {
        val petrolNozzles = listOf(
            Triple(1, etPetrolN1Opening, etPetrolN1Closing),
            Triple(2, etPetrolN2Opening, etPetrolN2Closing),
            Triple(3, etPetrolN3Opening, etPetrolN3Closing),
            Triple(4, etPetrolN4Opening, etPetrolN4Closing)
        )
        val dieselNozzles = listOf(
            Triple(1, etDieselN1Opening, etDieselN1Closing),
            Triple(2, etDieselN2Opening, etDieselN2Closing),
            Triple(3, etDieselN3Opening, etDieselN3Closing),
            Triple(4, etDieselN4Opening, etDieselN4Closing)
        )

        petrolNozzles.forEach { (n, open, close) ->
            open.addTextChangedListener(makeWatcher { calculatePetrolNozzle(n) })
            close.addTextChangedListener(makeWatcher { calculatePetrolNozzle(n) })
        }
        dieselNozzles.forEach { (n, open, close) ->
            open.addTextChangedListener(makeWatcher { calculateDieselNozzle(n) })
            close.addTextChangedListener(makeWatcher { calculateDieselNozzle(n) })
        }
        etPetrolPrice.addTextChangedListener(makeWatcher { (1..4).forEach { calculatePetrolNozzle(it) } })
        etDieselPrice.addTextChangedListener(makeWatcher { (1..4).forEach { calculateDieselNozzle(it) } })
    }

    private fun makeWatcher(action: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { action() }
    }

    // ─────────────────────────────────────────────────────────────
    // Calculations
    // ─────────────────────────────────────────────────────────────

    private fun getVal(et: EditText): Double = et.text.toString().toDoubleOrNull() ?: 0.0

    private fun calculatePetrolNozzle(n: Int) {
        val (openEt, closeEt, litresTv, amountTv) = when (n) {
            1    -> Quad(etPetrolN1Opening, etPetrolN1Closing, tvPetrolN1Litres, tvPetrolN1Amount)
            2    -> Quad(etPetrolN2Opening, etPetrolN2Closing, tvPetrolN2Litres, tvPetrolN2Amount)
            3    -> Quad(etPetrolN3Opening, etPetrolN3Closing, tvPetrolN3Litres, tvPetrolN3Amount)
            else -> Quad(etPetrolN4Opening, etPetrolN4Closing, tvPetrolN4Litres, tvPetrolN4Amount)
        }
        val litres = maxOf(0.0, getVal(openEt) - getVal(closeEt))
        val amount = litres * getVal(etPetrolPrice)
        petrolLitres[n - 1]  = litres
        petrolAmounts[n - 1] = amount
        litresTv.text = String.format("%.2f", litres)
        amountTv.text = String.format("%.2f", amount)
        updatePetrolTotals()
    }

    private fun calculateDieselNozzle(n: Int) {
        val (openEt, closeEt, litresTv, amountTv) = when (n) {
            1    -> Quad(etDieselN1Opening, etDieselN1Closing, tvDieselN1Litres, tvDieselN1Amount)
            2    -> Quad(etDieselN2Opening, etDieselN2Closing, tvDieselN2Litres, tvDieselN2Amount)
            3    -> Quad(etDieselN3Opening, etDieselN3Closing, tvDieselN3Litres, tvDieselN3Amount)
            else -> Quad(etDieselN4Opening, etDieselN4Closing, tvDieselN4Litres, tvDieselN4Amount)
        }
        val litres = maxOf(0.0, getVal(openEt) - getVal(closeEt))
        val amount = litres * getVal(etDieselPrice)
        dieselLitres[n - 1]  = litres
        dieselAmounts[n - 1] = amount
        litresTv.text = String.format("%.2f", litres)
        amountTv.text = String.format("%.2f", amount)

        updateDieselTotals()
    }

    private fun updatePetrolTotals() {

        tvPetrolTotalLitres.text = "Total: ${String.format("%.2f", petrolLitres.sum())} L"
        tvPetrolTotalAmount.text = "₹ ${String.format("%.2f", petrolAmounts.sum())}"
    }

    private fun updateDieselTotals() {
        tvDieselTotalLitres.text = "Total: ${String.format("%.2f", dieselLitres.sum())} L"
        tvDieselTotalAmount.text = "₹ ${String.format("%.2f", dieselAmounts.sum())}"
    }

    // ─────────────────────────────────────────────────────────────
    // Validation & Save
    // ─────────────────────────────────────────────────────────────

    private fun validateInputs(): Boolean {
        // Only validate rows that are currently visible (active nozzles)
        val activeCount = availableNozzles.size

        val petrolNozzles = listOf(
            etPetrolN1Opening to etPetrolN1Closing,
            etPetrolN2Opening to etPetrolN2Closing,
            etPetrolN3Opening to etPetrolN3Closing,
            etPetrolN4Opening to etPetrolN4Closing
        ).take(activeCount)

        val dieselNozzles = listOf(
            etDieselN1Opening to etDieselN1Closing,
            etDieselN2Opening to etDieselN2Closing,
            etDieselN3Opening to etDieselN3Closing,
            etDieselN4Opening to etDieselN4Closing
        ).take(activeCount)

        for ((openEt, closeEt) in petrolNozzles + dieselNozzles) {
            val o = getVal(openEt); val c = getVal(closeEt)
            if (o < 0 || c < 0) {
                Toast.makeText(this, "Readings cannot be negative.", Toast.LENGTH_SHORT).show()
                return false
            }
            if (o == 0.0 && c == 0.0) continue
            if (c > o) {
                Toast.makeText(this, "Closing reading cannot exceed opening reading.", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        if (getVal(etPetrolPrice) <= 0 || getVal(etDieselPrice) <= 0) {
            Toast.makeText(this, "Price must be greater than 0.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true

    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {

            if (!validateInputs()) return@setOnClickListener

            lifecycleScope.launch {
                try {
                    val petrolPrice = getVal(etPetrolPrice)
                    val dieselPrice = getVal(etDieselPrice)

                    savePetrolNozzles(petrolPrice)
                    saveDieselNozzles(dieselPrice)
                    prefsManager.saveFuelPrices(petrolPrice, dieselPrice)

                    val msg = "Fuel Entry Saved!\n" +
                              "Petrol: ${String.format("%.2f", petrolLitres.sum())} L\n" +
                              "Diesel: ${String.format("%.2f", dieselLitres.sum())} L"
                    Toast.makeText(this@FuelEntryActivity, msg, Toast.LENGTH_LONG).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@FuelEntryActivity,
                        "Error saving fuel entry: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
        }
    }


    private suspend fun savePetrolNozzles(price: Double) {
        val nozzles = listOf(
            1 to (etPetrolN1Opening to etPetrolN1Closing),
            2 to (etPetrolN2Opening to etPetrolN2Closing),
            3 to (etPetrolN3Opening to etPetrolN3Closing),
            4 to (etPetrolN4Opening to etPetrolN4Closing)
        ).take(availableNozzles.size)   // Only save active nozzles

        nozzles.forEach { (num, pair) ->
            val o = getVal(pair.first); val c = getVal(pair.second)
            if (o > 0 || c > 0) repository.saveFuelEntry("petrol", num, o, c, price)
        }
    }

    private suspend fun saveDieselNozzles(price: Double) {
        val nozzles = listOf(
            1 to (etDieselN1Opening to etDieselN1Closing),
            2 to (etDieselN2Opening to etDieselN2Closing),
            3 to (etDieselN3Opening to etDieselN3Closing),
            4 to (etDieselN4Opening to etDieselN4Closing)
        ).take(availableNozzles.size)   // Only save active nozzles

        nozzles.forEach { (num, pair) ->
            val o = getVal(pair.first); val c = getVal(pair.second)
            if (o > 0 || c > 0) repository.saveFuelEntry("diesel", num, o, c, price)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Misc
    // ─────────────────────────────────────────────────────────────

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
                tvDate.text = DateUtils.formatForDisplay(selectedDate.time)
            },
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private data class Quad(
        val openEt:   EditText,
        val closeEt:  EditText,
        val litresTv: TextView,
        val amountTv: TextView
    )

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}



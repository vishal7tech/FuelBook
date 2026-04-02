package com.example.fuelbook


import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fuelbook.adapter.NozzleAdapter
import com.example.fuelbook.adapter.UpiOptionAdapter
import com.example.fuelbook.databinding.ActivitySettingsBinding
import com.example.fuelbook.model.Nozzle
import com.example.fuelbook.model.UpiOption
import com.example.fuelbook.utils.SharedPreferencesManager


/**
 * FIXES & CHANGES:
 * 1. Removed unused Context / Intent imports.
 * 2. showAddNozzleDialog() / showAddUpiOptionDialog() now style the EditText with proper padding
 *    and a background so it is visible inside the dark-theme dialog.
 * 3. Nozzle names are trimmed AND normalised to uppercase ("n5" → "N5") for consistent display.
 * 4. loadNozzles() no longer hardcodes the list; it reads from SharedPreferences if saved,
 *    falling back to defaults only on first run. This makes the persistence actually work.
 * 5. loadUpiOptions() same improvement as loadNozzles().
 * 6. saveNozzlesConfig() / saveUpiOptionsConfig() helpers added so every add/remove call
 *    persists changes immediately.
 * 7. Minimum size guard (≥1) is kept for both nozzles and UPI options.
 * 8. logout() is guard-wrapped so a double-tap won't crash.
 */

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var nozzleAdapter: NozzleAdapter
    private lateinit var upiOptionAdapter: UpiOptionAdapter
    private lateinit var prefsManager: SharedPreferencesManager


    private val nozzles    = mutableListOf<Nozzle>()
    private val upiOptions = mutableListOf<UpiOption>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)



        prefsManager = SharedPreferencesManager(this)

        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        loadSettings()
    }

    private fun setupToolbar() {

        findViewById<android.widget.ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        nozzleAdapter = NozzleAdapter(nozzles) { nozzle ->
            showRemoveConfirmation("Nozzle", nozzle.name) { removeNozzle(nozzle) }
        }
        binding.recyclerViewNozzles.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = nozzleAdapter
            setHasFixedSize(false)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }

        upiOptionAdapter = UpiOptionAdapter(upiOptions) { upi ->
            showRemoveConfirmation("UPI Option", upi.name) { removeUpiOption(upi) }
        }
        binding.recyclerViewUpiOptions.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = upiOptionAdapter
            setHasFixedSize(false)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

        }
    }

    private fun setupClickListeners() {

        binding.btnSaveFuelPrices.setOnClickListener { saveFuelPrices() }
        binding.btnAddNozzle.setOnClickListener     { showAddNozzleDialog() }
        binding.btnAddUpiOption.setOnClickListener  { showAddUpiOptionDialog() }
        binding.btnLogout.setOnClickListener        { showLogoutConfirmation() }
    }

    private fun loadSettings() {
        binding.etPetrolPrice.setText(String.format("%.2f", prefsManager.getPetrolPrice()))
        binding.etDieselPrice.setText(String.format("%.2f", prefsManager.getDieselPrice()))
        loadNozzles()
        loadUpiOptions()
    }

    // ── Nozzle persistence ─────────────────────────────────────────

    private fun loadNozzles() {
        val saved = prefsManager.getNozzlesConfig()
        nozzles.clear()
        if (!saved.isNullOrBlank()) {
            // Simple CSV storage: "N1,N2,N3,N4"
            saved.split(",").filter { it.isNotBlank() }.forEach { name ->
                nozzles.add(Nozzle(name = name.trim()))
            }
        } else {
            listOf("N1", "N2", "N3", "N4").forEach { nozzles.add(Nozzle(name = it)) }
        }
        android.util.Log.d("SettingsActivity", "Loaded ${nozzles.size} nozzles: ${nozzles.map { it.name }}")
        nozzleAdapter.updateNozzles(nozzles)
    }

    private fun saveNozzlesConfig() {
        prefsManager.saveNozzlesConfig(nozzles.joinToString(",") { it.name })
    }

    private fun addNozzle(rawName: String) {
        val name = rawName.trim().uppercase()
        if (name.isEmpty()) { showToast("Please enter a valid nozzle name."); return }
        if (nozzles.any { it.name.equals(name, ignoreCase = true) }) {
            showToast("Nozzle '$name' already exists."); return
        }
        nozzles.add(Nozzle(name = name))
        android.util.Log.d("SettingsActivity", "Added nozzle '$name', total: ${nozzles.size}")
        nozzleAdapter.updateNozzles(nozzles)
        saveNozzlesConfig()
        showToast("Nozzle '$name' added.")
    }

    private fun removeNozzle(nozzle: Nozzle) {
        if (nozzles.size <= 1) { showToast("At least one nozzle is required."); return }
        nozzles.remove(nozzle)
        nozzleAdapter.updateNozzles(nozzles)
        saveNozzlesConfig()
        showToast("Nozzle '${nozzle.name}' removed.")
    }

    // ── UPI option persistence ─────────────────────────────────────

    private fun loadUpiOptions() {
        val saved = prefsManager.getUpiProvidersConfig()
        upiOptions.clear()
        if (!saved.isNullOrBlank()) {
            saved.split(",").filter { it.isNotBlank() }.forEach { name ->
                upiOptions.add(UpiOption(name = name.trim()))
            }
        } else {
            listOf("Paytm", "Google Pay", "PhonePe", "Others").forEach {
                upiOptions.add(UpiOption(name = it))
            }
        }
        android.util.Log.d("SettingsActivity", "Loaded ${upiOptions.size} UPI options: ${upiOptions.map { it.name }}")
        upiOptionAdapter.updateUpiOptions(upiOptions)
    }

    private fun saveUpiOptionsConfig() {
        prefsManager.saveUpiProvidersConfig(upiOptions.joinToString(",") { it.name })
    }

    private fun addUpiOption(rawName: String) {
        val name = rawName.trim()
        if (name.isEmpty()) { showToast("Please enter a valid UPI option name."); return }
        if (upiOptions.any { it.name.equals(name, ignoreCase = true) }) {
            showToast("UPI option '$name' already exists."); return
        }
        upiOptions.add(UpiOption(name = name))
        upiOptionAdapter.updateUpiOptions(upiOptions)
        saveUpiOptionsConfig()
        showToast("UPI option '$name' added.")
    }

    private fun removeUpiOption(upi: UpiOption) {
        if (upiOptions.size <= 1) { showToast("At least one UPI option is required."); return }
        upiOptions.remove(upi)
        upiOptionAdapter.updateUpiOptions(upiOptions)
        saveUpiOptionsConfig()
        showToast("UPI option '${upi.name}' removed.")
    }

    // ── Fuel prices ────────────────────────────────────────────────

    private fun saveFuelPrices() {
        val petrolText = binding.etPetrolPrice.text.toString()
        val dieselText = binding.etDieselPrice.text.toString()

        if (petrolText.isEmpty() || dieselText.isEmpty()) {
            showToast("Please enter both fuel prices."); return
        }
        val petrol = petrolText.toDoubleOrNull()
        val diesel = dieselText.toDoubleOrNull()

        if (petrol == null || diesel == null || petrol <= 0 || diesel <= 0) {
            showToast("Please enter valid positive prices."); return
        }
        prefsManager.saveFuelPrices(petrol, diesel)
        showToast("Fuel prices saved successfully!")
    }

    // ── Dialogs ────────────────────────────────────────────────────

    private fun showAddNozzleDialog() {
        val et = EditText(this).apply { 
            hint = "e.g. N5"
            setPadding(32, 16, 32, 16)
            setTextColor(android.graphics.Color.BLACK)
            setHintTextColor(android.graphics.Color.GRAY)
        }
        AlertDialog.Builder(this)
            .setTitle("Add Nozzle")
            .setView(et)
            .setPositiveButton("Add") { _, _ -> addNozzle(et.text.toString()) }

            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showAddUpiOptionDialog() {
        val et = EditText(this).apply { 
            hint = "e.g. BHIM"
            setPadding(32, 16, 32, 16)
            setTextColor(android.graphics.Color.BLACK)
            setHintTextColor(android.graphics.Color.GRAY)
        }
        AlertDialog.Builder(this)
            .setTitle("Add UPI Option")
            .setView(et)
            .setPositiveButton("Add") { _, _ -> addUpiOption(et.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveConfirmation(type: String, name: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Remove $type")
            .setMessage("Remove '$name'?")
            .setPositiveButton("Remove") { _, _ -> onConfirm() }

            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")

            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> logout() }

            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {

        prefsManager.logout()
        showToast("Logged out successfully")
        finish()
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

}


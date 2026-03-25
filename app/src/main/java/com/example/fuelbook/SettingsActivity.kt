package com.example.fuelbook

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fuelbook.adapter.NozzleAdapter
import com.example.fuelbook.adapter.UpiOptionAdapter
import com.example.fuelbook.databinding.ActivitySettingsBinding
import com.example.fuelbook.model.Nozzle
import com.example.fuelbook.model.UpiOption
import com.example.fuelbook.utils.SharedPreferencesManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var nozzleAdapter: NozzleAdapter
    private lateinit var upiOptionAdapter: UpiOptionAdapter
    private lateinit var prefsManager: SharedPreferencesManager
    
    private var nozzles = mutableListOf<Nozzle>()
    private var upiOptions = mutableListOf<UpiOption>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        prefsManager = SharedPreferencesManager(this)

        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        loadSettings()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        // Nozzles RecyclerView
        nozzleAdapter = NozzleAdapter(nozzles) { nozzle ->
            showRemoveConfirmation("Nozzle", nozzle.name) {
                removeNozzle(nozzle)
            }
        }
        
        binding.recyclerViewNozzles.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = nozzleAdapter
        }

        // UPI Options RecyclerView
        upiOptionAdapter = UpiOptionAdapter(upiOptions) { upiOption ->
            showRemoveConfirmation("UPI Option", upiOption.name) {
                removeUpiOption(upiOption)
            }
        }
        
        binding.recyclerViewUpiOptions.apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = upiOptionAdapter
        }
    }

    private fun setupClickListeners() {
        // Save Fuel Prices
        binding.btnSaveFuelPrices.setOnClickListener {
            saveFuelPrices()
        }

        // Add Nozzle
        binding.btnAddNozzle.setOnClickListener {
            showAddNozzleDialog()
        }

        // Add UPI Option
        binding.btnAddUpiOption.setOnClickListener {
            showAddUpiOptionDialog()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadSettings() {
        // Load fuel prices from SharedPreferencesManager
        val petrolPrice = prefsManager.getPetrolPrice()
        val dieselPrice = prefsManager.getDieselPrice()
        
        binding.etPetrolPrice.setText(petrolPrice.toString())
        binding.etDieselPrice.setText(dieselPrice.toString())

        // Load nozzles
        loadNozzles()
        
        // Load UPI options
        loadUpiOptions()
    }

    private fun loadNozzles() {
        // Load default nozzles (can be extended to load from SharedPreferences)
        val defaultNozzles = listOf("N1", "N2", "N3", "N4")
        nozzles.clear()
        
        defaultNozzles.forEach { nozzleName ->
            nozzles.add(Nozzle(name = nozzleName))
        }
        
        // Debug: Log the number of nozzles loaded
        android.util.Log.d("SettingsActivity", "Loaded ${nozzles.size} nozzles: ${nozzles.map { it.name }}")
        
        nozzleAdapter.updateNozzles(nozzles)
    }

    private fun loadUpiOptions() {
        val defaultUpiOptions = listOf("Paytm", "Google Pay", "PhonePe", "Others")
        val savedUpiOptionsJson = prefsManager.getUpiProvidersConfig()
        
        upiOptions.clear()
        
        if (savedUpiOptionsJson != null) {
            // TODO: Parse JSON and load saved options
            // For now, use default options
            defaultUpiOptions.forEach { optionName ->
                upiOptions.add(UpiOption(name = optionName))
            }
        } else {
            defaultUpiOptions.forEach { optionName ->
                upiOptions.add(UpiOption(name = optionName))
            }
        }
        
        upiOptionAdapter.updateUpiOptions(upiOptions)
    }

    private fun saveFuelPrices() {
        val petrolPriceText = binding.etPetrolPrice.text.toString()
        val dieselPriceText = binding.etDieselPrice.text.toString()
        
        if (petrolPriceText.isEmpty() || dieselPriceText.isEmpty()) {
            Toast.makeText(this, "Please enter both fuel prices", Toast.LENGTH_SHORT).show()
            return
        }
        
        val petrolPrice = petrolPriceText.toDoubleOrNull()
        val dieselPrice = dieselPriceText.toDoubleOrNull()
        
        if (petrolPrice == null || dieselPrice == null || petrolPrice <= 0 || dieselPrice <= 0) {
            Toast.makeText(this, "Please enter valid positive prices", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Save to SharedPreferencesManager
        prefsManager.saveFuelPrices(petrolPrice, dieselPrice)
        
        Toast.makeText(this, "Fuel prices saved successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun showAddNozzleDialog() {
        val editText = EditText(this)
        editText.hint = "N5"
        
        AlertDialog.Builder(this)
            .setTitle("Add Nozzle")
            .setMessage("Enter nozzle name (e.g., N5, N6):")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val nozzleName = editText.text.toString().trim()
                if (nozzleName.isNotEmpty()) {
                    addNozzle(nozzleName)
                } else {
                    Toast.makeText(this, "Please enter a valid nozzle name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNozzle(name: String) {
        if (nozzles.any { it.name.equals(name, ignoreCase = true) }) {
            Toast.makeText(this, "Nozzle '$name' already exists", Toast.LENGTH_SHORT).show()
            return
        }
        
        val newNozzle = Nozzle(name = name)
        nozzles.add(newNozzle)
        nozzleAdapter.updateNozzles(nozzles)
        
        // TODO: Save to SharedPreferencesManager as JSON
        // For now, just update in memory
        
        Toast.makeText(this, "Nozzle '$name' added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun removeNozzle(nozzle: Nozzle) {
        if (nozzles.size <= 1) {
            Toast.makeText(this, "At least one nozzle is required", Toast.LENGTH_SHORT).show()
            return
        }
        
        nozzles.remove(nozzle)
        nozzleAdapter.updateNozzles(nozzles)
        
        // TODO: Save to SharedPreferencesManager as JSON
        // For now, just update in memory
        
        Toast.makeText(this, "Nozzle '${nozzle.name}' removed", Toast.LENGTH_SHORT).show()
    }

    private fun showAddUpiOptionDialog() {
        val editText = EditText(this)
        editText.hint = "BHIM"
        
        AlertDialog.Builder(this)
            .setTitle("Add UPI Option")
            .setMessage("Enter UPI option name:")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val upiName = editText.text.toString().trim()
                if (upiName.isNotEmpty()) {
                    addUpiOption(upiName)
                } else {
                    Toast.makeText(this, "Please enter a valid UPI option name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addUpiOption(name: String) {
        if (upiOptions.any { it.name.equals(name, ignoreCase = true) }) {
            Toast.makeText(this, "UPI option '$name' already exists", Toast.LENGTH_SHORT).show()
            return
        }
        
        val newUpiOption = UpiOption(name = name)
        upiOptions.add(newUpiOption)
        upiOptionAdapter.updateUpiOptions(upiOptions)
        
        // Save to SharedPreferencesManager
        val upiOptionNames = upiOptions.map { it.name }
        // TODO: Save as JSON
        // For now, just update in memory
        
        Toast.makeText(this, "UPI option '$name' added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun removeUpiOption(upiOption: UpiOption) {
        if (upiOptions.size <= 1) {
            Toast.makeText(this, "At least one UPI option is required", Toast.LENGTH_SHORT).show()
            return
        }
        
        upiOptions.remove(upiOption)
        upiOptionAdapter.updateUpiOptions(upiOptions)
        
        // Save to SharedPreferencesManager
        val upiOptionNames = upiOptions.map { it.name }
        // TODO: Save as JSON
        // For now, just update in memory
        
        Toast.makeText(this, "UPI option '${upiOption.name}' removed", Toast.LENGTH_SHORT).show()
    }

    private fun showRemoveConfirmation(itemType: String, itemName: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Remove $itemType")
            .setMessage("Are you sure you want to remove '$itemName'?")
            .setPositiveButton("Remove") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? This will return you to the login screen.")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        // Clear login state using SharedPreferencesManager
        prefsManager.logout()
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate to LoginActivity (when implemented)
        // For now, just finish and go back to main
        finish()
        
        // TODO: Start LoginActivity when implemented
        // val intent = Intent(this, LoginActivity::class.java)
        // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // startActivity(intent)
    }
}

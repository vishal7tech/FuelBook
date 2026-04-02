package com.example.fuelbook

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fuelbook.utils.SharedPreferencesManager
import com.google.android.material.button.MaterialButton

class ProfileActivity : AppCompatActivity() {

    private lateinit var etPumpName: EditText
    private lateinit var etOwnerName: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: MaterialButton
    private lateinit var prefsManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_profile)
            android.util.Log.d("ProfileActivity", "ProfileActivity started successfully")
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error setting up profile layout", e)
            Toast.makeText(this, "Error loading profile screen", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        prefsManager = SharedPreferencesManager(this)

        try {
            initViews()
            setupClickListeners()
            loadProfileData()
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error initializing profile components", e)
            Toast.makeText(this, "Error initializing profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        etPumpName = findViewById(R.id.etPumpName)
        etOwnerName = findViewById(R.id.etOwnerName)
        etAddress = findViewById(R.id.etAddress)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupClickListeners() {
        findViewById<android.widget.ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
        
        btnSave.setOnClickListener { saveProfile() }
    }

    private fun loadProfileData() {
        etPumpName.setText(prefsManager.getCurrentPumpName())
        etOwnerName.setText(prefsManager.getCurrentUserName())
        
        // Load address from SharedPreferences (add new key if not exists)
        val savedAddress = prefsManager.getSharedPreferences().getString("pump_address", "")
        etAddress.setText(savedAddress)
    }

    private fun saveProfile() {
        val pumpName = etPumpName.text.toString().trim()
        val ownerName = etOwnerName.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (pumpName.isEmpty()) {
            Toast.makeText(this, "Please enter pump name", Toast.LENGTH_SHORT).show()
            return
        }

        if (ownerName.isEmpty()) {
            Toast.makeText(this, "Please enter owner name", Toast.LENGTH_SHORT).show()
            return
        }

        // Save to SharedPreferences
        prefsManager.saveUserSession(
            prefsManager.getCurrentUserId(),
            ownerName,
            pumpName
        )
        
        // Save address separately
        prefsManager.getSharedPreferences().edit().putString("pump_address", address).apply()

        Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
        
        // Set result to indicate profile was updated
        setResult(RESULT_OK)
        finish()
    }
}

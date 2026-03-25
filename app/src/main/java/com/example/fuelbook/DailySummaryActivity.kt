package com.example.fuelbook

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.fuelbook.database.FuelBookDatabase
import com.example.fuelbook.database.FuelBookRepository
import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment

class DailySummaryActivity : AppCompatActivity() {

    private lateinit var tvDate: TextView
    private lateinit var tvPetrolLitres: TextView
    private lateinit var tvPetrolAmount: TextView
    private lateinit var tvDieselLitres: TextView
    private lateinit var tvDieselAmount: TextView
    private lateinit var tvCashTotal: TextView
    private lateinit var tvUpiTotal: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var tvFuelTotal: TextView
    private lateinit var progressBar: View
    private lateinit var contentLayout: View

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val currentDate = Calendar.getInstance().apply {
        // Using same fixed date as other screens for consistency during testing
        set(2026, Calendar.JANUARY, 4)
    }

    private lateinit var database: FuelBookDatabase
    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager
    private var currentSummaryData: FuelBookRepository.DailySummary? = null

    // Permission launcher for storage
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            generateAndDownloadPdf()
        } else {
            Toast.makeText(this, "Storage permission required for PDF download", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_daily_summary)

            // Initialize database and repository with error handling
            try {
                database = FuelBookDatabase.getDatabase(this, lifecycleScope)
                repository = FuelBookRepository(database, SharedPreferencesManager(this))
                prefsManager = SharedPreferencesManager(this)
            } catch (e: Exception) {
                showErrorAndFinish("Database initialization failed: ${e.message}")
                return
            }

            initViews()
            setupToolbar()
            setupDate()
            
            // Setup button click listeners with error handling
            try {
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveReport)?.setOnClickListener {
                    safeExecute("Save Report") { saveDailyReport() }
                }

                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDownloadPdf)?.setOnClickListener {
                    safeExecute("Download PDF") { checkStoragePermissionAndDownloadPdf() }
                }
            } catch (e: Exception) {
                showError("Error setting up buttons: ${e.message}")
            }
            
            // Load data
            loadTodaySummary()
        } catch (e: Exception) {
            showErrorAndFinish("Failed to create Daily Summary: ${e.message}")
        }
    }

    private fun initViews() {
        try {
            tvDate = findViewById(R.id.tvDate) ?: throw IllegalStateException("tvDate not found in layout")
            tvPetrolLitres = findViewById(R.id.tvPetrolLitres) ?: throw IllegalStateException("tvPetrolLitres not found in layout")
            tvPetrolAmount = findViewById(R.id.tvPetrolAmount) ?: throw IllegalStateException("tvPetrolAmount not found in layout")
            tvDieselLitres = findViewById(R.id.tvDieselLitres) ?: throw IllegalStateException("tvDieselLitres not found in layout")
            tvDieselAmount = findViewById(R.id.tvDieselAmount) ?: throw IllegalStateException("tvDieselAmount not found in layout")
            tvCashTotal = findViewById(R.id.tvCashTotal) ?: throw IllegalStateException("tvCashTotal not found in layout")
            tvUpiTotal = findViewById(R.id.tvUpiTotal) ?: throw IllegalStateException("tvUpiTotal not found in layout")
            tvGrandTotal = findViewById(R.id.tvGrandTotal) ?: throw IllegalStateException("tvGrandTotal not found in layout")
            tvFuelTotal = findViewById(R.id.tvFuelTotal) ?: throw IllegalStateException("tvFuelTotal not found in layout")
            progressBar = findViewById(R.id.progressBar) ?: throw IllegalStateException("progressBar not found in layout")
            contentLayout = findViewById(R.id.contentLayout) ?: throw IllegalStateException("contentLayout not found in layout")
        } catch (e: Exception) {
            showErrorAndFinish("Error initializing views: ${e.message}")
        }
    }

    private fun setupToolbar() {
        try {
            val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            if (toolbar == null) {
                showErrorAndFinish("Toolbar not found in layout")
                return
            }
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener { finish() }
        } catch (e: Exception) {
            showErrorAndFinish("Error setting up toolbar: ${e.message}")
        }
    }

    private fun setupDate() {
        tvDate.text = "Date: ${dateFormat.format(currentDate.time)}"
    }

    private fun loadTodaySummary() {
        safeExecute("Load Summary") {
            showLoading(true)

            lifecycleScope.launch {
                try {
                    val summary = repository.getDailySummary()
                    currentSummaryData = summary
                    
                    withContext(Dispatchers.Main) {
                        updateUI(summary)
                        showLoading(false)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        showToast("Error loading data: ${e.message}")
                        // Load dummy data as fallback
                        loadDummyData()
                    }
                }
            }
        }
    }

    private fun loadDummyData() {
        // Fallback dummy data for testing
        val dummySummary = FuelBookRepository.DailySummary(
            petrolLitres = 245.50,
            petrolAmount = 24550.0,
            dieselLitres = 380.75,
            dieselAmount = 38075.0,
            cashTotal = 35000.0,
            upiTotal = 27625.0,
            fuelSaleTotal = 62625.0,
            collectionTotal = 62625.0,
            difference = 0.0
        )
        currentSummaryData = dummySummary
        updateUI(dummySummary)
    }

    private fun updateUI(data: FuelBookRepository.DailySummary) {
        try {
            if (!::tvPetrolLitres.isInitialized || !::tvPetrolAmount.isInitialized) {
                showError("Views not properly initialized")
                return
            }

            tvPetrolLitres.text = "Total Litres: ${String.format("%.2f", data.petrolLitres)} L"
            tvPetrolAmount.text = "₹${String.format("%.2f", data.petrolAmount)}"

            tvDieselLitres.text = "Total Litres: ${String.format("%.2f", data.dieselLitres)} L"
            tvDieselAmount.text = "₹${String.format("%.2f", data.dieselAmount)}"

            tvCashTotal.text = "₹${String.format("%.2f", data.cashTotal)}"
            tvUpiTotal.text = "₹${String.format("%.2f", data.upiTotal)}"
            tvGrandTotal.text = "₹${String.format("%.2f", data.collectionTotal)}"

            // Set fuel total if view exists
            tvFuelTotal.text = "₹${String.format("%.2f", data.fuelSaleTotal)}"

            // Apply colorful styling based on values
            applyColorStyling(data)
        } catch (e: Exception) {
            showError("Error updating UI: ${e.message}")
        }
    }

    private fun applyColorStyling(data: FuelBookRepository.DailySummary) {
        try {
            // Green for positive amounts
            tvPetrolAmount.setTextColor(getColor(android.R.color.holo_green_dark))
            tvDieselAmount.setTextColor(getColor(android.R.color.holo_green_dark))
            tvCashTotal.setTextColor(getColor(android.R.color.holo_green_dark))
            tvUpiTotal.setTextColor(getColor(android.R.color.holo_green_dark))
            tvGrandTotal.setTextColor(getColor(android.R.color.white))
            tvFuelTotal.setTextColor(getColor(android.R.color.holo_green_dark))
        } catch (e: Exception) {
            // Silent fail for color errors
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (::progressBar.isInitialized && ::contentLayout.isInitialized) {
                progressBar.visibility = if (show) View.VISIBLE else View.GONE
                contentLayout.visibility = if (show) View.GONE else View.VISIBLE
            }
        } catch (e: Exception) {
            // Silent fail for loading state errors
        }
    }

    private fun saveDailyReport() {
        currentSummaryData?.let { data ->
            if (data.fuelSaleTotal <= 0 && data.cashTotal <= 0 && data.upiTotal <= 0) {
                Toast.makeText(this, "No data to save. Please add fuel and payment entries first.", Toast.LENGTH_LONG).show()
                return
            }

            lifecycleScope.launch {
                try {
                    val reportId = repository.saveDailyReport()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DailySummaryActivity, "Daily report saved successfully! (ID: $reportId)", Toast.LENGTH_LONG).show()
                        // Refresh data after saving
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadTodaySummary()
                        }, 1000)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DailySummaryActivity, "Error saving report: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } ?: run {
            Toast.makeText(this, "No data available to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermissionAndDownloadPdf() {
        currentSummaryData?.let { data ->
            if (data.fuelSaleTotal <= 0 && data.cashTotal <= 0 && data.upiTotal <= 0) {
                Toast.makeText(this, "No data to export. Please add entries first.", Toast.LENGTH_SHORT).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    generateAndDownloadPdf()
                } else {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            } else {
                generateAndDownloadPdf()
            }
        } ?: run {
            Toast.makeText(this, "No data available to export", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateAndDownloadPdf() {
        lifecycleScope.launch {
            try {
                val pdfFile = createPdfDocument()
                withContext(Dispatchers.Main) {
                    sharePdfFile(pdfFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DailySummaryActivity, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun createPdfDocument(): File = withContext(Dispatchers.IO) {
        val data = currentSummaryData ?: throw IllegalStateException("No data available")

        // Create PDF file
        val fileName = "FuelBook_Report_${DateUtils.formatForFile(DateUtils.currentDate)}.pdf"
        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PumpBook")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val pdfFile = File(downloadsDir, fileName)

        // Create PDF document
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paint for text
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }

        val normalPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
        }

        var yPosition = 50f

        // Title
        canvas.drawText("FUELBOOK DAILY REPORT", 180f, yPosition, paint)
        yPosition += 40

        // Date
        paint.textSize = 18f
        canvas.drawText("Date: ${DateUtils.currentDate}", 50f, yPosition, paint)
        yPosition += 40

        // Draw line
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 20

        // Fuel Details
        paint.textSize = 20f
        canvas.drawText("FUEL SALES", 50f, yPosition, paint)
        yPosition += 30

        normalPaint.textSize = 16f
        canvas.drawText("Petrol Reading:        ${String.format("%.0f", data.petrolLitres)}        X       ${String.format("%.0f", data.petrolAmount / if (data.petrolLitres > 0) data.petrolLitres else 1.0)}       = ${String.format("%.2f", data.petrolAmount)}", 50f, yPosition, normalPaint)
        yPosition += 25
        canvas.drawText("Diesel Reading:        ${String.format("%.0f", data.dieselLitres)}        X       ${String.format("%.0f", data.dieselAmount / if (data.dieselLitres > 0) data.dieselLitres else 1.0)}       = ${String.format("%.2f", data.dieselAmount)}", 50f, yPosition, normalPaint)
        yPosition += 30

        // Draw line
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 20

        canvas.drawText("Total Sale:                                              ${String.format("%.2f", data.fuelSaleTotal)}", 50f, yPosition, normalPaint)
        yPosition += 40

        // Payment Details
        paint.textSize = 20f
        canvas.drawText("PAYMENT COLLECTIONS", 50f, yPosition, paint)
        yPosition += 30

        normalPaint.textSize = 16f
        canvas.drawText("Cash Total:                                             ${String.format("%.2f", data.cashTotal)}", 50f, yPosition, normalPaint)
        yPosition += 25
        canvas.drawText("UPI Total:                                                ${String.format("%.2f", data.upiTotal)}", 50f, yPosition, normalPaint)
        yPosition += 30

        // Draw line
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 20

        canvas.drawText("Total Collection:                                       ${String.format("%.2f", data.collectionTotal)}", 50f, yPosition, normalPaint)
        yPosition += 40

        // Difference
        paint.textSize = 20f
        canvas.drawText("DIFFERENCE", 50f, yPosition, paint)
        yPosition += 30

        val differenceText = if (data.difference >= 0) "+${String.format("%.2f", data.difference)}" else "${String.format("%.2f", data.difference)}"
        canvas.drawText("Difference:                                             $differenceText", 50f, yPosition, normalPaint)
        yPosition += 40

        // Footer line
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 30

        // Footer
        paint.textSize = 12f
        canvas.drawText("Generated on: ${DateUtils.currentDate} 13:45", 50f, yPosition, paint)
        yPosition += 20
        canvas.drawText("Generated by: FuelBook App", 50f, yPosition, paint)

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(FileOutputStream(pdfFile))
        pdfDocument.close()

        pdfFile
    }

    private fun sharePdfFile(pdfFile: File) {
        safeExecute("Share PDF") {
            try {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    pdfFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Daily Fuel Report - ${dateFormat.format(currentDate.time)}")
                    putExtra(Intent.EXTRA_TEXT, "Please find attached the daily fuel report.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(intent, "Share Fuel Report"))
                showToast("PDF generated successfully!")
            } catch (e: Exception) {
                showToast("Error sharing PDF: ${e.message}")
            }
        }
    }

    // Comprehensive Error Handling Methods
    private fun safeExecute(operation: String, action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            showError("Error in $operation: ${e.message}")
        }
    }

    private fun showError(message: String) {
        try {
            showToast(message)
        } catch (e: Exception) {
            // Fallback if even toast fails
            runCatching {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showErrorAndFinish(message: String) {
        try {
            showToast(message)
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 2000)
        } catch (e: Exception) {
            // Immediate finish if toast fails
            finish()
        }
    }

    private fun showToast(message: String) {
        try {
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            // Silent fail for toast errors
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
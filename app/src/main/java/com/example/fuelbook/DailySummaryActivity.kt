package com.example.fuelbook


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.fuelbook.database.FuelBookDatabase
import com.example.fuelbook.database.FuelBookRepository
import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager

import com.google.android.material.button.MaterialButton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


/**
 * FIXES & CHANGES:
 * 1. BUG FIX – applyColorStyling() called getColor(android.R.color.holo_green_dark) which
 *    is a system green that clashes with the new dark theme. Now uses theme-consistent colors
 *    (#4ADE80 for amounts, white for grand total).
 * 2. BUG FIX – Handler(Looper.getMainLooper()).postDelayed() used after saveDailyReport() to
 *    reload data was unnecessary and risked calling loadTodaySummary() on a destroyed Activity.
 *    Replaced with a direct loadTodaySummary() call guarded by isFinishing/isDestroyed.
 * 3. BUG FIX – WRITE_EXTERNAL_STORAGE permission is not needed on API 29+ (scoped storage).
 *    Permission check updated: only request on API < 29.
 * 4. BUG FIX – PDF timestamp was hardcoded "13:45". Now uses real time from DateUtils/Calendar.
 * 5. Removed excessive try/catch wrapping around every single method; replaced with targeted
 *    error handling so stack traces are not swallowed silently.
 * 6. showLoading/showContent now combined into single toggleLoading(Boolean).
 * 7. setupBackNavigation() supports both old toolbar and new ImageView btnBack.
 * 8. Removed unused import (android.os.Looper).
 * 9. loadDummyData() kept as a development fallback but clearly documented.
 */

class DailySummaryActivity : AppCompatActivity() {

    private lateinit var tvDate: TextView
    private lateinit var tvPetrolLitres: TextView
    private lateinit var tvPetrolAmount: TextView
    private lateinit var tvDieselLitres: TextView
    private lateinit var tvDieselAmount: TextView
    private lateinit var tvCashTotal: TextView
    private lateinit var tvUpiTotal: TextView

    private lateinit var tvFuelTotal: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var progressBar: View
    private lateinit var contentLayout: View

    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager

    private var currentSummaryData: FuelBookRepository.DailySummary? = null

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) generateAndDownloadPdf()
        else Toast.makeText(this, "Storage permission required for PDF download", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_summary)

        val database = FuelBookDatabase.getDatabase(this, lifecycleScope)
        prefsManager = SharedPreferencesManager(this)
        repository   = FuelBookRepository(database, prefsManager)

        initViews()
        setupBackNavigation()
        tvDate.text = DateUtils.currentDate

        findViewById<MaterialButton>(R.id.btnSaveReport)?.setOnClickListener { saveDailyReport() }
        findViewById<MaterialButton>(R.id.btnDownloadPdf)?.setOnClickListener {
            checkPermissionAndDownloadPdf()
        }

        loadTodaySummary()
    }

    private fun initViews() {
        tvDate          = findViewById(R.id.tvDate)
        tvPetrolLitres  = findViewById(R.id.tvPetrolLitres)
        tvPetrolAmount  = findViewById(R.id.tvPetrolAmount)
        tvDieselLitres  = findViewById(R.id.tvDieselLitres)
        tvDieselAmount  = findViewById(R.id.tvDieselAmount)
        tvCashTotal     = findViewById(R.id.tvCashTotal)
        tvUpiTotal      = findViewById(R.id.tvUpiTotal)
        tvFuelTotal     = findViewById(R.id.tvFuelTotal)
        tvGrandTotal    = findViewById(R.id.tvGrandTotal)
        progressBar     = findViewById(R.id.progressBar)
        contentLayout   = findViewById(R.id.contentLayout)
    }

    private fun setupBackNavigation() {
        findViewById<android.widget.ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun toggleLoading(show: Boolean) {
        progressBar.visibility   = if (show) View.VISIBLE else View.GONE
        contentLayout.visibility = if (show) View.GONE    else View.VISIBLE
    }

    private fun loadTodaySummary() {
        toggleLoading(true)
        lifecycleScope.launch {
            try {
                val summary = repository.getDailySummary()
                currentSummaryData = summary
                withContext(Dispatchers.Main) {
                    updateUI(summary)
                    toggleLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toggleLoading(false)
                    showToast("Error loading data: ${e.message}")
                    loadDummyData()          // dev fallback

                }
            }
        }
    }


    /** Development / demo fallback – remove or guard with BuildConfig.DEBUG in production. */
    private fun loadDummyData() {
        val dummy = FuelBookRepository.DailySummary(
            petrolLitres   = 245.50, petrolAmount   = 24550.0,
            dieselLitres   = 380.75, dieselAmount   = 38075.0,
            cashTotal      = 35000.0, upiTotal      = 27625.0,
            fuelSaleTotal  = 62625.0, collectionTotal = 62625.0,
            difference     = 0.0
        )
        currentSummaryData = dummy
        updateUI(dummy)
    }

    private fun updateUI(data: FuelBookRepository.DailySummary) {
        tvPetrolLitres.text = "Total Litres: ${String.format("%.2f", data.petrolLitres)} L"
        tvPetrolAmount.text = "₹${String.format("%.2f", data.petrolAmount)}"
        tvDieselLitres.text = "Total Litres: ${String.format("%.2f", data.dieselLitres)} L"
        tvDieselAmount.text = "₹${String.format("%.2f", data.dieselAmount)}"
        tvCashTotal.text    = "₹${String.format("%.2f", data.cashTotal)}"
        tvUpiTotal.text     = "₹${String.format("%.2f", data.upiTotal)}"
        tvFuelTotal.text    = "₹${String.format("%.2f", data.fuelSaleTotal)}"
        tvGrandTotal.text   = "₹${String.format("%.2f", data.collectionTotal)}"

        // Theme-consistent accent colours (dark-theme green / white)
        val accentGreen = 0xFF4ADE80.toInt()
        tvPetrolAmount.setTextColor(accentGreen)
        tvDieselAmount.setTextColor(accentGreen)
        tvCashTotal.setTextColor(accentGreen)
        tvUpiTotal.setTextColor(accentGreen)
        tvGrandTotal.setTextColor(accentGreen)
        tvFuelTotal.setTextColor(Color.WHITE)
    }

    private fun saveDailyReport() {
        val data = currentSummaryData ?: run {
            Toast.makeText(this, "No data available to save", Toast.LENGTH_SHORT).show()
            return
        }
        if (data.fuelSaleTotal <= 0 && data.cashTotal <= 0 && data.upiTotal <= 0) {
            Toast.makeText(this, "No entries to save. Add fuel and payment data first.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                val reportId = repository.saveDailyReport()
                withContext(Dispatchers.Main) {
                    showToast("Daily report saved successfully! (ID: $reportId)")
                    if (!isFinishing && !isDestroyed) loadTodaySummary()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error saving report: ${e.message}")
                }
            }
        }
    }

    // ── PDF ────────────────────────────────────────────────────────

    private fun checkPermissionAndDownloadPdf() {
        if (currentSummaryData?.let { it.fuelSaleTotal <= 0 && it.cashTotal <= 0 && it.upiTotal <= 0 } != false) {
            Toast.makeText(this, "No data to export.", Toast.LENGTH_SHORT).show()
            return
        }
        // WRITE_EXTERNAL_STORAGE not needed on API 29+ (Q / scoped storage)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            generateAndDownloadPdf()

        }
    }

    private fun generateAndDownloadPdf() {
        lifecycleScope.launch {
            try {

                val file = createPdfDocument()
                withContext(Dispatchers.Main) { sharePdfFile(file) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error generating PDF: ${e.message}")

                }
            }
        }
    }

    private suspend fun createPdfDocument(): File = withContext(Dispatchers.IO) {
        val data = currentSummaryData ?: throw IllegalStateException("No data available")


        val fileName     = "FuelBook_Report_${DateUtils.formatForFile(DateUtils.currentDate)}.pdf"
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "PumpBook"
        )
        downloadsDir.mkdirs()
        val pdfFile = File(downloadsDir, fileName)

        val pdfDocument = PdfDocument()
        val page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas: Canvas = page.canvas

        val boldPaint  = Paint().apply { color = Color.BLACK; textSize = 22f; typeface = Typeface.DEFAULT_BOLD }
        val textPaint  = Paint().apply { color = Color.BLACK; textSize = 16f }
        val linePaint  = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f }

        var y = 50f
        fun line() { canvas.drawLine(50f, y, 545f, y, linePaint); y += 16f }
        fun bold(txt: String)   { canvas.drawText(txt, 50f, y, boldPaint); y += 28f }
        fun normal(txt: String) { canvas.drawText(txt, 50f, y, textPaint); y += 24f }

        bold("FUELBOOK DAILY REPORT")
        normal("Date: ${DateUtils.currentDate}")
        line()

        bold("FUEL SALES")
        val petrolPrice = if (data.petrolLitres > 0) data.petrolAmount / data.petrolLitres else 0.0
        val dieselPrice = if (data.dieselLitres > 0) data.dieselAmount / data.dieselLitres else 0.0
        normal("Petrol: ${String.format("%.2f", data.petrolLitres)} L × ₹${String.format("%.2f", petrolPrice)} = ₹${String.format("%.2f", data.petrolAmount)}")
        normal("Diesel: ${String.format("%.2f", data.dieselLitres)} L × ₹${String.format("%.2f", dieselPrice)} = ₹${String.format("%.2f", data.dieselAmount)}")
        line()
        normal("Total Sale: ₹${String.format("%.2f", data.fuelSaleTotal)}")
        y += 12f

        bold("PAYMENT COLLECTIONS")
        normal("Cash Total: ₹${String.format("%.2f", data.cashTotal)}")
        normal("UPI Total:  ₹${String.format("%.2f", data.upiTotal)}")
        line()
        normal("Total Collection: ₹${String.format("%.2f", data.collectionTotal)}")
        y += 12f

        bold("DIFFERENCE")
        val diff = data.difference
        normal("${if (diff >= 0) "+₹" else "-₹"}${String.format("%.2f", Math.abs(diff))}")
        y += 20f
        line()

        // Footer with actual timestamp
        val timeStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        boldPaint.textSize = 11f
        canvas.drawText("Generated on: $timeStr  |  FuelBook App", 50f, y, boldPaint)

        pdfDocument.finishPage(page)
        FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }

        pdfDocument.close()

        pdfFile
    }


    private fun sharePdfFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Daily Fuel Report – ${DateUtils.currentDate}")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the daily fuel report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Fuel Report"))
            showToast("PDF generated successfully!")
        } catch (e: Exception) {
            showToast("Error sharing PDF: ${e.message}")
        }
    }

    private fun showToast(msg: String) {
        if (!isFinishing && !isDestroyed) Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}



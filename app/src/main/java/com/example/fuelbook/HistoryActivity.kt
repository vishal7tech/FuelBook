package com.example.fuelbook

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fuelbook.database.FuelBookDatabase
import com.example.fuelbook.database.FuelBookRepository
import com.example.fuelbook.database.ReportHistory
import com.example.fuelbook.adapter.HistoryAdapter
import com.example.fuelbook.databinding.ActivityHistoryBinding
import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager
import kotlinx.coroutines.launch
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var database: FuelBookDatabase
    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager
    
    private var allReports = mutableListOf<ReportHistory>()
    private var filteredReports = mutableListOf<ReportHistory>()

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    // Permission launcher for storage
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with PDF generation
            currentReport?.let { generatePdf(it) }
        } else {
            Toast.makeText(this, "Storage permission is required to generate PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private var currentReport: ReportHistory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize database and repository
        database = FuelBookDatabase.getDatabase(this, lifecycleScope)
        repository = FuelBookRepository(database, SharedPreferencesManager(this))
        prefsManager = SharedPreferencesManager(this)

        setupToolbar()
        setupRecyclerView()
        setupDatePickers()
        setupClickListeners()
        loadReports()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(filteredReports) { report ->
            showReportDetailsDialog(report)
        }
        
        binding.recyclerViewReports.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {
            showDatePickerDialog { date ->
                startDate = date
                binding.etStartDate.setText(dateFormat.format(date.time))
            }
        }

        binding.etEndDate.setOnClickListener {
            showDatePickerDialog { date ->
                endDate = date
                binding.etEndDate.setText(dateFormat.format(date.time))
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            filterReports()
        }
    }

    private fun showDatePickerDialog(onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun filterReports() {
        filteredReports.clear()

        if (startDate == null && endDate == null) {
            filteredReports.addAll(allReports)
        } else {
            val startDateStr = startDate?.let { dateFormat.format(it.time) }
            val endDateStr = endDate?.let { dateFormat.format(it.time) }
            
            lifecycleScope.launch {
                try {
                    val reports = if (startDateStr != null && endDateStr != null) {
                        repository.getReportsByDateRange(startDateStr, endDateStr)
                    } else {
                        repository.getAllReports()
                    }
                    
                    filteredReports.clear()
                    filteredReports.addAll(reports)
                    
                    runOnUiThread {
                        historyAdapter.updateReports(filteredReports)
                        updateEmptyState()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@HistoryActivity, "Error filtering reports: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return
        }

        filteredReports.sortByDescending { it.date }
        historyAdapter.updateReports(filteredReports)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        binding.emptyStateLayout.visibility = if (filteredReports.isEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    private fun showReportDetailsDialog(report: ReportHistory) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_details, null)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Setup dialog content
        setupDialogContent(dialogView, report, dialog)
        
        dialog.show()
    }

    private fun setupDialogContent(dialogView: android.view.View, report: ReportHistory, dialog: AlertDialog) {
        // Find views
        val tvDate = dialogView.findViewById<android.widget.TextView>(R.id.tvReportDate)
        val tvPetrol = dialogView.findViewById<android.widget.TextView>(R.id.tvPetrolDetails)
        val tvDiesel = dialogView.findViewById<android.widget.TextView>(R.id.tvDieselDetails)
        val tvCash = dialogView.findViewById<android.widget.TextView>(R.id.tvCashDetails)
        val tvUpi = dialogView.findViewById<android.widget.TextView>(R.id.tvUpiDetails)
        val tvGrandTotal = dialogView.findViewById<android.widget.TextView>(R.id.tvGrandTotalDetails)
        val tvDifference = dialogView.findViewById<android.widget.TextView>(R.id.tvDifferenceDetails)
        val btnGeneratePdf = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGeneratePdf)
        val btnDelete = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)
        val btnOk = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk)

        // Set data
        tvDate.text = "Date: ${report.date}"
        tvPetrol.text = "Petrol: ${String.format("%.2f", report.petrolLitres)}L - ₹${String.format("%.2f", report.petrolAmount)}"
        tvDiesel.text = "Diesel: ${String.format("%.2f", report.dieselLitres)}L - ₹${String.format("%.2f", report.dieselAmount)}"
        tvCash.text = "Cash: ₹${String.format("%.2f", report.cashTotal)}"
        tvUpi.text = "UPI: ₹${String.format("%.2f", report.upiTotal)}"
        tvGrandTotal.text = "Total Collection: ₹${String.format("%.2f", report.collectionTotal)}"
        
        // Set difference with color
        val differenceText = if (report.difference >= 0) "+${String.format("%.2f", report.difference)}" else "${String.format("%.2f", report.difference)}"
        tvDifference.text = "Difference: $differenceText"
        tvDifference.setTextColor(
            if (report.difference >= 0) {
                getColor(android.R.color.holo_green_dark)
            } else {
                getColor(R.color.red)
            }
        )

        // Setup button clicks
        btnGeneratePdf.setOnClickListener {
            currentReport = report
            checkStoragePermissionAndGeneratePdf()
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation(report, dialog)
        }

        btnOk.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun checkStoragePermissionAndGeneratePdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                currentReport?.let { generatePdf(it) }
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            currentReport?.let { generatePdf(it) }
        }
    }

    private fun generatePdf(report: ReportHistory) {
        lifecycleScope.launch {
            try {
                val pdfFile = createPdfDocument(report)
                runOnUiThread {
                    sharePdfFile(pdfFile, report)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@HistoryActivity, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun createPdfDocument(report: ReportHistory): File = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // Create PDF file
        val fileName = "FuelBook_Report_${DateUtils.formatForFile(report.date)}.pdf"
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
        canvas.drawText("FUELBOOK REPORT", 200f, yPosition, paint)
        yPosition += 40

        // Date
        paint.textSize = 18f
        canvas.drawText("Date: ${report.date}", 50f, yPosition, paint)
        yPosition += 40

        // Draw line
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 20

        // Fuel Details
        paint.textSize = 20f
        canvas.drawText("FUEL SALES", 50f, yPosition, paint)
        yPosition += 30

        normalPaint.textSize = 16f
        canvas.drawText("Petrol:        ${String.format("%.2f", report.petrolLitres)} L        =        ₹${String.format("%.2f", report.petrolAmount)}", 50f, yPosition, normalPaint)
        yPosition += 25
        canvas.drawText("Diesel:        ${String.format("%.2f", report.dieselLitres)} L        =        ₹${String.format("%.2f", report.dieselAmount)}", 50f, yPosition, normalPaint)
        yPosition += 30

        // Draw line
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 20

        canvas.drawText("Total Sale:                                              ₹${String.format("%.2f", report.fuelSaleTotal)}", 50f, yPosition, normalPaint)
        yPosition += 40

        // Payment Details
        paint.textSize = 20f
        canvas.drawText("PAYMENT COLLECTIONS", 50f, yPosition, paint)
        yPosition += 30

        normalPaint.textSize = 16f
        canvas.drawText("Cash Total:                                             ₹${String.format("%.2f", report.cashTotal)}", 50f, yPosition, normalPaint)
        yPosition += 25
        canvas.drawText("UPI Total:                                                ₹${String.format("%.2f", report.upiTotal)}", 50f, yPosition, normalPaint)
        yPosition += 30

        // Draw line
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 20

        canvas.drawText("Total Collection:                                       ₹${String.format("%.2f", report.collectionTotal)}", 50f, yPosition, normalPaint)
        yPosition += 40

        // Difference
        paint.textSize = 20f
        canvas.drawText("DIFFERENCE", 50f, yPosition, paint)
        yPosition += 30

        val differenceText = if (report.difference >= 0) "+${String.format("%.2f", report.difference)}" else "${String.format("%.2f", report.difference)}"
        canvas.drawText("Difference:                                             $differenceText", 50f, yPosition, normalPaint)
        yPosition += 40

        // Footer line
        canvas.drawLine(50f, yPosition, 545f, yPosition, normalPaint)
        yPosition += 30

        // Footer
        paint.textSize = 12f
        canvas.drawText("Generated on: ${DateUtils.currentDate}", 50f, yPosition, paint)
        yPosition += 20
        canvas.drawText("Generated by: FuelBook App", 50f, yPosition, paint)

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(FileOutputStream(pdfFile))
        pdfDocument.close()

        pdfFile
    }

    private fun sharePdfFile(pdfFile: File, report: ReportHistory) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Fuel Report - ${report.date}")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the fuel report for ${report.date}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share Fuel Report"))
            Toast.makeText(this, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(report: ReportHistory, dialog: AlertDialog) {
        AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to delete report for ${report.date}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteReport(report)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReport(report: ReportHistory) {
        lifecycleScope.launch {
            try {
                repository.deleteReportByDate(report.date)
                allReports.removeAll { it.date == report.date }
                filteredReports.removeAll { it.date == report.date }
                
                runOnUiThread {
                    historyAdapter.updateReports(filteredReports)
                    updateEmptyState()
                    Toast.makeText(this@HistoryActivity, "Report deleted successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@HistoryActivity, "Error deleting report: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadReports() {
        lifecycleScope.launch {
            try {
                val reports = repository.getAllReports()
                allReports.clear()
                allReports.addAll(reports)
                
                runOnUiThread {
                    filterReports()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@HistoryActivity, "Error loading reports: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Load sample data as fallback
                    loadSampleData()
                }
            }
        }
    }

    private fun loadSampleData() {
        // Generate sample data for demonstration
        val calendar = Calendar.getInstance()
        
        val sampleReports = mutableListOf<ReportHistory>()
        for (i in 0..5) {
            calendar.add(Calendar.DAY_OF_MONTH, -i)
            val dateStr = dateFormat.format(calendar.time)
            
            val petrolLitres = (50..200).random().toDouble()
            val dieselLitres = (30..150).random().toDouble()
            val petrolAmount = petrolLitres * 104.0
            val dieselAmount = dieselLitres * 89.0
            val cashTotal = (2000..10000).random().toDouble()
            val upiTotal = (1000..8000).random().toDouble()
            
            val report = ReportHistory(
                userId = prefsManager.getCurrentUserId(),
                date = dateStr,
                petrolLitres = petrolLitres,
                petrolAmount = petrolAmount,
                dieselLitres = dieselLitres,
                dieselAmount = dieselAmount,
                fuelSaleTotal = petrolAmount + dieselAmount,
                cashTotal = cashTotal,
                upiTotal = upiTotal,
                collectionTotal = cashTotal + upiTotal,
                difference = (cashTotal + upiTotal) - (petrolAmount + dieselAmount)
            )
            
            sampleReports.add(report)
        }
        
        allReports.clear()
        allReports.addAll(sampleReports)
        filterReports()
    }
}

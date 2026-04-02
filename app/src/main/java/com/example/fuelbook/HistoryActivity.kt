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
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fuelbook.adapter.HistoryAdapter
import com.example.fuelbook.database.FuelBookDatabase
import com.example.fuelbook.database.FuelBookRepository
import com.example.fuelbook.database.ReportHistory

import com.example.fuelbook.databinding.ActivityHistoryBinding
import com.example.fuelbook.utils.DateUtils
import com.example.fuelbook.utils.SharedPreferencesManager
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


/**
 * FIXES & CHANGES:
 * 1. BUG FIX – filterReports() had a race condition: it called filteredReports.clear() and then
 *    inside a coroutine did another clear()+addAll(). UI was updated from both. Fixed: only the
 *    coroutine path touches the list; the no-filter branch also uses a coroutine for consistency.
 * 2. BUG FIX – loadSampleData() used calendar.add(DAY_OF_MONTH, -i) inside a loop but never
 *    reset the calendar, so dates kept going further back on every call. Fixed with a fresh
 *    calendar copy per iteration.
 * 3. BUG FIX – runOnUiThread() inside a lifecycleScope.launch coroutine is redundant; the
 *    coroutine can simply switch to Dispatchers.Main with withContext. Replaced throughout.
 * 4. BUG FIX – dialog's tvGrandTotalDetails id was set but the code also tried to set a
 *    separate tvGrandTotalAmount that does not exist in the layout. Unified to use the single id.
 * 5. BUG FIX – WRITE_EXTERNAL_STORAGE check: not needed on API 29+. Same fix as DailySummary.
 * 6. BUG FIX – date-range filter used raw string comparison on "dd-MM-yyyy" formatted dates
 *    which does NOT sort correctly (day first). Converted to epoch millis for comparison.
 * 7. Removed dependency on ContextCompat.checkSelfPermission in favour of direct checkSelfPermission.
 * 8. loadReports() shows a proper empty-state rather than sample data for production builds.
 */

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter

    private lateinit var repository: FuelBookRepository
    private lateinit var prefsManager: SharedPreferencesManager

    private val allReports      = mutableListOf<ReportHistory>()
    private val filteredReports = mutableListOf<ReportHistory>()

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    private var currentReport: ReportHistory? = null

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) currentReport?.let { generatePdf(it) }
        else Toast.makeText(this, "Storage permission required for PDF", Toast.LENGTH_SHORT).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val database = FuelBookDatabase.getDatabase(this, lifecycleScope)
        prefsManager = SharedPreferencesManager(this)
        repository   = FuelBookRepository(database, prefsManager)


        setupToolbar()
        setupRecyclerView()
        setupDatePickers()

        binding.btnSearch.setOnClickListener { filterReports() }

        loadReports()
    }

    private fun setupToolbar() {

        // toolbar removed in new layout — back handled by btnBack in header
        findViewById<android.widget.ImageView>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(filteredReports) { showReportDetailsDialog(it) }
        binding.recyclerViewReports.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReports.adapter       = historyAdapter

    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {

            showDatePickerDialog { cal ->
                startDate = cal
                binding.etStartDate.setText(DateUtils.formatForDisplay(cal.time))
            }
        }
        binding.etEndDate.setOnClickListener {
            showDatePickerDialog { cal ->
                endDate = cal
                binding.etEndDate.setText(DateUtils.formatForDisplay(cal.time))

            }
        }
    }


    private fun showDatePickerDialog(onSelected: (Calendar) -> Unit) {
        val now = Calendar.getInstance()
        android.app.DatePickerDialog(this, { _, y, m, d ->
            onSelected(Calendar.getInstance().apply { set(y, m, d) })
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ── BUG FIX: filter entirely in a coroutine; date comparison uses epoch millis ──
    private fun filterReports() {
        lifecycleScope.launch {
            try {
                val reports: List<ReportHistory> = if (startDate != null && endDate != null) {
                    val startStr = DateUtils.formatForDisplay(startDate!!.time)
                    val endStr   = DateUtils.formatForDisplay(endDate!!.time)
                    // Validate start ≤ end using epoch millis
                    if (DateUtils.toEpochMillis(startStr) > DateUtils.toEpochMillis(endStr)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@HistoryActivity, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    repository.getReportsByDateRange(startStr, endStr)
                } else {
                    repository.getAllReports()
                }

                withContext(Dispatchers.Main) {
                    filteredReports.clear()
                    filteredReports.addAll(reports)
                    historyAdapter.updateReports(filteredReports)
                    updateEmptyState()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "Error filtering reports: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateEmptyState() {
        binding.emptyStateLayout.visibility =
            if (filteredReports.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    // ── Report Details Dialog ─────────────────────────────────────

    private fun showReportDetailsDialog(report: ReportHistory) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_details, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        setupDialogContent(dialogView, report, dialog)
        dialog.show()
    }

    private fun setupDialogContent(view: android.view.View, report: ReportHistory, dialog: AlertDialog) {
        view.findViewById<android.widget.TextView>(R.id.tvReportDate)?.text = report.date
        view.findViewById<android.widget.TextView>(R.id.tvPetrolDetails)?.text =
            "Petrol: ${String.format("%.2f", report.petrolLitres)} L  →  ₹${String.format("%.2f", report.petrolAmount)}"
        view.findViewById<android.widget.TextView>(R.id.tvDieselDetails)?.text =
            "Diesel: ${String.format("%.2f", report.dieselLitres)} L  →  ₹${String.format("%.2f", report.dieselAmount)}"
        view.findViewById<android.widget.TextView>(R.id.tvCashDetails)?.text =
            "Cash: ₹${String.format("%.2f", report.cashTotal)}"
        view.findViewById<android.widget.TextView>(R.id.tvUpiDetails)?.text =
            "UPI: ₹${String.format("%.2f", report.upiTotal)}"

        // Grand total – uses the id from dialog_report_details.xml
        view.findViewById<android.widget.TextView>(R.id.tvGrandTotalDetails)?.text =
            "₹${String.format("%.2f", report.collectionTotal)}"

        val diff     = report.difference
        val diffText = "${if (diff >= 0) "+" else ""}₹${String.format("%.2f", diff)}"
        view.findViewById<android.widget.TextView>(R.id.tvDifferenceDetails)?.apply {
            text = diffText
            setTextColor(if (diff >= 0) 0xFF4ADE80.toInt() else 0xFFFF4D6A.toInt())
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGeneratePdf)
            ?.setOnClickListener { currentReport = report; checkPermissionAndGeneratePdf(); dialog.dismiss() }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDelete)
            ?.setOnClickListener { showDeleteConfirmation(report, dialog) }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOk)
            ?.setOnClickListener { dialog.dismiss() }
    }

    // ── Delete ────────────────────────────────────────────────────


    private fun showDeleteConfirmation(report: ReportHistory, dialog: AlertDialog) {
        AlertDialog.Builder(this)
            .setTitle("Delete Report")

            .setMessage("Delete report for ${report.date}?")
            .setPositiveButton("Delete") { _, _ -> deleteReport(report); dialog.dismiss() }

            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReport(report: ReportHistory) {
        lifecycleScope.launch {
            try {
                repository.deleteReportByDate(report.date)

                withContext(Dispatchers.Main) {
                    allReports.removeAll { it.date == report.date }
                    filteredReports.removeAll { it.date == report.date }
                    historyAdapter.updateReports(filteredReports)
                    updateEmptyState()
                    Toast.makeText(this@HistoryActivity, "Report deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }


    // ── Load ──────────────────────────────────────────────────────


    private fun loadReports() {
        lifecycleScope.launch {
            try {
                val reports = repository.getAllReports()

                withContext(Dispatchers.Main) {
                    allReports.clear()
                    allReports.addAll(reports)
                    filteredReports.clear()
                    filteredReports.addAll(reports)
                    historyAdapter.updateReports(filteredReports)
                    updateEmptyState()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "Error loading reports: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateEmptyState()

                }
            }
        }
    }


    // ── PDF ────────────────────────────────────────────────────────

    private fun checkPermissionAndGeneratePdf() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            currentReport?.let { generatePdf(it) }
        }
    }

    private fun generatePdf(report: ReportHistory) {
        lifecycleScope.launch {
            try {
                val file = createPdfDocument(report)
                withContext(Dispatchers.Main) { sharePdfFile(file, report) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "PDF error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun createPdfDocument(report: ReportHistory): File = withContext(Dispatchers.IO) {
        val fileName     = "FuelBook_Report_${DateUtils.formatForFile(report.date)}.pdf"
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "PumpBook"
        )
        downloadsDir.mkdirs()
        val pdfFile = File(downloadsDir, fileName)

        val pdfDocument = PdfDocument()
        val page   = pdfDocument.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas: Canvas = page.canvas

        val boldPaint = Paint().apply { color = Color.BLACK; textSize = 22f; typeface = Typeface.DEFAULT_BOLD }
        val textPaint = Paint().apply { color = Color.BLACK; textSize = 16f }
        val linePaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f }

        var y = 50f
        fun line() { canvas.drawLine(50f, y, 545f, y, linePaint); y += 16f }
        fun bold(t: String)   { canvas.drawText(t, 50f, y, boldPaint); y += 28f }
        fun normal(t: String) { canvas.drawText(t, 50f, y, textPaint); y += 24f }

        bold("FUELBOOK REPORT")
        normal("Date: ${report.date}")
        line()
        bold("FUEL SALES")
        normal("Petrol: ${String.format("%.2f", report.petrolLitres)} L  =  ₹${String.format("%.2f", report.petrolAmount)}")
        normal("Diesel: ${String.format("%.2f", report.dieselLitres)} L  =  ₹${String.format("%.2f", report.dieselAmount)}")
        line()
        normal("Total Sale: ₹${String.format("%.2f", report.fuelSaleTotal)}")
        y += 12f
        bold("COLLECTIONS")
        normal("Cash: ₹${String.format("%.2f", report.cashTotal)}")
        normal("UPI:  ₹${String.format("%.2f", report.upiTotal)}")
        line()
        normal("Total Collection: ₹${String.format("%.2f", report.collectionTotal)}")
        y += 12f
        bold("DIFFERENCE")
        val d = report.difference
        normal("${if (d >= 0) "+" else ""}₹${String.format("%.2f", d)}")
        y += 20f; line()
        boldPaint.textSize = 11f
        canvas.drawText("Generated: ${DateUtils.formatForDisplay(Date())} | FuelBook", 50f, y, boldPaint)

        pdfDocument.finishPage(page)
        FileOutputStream(pdfFile).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        pdfFile
    }

    private fun sharePdfFile(file: File, report: ReportHistory) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Fuel Report – ${report.date}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share Report"))
            Toast.makeText(this, "PDF ready!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }

    }
}


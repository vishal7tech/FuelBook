package com.example.fuelbook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fuelbook.database.ReportHistory
import com.example.fuelbook.databinding.ItemHistoryReportBinding

/**
 * FIXES & CHANGES:
 * 1. Replaced notifyDataSetChanged() with DiffUtil for efficient, animated updates.
 * 2. Added null-safe check before accessing binding views.
 * 3. tvReportDateDay / tvReportDateMonth split from the date string to match the new
 *    item_history_report.xml which uses separate TextViews for day and month.
 *    Falls back gracefully if the date format is unexpected.
 * 4. Adapter now takes an immutable List internally and creates a copy on update
 *    to avoid external mutation bugs.
 */
class HistoryAdapter(
    reports: List<ReportHistory>,
    private val onItemClick: (ReportHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ReportViewHolder>() {

    private var reports: List<ReportHistory> = reports.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemHistoryReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false

        )
        return ReportViewHolder(binding)
    }


    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) =
        holder.bind(reports[position])


    override fun getItemCount(): Int = reports.size

    fun updateReports(newReports: List<ReportHistory>) {

        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = reports.size
            override fun getNewListSize() = newReports.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                reports[oldPos].id == newReports[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                reports[oldPos] == newReports[newPos]
        })
        reports = newReports.toList()
        diff.dispatchUpdatesTo(this)
    }

    inner class ReportViewHolder(private val binding: ItemHistoryReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: ReportHistory) {
            // Parse date parts for the badge (format: "dd-MM-yyyy")
            val parts = report.date.split("-")
            val day   = parts.getOrNull(0) ?: report.date
            val month = parts.getOrNull(1)?.toIntOrNull()?.let {
                listOf("JAN","FEB","MAR","APR","MAY","JUN",
                       "JUL","AUG","SEP","OCT","NOV","DEC").getOrElse(it - 1) { "" }
            } ?: ""

            binding.tvReportDate.text       = report.date
            // Optional badge views – only present in the new dark-theme item layout
            binding.root.findViewById<android.widget.TextView>(
                com.example.fuelbook.R.id.tvReportDateDay)?.text   = day
            binding.root.findViewById<android.widget.TextView>(
                com.example.fuelbook.R.id.tvReportDateMonth)?.text = month

            binding.tvPetrolAmount.text = "₹${String.format("%.0f", report.petrolAmount)}"
            binding.tvDieselAmount.text = "₹${String.format("%.0f", report.dieselAmount)}"
            binding.tvGrandTotal.text   = "₹${String.format("%.0f", report.collectionTotal)}"

            binding.root.setOnClickListener { onItemClick(report) }

        }
    }
}


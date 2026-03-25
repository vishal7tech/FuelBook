package com.example.fuelbook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fuelbook.databinding.ItemHistoryReportBinding
import com.example.fuelbook.database.ReportHistory

class HistoryAdapter(
    private var reports: List<ReportHistory>,
    private val onItemClick: (ReportHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemHistoryReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(reports[position])
    }

    override fun getItemCount(): Int = reports.size

    fun updateReports(newReports: List<ReportHistory>) {
        reports = newReports
        notifyDataSetChanged()
    }

    inner class ReportViewHolder(private val binding: ItemHistoryReportBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(report: ReportHistory) {
            binding.apply {
                tvReportDate.text = report.date
                tvPetrolAmount.text = "₹${String.format("%.0f", report.petrolAmount)}"
                tvDieselAmount.text = "₹${String.format("%.0f", report.dieselAmount)}"
                tvGrandTotal.text = "₹${String.format("%.0f", report.collectionTotal)}"
                
                root.setOnClickListener {
                    onItemClick(report)
                }
            }
        }
    }
}

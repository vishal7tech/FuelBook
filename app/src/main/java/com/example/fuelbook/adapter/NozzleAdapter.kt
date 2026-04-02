package com.example.fuelbook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil

import androidx.recyclerview.widget.RecyclerView
import com.example.fuelbook.databinding.ItemNozzleBinding
import com.example.fuelbook.model.Nozzle


/**
 * FIXES & CHANGES:
 * 1. Replaced notifyDataSetChanged() with DiffUtil for smooth, animated updates.
 * 2. Internal list is now a defensive copy (toList()) so external mutations don't
 *    silently corrupt the adapter's state.
 * 3. updateNozzles() accepts both MutableList and plain List.
 */
class NozzleAdapter(
    nozzles: MutableList<Nozzle>,
    private val onRemoveClick: (Nozzle) -> Unit
) : RecyclerView.Adapter<NozzleAdapter.NozzleViewHolder>() {

    private var nozzles: List<Nozzle> = nozzles.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NozzleViewHolder {
        val binding = ItemNozzleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false

        )
        return NozzleViewHolder(binding)
    }


    override fun onBindViewHolder(holder: NozzleViewHolder, position: Int) =
        holder.bind(nozzles[position])

    override fun getItemCount(): Int = nozzles.size

    fun updateNozzles(newNozzles: List<Nozzle>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = nozzles.size
            override fun getNewListSize() = newNozzles.size
            override fun areItemsTheSame(o: Int, n: Int) = nozzles[o].id == newNozzles[n].id
            override fun areContentsTheSame(o: Int, n: Int) = nozzles[o] == newNozzles[n]
        })
        nozzles = newNozzles.toList()
        diff.dispatchUpdatesTo(this)
    }

    inner class NozzleViewHolder(private val binding: ItemNozzleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(nozzle: Nozzle) {
            binding.tvNozzleName.text = nozzle.name
            binding.btnRemoveNozzle.setOnClickListener { onRemoveClick(nozzle) }

        }
    }
}


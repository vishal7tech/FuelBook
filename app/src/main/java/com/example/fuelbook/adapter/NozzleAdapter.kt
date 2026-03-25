package com.example.fuelbook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fuelbook.databinding.ItemNozzleBinding
import com.example.fuelbook.model.Nozzle

class NozzleAdapter(
    private var nozzles: MutableList<Nozzle>,
    private val onRemoveClick: (Nozzle) -> Unit
) : RecyclerView.Adapter<NozzleAdapter.NozzleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NozzleViewHolder {
        val binding = ItemNozzleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NozzleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NozzleViewHolder, position: Int) {
        holder.bind(nozzles[position])
    }

    override fun getItemCount(): Int = nozzles.size

    fun updateNozzles(newNozzles: MutableList<Nozzle>) {
        nozzles = newNozzles
        notifyDataSetChanged()
    }

    inner class NozzleViewHolder(private val binding: ItemNozzleBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(nozzle: Nozzle) {
            binding.apply {
                tvNozzleName.text = nozzle.name
                
                btnRemoveNozzle.setOnClickListener {
                    onRemoveClick(nozzle)
                }
            }
        }
    }
}

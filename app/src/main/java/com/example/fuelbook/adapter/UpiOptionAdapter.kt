package com.example.fuelbook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fuelbook.databinding.ItemUpiOptionBinding
import com.example.fuelbook.model.UpiOption

class UpiOptionAdapter(
    private var upiOptions: MutableList<UpiOption>,
    private val onRemoveClick: (UpiOption) -> Unit
) : RecyclerView.Adapter<UpiOptionAdapter.UpiOptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpiOptionViewHolder {
        val binding = ItemUpiOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UpiOptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpiOptionViewHolder, position: Int) {
        holder.bind(upiOptions[position])
    }

    override fun getItemCount(): Int = upiOptions.size

    fun updateUpiOptions(newUpiOptions: MutableList<UpiOption>) {
        upiOptions = newUpiOptions
        notifyDataSetChanged()
    }

    inner class UpiOptionViewHolder(private val binding: ItemUpiOptionBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(upiOption: UpiOption) {
            binding.apply {
                tvUpiName.text = upiOption.name
                
                btnRemoveUpi.setOnClickListener {
                    onRemoveClick(upiOption)
                }
            }
        }
    }
}

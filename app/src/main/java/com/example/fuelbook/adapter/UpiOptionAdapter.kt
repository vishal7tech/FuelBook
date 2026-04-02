package com.example.fuelbook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil

import androidx.recyclerview.widget.RecyclerView
import com.example.fuelbook.databinding.ItemUpiOptionBinding
import com.example.fuelbook.model.UpiOption


/**
 * FIXES & CHANGES:
 * 1. Replaced notifyDataSetChanged() with DiffUtil – identical pattern to NozzleAdapter.
 * 2. Internal list is a defensive copy.
 * 3. updateUpiOptions() accepts both MutableList and plain List.
 */
class UpiOptionAdapter(
    upiOptions: MutableList<UpiOption>,
    private val onRemoveClick: (UpiOption) -> Unit
) : RecyclerView.Adapter<UpiOptionAdapter.UpiOptionViewHolder>() {

    private var upiOptions: List<UpiOption> = upiOptions.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpiOptionViewHolder {
        val binding = ItemUpiOptionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false

        )
        return UpiOptionViewHolder(binding)
    }


    override fun onBindViewHolder(holder: UpiOptionViewHolder, position: Int) =
        holder.bind(upiOptions[position])

    override fun getItemCount(): Int = upiOptions.size

    fun updateUpiOptions(newOptions: List<UpiOption>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = upiOptions.size
            override fun getNewListSize() = newOptions.size
            override fun areItemsTheSame(o: Int, n: Int) = upiOptions[o].id == newOptions[n].id
            override fun areContentsTheSame(o: Int, n: Int) = upiOptions[o] == newOptions[n]
        })
        upiOptions = newOptions.toList()
        diff.dispatchUpdatesTo(this)
    }

    inner class UpiOptionViewHolder(private val binding: ItemUpiOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(upiOption: UpiOption) {
            binding.tvUpiName.text = upiOption.name
            binding.btnRemoveUpi.setOnClickListener { onRemoveClick(upiOption) }

        }
    }
}


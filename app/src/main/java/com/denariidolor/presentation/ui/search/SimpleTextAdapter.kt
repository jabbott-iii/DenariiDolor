package com.denariidolor.presentation.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.denariidolor.databinding.ItemSimpleTextBinding

class SimpleTextAdapter : RecyclerView.Adapter<SimpleTextAdapter.ViewHolder>() {
    private val items = mutableListOf<String>()

    fun submit(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSimpleTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemSimpleTextBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(value: String) {
            binding.tvItemText.text = value
        }
    }
}

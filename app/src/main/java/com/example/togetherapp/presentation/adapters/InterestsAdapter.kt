package com.example.togetherapp.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.databinding.ItemInterestBinding
import com.example.togetherapp.domain.models.Interest

class InterestsAdapter(
    private var items: List<Interest>,
    private var selectedIds: Set<Int>,
    private val onToggle: (Int) -> Unit
) : RecyclerView.Adapter<InterestsAdapter.InterestViewHolder>() {

    inner class InterestViewHolder(
        private val binding: ItemInterestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Interest) {
            binding.checkInterest.setOnCheckedChangeListener(null)
            binding.checkInterest.text = item.name
            binding.checkInterest.isChecked = selectedIds.contains(item.id)

            binding.checkInterest.setOnCheckedChangeListener { _, _ ->
                onToggle(item.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val binding = ItemInterestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InterestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Interest>, newSelectedIds: Set<Int>) {
        items = newItems
        selectedIds = newSelectedIds
        notifyDataSetChanged()
    }
}
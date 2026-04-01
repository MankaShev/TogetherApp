package com.example.togetherapp.presentation.screens.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.R
import com.example.togetherapp.domain.models.CollectionModel

class FriendProfileCollectionsAdapter(
    private val onCollectionClick: (CollectionModel) -> Unit
) : RecyclerView.Adapter<FriendProfileCollectionsAdapter.CollectionViewHolder>() {

    private val items = mutableListOf<CollectionModel>()

    fun submitList(newItems: List<CollectionModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return CollectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CollectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(item: CollectionModel) {
            text1.text = item.title
            text2.text = "Доступ: ${item.access_type}"

            itemView.setOnClickListener {
                onCollectionClick(item)
            }
        }
    }
}
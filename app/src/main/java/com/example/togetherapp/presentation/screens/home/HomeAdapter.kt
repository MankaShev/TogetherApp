package com.example.togetherapp.presentation.screens.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.databinding.CardViewBinding
import com.example.togetherapp.domain.models.CollectionModel

class HomeAdapter(
    private val onItemClick: (CollectionModel) -> Unit
) : RecyclerView.Adapter<HomeAdapter.CollectionViewHolder>() {

    private var items = listOf<CollectionModel>()

    fun submitList(newItems: List<CollectionModel>) {
        items = newItems

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val binding = CardViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CollectionViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class CollectionViewHolder(
        private val binding: CardViewBinding,
        private val onItemClick: (CollectionModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(collection: CollectionModel) {
            // Заполняем данными
            binding.collectionName.text = collection.title
            binding.collectionDescription.text = collection.description
            // Если будет количество мест, добавь функцию в CollectionModel:
            // binding.numPlaces.text = "${collection.placeCount} мест"

            // Обработка клика
            binding.root.setOnClickListener {
                onItemClick(collection)
            }
        }
    }
}
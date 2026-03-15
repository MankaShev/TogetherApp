package com.example.togetherapp.presentation.screens.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.databinding.CardViewBinding
import com.example.togetherapp.domain.models.Collection

class HomeAdapter(
    private val onItemClick: (Collection) -> Unit
) : RecyclerView.Adapter<HomeAdapter.CollectionViewHolder>() {

    private var items = listOf<Collection>()

    fun submitList(newItems: List<Collection>) {
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
        private val onItemClick: (Collection) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(collection: Collection) {
            // Заполняем данными
            binding.collectionName.text = collection.name  // или collection.title?
            binding.collectionDescription.text = collection.description
            // У нас в коллекции пока нет длины массива place, наверн нужно добавить потом коллекции функкцию getlen и возыращать там длину коллекции
            //binding.numPlaces.text = "${collection.placeCount} мест"

            // Обработка клика
            binding.root.setOnClickListener {
                onItemClick(collection)
            }
        }
    }
}
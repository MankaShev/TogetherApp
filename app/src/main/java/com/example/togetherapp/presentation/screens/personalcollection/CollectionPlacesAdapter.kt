package com.example.togetherapp.presentation.screens.personalcollection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.databinding.ItemPlaceCardBinding
import com.example.togetherapp.domain.models.Place

class CollectionPlacesAdapter : RecyclerView.Adapter<CollectionPlacesAdapter.PlaceViewHolder>() {

    private val items = mutableListOf<Place>()

    fun submitList(newItems: List<Place>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class PlaceViewHolder(
        private val binding: ItemPlaceCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: Place) {
            binding.tvPlaceTitle.text = place.title
            binding.tvPlaceAddress.text = place.address ?: "Адрес не указан"

            val description = place.description?.trim().orEmpty()
            if (description.isBlank()) {
                binding.tvPlaceDescription.visibility = View.GONE
            } else {
                binding.tvPlaceDescription.visibility = View.VISIBLE
                binding.tvPlaceDescription.text = description
            }
        }
    }
}
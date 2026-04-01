package com.example.togetherapp.presentation.screens.personalcollection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.databinding.ItemPlaceCardBinding
import com.example.togetherapp.domain.models.CollectionPlaceWithPlace

class CollectionPlacesAdapter(
    private val onCheckInClicked: (item: CollectionPlaceWithPlace) -> Unit,
    private val onRemoveClicked: (linkId: Int) -> Unit,
    private val onRouteClicked: (item: CollectionPlaceWithPlace) -> Unit
) : RecyclerView.Adapter<CollectionPlacesAdapter.PlaceViewHolder>() {

    private val items = mutableListOf<CollectionPlaceWithPlace>()

    fun submitList(newItems: List<CollectionPlaceWithPlace>) {
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
        return PlaceViewHolder(
            binding = binding,
            onCheckInClicked = onCheckInClicked,
            onRemoveClicked = onRemoveClicked,
            onRouteClicked = onRouteClicked
        )
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class PlaceViewHolder(
        private val binding: ItemPlaceCardBinding,
        private val onCheckInClicked: (item: CollectionPlaceWithPlace) -> Unit,
        private val onRemoveClicked: (linkId: Int) -> Unit,
        private val onRouteClicked: (item: CollectionPlaceWithPlace) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CollectionPlaceWithPlace) {
            val place = item.place

            binding.tvPlaceTitle.text = place.title
            binding.tvPlaceAddress.text = place.address ?: "Адрес не указан"

            val description = place.description?.trim().orEmpty()
            if (description.isBlank()) {
                binding.tvPlaceDescription.visibility = View.GONE
                binding.tvPlaceDescription.text = ""
            } else {
                binding.tvPlaceDescription.visibility = View.VISIBLE
                binding.tvPlaceDescription.text = description
            }

            binding.cbVisited.setOnCheckedChangeListener(null)
            binding.cbVisited.setOnClickListener(null)

            binding.cbVisited.isChecked = item.isVisited
            binding.cbVisited.isEnabled = !item.isVisited

            if (!item.isVisited) {
                binding.cbVisited.setOnClickListener {
                    binding.cbVisited.isChecked = false
                    onCheckInClicked(item)
                }
            }

            binding.btnRemovePlace.setOnClickListener {
                onRemoveClicked(item.linkId)
            }

            binding.btnRouteToPlace.setOnClickListener {
                onRouteClicked(item)
            }
        }
    }
}
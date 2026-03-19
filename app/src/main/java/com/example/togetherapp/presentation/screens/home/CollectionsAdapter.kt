package com.example.togetherapp.presentation.screens.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.R
import com.example.togetherapp.domain.models.CollectionModel

class CollectionsAdapter(
    private val collections: List<CollectionModel>
) : RecyclerView.Adapter<CollectionsAdapter.CollectionViewHolder>() {

    class CollectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.collectionName)
        val description: TextView = view.findViewById(R.id.collectionDescription)
        val numPlaces: TextView = view.findViewById(R.id.numPlaces)
        val accessType: TextView = view.findViewById(R.id.collectionAccessType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view, parent, false)
        return CollectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        val collection = collections[position]

        holder.title.text = collection.title
        holder.description.text = collection.description ?: ""

        holder.numPlaces.text = when (collection.placesCount) {
            1 -> "1 место"
            2, 3, 4 -> "${collection.placesCount} места"
            else -> "${collection.placesCount} мест"
        }

        holder.accessType.text = when (collection.access_type) {
            "private" -> "Приватная"
            "public" -> "Публичная"
            "friends" -> "Для друзей"
            else -> collection.access_type
        }
    }

    override fun getItemCount(): Int = collections.size
}
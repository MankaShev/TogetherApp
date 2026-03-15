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
    }

    override fun getItemCount(): Int {
        return collections.size
    }
}
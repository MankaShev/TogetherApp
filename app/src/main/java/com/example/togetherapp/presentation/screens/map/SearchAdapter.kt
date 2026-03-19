package com.example.togetherapp.presentation.screens.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.R
import com.yandex.mapkit.search.BusinessObjectMetadata
import com.yandex.mapkit.GeoObjectCollection

class SearchAdapter(
    private val onResultClick: (GeoObjectCollection.Item) -> Unit
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    // В этой версии SDK список результатов — это список GeoObjectCollection.Item
    private var items: List<GeoObjectCollection.Item> = emptyList()

    fun submitList(newList: List<GeoObjectCollection.Item>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val item = items[position]

        // Достаем сам GeoObject из Item
        val obj = item.obj
        val metadata = obj?.metadataContainer?.getItem(BusinessObjectMetadata::class.java)

        holder.tvName.text = metadata?.name ?: "Неизвестное место"
        holder.tvAddress.text = metadata?.address?.formattedAddress ?: ""

        holder.itemView.setOnClickListener { onResultClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvResultName)
        val tvAddress: TextView = view.findViewById(R.id.tvResultAddress)
    }
}
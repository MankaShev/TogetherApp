package com.example.togetherapp.presentation.screens.personalcollection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.R
import com.example.togetherapp.domain.models.CollectionModel

// Sealed class для элементов списка
sealed class ListItem {
    object CreateButton : ListItem()
    data class CollectionItem(val collection: CollectionModel) : ListItem()
}

class PersonalCollectionsAdapter(
    private val onCreateClick: () -> Unit,  // Для кнопки "Создать"
    private val onItemClick: (CollectionModel) -> Unit  // Для обычных подборок
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = listOf<ListItem>()

    fun submitList(collections: List<CollectionModel>) {
        // Всегда добавляем кнопку "Создать" в начало списка
        items = listOf(ListItem.CreateButton) + collections.map { ListItem.CollectionItem(it) }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.CreateButton -> 0
            is ListItem.CollectionItem -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            0 -> {
                val view = inflater.inflate(R.layout.item_create_button, parent, false)
                CreateButtonViewHolder(view, onCreateClick)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_collection, parent, false)
                CollectionViewHolder(view, onItemClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CreateButtonViewHolder -> holder.bind()
            is CollectionViewHolder -> {
                val item = items[position] as ListItem.CollectionItem
                holder.bind(item.collection)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // ViewHolder для кнопки "Создать подборку"
    class CreateButtonViewHolder(
        itemView: View,
        onClick: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val buttonTitle: TextView? = itemView.findViewById(R.id.tvTitle)

        init {
            itemView.setOnClickListener { onClick() }
        }

        fun bind() {
            // Если нужно, можно обновлять текст кнопки здесь
            buttonTitle?.text = "Создать подборку"
        }
    }

    // ViewHolder для обычной подборки
    class CollectionViewHolder(
        itemView: View,
        private val onItemClick: (CollectionModel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameView: TextView = itemView.findViewById(R.id.collectionName)
        private val descriptionView: TextView = itemView.findViewById(R.id.collectionDescription)
        private val placesView: TextView = itemView.findViewById(R.id.numPlaces)
        private val accessTypeView: TextView = itemView.findViewById(R.id.collectionAccessType)

        fun bind(collection: CollectionModel) {
            nameView.text = collection.title
            descriptionView.text = collection.description ?: ""

            placesView.text = when (collection.placesCount) {
                1 -> "1 место"
                2, 3, 4 -> "${collection.placesCount} места"
                else -> "${collection.placesCount} мест"
            }

            accessTypeView.text = when (collection.access_type) {
                "private" -> "Приватная"
                "public" -> "Публичная"
                "friends" -> "Для друзей"
                else -> collection.access_type
            }

            itemView.setOnClickListener {
                onItemClick(collection)
            }
        }
    }
}
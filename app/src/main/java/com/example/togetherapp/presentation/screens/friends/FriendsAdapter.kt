package com.example.togetherapp.presentation.screens.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.R
import com.example.togetherapp.domain.models.FriendUserItem

class FriendsAdapter(
    private val onFriendClick: (FriendUserItem) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private val items = mutableListOf<FriendUserItem>()

    fun submitList(newItems: List<FriendUserItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLogin: TextView = itemView.findViewById(R.id.tvLogin)

        fun bind(item: FriendUserItem) {
            tvLogin.text = item.login
            itemView.setOnClickListener {
                onFriendClick(item)
            }
        }
    }
}
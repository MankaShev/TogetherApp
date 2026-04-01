package com.example.togetherapp.presentation.screens.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.R
import com.example.togetherapp.domain.models.FriendRelationState
import com.example.togetherapp.domain.models.FriendUserItem

class SearchUsersAdapter(
    private val onAddClick: (FriendUserItem) -> Unit
) : RecyclerView.Adapter<SearchUsersAdapter.SearchUserViewHolder>() {

    private val items = mutableListOf<FriendUserItem>()

    fun submitList(newItems: List<FriendUserItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchUserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_user, parent, false)
        return SearchUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchUserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SearchUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvLogin: TextView = itemView.findViewById(R.id.tvLogin)
        private val btnAction: Button = itemView.findViewById(R.id.btnAction)

        fun bind(item: FriendUserItem) {
            tvLogin.text = item.login

            when (item.relationState) {
                FriendRelationState.NONE -> {
                    btnAction.text = "Добавить"
                    btnAction.isEnabled = true
                    btnAction.setOnClickListener { onAddClick(item) }
                }
                FriendRelationState.OUTGOING_REQUEST -> {
                    btnAction.text = "Отправлено"
                    btnAction.isEnabled = false
                }
                FriendRelationState.INCOMING_REQUEST -> {
                    btnAction.text = "Входящая"
                    btnAction.isEnabled = false
                }
                FriendRelationState.FRIENDS -> {
                    btnAction.text = "В друзьях"
                    btnAction.isEnabled = false
                }
            }
        }
    }
}
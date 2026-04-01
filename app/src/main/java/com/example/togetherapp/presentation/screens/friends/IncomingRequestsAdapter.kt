package com.example.togetherapp.presentation.screens.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.R
import com.example.togetherapp.domain.models.IncomingFriendRequestItem

class IncomingRequestsAdapter(
    private val onAcceptClick: (IncomingFriendRequestItem) -> Unit,
    private val onDeclineClick: (IncomingFriendRequestItem) -> Unit
) : RecyclerView.Adapter<IncomingRequestsAdapter.IncomingRequestViewHolder>() {

    private val items = mutableListOf<IncomingFriendRequestItem>()

    fun submitList(newItems: List<IncomingFriendRequestItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomingRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incoming_request, parent, false)
        return IncomingRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncomingRequestViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class IncomingRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvLogin: TextView = itemView.findViewById(R.id.tvLogin)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnDecline: Button = itemView.findViewById(R.id.btnDecline)

        fun bind(item: IncomingFriendRequestItem) {
            tvLogin.text = item.login
            btnAccept.setOnClickListener { onAcceptClick(item) }
            btnDecline.setOnClickListener { onDeclineClick(item) }
        }
    }
}
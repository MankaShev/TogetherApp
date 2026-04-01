package com.example.togetherapp.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CreateFriendRequest(
    val sender_id: Int,
    val receiver_id: Int,
    val status: String = "pending"
)
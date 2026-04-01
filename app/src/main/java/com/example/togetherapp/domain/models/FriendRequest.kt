package com.example.togetherapp.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    val id: Int,
    val sender_id: Int,
    val receiver_id: Int,
    val status: String,
    val created_at: String? = null,
    val updated_at: String? = null
) {
    fun statusEnum(): FriendRequestStatus {
        return FriendRequestStatus.fromDb(status)
    }
}
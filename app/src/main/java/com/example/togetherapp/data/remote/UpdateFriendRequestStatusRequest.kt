package com.example.togetherapp.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class UpdateFriendRequestStatusRequest(
    val status: String,
    val updated_at: String
)
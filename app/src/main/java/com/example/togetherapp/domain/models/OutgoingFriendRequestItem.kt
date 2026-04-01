package com.example.togetherapp.domain.models

data class OutgoingFriendRequestItem(
    val requestId: Int,
    val receiverId: Int,
    val login: String,
    val avatarUrl: String?
)
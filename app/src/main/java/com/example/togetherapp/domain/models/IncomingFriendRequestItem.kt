package com.example.togetherapp.domain.models

data class IncomingFriendRequestItem(
    val requestId: Int,
    val senderId: Int,
    val login: String,
    val avatarUrl: String?
)
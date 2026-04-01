package com.example.togetherapp.domain.models

data class FriendUserItem(
    val id: Int,
    val login: String,
    val avatarUrl: String?,
    val relationState: FriendRelationState
)
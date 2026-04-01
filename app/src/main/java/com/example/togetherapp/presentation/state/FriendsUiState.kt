package com.example.togetherapp.presentation.state

import com.example.togetherapp.domain.models.FriendUserItem
import com.example.togetherapp.domain.models.IncomingFriendRequestItem
import com.example.togetherapp.domain.models.OutgoingFriendRequestItem

data class FriendsUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FriendUserItem> = emptyList(),
    val incomingRequests: List<IncomingFriendRequestItem> = emptyList(),
    val outgoingRequests: List<OutgoingFriendRequestItem> = emptyList(),
    val friends: List<FriendUserItem> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSearchEmpty: Boolean = false,
    val isIncomingEmpty: Boolean = false,
    val isFriendsEmpty: Boolean = false
)
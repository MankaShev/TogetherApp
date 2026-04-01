package com.example.togetherapp.presentation.state

import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.models.User

data class FriendProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val collections: List<CollectionModel> = emptyList(),
    val errorMessage: String? = null,
    val isCollectionsEmpty: Boolean = false,
    val accessLabel: String = ""
)
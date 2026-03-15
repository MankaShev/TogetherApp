package com.example.togetherapp.presentation.state

import com.example.togetherapp.domain.models.Collection

sealed class CollectionsUiState {
    object Loading : CollectionsUiState()
    object Unauthorized : CollectionsUiState()
    object Empty : CollectionsUiState()
    data class Error(val message: String) : CollectionsUiState()
    data class Success(val collections: List<Collection>) : CollectionsUiState()
}
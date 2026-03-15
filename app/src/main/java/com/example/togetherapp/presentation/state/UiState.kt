package com.example.togetherapp.presentation.state

sealed class UiState<T> {

    class Loading : UiState<Nothing>()

    data class Success<T>(
        val data: T
    ) : UiState<T>()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : UiState<Nothing>()
}
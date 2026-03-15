package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.models.Collection
import com.example.togetherapp.presentation.state.CollectionsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CollectionsViewModel : ViewModel() {

    private val _state = MutableLiveData<CollectionsUiState>()
    val state: LiveData<CollectionsUiState> = _state

    // Флаг авторизации для демонстрации
    private var isUserLoggedIn = false // Поставьте true для теста с данными

    init {
        loadCollections()
    }

    fun loadCollections() {
        if (!isUserLoggedIn) {
            _state.value = CollectionsUiState.Unauthorized
            return
        }

        _state.value = CollectionsUiState.Loading

        viewModelScope.launch {
            delay(1500)

            // Тестовые данные
            val collections = listOf(
                Collection(1, "Томск", "Интересные места Томска"),
                Collection(2, "Завтраки", "Лучшие места для завтрака"),
                Collection(3, "Тюмень", "Достопримечательности Тюмени"),
                Collection(4, "Музеи", "Интересные музеи")
            )

            _state.value = CollectionsUiState.Success(collections)
        }
    }

    fun retry() {
        loadCollections()
    }

    fun login() {
        isUserLoggedIn = true
        loadCollections()
    }
}
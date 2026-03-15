package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.presentation.state.CollectionsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CollectionsViewModel : ViewModel() {

    private val _state = MutableLiveData<CollectionsUiState>()
    val state: LiveData<CollectionsUiState> = _state

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
                CollectionModel(
                    id = 1,
                    user_id = 101,
                    title = "Томск",
                    description = "Интересные места Томска",
                    access_type = "public",
                    created_at = "2026-03-15"
                ),
                CollectionModel(
                    id = 2,
                    user_id = 102,
                    title = "Завтраки",
                    description = "Лучшие места для завтрака",
                    access_type = "public",
                    created_at = "2026-03-15"
                ),
                CollectionModel(
                    id = 3,
                    user_id = 103,
                    title = "Тюмень",
                    description = "Достопримечательности Тюмени",
                    access_type = "public",
                    created_at = "2026-03-15"
                ),
                CollectionModel(
                    id = 4,
                    user_id = 104,
                    title = "Музеи",
                    description = "Интересные музеи",
                    access_type = "public",
                    created_at = "2026-03-15"
                )
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
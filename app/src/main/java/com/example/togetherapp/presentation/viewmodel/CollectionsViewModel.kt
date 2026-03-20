package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.repository.CollectionRepository
import com.example.togetherapp.presentation.state.CollectionsUiState
import kotlinx.coroutines.launch

class CollectionsViewModel(
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    private val _state = MutableLiveData<CollectionsUiState>()
    val state: LiveData<CollectionsUiState> = _state

    init {
        loadCollections()
    }

    fun loadCollections() {
        _state.value = CollectionsUiState.Loading

        viewModelScope.launch {
            try {

                val collectionsFromDb = collectionRepository.getCollections()

                if (collectionsFromDb.isNotEmpty()) {
                    _state.value = CollectionsUiState.Success(collectionsFromDb)
                } else {

                    // ТЕСТОВЫЕ ДАННЫЕ (если БД пустая)
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

            } catch (e: Exception) {

                // Если ошибка сети — тоже показываем тестовые данные
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
    }

    fun createCollection(name: String, description: String) {
        viewModelScope.launch {
            try {
                collectionRepository.createCollection(name, description)
                loadCollections()
            } catch (e: Exception) {
                _state.value = CollectionsUiState.Error(
                    e.message ?: "Ошибка при создании"
                )
            }
        }
    }
    fun retry() {
        loadCollections()
    }
    fun addPlaceToCollection(collectionId: Int) {
        // TODO: реализуем позже
    }
}
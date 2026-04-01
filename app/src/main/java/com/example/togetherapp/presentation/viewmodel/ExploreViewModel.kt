package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.presentation.state.UiState
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val repository: CollectionRepositoryImpl
) : ViewModel() {

    private val _collectionsState = MutableLiveData<UiState<List<CollectionModel>>>()
    val collectionsState: LiveData<UiState<List<CollectionModel>>> = _collectionsState

    private var currentQuery: String = ""

    fun loadPublicCollections() {
        viewModelScope.launch {
            _collectionsState.value = UiState.Loading
            try {
                val collections = repository.getPublicCollections()
                _collectionsState.value = UiState.Success(collections)
            } catch (e: Exception) {
                _collectionsState.value = UiState.Error(
                    e.message ?: "Ошибка загрузки публичных подборок"
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentQuery = query

        viewModelScope.launch {
            _collectionsState.value = UiState.Loading
            try {
                val result = if (query.isBlank()) {
                    repository.getPublicCollections()
                } else {
                    repository.searchPublicCollections(query)
                }
                _collectionsState.value = UiState.Success(result)
            } catch (e: Exception) {
                _collectionsState.value = UiState.Error(
                    e.message ?: "Ошибка поиска публичных подборок"
                )
            }
        }
    }

    fun retry() {
        onSearchQueryChanged(currentQuery)
    }
}
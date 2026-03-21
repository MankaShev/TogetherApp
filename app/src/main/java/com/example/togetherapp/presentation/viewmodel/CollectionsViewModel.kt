package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.models.SelectedPlace
import com.example.togetherapp.domain.repository.CollectionRepository
import com.example.togetherapp.domain.usecase.AddPlaceToCollectionUseCase
import com.example.togetherapp.presentation.state.CollectionsUiState
import kotlinx.coroutines.launch

class CollectionsViewModel(
    private val collectionRepository: CollectionRepository,
    private val addPlaceToCollectionUseCase: AddPlaceToCollectionUseCase
) : ViewModel() {

    private val _state = MutableLiveData<CollectionsUiState>()
    val state: LiveData<CollectionsUiState> = _state

    private val _addPlaceResult = MutableLiveData<AddPlaceResult?>()
    val addPlaceResult: LiveData<AddPlaceResult?> = _addPlaceResult

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
                    _state.value = CollectionsUiState.Empty
                }

            } catch (e: Exception) {
                _state.value = CollectionsUiState.Error(
                    e.message ?: "Ошибка загрузки коллекций"
                )
            }
        }
    }

    fun createCollection(name: String, description: String, accessType: String) {
        viewModelScope.launch {
            try {
                collectionRepository.createCollection(name, description, accessType)
                loadCollections()
            } catch (e: Exception) {
                _state.value = CollectionsUiState.Error(
                    e.message ?: "Ошибка при создании"
                )
            }
        }
    }

    data class AddPlaceResult(
        val collectionName: String,
        val collectionId: Int,
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    fun addPlaceToCollection(
        collectionId: Int,
        collectionName: String,
        place: SelectedPlace
    ) {
        viewModelScope.launch {
            try {
                addPlaceToCollectionUseCase.execute(collectionId, place)

                _addPlaceResult.value = AddPlaceResult(
                    collectionName = collectionName,
                    collectionId = collectionId,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _addPlaceResult.value = AddPlaceResult(
                    collectionName = collectionName,
                    collectionId = collectionId,
                    isSuccess = false,
                    errorMessage = e.message ?: "Ошибка"
                )
            }
        }
    }

    fun consumeAddPlaceResult() {
        _addPlaceResult.value = null
    }

    fun retry() {
        loadCollections()
    }
}
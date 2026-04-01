package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
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

                _state.value = if (collectionsFromDb.isNotEmpty()) {
                    CollectionsUiState.Success(collectionsFromDb)
                } else {
                    CollectionsUiState.Empty
                }
            } catch (e: Exception) {
                _state.value = CollectionsUiState.Error(
                    e.message ?: "Ошибка загрузки коллекций"
                )
            }
        }
    }

    fun createCollection(
        name: String,
        description: String,
        accessType: String
    ) {
        viewModelScope.launch {
            try {
                val normalizedName = name.trim()
                val normalizedDescription = description.trim()

                if (normalizedName.isBlank()) {
                    _state.value = CollectionsUiState.Error(
                        "Название подборки не может быть пустым"
                    )
                    return@launch
                }

                collectionRepository.createCollection(
                    name = normalizedName,
                    description = normalizedDescription,
                    accessType = accessType,
                    deadlineAt = null
                )

                loadCollections()
            } catch (e: Exception) {
                val message = when (e.message) {
                    CollectionRepositoryImpl.ERROR_COLLECTION_ALREADY_EXISTS ->
                        "Подборка с таким названием уже существует"
                    else ->
                        e.message ?: "Ошибка при создании подборки"
                }

                _state.value = CollectionsUiState.Error(message)
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
                    errorMessage = e.message ?: "Ошибка добавления места"
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
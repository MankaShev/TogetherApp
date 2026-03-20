package com.example.togetherapp.presentation.screens.personalcollection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.repository.CollectionRepository
import kotlinx.coroutines.launch

class CollectionDetailViewModel(
    private val repository: CollectionRepository
) : ViewModel() {

    private val _places = MutableLiveData<List<Place>>()
    val places: LiveData<List<Place>> = _places

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadPlaces(collectionId: Int) {
        viewModelScope.launch {
            try {
                val result = repository.getPlacesForCollection(collectionId)
                _places.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка загрузки мест"
            }
        }
    }
}
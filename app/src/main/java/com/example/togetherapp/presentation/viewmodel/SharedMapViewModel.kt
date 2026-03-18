package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.togetherapp.domain.models.Place

class SharedMapViewModel : ViewModel() {

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    private val _selectedPlace = MutableLiveData<Place?>()
    val selectedPlace: LiveData<Place?> = _selectedPlace

    sealed class NavigationEvent {
        data class BackToMapWithResult(
            val collectionName: String,
            val collectionId: Int
        ) : NavigationEvent()
    }

    fun onPlaceAddedToCollection(collectionName: String, collectionId: Int) {
        _navigationEvent.value = NavigationEvent.BackToMapWithResult(
            collectionName = collectionName,
            collectionId = collectionId
        )
    }

    fun setSelectedPlace(place: Place) {
        _selectedPlace.value = place
    }

    fun clearSelectedPlace() {
        _selectedPlace.value = null
    }

    fun consumeNavigationEvent() {
        _navigationEvent.value = null
    }
}
package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.togetherapp.domain.models.SelectedPlace

class SharedMapViewModel : ViewModel() {

    // Выбранное место на карте для добавления в подборку
    private val _selectedPlace = MutableLiveData<SelectedPlace?>()
    val selectedPlace: LiveData<SelectedPlace?> = _selectedPlace

    fun setSelectedPlace(place: SelectedPlace) {
        _selectedPlace.value = place
    }

    fun clearSelectedPlace() {
        _selectedPlace.value = null
    }

    // Одно место для построения маршрута
    private val _routePlace = MutableLiveData<SelectedPlace?>()
    val routePlace: LiveData<SelectedPlace?> = _routePlace

    fun setRoutePlace(place: SelectedPlace) {
        _routePlace.value = place
    }

    fun clearRoutePlace() {
        _routePlace.value = null
    }

    // Список мест для построения маршрута по всей подборке
    private val _routePlaces = MutableLiveData<List<SelectedPlace>>(emptyList())
    val routePlaces: LiveData<List<SelectedPlace>> = _routePlaces

    fun setRoutePlaces(places: List<SelectedPlace>) {
        _routePlaces.value = places
    }

    fun clearRoutePlaces() {
        _routePlaces.value = emptyList()
    }

    // Навигационные события между экранами
    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    fun onPlaceAddedToCollection(collectionName: String, collectionId: Int) {
        _navigationEvent.value = NavigationEvent.BackToMapWithResult(
            collectionName = collectionName,
            collectionId = collectionId
        )
    }

    fun consumeNavigationEvent() {
        _navigationEvent.value = null
    }

    sealed class NavigationEvent {
        data class BackToMapWithResult(
            val collectionName: String,
            val collectionId: Int
        ) : NavigationEvent()
    }
}
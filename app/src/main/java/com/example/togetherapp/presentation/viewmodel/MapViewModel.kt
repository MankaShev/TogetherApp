package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.usecase.AddPlaceToCollectionUseCase
import kotlinx.coroutines.launch
import java.util.UUID

class MapViewModel(
    private val addPlaceUseCase: AddPlaceToCollectionUseCase
) : ViewModel() {

    // Добавляем параметр address
    fun onPlaceSelected(name: String, address: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            val newPlace = Place(
                id = UUID.randomUUID(),
                name = name,
                latitude = lat,
                longitude = lon,
                address = address
            )
            addPlaceUseCase.execute(collectionId = 1, place = newPlace)
        }
    }
}
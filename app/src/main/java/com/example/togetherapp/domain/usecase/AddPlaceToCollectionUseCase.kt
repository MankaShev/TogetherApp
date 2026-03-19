package com.example.togetherapp.domain.usecase

import com.example.togetherapp.domain.models.SelectedPlace
import com.example.togetherapp.domain.repository.CollectionRepository

class AddPlaceToCollectionUseCase(
    private val repository: CollectionRepository
) {
    suspend fun execute(collectionId: Int, place: SelectedPlace) {
        repository.addPlace(collectionId, place)
    }
}
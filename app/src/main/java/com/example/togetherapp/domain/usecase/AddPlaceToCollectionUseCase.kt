package com.example.togetherapp.domain.usecase

import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.repository.CollectionRepository

class AddPlaceToCollectionUseCase(
    private val repository: CollectionRepository
) {
    suspend fun execute(collectionId: Int, place: Place) {
        repository.addPlace(collectionId, place)
    }
}
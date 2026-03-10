package com.example.togetherapp.domain.usecase

import com.example.togetherapp.domain.models.Collection
import com.example.togetherapp.domain.repository.CollectionRepository

class GetCollectionsUseCase(
    private val repository: CollectionRepository
) {
    suspend fun execute(): List<Collection> {
        return repository.getCollections()
    }
}
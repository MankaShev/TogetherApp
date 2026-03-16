package com.example.togetherapp.domain.usecase

import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.repository.CollectionRepository

class GetCollectionsUseCase(
    private val repository: CollectionRepository
) {
    suspend fun execute(): List<CollectionModel> {
        return repository.getCollections()
    }
}
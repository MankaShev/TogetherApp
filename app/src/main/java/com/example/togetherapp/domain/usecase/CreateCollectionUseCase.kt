package com.example.togetherapp.domain.usecase

import com.example.togetherapp.domain.repository.CollectionRepository

class CreateCollectionUseCase(
    private val repository: CollectionRepository
) {

    suspend fun execute(
        name: String,
        description: String
    ) {
        repository.createCollection(name, description)
    }
}
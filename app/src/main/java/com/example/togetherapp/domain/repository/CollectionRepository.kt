package com.example.togetherapp.domain.repository

import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.models.Place

interface CollectionRepository {

    suspend fun getCollections(): List<CollectionModel>

    suspend fun createCollection(
        name: String,
        description: String
    )

    suspend fun addPlace(
        collectionId: Int,
        place: Place
    )
}
package com.example.togetherapp.domain.repository

import com.example.togetherapp.domain.models.Collection
import com.example.togetherapp.domain.models.Place

interface CollectionRepository {

    suspend fun getCollections(): List<Collection>

    suspend fun createCollection(
        name: String,
        description: String
    )

    suspend fun addPlace(
        collectionId: Int,
        place: Place
    )
}
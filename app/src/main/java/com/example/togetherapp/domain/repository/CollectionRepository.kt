package com.example.togetherapp.domain.repository

import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.models.SelectedPlace

interface CollectionRepository {
    suspend fun getCollections(): List<CollectionModel>
    suspend fun createCollection(name: String, description: String, accessType: String)
    suspend fun addPlace(collectionId: Int, place: SelectedPlace)
}
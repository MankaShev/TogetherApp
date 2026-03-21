package com.example.togetherapp.domain.repository

import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.models.SelectedPlace
import  com.example.togetherapp.domain.models.Place

interface CollectionRepository {
    suspend fun getCollections(): List<CollectionModel>
    suspend fun createCollection(name: String, description: String, accessType: String)
    suspend fun addPlace(collectionId: Int, place: SelectedPlace)
    suspend fun getPlacesForCollection(collectionId: Int): List<Place>
}
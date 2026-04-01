package com.example.togetherapp.domain.repository

import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.models.CollectionPlace
import com.example.togetherapp.domain.models.CollectionProgress
import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.models.SelectedPlace

interface CollectionRepository {
    suspend fun getCollections(): List<CollectionModel>

    suspend fun getCollectionsByOwner(userId: Int): List<CollectionModel>

    suspend fun getUserCollectionsForViewer(
        ownerUserId: Int,
        viewerUserId: Int
    ): List<CollectionModel>

    suspend fun getCollectionById(collectionId: Int): CollectionModel?

    suspend fun createCollection(
        name: String,
        description: String,
        accessType: String,
        deadlineAt: String? = null
    )

    suspend fun updateCollection(
        collectionId: Int,
        title: String,
        description: String,
        accessType: String,
        deadlineAt: String? = null
    )

    suspend fun deleteCollection(collectionId: Int)

    suspend fun updateCollectionAccessType(
        collectionId: Int,
        accessType: String
    )

    suspend fun addPlace(
        collectionId: Int,
        place: SelectedPlace
    )

    suspend fun removePlaceFromCollection(linkId: Int)

    suspend fun setPlaceVisited(
        linkId: Int,
        isVisited: Boolean
    )

    suspend fun copyCollection(
        sourceCollectionId: Int,
        newTitle: String,
        newDescription: String,
        accessType: String,
        deadlineAt: String?
    )

    suspend fun ensureShareToken(collectionId: Int): String

    suspend fun getCollectionByShareToken(token: String): CollectionModel?

    suspend fun getCollectionPlaces(collectionId: Int): List<CollectionPlace>

    suspend fun getPlacesForCollection(collectionId: Int): List<Place>

    suspend fun getCollectionProgress(collectionId: Int): CollectionProgress
}
package com.example.togetherapp.data.mock

import com.example.togetherapp.domain.models.Collection
import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.repository.CollectionRepository

class MockCollectionRepository : CollectionRepository {

    private val collections = MockData.mockCollections.toMutableList()

    // отдельный словарь для хранения связей коллекций и мест
    private val collectionPlaces = mutableMapOf<Int, MutableList<Place>>().apply {
        this[1] = mutableListOf(MockData.mockPlaces[0])
        this[2] = mutableListOf(MockData.mockPlaces[1])
        this[3] = mutableListOf(MockData.mockPlaces[2])
        this[4] = mutableListOf()
    }

    override suspend fun getCollections(): List<Collection> {
        return collections
    }

    override suspend fun createCollection(
        name: String,
        description: String
    ) {
        val newId = (collections.maxOfOrNull { it.id } ?: 0) + 1
        val newCollection = Collection(
            id = newId,
            name = name,
            description = description
        )

        collections.add(newCollection)
        collectionPlaces[newId] = mutableListOf()
    }

    override suspend fun addPlace(
        collectionId: Int,
        place: Place
    ) {
        val places = collectionPlaces.getOrPut(collectionId) { mutableListOf() }

        val placeExists = places.any { it.id == place.id }
        if (!placeExists) {
            places.add(place)
        }
    }
    fun getPlacesForCollection(collectionId: Int): List<Place> {
        return collectionPlaces[collectionId] ?: emptyList()
    }
}
package com.example.togetherapp.data

import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.models.Collection
import com.example.togetherapp.domain.repository.CollectionRepository

class MyRepositoryImpl : CollectionRepository {

    // 1. Метод добавления места в коллекцию
    override suspend fun addPlace(collectionId: Int, place: Place) {
        println("Data Layer: Место ${place.name} добавлено в коллекцию $collectionId")
    }

    // 2. Метод для получения списков (заглушка)
    override suspend fun getCollections(): List<Collection> {
        println("Data Layer: Запрошен список коллекций")
        return emptyList() // Возвращаем пустой список, чтобы всё не сломалось
    }

    // 3. Метод для создания списка (заглушка)
    override suspend fun createCollection(name: String, description: String) {
        println("Data Layer: Создаем новую коллекцию: $name")
    }
}
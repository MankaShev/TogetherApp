package com.example.togetherapp.data.mock

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class MockRepositoryTest {

    @Test
    fun testGetCollections() = runBlocking {
        println("Тест 1: getCollections()")

        val repository = MockCollectionRepository()
        val collections = repository.getCollections()

        assertNotNull("Список коллекций не должен быть null", collections)
        assertFalse("Список коллекций не должен быть пустым", collections.isEmpty())
        assertEquals("Должно быть 4 коллекции", 4, collections.size)

        println("getCollections() прошел проверку: найдено ${collections.size} коллекций")
    }

    @Test
    fun testCreateCollection() = runBlocking {
        println("Тест 2: createCollection()")

        val repository = MockCollectionRepository()
        val initialSize = repository.getCollections().size

        repository.createCollection(
            name = "Тестовая коллекция",
            description = "Создано в тесте"
        )

        val newSize = repository.getCollections().size

        assertEquals("Размер должен увеличиться на 1", initialSize + 1, newSize)

        val collections = repository.getCollections()
        val lastCollection = collections.last()
        assertEquals("Имя коллекции должно совпадать", "Тестовая коллекция", lastCollection.name)
        assertEquals("Описание должно совпадать", "Создано в тесте", lastCollection.description)

        println("createCollection() прошел проверку: создана коллекция '${lastCollection.name}'")
    }

    @Test
    fun testAddPlace() = runBlocking {
        println("Тест 3: addPlace()")

        val repository = MockCollectionRepository()

        val collections = repository.getCollections()
        assertTrue("Должна быть хотя бы одна коллекция", collections.isNotEmpty())

        val collectionId = collections.first().id
        val placeToAdd = MockData.mockPlaces[0]

        repository.addPlace(
            collectionId = collectionId,
            place = placeToAdd
        )

        val placesInCollection = repository.getPlacesForCollection(collectionId)

        assertTrue("Место должно быть в коллекции", placesInCollection.contains(placeToAdd))
        assertTrue("В коллекции должно быть хотя бы одно место", placesInCollection.isNotEmpty())

        println("addPlace() прошел проверку: место '${placeToAdd.name}' добавлено в коллекцию")
        println("   Теперь в коллекции ${placesInCollection.size} мест")
    }

    @Test
    fun testAllMethodsTogether() = runBlocking {
        println("Тест всех методов")

        val repository = MockCollectionRepository()

        var collections = repository.getCollections()
        println("Начальное состояние: ${collections.size} коллекций")

        println("\nСоздаем новую коллекцию")
        repository.createCollection(
            name = "Мой маршрут выходного дня",
            description = "Интересные места для прогулки"
        )

        collections = repository.getCollections()
        assertEquals("Должно стать на 1 коллекцию больше", 5, collections.size)
        println("Коллекция создана успешно")

        println("\nДобавляем места")
        val newCollectionId = collections.last().id
        val placesToAdd = listOf(
            MockData.mockPlaces[0],
            MockData.mockPlaces[2],
            MockData.mockPlaces[4]
        )

        placesToAdd.forEach { place ->
            repository.addPlace(newCollectionId, place)
            println("   + Добавлено: ${place.name}")
        }

        val placesInCollection = repository.getPlacesForCollection(newCollectionId)
        assertEquals("Должно быть 3 места", 3, placesInCollection.size)
        println("\nВсе места добавлены успешно")

        println("\nИтоговый результат")
        println("   Всего коллекций: ${collections.size}")
        println("   Мест в новой коллекции: ${placesInCollection.size}")
        placesInCollection.forEachIndexed { index, place ->
            println("     ${index + 1}. ${place.name}")
        }

        println("\nВсе тесты успешные")
    }
}
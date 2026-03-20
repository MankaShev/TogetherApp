package com.example.togetherapp.data.repository

import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.models.SelectedPlace
import com.example.togetherapp.domain.repository.CollectionRepository
import com.example.togetherapp.domain.repository.PlaceRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class CollectionInsertDto(
    val user_id: Int,
    val title: String,
    val description: String,
    val access_type: String,
    val created_at: String
)

@Serializable
data class CollectionPlaceInsertDto(
    val collection_id: Int,
    val place_id: Int,
    val is_visited: Boolean,
    val note: String,
    val order_index: Int
)

@Serializable
data class CollectionPlaceDbDto(
    val id: Int,
    val collection_id: Int,
    val place_id: Int,
    val is_visited: Boolean,
    val note: String? = null,
    val order_index: Int
)

class CollectionRepositoryImpl(
    private val sessionManager: SessionManager,
    private val placeRepository: PlaceRepository
) : CollectionRepository {

    override suspend fun getCollections(): List<CollectionModel> {
        val userId = sessionManager.getUserId()
        if (userId == -1) return emptyList()

        val collections = SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()
            .filter { it.user_id == userId }

        val collectionPlaces = SupabaseClient.supabase
            .from("collection_places")
            .select()
            .decodeList<CollectionPlaceDbDto>()

        val placesCountMap = collectionPlaces
            .groupingBy { it.collection_id }
            .eachCount()

        return collections.map { collection ->
            collection.copy(
                placesCount = placesCountMap[collection.id] ?: 0
            )
        }
    }

    override suspend fun createCollection(name: String, description: String, accessType: String) {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        val dto = CollectionInsertDto(
            user_id = userId,
            title = name,
            description = description,
            access_type = accessType,
            created_at = System.currentTimeMillis().toString()
        )

        SupabaseClient.supabase
            .from("collections")
            .insert(dto)
    }

    override suspend fun addPlace(collectionId: Int, place: SelectedPlace) {
        try {
            // 1. Получаем или создаем место в таблице places
            val savedPlace = placeRepository.getOrCreatePlace(place)

            // 2. Проверяем, есть ли уже связь
            val existingLinks = SupabaseClient.supabase
                .from("collection_places")
                .select()
                .decodeList<CollectionPlaceDbDto>()
                .filter {
                    it.collection_id == collectionId &&
                            it.place_id == savedPlace.id
                }

            if (existingLinks.isNotEmpty()) return

            // 3. Создаем связь
            val dto = CollectionPlaceInsertDto(
                collection_id = collectionId,
                place_id = savedPlace.id,
                is_visited = false,
                note = "",
                order_index = 0
            )

            SupabaseClient.supabase
                .from("collection_places")
                .insert(dto)

        } catch (e: Exception) {
            println("Ошибка при добавлении места: ${e.message}")
            throw e
        }
    }
}
package com.example.togetherapp.data.repository

import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.repository.CollectionRepository
import io.github.jan.supabase.postgrest.from

class CollectionRepositoryImpl(
    private val sessionManager: SessionManager
) : CollectionRepository {

    override suspend fun getCollections(): List<CollectionModel> {

        val userId = sessionManager.getUserId()
        if (userId == -1) return emptyList()

        val collections = SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()

        // фильтрация по пользователю
        return collections.filter { it.user_id == userId }
    }

    override suspend fun createCollection(
        name: String,
        description: String
    ) {

        val userId = sessionManager.getUserId()
        if (userId == -1) return

        SupabaseClient.supabase
            .from("collections")
            .insert(
                mapOf(
                    "user_id" to userId,
                    "title" to name,
                    "description" to description,
                    "access_type" to "private",
                    "created_at" to System.currentTimeMillis().toString()
                )
            )
    }

    override suspend fun addPlace(
        collectionId: Int,
        place: Place
    ) {

        SupabaseClient.supabase
            .from("collection_places")
            .insert(
                mapOf(
                    "collection_id" to collectionId,
                    "place_id" to place.id,
                    "is_visited" to false,
                    "note" to "",
                    "order_index" to 0
                )
            )
    }
}
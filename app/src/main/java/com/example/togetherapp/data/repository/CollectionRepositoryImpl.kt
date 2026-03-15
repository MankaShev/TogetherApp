package com.example.togetherapp.data.repository

import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.repository.CollectionRepository
import io.github.jan.supabase.postgrest.from

class CollectionRepositoryImpl : CollectionRepository {

    override suspend fun getCollections(): List<CollectionModel> {

        return SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList()

    }

    override suspend fun createCollection(
        name: String,
        description: String
    ) {

        SupabaseClient.supabase
            .from("collections")
            .insert(
                mapOf(
                    "title" to name,
                    "description" to description
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
                    "place_id" to place.id
                )
            )

    }
}
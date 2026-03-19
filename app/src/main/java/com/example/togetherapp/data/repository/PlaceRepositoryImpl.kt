package com.example.togetherapp.data.repository

import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.models.SelectedPlace
import com.example.togetherapp.domain.repository.PlaceRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
data class PlaceInsertDto(
    val external_id: Int? = null,
    val title: String,
    val description: String? = null,
    val longitude: Double,
    val latitude: Double,
    val address: String? = null
)

class PlaceRepositoryImpl : PlaceRepository {

    override suspend fun getOrCreatePlace(place: SelectedPlace): Place {
        val existingPlaces = SupabaseClient.supabase
            .from("places")
            .select()
            .decodeList<Place>()
            .filter {
                it.external_id == place.external_id &&
                        it.title == place.title
            }

        if (existingPlaces.isNotEmpty()) {
            return existingPlaces.first()
        }

        val dto = PlaceInsertDto(
            external_id = place.external_id,
            title = place.title,
            description = place.description,
            longitude = place.longitude,
            latitude = place.latitude,
            address = place.address
        )

        val insertedPlaces = SupabaseClient.supabase
            .from("places")
            .insert(dto) {
                select()
            }
            .decodeList<Place>()

        return insertedPlaces.firstOrNull()
            ?: throw IllegalStateException("Не удалось создать место в таблице places")
    }
}
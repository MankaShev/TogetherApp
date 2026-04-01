package com.example.togetherapp.data.repository

import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.domain.models.CollectionPlace
import com.example.togetherapp.domain.models.CollectionProgress
import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.models.SelectedPlace
import com.example.togetherapp.domain.repository.CollectionRepository
import com.example.togetherapp.domain.repository.FriendRepository
import com.example.togetherapp.domain.repository.PlaceRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class ShareTokenUpdateDto(
    val share_token: String
)

@Serializable
data class CollectionInsertDto(
    val user_id: Int,
    val title: String,
    val description: String,
    val access_type: String,
    val created_at: String,
    val updated_at: String,
    val deadline_at: String? = null
)

@Serializable
data class CollectionUpdateDto(
    val title: String,
    val description: String,
    val access_type: String,
    val updated_at: String,
    val deadline_at: String? = null
)

@Serializable
data class CollectionAccessUpdateDto(
    val access_type: String,
    val updated_at: String
)

@Serializable
data class CollectionPlaceInsertDto(
    val collection_id: Int,
    val place_id: Int,
    val is_visited: Boolean = false,
    val note: String? = null,
    val order_index: Int = 0
)

@Serializable
data class CollectionPlaceUpdateDto(
    val is_visited: Boolean
)

class CollectionRepositoryImpl(
    private val sessionManager: SessionManager,
    private val placeRepository: PlaceRepository,
    private val friendRepository: FriendRepository = FriendRepositoryImpl()
) : CollectionRepository {

    companion object {
        const val ERROR_COLLECTION_ALREADY_EXISTS = "COLLECTION_ALREADY_EXISTS"
    }

    private fun nowIso(): String = Instant.now().toString()

    override suspend fun getCollections(): List<CollectionModel> {
        val userId = sessionManager.getUserId()
        if (userId == -1) return emptyList()

        return getCollectionsByOwner(userId)
    }

    override suspend fun getCollectionsByOwner(userId: Int): List<CollectionModel> {
        val collections = SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()
            .filter { it.user_id == userId }

        return attachCollectionStats(collections)
    }

    override suspend fun getUserCollectionsForViewer(
        ownerUserId: Int,
        viewerUserId: Int
    ): List<CollectionModel> {
        val ownerCollections = getCollectionsByOwner(ownerUserId)

        if (ownerUserId == viewerUserId) {
            return ownerCollections
        }

        val areFriends = friendRepository.areFriends(ownerUserId, viewerUserId)

        val filtered = ownerCollections.filter { collection ->
            when (collection.access_type.lowercase()) {
                "public" -> true
                "friends" -> areFriends
                "private" -> false
                else -> false
            }
        }

        return filtered
    }

    override suspend fun getCollectionById(collectionId: Int): CollectionModel? {
        val collections = SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()
            .filter { it.id == collectionId }

        val collection = collections.firstOrNull() ?: return null
        return attachCollectionStats(listOf(collection)).firstOrNull()
    }

    override suspend fun ensureShareToken(collectionId: Int): String {
        val collections = SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()
            .filter { it.id == collectionId }

        val collection = collections.firstOrNull()
            ?: throw IllegalStateException("Подборка не найдена")

        val existingToken = collection.share_token
        if (!existingToken.isNullOrBlank()) {
            return existingToken
        }

        val newToken = UUID.randomUUID().toString()

        val dto = ShareTokenUpdateDto(
            share_token = newToken
        )

        SupabaseClient.supabase
            .from("collections")
            .update(dto) {
                filter {
                    eq("id", collectionId)
                }
            }

        return newToken
    }

    override suspend fun getCollectionByShareToken(token: String): CollectionModel? {
        val collections = SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()
            .filter { it.share_token == token }

        val collection = collections.firstOrNull() ?: return null
        return attachCollectionStats(listOf(collection)).firstOrNull()
    }

    override suspend fun createCollection(
        name: String,
        description: String,
        accessType: String,
        deadlineAt: String?
    ) {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        val normalizedTitle = name.trim()

        if (normalizedTitle.isBlank()) {
            throw IllegalArgumentException("Название подборки не может быть пустым")
        }

        val alreadyExists = hasCollectionWithTitle(
            userId = userId,
            title = normalizedTitle
        )

        if (alreadyExists) {
            throw IllegalStateException(ERROR_COLLECTION_ALREADY_EXISTS)
        }

        val now = nowIso()

        val dto = CollectionInsertDto(
            user_id = userId,
            title = normalizedTitle,
            description = description,
            access_type = accessType,
            created_at = now,
            updated_at = now,
            deadline_at = deadlineAt
        )

        SupabaseClient.supabase
            .from("collections")
            .insert(dto)
    }

    override suspend fun updateCollection(
        collectionId: Int,
        title: String,
        description: String,
        accessType: String,
        deadlineAt: String?
    ) {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        val normalizedTitle = title.trim()

        if (normalizedTitle.isBlank()) {
            throw IllegalArgumentException("Название подборки не может быть пустым")
        }

        val alreadyExists = hasCollectionWithTitle(
            userId = userId,
            title = normalizedTitle,
            excludeCollectionId = collectionId
        )

        if (alreadyExists) {
            throw IllegalStateException(ERROR_COLLECTION_ALREADY_EXISTS)
        }

        val dto = CollectionUpdateDto(
            title = normalizedTitle,
            description = description,
            access_type = accessType,
            updated_at = nowIso(),
            deadline_at = deadlineAt
        )

        SupabaseClient.supabase
            .from("collections")
            .update(dto) {
                filter {
                    eq("id", collectionId)
                }
            }
    }

    override suspend fun deleteCollection(collectionId: Int) {
        SupabaseClient.supabase
            .from("collections")
            .delete {
                filter {
                    eq("id", collectionId)
                }
            }
    }

    override suspend fun updateCollectionAccessType(
        collectionId: Int,
        accessType: String
    ) {
        val dto = CollectionAccessUpdateDto(
            access_type = accessType,
            updated_at = nowIso()
        )

        SupabaseClient.supabase
            .from("collections")
            .update(dto) {
                filter {
                    eq("id", collectionId)
                }
            }
    }

    override suspend fun addPlace(
        collectionId: Int,
        place: SelectedPlace
    ) {
        try {
            val savedPlace = placeRepository.getOrCreatePlace(place)

            val existingLinks = SupabaseClient.supabase
                .from("collection_places")
                .select()
                .decodeList<CollectionPlace>()
                .filter {
                    it.collection_id == collectionId &&
                            it.place_id == savedPlace.id
                }

            if (existingLinks.isNotEmpty()) return

            val dto = CollectionPlaceInsertDto(
                collection_id = collectionId,
                place_id = savedPlace.id,
                is_visited = false,
                note = null,
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

    override suspend fun removePlaceFromCollection(linkId: Int) {
        SupabaseClient.supabase
            .from("collection_places")
            .delete {
                filter {
                    eq("id", linkId)
                }
            }
    }

    override suspend fun setPlaceVisited(
        linkId: Int,
        isVisited: Boolean
    ) {
        val dto = CollectionPlaceUpdateDto(
            is_visited = isVisited
        )

        SupabaseClient.supabase
            .from("collection_places")
            .update(dto) {
                filter {
                    eq("id", linkId)
                }
            }
    }

    override suspend fun copyCollection(
        sourceCollectionId: Int,
        newTitle: String,
        newDescription: String,
        accessType: String,
        deadlineAt: String?
    ) {
        val userId = sessionManager.getUserId()
        if (userId == -1) return

        val normalizedTitle = newTitle.trim()

        if (normalizedTitle.isBlank()) {
            throw IllegalArgumentException("Название подборки не может быть пустым")
        }

        val alreadyExists = hasCollectionWithTitle(
            userId = userId,
            title = normalizedTitle
        )

        if (alreadyExists) {
            throw IllegalStateException(ERROR_COLLECTION_ALREADY_EXISTS)
        }

        val now = nowIso()

        val newCollectionDto = CollectionInsertDto(
            user_id = userId,
            title = normalizedTitle,
            description = newDescription,
            access_type = accessType,
            created_at = now,
            updated_at = now,
            deadline_at = deadlineAt
        )

        SupabaseClient.supabase
            .from("collections")
            .insert(newCollectionDto)

        val allCollections = SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()

        val newCollection = allCollections
            .filter { it.user_id == userId && it.title == normalizedTitle }
            .maxByOrNull { it.id }
            ?: throw IllegalStateException("Не удалось найти созданную копию подборки")

        val sourceLinks = SupabaseClient.supabase
            .from("collection_places")
            .select()
            .decodeList<CollectionPlace>()
            .filter { it.collection_id == sourceCollectionId }
            .sortedBy { it.order_index }

        sourceLinks.forEach { sourceLink ->
            val dto = CollectionPlaceInsertDto(
                collection_id = newCollection.id,
                place_id = sourceLink.place_id,
                is_visited = false,
                note = sourceLink.note,
                order_index = sourceLink.order_index
            )

            SupabaseClient.supabase
                .from("collection_places")
                .insert(dto)
        }
    }

    override suspend fun getCollectionPlaces(collectionId: Int): List<CollectionPlace> {
        return SupabaseClient.supabase
            .from("collection_places")
            .select()
            .decodeList<CollectionPlace>()
            .filter { it.collection_id == collectionId }
            .sortedBy { it.order_index }
    }

    override suspend fun getPlacesForCollection(collectionId: Int): List<Place> {
        val links = getCollectionPlaces(collectionId)

        if (links.isEmpty()) return emptyList()

        val allPlaces = SupabaseClient.supabase
            .from("places")
            .select()
            .decodeList<Place>()

        val placeIds = links.map { it.place_id }.toSet()

        return allPlaces.filter { it.id in placeIds }
    }

    override suspend fun getCollectionProgress(collectionId: Int): CollectionProgress {
        val links = getCollectionPlaces(collectionId)

        val totalCount = links.size
        val visitedCount = links.count { it.is_visited }

        return CollectionProgress(
            totalCount = totalCount,
            visitedCount = visitedCount
        )
    }

    suspend fun getPublicCollections(): List<CollectionModel> {
        val collections = SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()
            .filter { it.access_type == "public" }

        return attachCollectionStats(collections)
    }

    suspend fun searchPublicCollections(query: String): List<CollectionModel> {
        val normalizedQuery = query.trim().lowercase()

        val collections = SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()
            .filter {
                it.access_type == "public" &&
                        it.title.lowercase().contains(normalizedQuery)
            }

        return attachCollectionStats(collections)
    }

    private suspend fun attachCollectionStats(
        collections: List<CollectionModel>
    ): List<CollectionModel> {
        val collectionPlaces = SupabaseClient.supabase
            .from("collection_places")
            .select()
            .decodeList<CollectionPlace>()

        val placesCountMap = collectionPlaces
            .groupingBy { it.collection_id }
            .eachCount()

        val visitedPlacesCountMap = collectionPlaces
            .filter { it.is_visited }
            .groupingBy { it.collection_id }
            .eachCount()

        return collections.map { collection ->
            collection.copy(
                placesCount = placesCountMap[collection.id] ?: 0,
                visitedPlacesCount = visitedPlacesCountMap[collection.id] ?: 0
            )
        }
    }

    private suspend fun hasCollectionWithTitle(
        userId: Int,
        title: String,
        excludeCollectionId: Int? = null
    ): Boolean {
        val normalizedTitle = title.trim().lowercase()

        return SupabaseClient.supabase
            .from("collections")
            .select()
            .decodeList<CollectionModel>()
            .any { collection ->
                collection.user_id == userId &&
                        collection.id != excludeCollectionId &&
                        collection.title.trim().lowercase() == normalizedTitle
            }
    }
}
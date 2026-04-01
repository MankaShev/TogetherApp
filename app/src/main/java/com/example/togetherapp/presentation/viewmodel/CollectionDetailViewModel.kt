package com.example.togetherapp.presentation.screens.personalcollection

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.models.CollectionPlaceWithPlace
import com.example.togetherapp.domain.models.CollectionProgress
import com.example.togetherapp.domain.repository.CollectionRepository
import kotlinx.coroutines.launch

class CollectionDetailViewModel(
    private val repository: CollectionRepository
) : ViewModel() {

    companion object {
        private const val CHECK_IN_RADIUS_METERS = 100f
    }

    private val _places = MutableLiveData<List<CollectionPlaceWithPlace>>(emptyList())
    val places: LiveData<List<CollectionPlaceWithPlace>> = _places

    private val _progress = MutableLiveData(CollectionProgress(0, 0))
    val progress: LiveData<CollectionProgress> = _progress

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _collectionUpdated = MutableLiveData<Boolean?>(null)
    val collectionUpdated: LiveData<Boolean?> = _collectionUpdated

    private val _collectionDeleted = MutableLiveData<Boolean?>(null)
    val collectionDeleted: LiveData<Boolean?> = _collectionDeleted

    private val _collectionCopied = MutableLiveData<Boolean?>(null)
    val collectionCopied: LiveData<Boolean?> = _collectionCopied

    private val _shareLink = MutableLiveData<String?>(null)
    val shareLink: LiveData<String?> = _shareLink

    fun loadCollectionData(collectionId: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val collectionPlaces = repository.getCollectionPlaces(collectionId)
                val places = repository.getPlacesForCollection(collectionId)

                val merged = collectionPlaces.mapNotNull { link ->
                    val place = places.firstOrNull { it.id == link.place_id } ?: return@mapNotNull null

                    CollectionPlaceWithPlace(
                        linkId = link.id,
                        isVisited = link.is_visited,
                        note = link.note,
                        orderIndex = link.order_index,
                        place = place
                    )
                }

                _places.value = merged
                _progress.value = repository.getCollectionProgress(collectionId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка загрузки подборки"
            } finally {
                _loading.value = false
            }
        }
    }

    fun tryCheckIn(
        item: CollectionPlaceWithPlace,
        collectionId: Int,
        userLatitude: Double,
        userLongitude: Double
    ) {
        val placeLatitude = item.place.latitude
        val placeLongitude = item.place.longitude

        if (placeLatitude == null || placeLongitude == null) {
            _error.value = "У этого места нет координат"
            return
        }

        if (item.isVisited) {
            _error.value = "Это место уже отмечено как посещённое"
            return
        }

        val distanceMeters = calculateDistanceMeters(
            startLat = userLatitude,
            startLon = userLongitude,
            endLat = placeLatitude,
            endLon = placeLongitude
        )

        if (distanceMeters <= CHECK_IN_RADIUS_METERS) {
            markPlaceVisited(
                linkId = item.linkId,
                collectionId = collectionId
            )
        } else {
            _error.value =
                "Вы слишком далеко от места. Сейчас расстояние примерно ${distanceMeters.toInt()} м."
        }
    }

    private fun markPlaceVisited(linkId: Int, collectionId: Int) {
        viewModelScope.launch {
            try {
                repository.setPlaceVisited(
                    linkId = linkId,
                    isVisited = true
                )
                loadCollectionData(collectionId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка отметки посещения"
            }
        }
    }

    private fun calculateDistanceMeters(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLon, endLat, endLon, results)
        return results[0]
    }

    fun removePlace(linkId: Int, collectionId: Int) {
        viewModelScope.launch {
            try {
                repository.removePlaceFromCollection(linkId)
                loadCollectionData(collectionId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка удаления места"
            }
        }
    }

    fun updateCollection(
        collectionId: Int,
        title: String,
        description: String,
        accessType: String,
        deadlineAt: String?
    ) {
        viewModelScope.launch {
            try {
                repository.updateCollection(
                    collectionId = collectionId,
                    title = title,
                    description = description,
                    accessType = accessType,
                    deadlineAt = deadlineAt
                )
                _collectionUpdated.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка обновления подборки"
            }
        }
    }

    fun updateCollectionAccessType(
        collectionId: Int,
        accessType: String
    ) {
        viewModelScope.launch {
            try {
                repository.updateCollectionAccessType(collectionId, accessType)
                _collectionUpdated.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка изменения приватности"
            }
        }
    }

    fun deleteCollection(collectionId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteCollection(collectionId)
                _collectionDeleted.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка удаления подборки"
            }
        }
    }

    fun copyCollection(
        sourceCollectionId: Int,
        newTitle: String,
        newDescription: String,
        accessType: String,
        deadlineAt: String?
    ) {
        viewModelScope.launch {
            try {
                repository.copyCollection(
                    sourceCollectionId = sourceCollectionId,
                    newTitle = newTitle,
                    newDescription = newDescription,
                    accessType = accessType,
                    deadlineAt = deadlineAt
                )
                _collectionCopied.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка копирования подборки"
            }
        }
    }

    fun generateShareLink(collectionId: Int) {
        viewModelScope.launch {
            try {
                val token = repository.ensureShareToken(collectionId)
                _shareLink.value = "togetherapp://collection/$token"
            } catch (e: Exception) {
                _error.value = e.message ?: "Не удалось создать ссылку"
            }
        }
    }

    fun clearUpdateState() {
        _collectionUpdated.value = null
    }

    fun clearDeleteState() {
        _collectionDeleted.value = null
    }

    fun clearCopyState() {
        _collectionCopied.value = null
    }

    fun clearShareLink() {
        _shareLink.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
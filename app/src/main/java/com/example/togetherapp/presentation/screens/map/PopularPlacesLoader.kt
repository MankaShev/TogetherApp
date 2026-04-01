package com.example.togetherapp.presentation.screens.map

import com.example.togetherapp.domain.models.SelectedPlace
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.BusinessObjectMetadata
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import java.util.concurrent.atomic.AtomicInteger

class PopularPlacesLoader(
    private val searchManager: SearchManager
) {

    private val categories = listOf(
        "кафе",
        "музей",
        "парк",
        "достопримечательность"
    )

    private val activeSessions = mutableListOf<Session>()

    fun cancel() {
        activeSessions.forEach { it.cancel() }
        activeSessions.clear()
    }

    fun loadPopularPlaces(
        center: Point,
        onSuccess: (List<SelectedPlace>) -> Unit,
        onError: (String) -> Unit
    ) {
        cancel()

        val boundingBox = BoundingBox(
            Point(center.latitude - 0.03, center.longitude - 0.03),
            Point(center.latitude + 0.03, center.longitude + 0.03)
        )

        val geometry = Geometry.fromBoundingBox(boundingBox)

        val options = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 10
            userPosition = center
        }

        val collected = mutableListOf<SelectedPlace>()
        val pendingRequests = AtomicInteger(categories.size)
        var hasAnySuccess = false

        categories.forEach { query ->
            val session = searchManager.submit(
                query,
                geometry,
                options,
                object : Session.SearchListener {
                    override fun onSearchResponse(response: Response) {
                        hasAnySuccess = true
                        collected += mapResponseToPlaces(response)
                        finishIfNeeded(
                            pendingRequests = pendingRequests,
                            collected = collected,
                            onSuccess = onSuccess,
                            onError = onError,
                            hasAnySuccess = hasAnySuccess
                        )
                    }

                    override fun onSearchError(error: com.yandex.runtime.Error) {
                        finishIfNeeded(
                            pendingRequests = pendingRequests,
                            collected = collected,
                            onSuccess = onSuccess,
                            onError = onError,
                            hasAnySuccess = hasAnySuccess
                        )
                    }
                }
            )

            activeSessions.add(session)
        }
    }

    private fun finishIfNeeded(
        pendingRequests: AtomicInteger,
        collected: List<SelectedPlace>,
        onSuccess: (List<SelectedPlace>) -> Unit,
        onError: (String) -> Unit,
        hasAnySuccess: Boolean
    ) {
        if (pendingRequests.decrementAndGet() != 0) return

        activeSessions.clear()

        if (!hasAnySuccess) {
            onError("Не удалось загрузить популярные места")
            return
        }

        val uniquePlaces = deduplicatePlaces(collected).take(20)
        onSuccess(uniquePlaces)
    }

    private fun mapResponseToPlaces(response: Response): List<SelectedPlace> {
        return response.collection.children.mapNotNull { child ->
            val obj = child.obj ?: return@mapNotNull null
            val point = obj.geometry.firstOrNull()?.point ?: return@mapNotNull null

            val metadata = obj.metadataContainer
                ?.getItem(BusinessObjectMetadata::class.java)
                ?: return@mapNotNull null

            val oid = try {
                metadata.getOid()
            } catch (e: Exception) {
                null
            }

            val externalId = if (!oid.isNullOrBlank()) {
                oid.hashCode()
            } else {
                (point.latitude * point.longitude).toInt()
            }

            SelectedPlace(
                external_id = externalId,
                title = metadata.name,
                latitude = point.latitude,
                longitude = point.longitude,
                description = null,
                address = metadata.address.formattedAddress
            )
        }
    }

    private fun deduplicatePlaces(items: List<SelectedPlace>): List<SelectedPlace> {
        val map = linkedMapOf<String, SelectedPlace>()

        items.forEach { place ->
            val key = buildString {
                append(place.title.trim().lowercase())
                append("|")
                append(String.format("%.5f", place.latitude))
                append("|")
                append(String.format("%.5f", place.longitude))
            }

            if (!map.containsKey(key)) {
                map[key] = place
            }
        }

        return map.values.toList()
    }
}
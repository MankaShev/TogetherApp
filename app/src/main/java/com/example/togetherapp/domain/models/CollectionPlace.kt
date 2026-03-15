package com.example.togetherapp.domain.models
import kotlinx.serialization.Serializable

@Serializable
data class CollectionPlace(
    val id: Int,
    val collection_id: Int,
    val place_id: Int,
    val is_visited: Boolean,
    val note: String?,
    val order_index: Int
)
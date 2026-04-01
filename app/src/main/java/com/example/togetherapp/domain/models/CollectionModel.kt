package com.example.togetherapp.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CollectionModel(
    val id: Int,
    val user_id: Int,
    val title: String,
    val description: String? = null,
    val access_type: String,          // private / public / friends
    val created_at: String,
    val updated_at: String? = null,
    val deadline_at: String? = null,
    val share_token: String? = null,
    val placesCount: Int = 0,
    val visitedPlacesCount: Int = 0
)
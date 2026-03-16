package com.example.togetherapp.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CollectionModel(
    val id: Int,
    val user_id: Int,
    val title: String,
    val description: String?,
    val access_type: String,
    val created_at: String
)

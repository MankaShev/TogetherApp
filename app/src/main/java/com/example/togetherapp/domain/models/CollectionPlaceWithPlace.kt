package com.example.togetherapp.domain.models

data class CollectionPlaceWithPlace(
    val linkId: Int,
    val isVisited: Boolean,
    val note: String?,
    val orderIndex: Int,
    val place: Place
)
package com.example.togetherapp.domain.models

data class SelectedPlace(
    val external_id: Int? = null,
    val title: String,
    val longitude: Double,
    val latitude: Double,
    val description: String? = null,
    val address: String? = null
)
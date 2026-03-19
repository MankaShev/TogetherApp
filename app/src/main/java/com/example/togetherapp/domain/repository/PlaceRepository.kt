package com.example.togetherapp.domain.repository

import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.domain.models.SelectedPlace

interface PlaceRepository {
    suspend fun getOrCreatePlace(place: SelectedPlace): Place
}
package com.example.togetherapp.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Interest(
    val id: Int,
    val name: String
)
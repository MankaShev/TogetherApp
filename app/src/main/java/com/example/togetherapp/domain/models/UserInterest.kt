package com.example.togetherapp.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class UserInterest(
    val id: Int,
    val user_id: Int,
    val interest_id: Int
)
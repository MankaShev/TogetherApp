package com.example.togetherapp.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class UserInterestRequest(
    val user_id: Int,
    val interest_id: Int
)
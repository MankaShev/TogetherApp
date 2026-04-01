package com.example.togetherapp.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RegisterUserRequest(
    val login: String,
    val password: String,
    val avatar_url: String? = null,
    val is_onboarding_completed: Boolean = false
)
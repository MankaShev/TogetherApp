package com.example.togetherapp.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val login: String,
    val password: String,
    val avatar_url: String? = null,
    val is_onboarding_completed: Boolean = false
)
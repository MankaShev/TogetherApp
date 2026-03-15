package com.example.togetherapp.domain.models

data class User(
    val id: Int,
    val login: String,
    val password: String,
    val avatar_url: String?,
    val is_onboarding_completed: Boolean
)
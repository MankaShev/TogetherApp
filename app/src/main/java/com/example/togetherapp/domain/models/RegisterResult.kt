package com.example.togetherapp.domain.models

sealed class RegisterResult {
    data class Success(val user: User) : RegisterResult()
    object UserAlreadyExists : RegisterResult()
    object NetworkError : RegisterResult()
    data class UnknownError(val message: String) : RegisterResult()
}
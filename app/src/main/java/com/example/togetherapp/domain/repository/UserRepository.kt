package com.example.togetherapp.domain.repository
import com.example.togetherapp.domain.models.User
interface UserRepository {
    suspend fun login(login: String, password: String): User?
    suspend fun getUserById(userId: Int): User?
    suspend fun registerUser(login: String, password: String, avatarUrl: String? = null): User?
}
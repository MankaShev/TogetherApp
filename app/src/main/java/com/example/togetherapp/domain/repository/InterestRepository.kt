package com.example.togetherapp.domain.repository

import com.example.togetherapp.domain.models.Interest

interface InterestRepository {
    suspend fun getAllInterests(): List<Interest>
    suspend fun getUserInterests(userId: Int): List<Interest>
    suspend fun saveUserInterests(userId: Int, interestIds: List<Int>): Boolean
    suspend fun markOnboardingCompleted(userId: Int): Boolean
}
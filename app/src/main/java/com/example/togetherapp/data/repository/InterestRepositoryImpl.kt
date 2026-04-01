package com.example.togetherapp.data.repository

import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.data.remote.UserInterestRequest
import com.example.togetherapp.domain.models.Interest
import com.example.togetherapp.domain.models.UserInterest
import com.example.togetherapp.domain.repository.InterestRepository
import io.github.jan.supabase.postgrest.from

class InterestRepositoryImpl : InterestRepository {

    private val client = SupabaseClient.supabase

    override suspend fun getAllInterests(): List<Interest> {
        return try {
            client
                .from("interests")
                .select()
                .decodeList<Interest>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getUserInterests(userId: Int): List<Interest> {
        return try {
            val userInterests = client
                .from("user_interests")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<UserInterest>()

            if (userInterests.isEmpty()) return emptyList()

            val allInterests = getAllInterests()
            val selectedIds = userInterests.map { it.interest_id }

            allInterests.filter { it.id in selectedIds }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun saveUserInterests(userId: Int, interestIds: List<Int>): Boolean {
        return try {
            client
                .from("user_interests")
                .delete {
                    filter {
                        eq("user_id", userId)
                    }
                }

            if (interestIds.isNotEmpty()) {
                val requestList = interestIds.map { interestId ->
                    UserInterestRequest(
                        user_id = userId,
                        interest_id = interestId
                    )
                }

                client
                    .from("user_interests")
                    .insert(requestList)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun markOnboardingCompleted(userId: Int): Boolean {
        return try {
            client
                .from("users")
                .update(
                    {
                        set("is_onboarding_completed", true)
                    }
                ) {
                    filter {
                        eq("id", userId)
                    }
                }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
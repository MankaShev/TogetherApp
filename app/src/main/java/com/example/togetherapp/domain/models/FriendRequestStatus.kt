package com.example.togetherapp.domain.models

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED;

    companion object {
        fun fromDb(value: String): FriendRequestStatus {
            return when (value.lowercase()) {
                "pending" -> PENDING
                "accepted" -> ACCEPTED
                "declined" -> DECLINED
                else -> PENDING
            }
        }
    }

    fun toDb(): String {
        return when (this) {
            PENDING -> "pending"
            ACCEPTED -> "accepted"
            DECLINED -> "declined"
        }
    }
}
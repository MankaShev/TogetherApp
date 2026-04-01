package com.example.togetherapp.domain.repository

import com.example.togetherapp.domain.models.FriendUserItem
import com.example.togetherapp.domain.models.IncomingFriendRequestItem
import com.example.togetherapp.domain.models.OutgoingFriendRequestItem

interface FriendRepository {

    suspend fun searchUsers(
        currentUserId: Int,
        query: String
    ): List<FriendUserItem>

    suspend fun sendFriendRequest(
        senderId: Int,
        receiverId: Int
    ): Result<Unit>

    suspend fun getIncomingRequests(
        currentUserId: Int
    ): List<IncomingFriendRequestItem>

    suspend fun getOutgoingRequests(
        currentUserId: Int
    ): List<OutgoingFriendRequestItem>

    suspend fun getFriends(
        currentUserId: Int
    ): List<FriendUserItem>

    suspend fun areFriends(
        userId1: Int,
        userId2: Int
    ): Boolean

    suspend fun acceptFriendRequest(
        requestId: Int
    ): Result<Unit>

    suspend fun declineFriendRequest(
        requestId: Int
    ): Result<Unit>
}
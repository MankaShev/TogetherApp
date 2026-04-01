package com.example.togetherapp.data.repository

import com.example.togetherapp.data.remote.CreateFriendRequest
import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.data.remote.UpdateFriendRequestStatusRequest
import com.example.togetherapp.domain.models.FriendRelationState
import com.example.togetherapp.domain.models.FriendRequest
import com.example.togetherapp.domain.models.FriendRequestStatus
import com.example.togetherapp.domain.models.FriendUserItem
import com.example.togetherapp.domain.models.IncomingFriendRequestItem
import com.example.togetherapp.domain.models.OutgoingFriendRequestItem
import com.example.togetherapp.domain.models.User
import com.example.togetherapp.domain.repository.FriendRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.time.Instant

class FriendRepositoryImpl : FriendRepository {

    private val client = SupabaseClient.supabase
    private val requestTimeout = 30_000L

    override suspend fun searchUsers(
        currentUserId: Int,
        query: String
    ): List<FriendUserItem> {
        return try {
            withTimeout(requestTimeout) {
                val trimmedQuery = query.trim()

                println("FriendRepository: searchUsers started")
                println("FriendRepository: currentUserId = $currentUserId")
                println("FriendRepository: query = '$trimmedQuery'")

                if (trimmedQuery.isBlank()) {
                    println("FriendRepository: query is blank, returning empty list")
                    return@withTimeout emptyList()
                }

                val allMatchedUsers = trySearchUsers(trimmedQuery)

                println("FriendRepository: matched users count = ${allMatchedUsers.size}")
                allMatchedUsers.forEach {
                    println("FriendRepository: matched user -> id=${it.id}, login=${it.login}")
                }

                val filteredUsers = allMatchedUsers.filter { it.id != currentUserId }

                println("FriendRepository: filtered users count (without current user) = ${filteredUsers.size}")

                if (filteredUsers.isEmpty()) {
                    return@withTimeout emptyList()
                }

                val currentUserRequests = getAllRequestsForUserInternal(currentUserId)

                println("FriendRepository: currentUserRequests count = ${currentUserRequests.size}")

                val result = filteredUsers.map { user ->
                    val relationState = resolveRelationState(
                        currentUserId = currentUserId,
                        targetUserId = user.id,
                        requests = currentUserRequests
                    )

                    FriendUserItem(
                        id = user.id,
                        login = user.login,
                        avatarUrl = user.avatar_url,
                        relationState = relationState
                    )
                }.sortedBy { it.login.lowercase() }

                println("FriendRepository: searchUsers result count = ${result.size}")
                result.forEach {
                    println("FriendRepository: result user -> id=${it.id}, login=${it.login}, relation=${it.relationState}")
                }

                result
            }
        } catch (e: TimeoutCancellationException) {
            println("FriendRepository: timeout in searchUsers")
            e.printStackTrace()
            emptyList()
        } catch (e: IOException) {
            println("FriendRepository: network error in searchUsers: ${e.message}")
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            println("FriendRepository: error in searchUsers: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun sendFriendRequest(
        senderId: Int,
        receiverId: Int
    ): Result<Unit> {
        return try {
            withTimeout(requestTimeout) {
                println("FriendRepository: sendFriendRequest senderId=$senderId receiverId=$receiverId")

                if (senderId == receiverId) {
                    return@withTimeout Result.failure(
                        IllegalArgumentException("Нельзя добавить себя в друзья")
                    )
                }

                val sender = getUserByIdInternal(senderId)
                val receiver = getUserByIdInternal(receiverId)

                if (sender == null) {
                    return@withTimeout Result.failure(
                        IllegalArgumentException("Отправитель не найден")
                    )
                }

                if (receiver == null) {
                    return@withTimeout Result.failure(
                        IllegalArgumentException("Получатель не найден")
                    )
                }

                val existingRequest = getRelationRequestBetweenUsers(senderId, receiverId)

                if (existingRequest != null) {
                    println("FriendRepository: existing request found -> id=${existingRequest.id}, status=${existingRequest.status}")

                    return@withTimeout when (existingRequest.statusEnum()) {
                        FriendRequestStatus.PENDING -> {
                            Result.failure(IllegalStateException("Заявка уже существует"))
                        }

                        FriendRequestStatus.ACCEPTED -> {
                            Result.failure(IllegalStateException("Вы уже друзья"))
                        }

                        FriendRequestStatus.DECLINED -> {
                            Result.failure(IllegalStateException("Заявка ранее была отклонена"))
                        }
                    }
                }

                val createRequest = CreateFriendRequest(
                    sender_id = senderId,
                    receiver_id = receiverId,
                    status = FriendRequestStatus.PENDING.toDb()
                )

                client
                    .from("friend_requests")
                    .insert(createRequest)

                println("FriendRepository: friend request inserted successfully")
                Result.success(Unit)
            }
        } catch (e: TimeoutCancellationException) {
            println("FriendRepository: timeout in sendFriendRequest")
            Result.failure(e)
        } catch (e: IOException) {
            println("FriendRepository: network error in sendFriendRequest: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            println("FriendRepository: error in sendFriendRequest: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    override suspend fun areFriends(
        userId1: Int,
        userId2: Int
    ): Boolean {
        return try {
            withTimeout(requestTimeout) {
                val direct = client
                    .from("friend_requests")
                    .select {
                        filter {
                            eq("sender_id", userId1)
                            eq("receiver_id", userId2)
                            eq("status", FriendRequestStatus.ACCEPTED.toDb())
                        }
                    }
                    .decodeList<FriendRequest>()

                val reverse = client
                    .from("friend_requests")
                    .select {
                        filter {
                            eq("sender_id", userId2)
                            eq("receiver_id", userId1)
                            eq("status", FriendRequestStatus.ACCEPTED.toDb())
                        }
                    }
                    .decodeList<FriendRequest>()

                (direct + reverse).isNotEmpty()
            }
        } catch (e: Exception) {
            println("FriendRepository: error in areFriends: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    override suspend fun getIncomingRequests(
        currentUserId: Int
    ): List<IncomingFriendRequestItem> {
        return try {
            withTimeout(requestTimeout) {
                println("FriendRepository: getIncomingRequests currentUserId=$currentUserId")

                val requests = client
                    .from("friend_requests")
                    .select {
                        filter {
                            eq("receiver_id", currentUserId)
                            eq("status", FriendRequestStatus.PENDING.toDb())
                        }
                    }
                    .decodeList<FriendRequest>()

                println("FriendRepository: incoming requests count = ${requests.size}")

                if (requests.isEmpty()) {
                    return@withTimeout emptyList()
                }

                val result = mutableListOf<IncomingFriendRequestItem>()

                for (request in requests) {
                    val sender = getUserByIdInternal(request.sender_id)
                    if (sender != null) {
                        result.add(
                            IncomingFriendRequestItem(
                                requestId = request.id,
                                senderId = sender.id,
                                login = sender.login,
                                avatarUrl = sender.avatar_url
                            )
                        )
                    }
                }

                println("FriendRepository: incoming mapped count = ${result.size}")
                result.sortedBy { it.login.lowercase() }
            }
        } catch (e: Exception) {
            println("FriendRepository: error in getIncomingRequests: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getOutgoingRequests(
        currentUserId: Int
    ): List<OutgoingFriendRequestItem> {
        return try {
            withTimeout(requestTimeout) {
                println("FriendRepository: getOutgoingRequests currentUserId=$currentUserId")

                val requests = client
                    .from("friend_requests")
                    .select {
                        filter {
                            eq("sender_id", currentUserId)
                            eq("status", FriendRequestStatus.PENDING.toDb())
                        }
                    }
                    .decodeList<FriendRequest>()

                println("FriendRepository: outgoing requests count = ${requests.size}")

                if (requests.isEmpty()) {
                    return@withTimeout emptyList()
                }

                val result = mutableListOf<OutgoingFriendRequestItem>()

                for (request in requests) {
                    val receiver = getUserByIdInternal(request.receiver_id)
                    if (receiver != null) {
                        result.add(
                            OutgoingFriendRequestItem(
                                requestId = request.id,
                                receiverId = receiver.id,
                                login = receiver.login,
                                avatarUrl = receiver.avatar_url
                            )
                        )
                    }
                }

                println("FriendRepository: outgoing mapped count = ${result.size}")
                result.sortedBy { it.login.lowercase() }
            }
        } catch (e: Exception) {
            println("FriendRepository: error in getOutgoingRequests: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getFriends(
        currentUserId: Int
    ): List<FriendUserItem> {
        return try {
            withTimeout(requestTimeout) {
                println("FriendRepository: getFriends currentUserId=$currentUserId")

                val sentAccepted = client
                    .from("friend_requests")
                    .select {
                        filter {
                            eq("sender_id", currentUserId)
                            eq("status", FriendRequestStatus.ACCEPTED.toDb())
                        }
                    }
                    .decodeList<FriendRequest>()

                val receivedAccepted = client
                    .from("friend_requests")
                    .select {
                        filter {
                            eq("receiver_id", currentUserId)
                            eq("status", FriendRequestStatus.ACCEPTED.toDb())
                        }
                    }
                    .decodeList<FriendRequest>()

                println("FriendRepository: sentAccepted = ${sentAccepted.size}, receivedAccepted = ${receivedAccepted.size}")

                val friendIds = mutableSetOf<Int>()

                sentAccepted.forEach { friendIds.add(it.receiver_id) }
                receivedAccepted.forEach { friendIds.add(it.sender_id) }

                if (friendIds.isEmpty()) {
                    println("FriendRepository: no friends found")
                    return@withTimeout emptyList()
                }

                val result = mutableListOf<FriendUserItem>()

                for (friendId in friendIds) {
                    val user = getUserByIdInternal(friendId)
                    if (user != null) {
                        result.add(
                            FriendUserItem(
                                id = user.id,
                                login = user.login,
                                avatarUrl = user.avatar_url,
                                relationState = FriendRelationState.FRIENDS
                            )
                        )
                    }
                }

                val sorted = result.sortedBy { it.login.lowercase() }
                println("FriendRepository: getFriends result count = ${sorted.size}")
                sorted
            }
        } catch (e: Exception) {
            println("FriendRepository: error in getFriends: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun acceptFriendRequest(requestId: Int): Result<Unit> {
        return updateFriendRequestStatus(
            requestId = requestId,
            status = FriendRequestStatus.ACCEPTED
        )
    }

    override suspend fun declineFriendRequest(requestId: Int): Result<Unit> {
        return updateFriendRequestStatus(
            requestId = requestId,
            status = FriendRequestStatus.DECLINED
        )
    }

    private suspend fun updateFriendRequestStatus(
        requestId: Int,
        status: FriendRequestStatus
    ): Result<Unit> {
        return try {
            withTimeout(requestTimeout) {
                println("FriendRepository: updateFriendRequestStatus requestId=$requestId status=${status.toDb()}")

                val body = UpdateFriendRequestStatusRequest(
                    status = status.toDb(),
                    updated_at = Instant.now().toString()
                )

                client
                    .from("friend_requests")
                    .update(body) {
                        filter {
                            eq("id", requestId)
                        }
                    }

                println("FriendRepository: updateFriendRequestStatus success")
                Result.success(Unit)
            }
        } catch (e: TimeoutCancellationException) {
            println("FriendRepository: timeout in updateFriendRequestStatus")
            Result.failure(e)
        } catch (e: IOException) {
            println("FriendRepository: network error in updateFriendRequestStatus: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            println("FriendRepository: error in updateFriendRequestStatus: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun trySearchUsers(query: String): List<User> {
        return try {
            client
                .from("users")
                .select {
                    filter {
                        ilike("login", "%$query%")
                    }
                }
                .decodeList<User>()
        } catch (e: Exception) {
            println("FriendRepository: ilike search failed: ${e.message}")
            e.printStackTrace()

            println("FriendRepository: fallback to full users load and local filter")

            val allUsers = client
                .from("users")
                .select()
                .decodeList<User>()

            allUsers.filter { user ->
                user.login.contains(query, ignoreCase = true)
            }
        }
    }

    private suspend fun getUserByIdInternal(userId: Int): User? {
        return try {
            val users = client
                .from("users")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<User>()

            val user = users.firstOrNull()
            println("FriendRepository: getUserByIdInternal($userId) -> ${user?.login}")
            user
        } catch (e: Exception) {
            println("FriendRepository: error in getUserByIdInternal($userId): ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private suspend fun getAllRequestsForUserInternal(
        currentUserId: Int
    ): List<FriendRequest> {
        return try {
            val sentRequests = client
                .from("friend_requests")
                .select {
                    filter {
                        eq("sender_id", currentUserId)
                    }
                }
                .decodeList<FriendRequest>()

            val receivedRequests = client
                .from("friend_requests")
                .select {
                    filter {
                        eq("receiver_id", currentUserId)
                    }
                }
                .decodeList<FriendRequest>()

            val combined = (sentRequests + receivedRequests).distinctBy { it.id }

            println(
                "FriendRepository: getAllRequestsForUserInternal -> sent=${sentRequests.size}, received=${receivedRequests.size}, total=${combined.size}"
            )

            combined
        } catch (e: Exception) {
            println("FriendRepository: error in getAllRequestsForUserInternal: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun getRelationRequestBetweenUsers(
        firstUserId: Int,
        secondUserId: Int
    ): FriendRequest? {
        return try {
            val direct = client
                .from("friend_requests")
                .select {
                    filter {
                        eq("sender_id", firstUserId)
                        eq("receiver_id", secondUserId)
                    }
                }
                .decodeList<FriendRequest>()

            val reverse = client
                .from("friend_requests")
                .select {
                    filter {
                        eq("sender_id", secondUserId)
                        eq("receiver_id", firstUserId)
                    }
                }
                .decodeList<FriendRequest>()

            (direct + reverse).firstOrNull()
        } catch (e: Exception) {
            println("FriendRepository: error in getRelationRequestBetweenUsers: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun resolveRelationState(
        currentUserId: Int,
        targetUserId: Int,
        requests: List<FriendRequest>
    ): FriendRelationState {
        val relation = requests.firstOrNull { request ->
            (request.sender_id == currentUserId && request.receiver_id == targetUserId) ||
                    (request.sender_id == targetUserId && request.receiver_id == currentUserId)
        } ?: return FriendRelationState.NONE

        return when (relation.statusEnum()) {
            FriendRequestStatus.ACCEPTED -> FriendRelationState.FRIENDS

            FriendRequestStatus.PENDING -> {
                if (relation.sender_id == currentUserId) {
                    FriendRelationState.OUTGOING_REQUEST
                } else {
                    FriendRelationState.INCOMING_REQUEST
                }
            }

            FriendRequestStatus.DECLINED -> FriendRelationState.NONE
        }
    }
}
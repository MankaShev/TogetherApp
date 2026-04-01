package com.example.togetherapp.data.repository

import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.data.remote.RegisterUserRequest
import com.example.togetherapp.domain.models.RegisterResult
import com.example.togetherapp.domain.models.User
import com.example.togetherapp.domain.repository.UserRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

class UserRepositoryImpl : UserRepository {

    private val client = SupabaseClient.supabase
    private val REQUEST_TIMEOUT = 30_000L

    override suspend fun login(login: String, password: String): User? {
        return try {
            println("UserRepository: попытка входа для login=$login")

            withTimeout(REQUEST_TIMEOUT) {
                val users = client
                    .from("users")
                    .select {
                        filter {
                            eq("login", login)
                            eq("password", password)
                        }
                    }
                    .decodeList<User>()

                users.firstOrNull()
            }
        } catch (e: Exception) {
            println("UserRepository: ошибка login: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun getUserById(userId: Int): User? {
        return try {
            println("UserRepository: getUserById($userId)")

            withTimeout(REQUEST_TIMEOUT) {
                val users = client
                    .from("users")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<User>()

                users.firstOrNull()
            }
        } catch (e: Exception) {
            println("UserRepository: ошибка getUserById: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun registerUser(
        login: String,
        password: String,
        avatarUrl: String?
    ): RegisterResult {
        return try {
            println("UserRepository: registerUser для login=$login")

            withTimeout(REQUEST_TIMEOUT) {
                val existingUsers = client
                    .from("users")
                    .select {
                        filter {
                            eq("login", login)
                        }
                    }
                    .decodeList<User>()

                if (existingUsers.isNotEmpty()) {
                    println("UserRepository: пользователь с таким логином уже существует")
                    return@withTimeout RegisterResult.UserAlreadyExists
                }

                val newUser = RegisterUserRequest(
                    login = login,
                    password = password,
                    avatar_url = avatarUrl,
                    is_onboarding_completed = false
                )

                client
                    .from("users")
                    .insert(newUser)

                val createdUsers = client
                    .from("users")
                    .select {
                        filter {
                            eq("login", login)
                            eq("password", password)
                        }
                    }
                    .decodeList<User>()

                val createdUser = createdUsers.firstOrNull()

                if (createdUser != null) {
                    println("UserRepository: зарегистрирован пользователь $createdUser")
                    RegisterResult.Success(createdUser)
                } else {
                    println("UserRepository: пользователь не найден после insert")
                    RegisterResult.UnknownError("Не удалось получить созданного пользователя")
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("UserRepository: таймаут регистрации")
            RegisterResult.NetworkError
        } catch (e: IOException) {
            println("UserRepository: ошибка сети при регистрации: ${e.message}")
            RegisterResult.NetworkError
        } catch (e: Exception) {
            println("UserRepository: ошибка registerUser: ${e.message}")
            e.printStackTrace()
            RegisterResult.UnknownError(e.message ?: "Неизвестная ошибка регистрации")
        }
    }
}
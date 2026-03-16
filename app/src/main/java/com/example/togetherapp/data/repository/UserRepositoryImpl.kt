package com.example.togetherapp.data.repository

import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.domain.models.User
import com.example.togetherapp.domain.repository.UserRepository
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import io.github.jan.supabase.postgrest.from
class UserRepositoryImpl : UserRepository {

    private val client = SupabaseClient.supabase
    private val REQUEST_TIMEOUT = 30_000L

    override suspend fun login(login: String, password: String): User? {
        return try {
            println("UserRepository: попытка входа для $login")

            val result = client.from("users")
                .select()

            val users = result.decodeList<User>()
            println("TEST USERS = $users")

            val user = users.firstOrNull { it.login == login && it.password == password }

            if (user != null) {
                println("UserRepository: найден пользователь $user")
            } else {
                println("UserRepository: пользователь не найден")
            }

            user
        } catch (e: Exception) {
            println("UserRepository: Ошибка ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun getUserById(userId: Int): User? {
        return try {
            println("UserRepository: getUserById($userId)")

            withTimeout(REQUEST_TIMEOUT) {
                val result = client.from("users").select(
                    columns = Columns.raw("*")
                ) {
                    filter {
                        eq("id", userId)
                    }
                }

                val usersData = result.data as? List<Map<String, Any?>>
                val userMap = usersData?.firstOrNull()

                if (userMap != null) {
                    val user = User(
                        id = userId,
                        login = userMap["login"] as? String ?: "",
                        password = userMap["password"] as? String ?: "",
                        avatar_url = userMap["avatar_url"] as? String,
                        is_onboarding_completed = userMap["is_onboarding_completed"] as? Boolean ?: false
                    )

                    println("UserRepository: найден пользователь $user")
                    user
                } else {
                    println("UserRepository: пользователь с id=$userId не найден")
                    null
                }
            }

        } catch (e: TimeoutCancellationException) {
            println("UserRepository: Таймаут getUserById")
            null
        } catch (e: Exception) {
            println("UserRepository: Exception ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun registerUser(login: String, password: String, avatarUrl: String?): User? {
        return try {
            println("UserRepository: registerUser для $login")

            withTimeout(REQUEST_TIMEOUT) {
                val result = client.from("users").insert(
                    mapOf(
                        "login" to login,
                        "password" to password,
                        "avatar_url" to avatarUrl,
                        "is_onboarding_completed" to false
                    )
                )

                val insertedData = result.data as? List<Map<String, Any?>>
                val userMap = insertedData?.firstOrNull()

                if (userMap != null) {
                    val user = User(
                        id = (userMap["id"] as? Number)?.toInt() ?: 0,
                        login = userMap["login"] as? String ?: "",
                        password = userMap["password"] as? String ?: "",
                        avatar_url = userMap["avatar_url"] as? String,
                        is_onboarding_completed = userMap["is_onboarding_completed"] as? Boolean ?: false
                    )
                    println("UserRepository: зарегистрирован пользователь $user")
                    user
                } else {
                    println("UserRepository: регистрация не удалась")
                    null
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("UserRepository: Таймаут регистрации")
            null
        } catch (e: Exception) {
            println("UserRepository: Exception ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
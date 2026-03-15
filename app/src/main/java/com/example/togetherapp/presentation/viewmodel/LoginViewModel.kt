package com.example.togetherapp.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.domain.models.User
import com.example.togetherapp.domain.repository.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    private val TAG = "LoginViewModel"
    private val sessionManager = SessionManager.getInstance(application)

    sealed class LoginState {
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String, val errorType: ErrorType) : LoginState()

        enum class ErrorType {
            INPUT_ERROR,
            NETWORK_ERROR
        }
    }

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(login: String, password: String) {
        if (login.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error(
                "Неверный логин или пароль",
                LoginState.ErrorType.INPUT_ERROR
            )
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                Log.i(TAG, "login: вызываем репозиторий login() для login=$login")
                val user = userRepository.login(login, password)
                Log.i(TAG, "login: получили результат от репозитория: $user")

                if (user != null) {
                    // Сохраняем пользователя в SessionManager
                    sessionManager.saveUser(user)
                    Log.i(TAG, "login: пользователь сохранён, userId=${user.id}")

                    _loginState.value = LoginState.Success(user)
                } else {
                    Log.w(TAG, "login: пользователь не найден или неверный пароль")
                    _loginState.value = LoginState.Error(
                        "Неверный логин или пароль",
                        LoginState.ErrorType.INPUT_ERROR
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "login: ошибка при логине", e)
                _loginState.value = LoginState.Error(
                    "Ошибка сети или сервера: ${e.message}",
                    LoginState.ErrorType.NETWORK_ERROR
                )
            }
        }
    }

    fun logout() {
        Log.i(TAG, "logout: очищаем сессию")
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = sessionManager.isLoggedIn()
        Log.i(TAG, "isLoggedIn: $loggedIn, current userId=${sessionManager.getUserId()}")
        return loggedIn
    }
}
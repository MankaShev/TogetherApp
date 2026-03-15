package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    // Sealed class для состояний входа
    sealed class LoginState {
        object Loading : LoginState()
        data class Success(val message: String = "Успешный вход") : LoginState()
        data class Error(val message: String, val errorType: ErrorType) : LoginState()

        enum class ErrorType {
            INPUT_ERROR,    // Ошибка ввода (неправильный логин/пароль)
            NETWORK_ERROR   // Ошибка сети
        }
    }

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, password: String) {
        // Показываем загрузку
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            // Имитация задержки сети
            delay(2000)

            // Простейшая валидация (заглушка)
            when {
                // Имитация ошибки сети (например, для теста)
                email == "network" -> {
                    _loginState.value = LoginState.Error(
                        "Нет подключения к интернету",
                        LoginState.ErrorType.NETWORK_ERROR
                    )
                }
                // Проверка формата email и пароля
                !email.contains("@") || password.length < 6 -> {
                    _loginState.value = LoginState.Error(
                        "Неверный email или пароль (мин. 6 символов)",
                        LoginState.ErrorType.INPUT_ERROR
                    )
                }
                else -> {
                    // Успешный вход
                    _loginState.value = LoginState.Success()
                }
            }
        }
    }

    // Метод для выхода (будет вызываться из ProfileFragment)
    fun logout() {
        // Очищаем данные сессии
        viewModelScope.launch {
            // Здесь можно очистить токены и т.д.
        }
    }
}
package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    // Sealed class для состояний регистрации
    sealed class RegisterState {
        object Sending : RegisterState()
        object Success : RegisterState()
        data class ErrorInput(val message: String) : RegisterState()
        object ErrorNetwork : RegisterState()
        object ErrorEmailExists : RegisterState()
    }

    private val _registerState = MutableLiveData<RegisterState>()
    val registerState: LiveData<RegisterState> = _registerState

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        // Показываем состояние отправки
        _registerState.value = RegisterState.Sending

        viewModelScope.launch {
            // Имитация задержки сети
            delay(2000)

            // Валидация
            when {
                // Проверка совпадения паролей
                password != confirmPassword -> {
                    _registerState.value = RegisterState.ErrorInput("Пароли не совпадают")
                }
                // Проверка длины пароля
                password.length < 6 -> {
                    _registerState.value = RegisterState.ErrorInput("Пароль должен содержать минимум 6 символов")
                }
                // Проверка формата email
                !email.contains("@") || !email.contains(".") -> {
                    _registerState.value = RegisterState.ErrorInput("Введите корректный email")
                }
                // Имитация существующего email (для теста)
                email == "test@test.com" -> {
                    _registerState.value = RegisterState.ErrorEmailExists
                }
                // Имитация ошибки сети (для теста)
                email == "network@test.com" -> {
                    _registerState.value = RegisterState.ErrorNetwork
                }
                // Успешная регистрация
                else -> {
                    _registerState.value = RegisterState.Success
                }
            }
        }
    }

    // Сброс состояния (если нужно вернуться к форме)
    fun resetState() {
        _registerState.value = null
    }
}
package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.data.repository.UserRepositoryImpl
import com.example.togetherapp.domain.models.RegisterResult
import com.example.togetherapp.domain.models.User
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val userRepository = UserRepositoryImpl()

    sealed class RegisterState {
        object Idle : RegisterState()
        object Sending : RegisterState()
        data class Success(val user: User) : RegisterState()
        data class ErrorInput(val message: String) : RegisterState()
        object ErrorNetwork : RegisterState()
        data class ErrorUserExists(val message: String) : RegisterState()
        data class ErrorUnknown(val message: String) : RegisterState()
    }

    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Idle)
    val registerState: LiveData<RegisterState> = _registerState

    fun register(login: String, password: String, confirmPassword: String) {
        val trimmedLogin = login.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirmPassword = confirmPassword.trim()

        when {
            trimmedLogin.isEmpty() -> {
                _registerState.value = RegisterState.ErrorInput("Введите логин")
                return
            }

            trimmedLogin.length < 3 -> {
                _registerState.value =
                    RegisterState.ErrorInput("Логин должен содержать минимум 3 символа")
                return
            }

            trimmedPassword.isEmpty() -> {
                _registerState.value = RegisterState.ErrorInput("Введите пароль")
                return
            }

            trimmedPassword.length < 6 -> {
                _registerState.value =
                    RegisterState.ErrorInput("Пароль должен содержать минимум 6 символов")
                return
            }

            trimmedPassword != trimmedConfirmPassword -> {
                _registerState.value = RegisterState.ErrorInput("Пароли не совпадают")
                return
            }
        }

        _registerState.value = RegisterState.Sending

        viewModelScope.launch {
            when (val result = userRepository.registerUser(
                login = trimmedLogin,
                password = trimmedPassword,
                avatarUrl = null
            )) {
                is RegisterResult.Success -> {
                    _registerState.value = RegisterState.Success(result.user)
                }

                is RegisterResult.UserAlreadyExists -> {
                    _registerState.value = RegisterState.ErrorUserExists(
                        "Пользователь с таким логином уже существует"
                    )
                }

                is RegisterResult.NetworkError -> {
                    _registerState.value = RegisterState.ErrorNetwork
                }

                is RegisterResult.UnknownError -> {
                    _registerState.value = RegisterState.ErrorUnknown(result.message)
                }
            }
        }
    }

    fun resetState() {
        _registerState.value = RegisterState.Idle
    }
}
package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.togetherapp.domain.usecase.LoginUseCase
import com.example.togetherapp.presentation.state.UiState

class LoginViewModel(private val loginUseCase: LoginUseCase) : ViewModel() {

    private val _loginState = MutableLiveData<UiState<Boolean>>()
    val loginState: LiveData<UiState<Boolean>> = _loginState

    fun login(username: String, password: String) {
        _loginState.value = UiState.Loading()

        try {
            val result = loginUseCase.execute(username, password)

            if (result) {
                _loginState.value = UiState.Success(true)
            } else {
                _loginState.value = UiState.Error("Invalid username or password")
            }
        } catch (e: Exception) {
            _loginState.value = UiState.Error(e.message ?: "Login failed")
        }
    }

    fun resetState() {
        _loginState.value = UiState.Empty()
    }
}
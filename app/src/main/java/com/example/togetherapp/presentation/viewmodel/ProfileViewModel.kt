package com.example.togetherapp.presentation.screens.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import com.example.togetherapp.presentation.state.ProfileUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

   // private val _state = MutableLiveData<ProfileUiState>()
//val state: LiveData<ProfileUiState> = _state

    init {
        loadProfile()
    }

    fun loadProfile() {
        // Проверка авторизации через SharedPreferences будет в реальном коде
        val isLoggedIn = checkIfUserIsLoggedIn()

        if (!isLoggedIn) {
            //_state.value = ProfileUiState.Unauthorized
            return
        }

        //_state.value = ProfileUiState.Loading

        viewModelScope.launch {
            delay(1500)
          //  val profileData = ProfileData(
//                userName = "Анна Петрова",
//                userEmail = "anna@example.com",
//                friendsCount = 12,
//                collectionsCount = 5
//            )
            //_state.value = ProfileUiState.Success(profileData)
        }
    }

    private fun checkIfUserIsLoggedIn(): Boolean {
        // Заглушка - в реальности будет обращение к SharedPreferences
        return false // Для теста неавторизованного состояния
    }

    fun logout() {
        // Очищаем данные авторизации
        // В реальности: удаляем токен, очищаем SharedPreferences
    }

    fun retry() {
        loadProfile()
    }
}
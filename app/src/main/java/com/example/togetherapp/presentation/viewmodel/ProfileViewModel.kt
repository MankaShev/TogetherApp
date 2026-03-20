package com.example.togetherapp.presentation.screens.profile

import android.app.Application
import androidx.lifecycle.*
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.domain.models.User
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager =
        SessionManager.getInstance(application.applicationContext)

    //  UI STATE
    private val _state = MutableLiveData<ProfileState>()
    val state: LiveData<ProfileState> = _state

    init {
        loadProfile()
    }

    fun loadProfile() {
        val userId = sessionManager.getUserId()

        // 👤 Гость
        if (userId == -1) {
            _state.value = ProfileState.Guest
            return
        }

        // Загрузка
        _state.value = ProfileState.Loading

        viewModelScope.launch {
            try {
                // Пока берём из SessionManager (MVP)
                val user = User(
                    id = userId,
                    login = sessionManager.getUserLogin() ?: "Без имени",
                    password = "", // это чтобы не было проблем
                    avatar_url = sessionManager.getUserAvatar()
                )
                _state.value = ProfileState.Success(user)

            } catch (e: Exception) {
                _state.value = ProfileState.Error(
                    e.message ?: "Ошибка загрузки профиля"
                )
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _state.value = ProfileState.Guest
    }

    //  Состояния экрана
    sealed class ProfileState {
        object Loading : ProfileState()
        object Guest : ProfileState()
        data class Success(val user: User) : ProfileState()
        data class Error(val message: String) : ProfileState()
    }
}
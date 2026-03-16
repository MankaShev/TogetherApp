package com.example.togetherapp.presentation.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.data.local.SessionManager
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    // Используем application context для singleton SessionManager
    private val sessionManager: SessionManager =
        SessionManager.getInstance(application.applicationContext)

    init {
        loadProfile()
    }

    fun loadProfile() {
        if (!sessionManager.isLoggedIn()) {
            // Пользователь не вошёл
            return
        }

        val userId = sessionManager.getUserId()
        // Загружаем данные профиля через userId из БД
        viewModelScope.launch {
            // TODO: запрос к репозиторию UserRepository.getUserById(userId)
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
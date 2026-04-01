package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.repository.CollectionRepository
import com.example.togetherapp.domain.repository.FriendRepository
import com.example.togetherapp.domain.repository.UserRepository
import com.example.togetherapp.presentation.state.FriendProfileUiState
import kotlinx.coroutines.launch

class FriendProfileViewModel(
    private val userRepository: UserRepository,
    private val collectionRepository: CollectionRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(FriendProfileUiState())
    val uiState: LiveData<FriendProfileUiState> = _uiState

    fun loadProfile(
        ownerUserId: Int,
        viewerUserId: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val user = userRepository.getUserById(ownerUserId)

                if (user == null) {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        errorMessage = "Пользователь не найден",
                        user = null,
                        collections = emptyList(),
                        isCollectionsEmpty = true
                    )
                    return@launch
                }

                val collections = collectionRepository.getUserCollectionsForViewer(
                    ownerUserId = ownerUserId,
                    viewerUserId = viewerUserId
                )

                val accessLabel = when {
                    ownerUserId == viewerUserId -> "Это ваш профиль"
                    friendRepository.areFriends(ownerUserId, viewerUserId) -> "Ваш друг"
                    else -> "Не в друзьях"
                }

                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    user = user,
                    collections = collections,
                    isCollectionsEmpty = collections.isEmpty(),
                    errorMessage = null,
                    accessLabel = accessLabel
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Ошибка загрузки профиля",
                    user = null,
                    collections = emptyList(),
                    isCollectionsEmpty = true
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }
}
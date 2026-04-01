package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.repository.FriendRepository
import com.example.togetherapp.presentation.state.FriendsUiState
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(FriendsUiState())
    val uiState: LiveData<FriendsUiState> = _uiState

    private var currentUserId: Int? = null

    fun init(userId: Int) {
        if (currentUserId == userId) return
        currentUserId = userId
        refreshAll()
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value?.copy(
            searchQuery = query,
            errorMessage = null,
            successMessage = null
        )
    }

    fun searchUsers() {
        val userId = currentUserId ?: return
        val query = _uiState.value?.searchQuery?.trim().orEmpty()

        if (query.isBlank()) {
            _uiState.value = _uiState.value?.copy(
                searchResults = emptyList(),
                isSearchEmpty = false,
                errorMessage = null,
                successMessage = null
            )
            return
        }

        viewModelScope.launch {
            setLoading(true)

            try {
                val results = friendRepository.searchUsers(
                    currentUserId = userId,
                    query = query
                )

                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    searchResults = results,
                    isSearchEmpty = results.isEmpty(),
                    errorMessage = null,
                    successMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    searchResults = emptyList(),
                    isSearchEmpty = false,
                    errorMessage = e.message ?: "Ошибка поиска пользователей",
                    successMessage = null
                )
            }
        }
    }

    fun loadIncomingRequests() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            setLoading(true)

            try {
                val requests = friendRepository.getIncomingRequests(userId)

                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    incomingRequests = requests,
                    isIncomingEmpty = requests.isEmpty(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    incomingRequests = emptyList(),
                    isIncomingEmpty = false,
                    errorMessage = e.message ?: "Ошибка загрузки входящих заявок"
                )
            }
        }
    }

    fun loadOutgoingRequests() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                val requests = friendRepository.getOutgoingRequests(userId)

                _uiState.value = _uiState.value?.copy(
                    outgoingRequests = requests,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    outgoingRequests = emptyList(),
                    errorMessage = e.message ?: "Ошибка загрузки исходящих заявок"
                )
            }
        }
    }

    fun loadFriends() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            setLoading(true)

            try {
                val friends = friendRepository.getFriends(userId)

                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    friends = friends,
                    isFriendsEmpty = friends.isEmpty(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    friends = emptyList(),
                    isFriendsEmpty = false,
                    errorMessage = e.message ?: "Ошибка загрузки списка друзей"
                )
            }
        }
    }

    fun sendFriendRequest(receiverId: Int) {
        val senderId = currentUserId ?: return

        viewModelScope.launch {
            setLoading(true)

            val result = friendRepository.sendFriendRequest(
                senderId = senderId,
                receiverId = receiverId
            )

            result.onSuccess {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    successMessage = "Заявка отправлена",
                    errorMessage = null
                )

                reloadSearchAndListsAfterAction()
            }.onFailure { error ->
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    successMessage = null,
                    errorMessage = error.message ?: "Не удалось отправить заявку"
                )
            }
        }
    }

    fun acceptFriendRequest(requestId: Int) {
        viewModelScope.launch {
            setLoading(true)

            val result = friendRepository.acceptFriendRequest(requestId)

            result.onSuccess {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    successMessage = "Заявка принята",
                    errorMessage = null
                )

                refreshAll()
            }.onFailure { error ->
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    successMessage = null,
                    errorMessage = error.message ?: "Не удалось принять заявку"
                )
            }
        }
    }

    fun declineFriendRequest(requestId: Int) {
        viewModelScope.launch {
            setLoading(true)

            val result = friendRepository.declineFriendRequest(requestId)

            result.onSuccess {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    successMessage = "Заявка отклонена",
                    errorMessage = null
                )

                refreshAll()
            }.onFailure { error ->
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    successMessage = null,
                    errorMessage = error.message ?: "Не удалось отклонить заявку"
                )
            }
        }
    }

    fun refreshAll() {
        loadIncomingRequests()
        loadOutgoingRequests()
        loadFriends()

        val currentQuery = _uiState.value?.searchQuery?.trim().orEmpty()
        if (currentQuery.isNotBlank()) {
            searchUsers()
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value?.copy(successMessage = null)
    }

    private fun reloadSearchAndListsAfterAction() {
        loadIncomingRequests()
        loadOutgoingRequests()
        loadFriends()

        val currentQuery = _uiState.value?.searchQuery?.trim().orEmpty()
        if (currentQuery.isNotBlank()) {
            searchUsers()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value?.copy(isLoading = isLoading)
    }
}
package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.togetherapp.domain.repository.FriendRepository

class FriendsViewModelFactory(
    private val friendRepository: FriendRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsViewModel::class.java)) {
            return FriendsViewModel(friendRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
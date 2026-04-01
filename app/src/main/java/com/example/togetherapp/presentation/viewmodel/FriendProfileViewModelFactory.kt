package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.togetherapp.domain.repository.CollectionRepository
import com.example.togetherapp.domain.repository.FriendRepository
import com.example.togetherapp.domain.repository.UserRepository

class FriendProfileViewModelFactory(
    private val userRepository: UserRepository,
    private val collectionRepository: CollectionRepository,
    private val friendRepository: FriendRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendProfileViewModel::class.java)) {
            return FriendProfileViewModel(
                userRepository = userRepository,
                collectionRepository = collectionRepository,
                friendRepository = friendRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
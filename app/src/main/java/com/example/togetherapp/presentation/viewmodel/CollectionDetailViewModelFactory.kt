package com.example.togetherapp.presentation.screens.personalcollection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.togetherapp.domain.repository.CollectionRepository

class CollectionDetailViewModelFactory(
    private val repository: CollectionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionDetailViewModel::class.java)) {
            return CollectionDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
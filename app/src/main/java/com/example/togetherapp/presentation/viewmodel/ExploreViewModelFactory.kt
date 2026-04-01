package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
import com.example.togetherapp.presentation.viewmodel.ExploreViewModel

class ExploreViewModelFactory(
    private val repository: CollectionRepositoryImpl
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
            return ExploreViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
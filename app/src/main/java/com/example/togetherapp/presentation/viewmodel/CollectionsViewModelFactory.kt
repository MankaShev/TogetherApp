package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.togetherapp.domain.repository.CollectionRepository
import com.example.togetherapp.domain.usecase.AddPlaceToCollectionUseCase

class CollectionsViewModelFactory(
    private val repository: CollectionRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(CollectionsViewModel::class.java)) {

            val addPlaceToCollectionUseCase = AddPlaceToCollectionUseCase(repository)
            return CollectionsViewModel(repository, addPlaceToCollectionUseCase) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
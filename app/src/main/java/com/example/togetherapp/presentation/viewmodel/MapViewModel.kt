package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.data.repository.InterestRepositoryImpl
import com.example.togetherapp.domain.models.Interest
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    sealed class MapState {
        object Idle : MapState()
        object LoadingInterests : MapState()
        data class InterestsLoaded(val interests: List<Interest>) : MapState()
        data class Error(val message: String) : MapState()
    }

    private val interestRepository = InterestRepositoryImpl()

    private val _mapState = MutableLiveData<MapState>(MapState.Idle)
    val mapState: LiveData<MapState> = _mapState

    fun loadUserInterests(userId: Int) {
        if (userId == -1) {
            _mapState.value = MapState.Error("Пользователь не авторизован")
            return
        }

        _mapState.value = MapState.LoadingInterests

        viewModelScope.launch {
            try {
                val interests = interestRepository.getUserInterests(userId)
                _mapState.value = MapState.InterestsLoaded(interests)
            } catch (e: Exception) {
                _mapState.value = MapState.Error(
                    e.message ?: "Не удалось загрузить интересы"
                )
            }
        }
    }

    fun resetState() {
        _mapState.value = MapState.Idle
    }
}
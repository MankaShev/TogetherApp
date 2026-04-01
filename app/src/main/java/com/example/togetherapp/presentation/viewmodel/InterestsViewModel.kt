package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.data.repository.InterestRepositoryImpl
import com.example.togetherapp.domain.models.Interest
import kotlinx.coroutines.launch

class InterestsViewModel : ViewModel() {

    sealed class InterestsState {
        object Idle : InterestsState()
        object Loading : InterestsState()
        data class Content(
            val interests: List<Interest>,
            val selectedIds: Set<Int>
        ) : InterestsState()
        object Saving : InterestsState()
        object Saved : InterestsState()
        data class Error(val message: String) : InterestsState()
    }

    private val repository = InterestRepositoryImpl()

    private val _state = MutableLiveData<InterestsState>(InterestsState.Idle)
    val state: LiveData<InterestsState> = _state

    private var cachedInterests: List<Interest> = emptyList()
    private val selectedIds = mutableSetOf<Int>()

    fun loadInterests(userId: Int, isEditMode: Boolean) {
        _state.value = InterestsState.Loading

        viewModelScope.launch {
            try {
                cachedInterests = repository.getAllInterests()

                if (cachedInterests.isEmpty()) {
                    _state.value = InterestsState.Error("Не удалось загрузить интересы")
                    return@launch
                }

                selectedIds.clear()

                if (isEditMode) {
                    val userInterests = repository.getUserInterests(userId)
                    selectedIds.addAll(userInterests.map { it.id })
                }

                _state.value = InterestsState.Content(
                    interests = cachedInterests,
                    selectedIds = selectedIds.toSet()
                )
            } catch (e: Exception) {
                _state.value = InterestsState.Error(
                    e.message ?: "Ошибка загрузки интересов"
                )
            }
        }
    }

    fun toggleInterest(interestId: Int) {
        if (selectedIds.contains(interestId)) {
            selectedIds.remove(interestId)
        } else {
            selectedIds.add(interestId)
        }

        _state.value = InterestsState.Content(
            interests = cachedInterests,
            selectedIds = selectedIds.toSet()
        )
    }

    fun saveInterests(userId: Int, completeOnboarding: Boolean) {
        if (selectedIds.isEmpty()) {
            _state.value = InterestsState.Error("Выберите хотя бы один интерес")
            return
        }

        _state.value = InterestsState.Saving

        viewModelScope.launch {
            try {
                val saved = repository.saveUserInterests(userId, selectedIds.toList())

                if (!saved) {
                    _state.value = InterestsState.Error("Не удалось сохранить интересы")
                    return@launch
                }

                if (completeOnboarding) {
                    repository.markOnboardingCompleted(userId)
                }

                _state.value = InterestsState.Saved
            } catch (e: Exception) {
                _state.value = InterestsState.Error(
                    e.message ?: "Ошибка сохранения интересов"
                )
            }
        }
    }
}
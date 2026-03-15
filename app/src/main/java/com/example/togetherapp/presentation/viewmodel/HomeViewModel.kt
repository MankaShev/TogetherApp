package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.models.Collection
import com.example.togetherapp.presentation.screens.home.CardType
import com.example.togetherapp.presentation.screens.home.HomeScreenData
import com.example.togetherapp.presentation.state.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    // Используем общий UiState с типом List<Collection>
    private val _collectionsState = MutableLiveData<UiState<List<Collection>>>()
    val collectionsState: LiveData<UiState<List<Collection>>> = _collectionsState

    // Дополнительные данные экрана
    private val _screenData = MutableLiveData(HomeScreenData(CardType.COLLECTION))
    val screenData: LiveData<HomeScreenData> = _screenData

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    init {
        loadData()
    }

    fun loadData() {
        _collectionsState.value = UiState.Loading

        viewModelScope.launch {
            delay(2000)

            val mockCollections = when (_screenData.value?.cardType) {
                CardType.COLLECTION -> listOf(
                    Collection(1, "Популярные места Москвы", "15 мест"),
                    Collection(2, "Лучшие кафе", "8 заведений"),
                    Collection(3, "Куда пойти с детьми", "12 мест")
                )
                CardType.PLACE -> listOf(
                    Collection(101, "Красная площадь", "Главная площадь страны"),
                    Collection(102, "Парк Горького", "Центральный парк"),
                    Collection(103, "ВДНХ", "Выставка достижений")
                )
                else -> emptyList()
            }

            _collectionsState.value = UiState.Success(mockCollections)
        }
    }

    fun selectTab(type: CardType) {
        _screenData.value = _screenData.value?.copy(cardType = type)
        loadData()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun retry() {
        loadData()
    }
}
package com.example.togetherapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.presentation.screens.home.CardType
import com.example.togetherapp.presentation.screens.home.HomeScreenData
import com.example.togetherapp.presentation.state.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _collectionsState = MutableLiveData<UiState<List<CollectionModel>>>()
    val collectionsState: LiveData<UiState<List<CollectionModel>>> = _collectionsState

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
            delay(1500) // имитация загрузки данных

            // Моковые данные с учетом структуры CollectionModel
            val mockCollections = when (_screenData.value?.cardType) {
                CardType.COLLECTION -> listOf(
                    CollectionModel(
                        id = 1,
                        user_id = 101,
                        title = "Популярные места Москвы",
                        description = "15 мест",
                        access_type = "public",
                        created_at = "2026-03-15"
                    ),
                    CollectionModel(
                        id = 2,
                        user_id = 102,
                        title = "Лучшие кафе",
                        description = "8 заведений",
                        access_type = "public",
                        created_at = "2026-03-10"
                    ),
                    CollectionModel(
                        id = 3,
                        user_id = 103,
                        title = "Куда пойти с детьми",
                        description = "12 мест",
                        access_type = "private",
                        created_at = "2026-02-20"
                    )
                )
                CardType.PLACE -> listOf(
                    CollectionModel(
                        id = 101,
                        user_id = 201,
                        title = "Красная площадь",
                        description = "Главная площадь страны",
                        access_type = "public",
                        created_at = "2026-01-01"
                    ),
                    CollectionModel(
                        id = 102,
                        user_id = 202,
                        title = "Парк Горького",
                        description = "Центральный парк",
                        access_type = "public",
                        created_at = "2026-01-05"
                    ),
                    CollectionModel(
                        id = 103,
                        user_id = 203,
                        title = "ВДНХ",
                        description = "Выставка достижений",
                        access_type = "private",
                        created_at = "2026-02-01"
                    )
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
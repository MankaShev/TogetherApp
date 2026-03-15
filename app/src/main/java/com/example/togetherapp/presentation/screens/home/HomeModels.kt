package com.example.togetherapp.presentation.screens.home



// Enum для типа карточки (должен быть здесь, а не во фрагменте)
enum class CardType {
    COLLECTION, PLACE
}

// Если нужны дополнительные UI-специфичные данные
data class HomeScreenData(
    val cardType: CardType,
    val searchQuery: String = ""
)
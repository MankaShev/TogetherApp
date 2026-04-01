package com.example.togetherapp.presentation.screens.map

object InterestSearchMapper {

    fun mapInterestToQueries(interestName: String): List<String> {
        val normalized = interestName.trim().lowercase()

        return when (normalized) {
            "кофе" -> listOf("кофейня", "кафе")
            "природа" -> listOf("парк", "сквер", "набережная")
            "учеба", "учёба" -> listOf("библиотека", "коворкинг", "университет")
            "тусовки" -> listOf("бар", "клуб", "караоке")
            "дешево", "дёшево" -> listOf("столовая", "фастфуд", "недорогое кафе")
            "романтично" -> listOf("ресторан", "смотровая площадка", "парк")
            else -> listOf(normalized).filter { it.isNotBlank() }
        }
    }

    fun mapInterestsToQueries(interests: List<String>): List<String> {
        return interests
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .flatMap { mapInterestToQueries(it) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}
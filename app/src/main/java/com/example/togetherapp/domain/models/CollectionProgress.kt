package com.example.togetherapp.domain.models

data class CollectionProgress(
    val totalCount: Int,
    val visitedCount: Int
) {
    val progressPercent: Int
        get() = if (totalCount == 0) 0 else (visitedCount * 100) / totalCount
}
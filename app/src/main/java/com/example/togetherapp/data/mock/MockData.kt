package com.example.togetherapp.data.mock

import com.example.togetherapp.domain.models.Collection
import com.example.togetherapp.domain.models.Place

object MockData {

    val mockPlaces = listOf(
        Place(
            id = 1,
            name = "Михайловская роща",
            latitude = 55.7558,
            longitude = 37.6176
        ),
        Place(
            id = 2,
            name = "Кофейня Парадокс",
            latitude = 55.7512,
            longitude = 37.6184
        ),
        Place(
            id = 3,
            name = "Музей славянской мифологии",
            latitude = 55.7412,
            longitude = 37.6214
        ),
        Place(
            id = 4,
            name = "Ресторан Чико",
            latitude = 55.7612,
            longitude = 37.6314
        ),
        Place(
            id = 5,
            name = "Набережная реки Ушайки",
            latitude = 55.7458,
            longitude = 37.6076
        )
    )

    // МОК-КОЛЛЕКЦИИ (Collections)
    val mockCollections = listOf(
        Collection(
            id = 1,
            name = "Лучшие парки Москвы",
            description = "Подборка живописных мест для прогулок"
        ),
        Collection(
            id = 2,
            name = "Уютные кофейни",
            description = "Где выпить вкусный кофе"
        ),
        Collection(
            id = 3,
            name = "Культурный досуг",
            description = "Музеи и выставки"
        ),
        Collection(
            id = 4,
            name = "Моя личная подборка",
            description = "Только для меня"
        )
    )
}
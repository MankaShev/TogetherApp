package com.example.togetherapp.presentation.screens.map

import com.example.togetherapp.domain.models.SelectedPlace

object PopularPlacesProvider {

    fun getPopularPlaces(): List<SelectedPlace> {
        return listOf(
            SelectedPlace(
                external_id = 1001,
                title = "Tomsk State University",
                latitude = 56.472222,
                longitude = 84.946111,
                description = "Один из самых известных университетов Томска",
                address = "Томск, пр. Ленина, 36"
            ),
            SelectedPlace(
                external_id = 1002,
                title = "Лагерный сад",
                latitude = 56.458520,
                longitude = 84.952178,
                description = "Популярное место для прогулок и отдыха",
                address = "Томск, Лагерный сад"
            ),
            SelectedPlace(
                external_id = 1003,
                title = "Новособорная площадь",
                latitude = 56.481134,
                longitude = 84.948673,
                description = "Центральная площадь Томска",
                address = "Томск, Новособорная площадь"
            ),
            SelectedPlace(
                external_id = 1004,
                title = "Городской сад",
                latitude = 56.480370,
                longitude = 84.957135,
                description = "Парк с аттракционами и прогулочной зоной",
                address = "Томск, ул. Герцена, 6"
            ),
            SelectedPlace(
                external_id = 1005,
                title = "Томский областной краеведческий музей",
                latitude = 56.484414,
                longitude = 84.947255,
                description = "Исторический музей в центре города",
                address = "Томск, пр. Ленина, 75"
            ),
            SelectedPlace(
                external_id = 1006,
                title = "Томская набережная",
                latitude = 56.495240,
                longitude = 84.934550,
                description = "Популярное место для прогулок у реки",
                address = "Томск, набережная реки Томи"
            )
        )
    }
}
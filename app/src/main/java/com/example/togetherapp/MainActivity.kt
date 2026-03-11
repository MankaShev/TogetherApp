package com.example.togetherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.togetherapp.domain.usecase.AddPlaceToCollectionUseCase
import com.example.togetherapp.presentation.TestMapScreen
import com.example.togetherapp.presentation.viewmodel.MapViewModel
import com.yandex.mapkit.MapKitFactory
import com.example.togetherapp.data.MyRepositoryImpl
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Устанавливаем API-ключ Яндекса
        MapKitFactory.setApiKey("e4f380a3-c4fa-4ddf-87e4-ac585da8b4f2")

        // 2. Инициализируем библиотеку карт
        MapKitFactory.initialize(this)

        // 3. Собираем зависимости ВРУЧНУЮ
        val repository = MyRepositoryImpl()
        val useCase = AddPlaceToCollectionUseCase(repository)
        val viewModel = MapViewModel(useCase)

        setContent {
            // 4. Запускаем наш экран и передаем ему готовую ViewModel
            TestMapScreen(viewModel = viewModel)
        }
    }

    // Обязательные методы для корректной работы карты
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}
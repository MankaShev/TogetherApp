package com.example.togetherapp.presentation

import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.togetherapp.presentation.viewmodel.MapViewModel
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.example.togetherapp.domain.models.Place
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.Animation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestMapScreen(viewModel: MapViewModel) {
    // --- БЛОК 1: ОКРУЖЕНИЕ И СОСТОЯНИЕ ---
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    // Храним выбранное место и его категорию
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    var placeCategory by remember { mutableStateOf("") }

    // Инициализируем менеджер поиска
    val searchManager = remember {
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    }

    // --- БЛОК 2: ЛОГИКА ОБРАБОТКИ НАЖАТИЙ ---
    val tapListener = remember {
        com.yandex.mapkit.layers.GeoObjectTapListener { event ->
            val point = event.geoObject.geometry.firstOrNull()?.point ?: return@GeoObjectTapListener true

            // Настройки поиска
            val searchOptions = SearchOptions().apply {
                searchTypes = SearchType.BIZ.value
                resultPageSize = 1
            }

            // Слушатель ответа от сервера Яндекса
            val searchListener = object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val searchResult = response.collection.children.firstOrNull()?.obj
                    val metadata = searchResult?.metadataContainer?.getItem(BusinessObjectMetadata::class.java)

                    if (metadata != null) {
                        // Вытаскиваем основную категорию (например, "Университет")
                        placeCategory = metadata.categories.firstOrNull()?.name ?: "Организация"

                        // Формируем объект нашего класса Place
                        selectedPlace = Place(
                            id = java.util.UUID.randomUUID(),
                            name = metadata.name,
                            latitude = point.latitude,
                            longitude = point.longitude,
                            address = metadata.address.formattedAddress
                        )
                    }
                }
                override fun onSearchError(error: com.yandex.runtime.Error) {
                    Toast.makeText(context, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
            }

            // Запускаем сам процесс поиска
            searchManager.submit(point, 16, searchOptions, searchListener)
            true
        }
    }

    // --- БЛОК 3: ИНТЕРФЕЙС (КАРТА И КАРТОЧКА) ---
    Box(modifier = Modifier.fillMaxSize()) {

        // 3.1. Отображение карты
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    val currentMap = mapWindow.map

                    // Устанавливаем камеру на ТГУ (Томск)
                    // Координаты главного корпуса ТГУ: 56.471116, 84.946636
                    currentMap.move(
                        CameraPosition(Point(56.471116, 84.946636), 16.5f, 0.0f, 0.0f),
                        Animation(Animation.Type.SMOOTH, 0f),
                        null
                    )

                    // Привязываем наш слушатель нажатий к карте
                    currentMap.addTapListener(tapListener)
                }
            }
        )

        // 3.2. Выезжающая панель (BottomSheet)
        if (selectedPlace != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedPlace = null },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                PlaceInfoCard(
                    place = selectedPlace!!,
                    category = placeCategory,
                    onSave = {
                        viewModel.onPlaceSelected(
                            selectedPlace!!.name,
                            selectedPlace!!.address,
                            selectedPlace!!.latitude,
                            selectedPlace!!.longitude
                        )
                        selectedPlace = null // Закрываем после сохранения
                    }
                )
            }
        }
    }
}

// --- БЛОК 4: ДИЗАЙН ВНУТРЕННОСТИ КАРТОЧКИ ---
@Composable
fun PlaceInfoCard(place: Place, category: String, onSave: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 48.dp)
    ) {
        // Категория серым мелким шрифтом
        Text(
            text = category.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Название места
        Text(
            text = place.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Адрес
        Text(
            text = place.address,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка действия
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)) // Синий цвет
        ) {
            Text("Добавить в коллекцию", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}
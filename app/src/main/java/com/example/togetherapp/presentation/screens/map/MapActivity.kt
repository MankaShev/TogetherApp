package com.example.togetherapp.presentation.screens.map

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.togetherapp.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var searchManager: SearchManager

    // [ВАЖНО] Сохраняем сессию в поле класса, чтобы её не удалил Garbage Collector
    private var searchSession: Session? = null

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Инициализация MapKit
        MapKitFactory.setApiKey("e4f380a3-c4fa-4ddf-87e4-ac585da8b4f2")
        MapKitFactory.initialize(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.map_screen)

        // 2. Инициализация View
        mapView = findViewById(R.id.mapview)
        val bottomSheet = findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // Скрываем шторку по умолчанию
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

        // 3. Создаем менеджер поиска
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        setupMap()
    }

    private fun setupMap() {
        val map = mapView.mapWindow.map

        // Устанавливаем камеру на ТГУ (Томск)
        map.move(
            CameraPosition(Point(56.471116, 84.946636), 16.5f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )

        // Слушатель нажатий на гео-объекты (здания, организации)
        map.addTapListener { mapObjectTapEvent ->
            val point = mapObjectTapEvent.geoObject.geometry.firstOrNull()?.point
            if (point != null) {
                startSearch(point)
            }
            true // Событие обработано
        }

        // Слушатель нажатий на карту (InputListener) для обработки кликов в пустые места
        map.addInputListener(object : InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                // Если кликнули в пустое место — скрываем шторку
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
            }

            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
                // При долгом нажатии тоже ищем объект
                startSearch(point)
            }
        })
    }

    private fun startSearch(point: Point) {
        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value or SearchType.GEO.value // Ищем и фирмы, и адреса
            resultPageSize = 1
        }

        // [ИСПРАВЛЕНИЕ ОШИБКИ FINALIZED] Приравниваем результат к переменной класса
        searchSession = searchManager.submit(
            point,
            16, // Уровень масштабирования для поиска
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val searchResult = response.collection.children.firstOrNull()?.obj

                    // Пытаемся достать данные организации (Business)
                    val bizMetadata = searchResult?.metadataContainer?.getItem(BusinessObjectMetadata::class.java)
                    // Пытаемся достать данные топонима (Адрес/Здание)
                    val geoMetadata = searchResult?.metadataContainer?.getItem(ToponymObjectMetadata::class.java)

                    val name = bizMetadata?.name ?: searchResult?.name ?: "Объект"
                    val address = bizMetadata?.address?.formattedAddress
                        ?: geoMetadata?.address?.formattedAddress
                        ?: "Адрес не определен"
                    val category = bizMetadata?.categories?.firstOrNull()?.name ?: "Место"

                    showPlaceCard(name, address, category)
                }

                override fun onSearchError(error: Error) {
                    Toast.makeText(this@MapActivity, "Ошибка поиска данных", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showPlaceCard(name: String, address: String, category: String) {
        // Заполняем данными TextView из map_screen.xml
        findViewById<TextView>(R.id.placeNameText)?.text = name
        findViewById<TextView>(R.id.placeAddressText)?.text = address
        findViewById<TextView>(R.id.categoryText)?.text = category.uppercase()

        // Показываем шторку
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

        // Кнопка сохранения в коллекцию
        findViewById<Button>(R.id.addToCollectionButton)?.setOnClickListener {
            Toast.makeText(this, "Место сохранено!", Toast.LENGTH_SHORT).show()
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    // Методы жизненного цикла для корректной работы MapKit
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
}
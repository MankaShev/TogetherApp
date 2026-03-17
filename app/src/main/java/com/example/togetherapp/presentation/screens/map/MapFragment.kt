package com.example.togetherapp.presentation.screens.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.togetherapp.databinding.FragmentMapBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session

class MapFragment : Fragment() {

    // _binding: Хранит ссылку на вью, может быть null, когда фрагмент уничтожен.
    private var _binding: FragmentMapBinding? = null

    // binding: Удобный способ обращаться к вью без знака "?". Работает только между onCreateView и onDestroyView.
    private val binding get() = _binding!!

    // mapView: Сама вью карты. Выносим в поле, чтобы иметь доступ из функций.
    private lateinit var mapView: MapView

    // searchManager: Он создает запросы. Должен жить всё время, пока жив фрагмент.
    private lateinit var searchManager: SearchManager

    // Сохраняем слушатель как поле класса, чтобы его не удалил Garbage Collector
    private var mapInputListener: com.yandex.mapkit.map.InputListener? = null

    // searchSession: Текущая сессия поиска.
    // ЗАЧЕМ: Когда мы ищем место, Яндекс создает "сессию". Если её не сохранить в поле класса,
    // Garbage Collector удалит её через секунду, и будет ошибка "finalized" и никакой логики поиска.
    private var searchSession: Session? = null

    // bottomSheetBehavior: Управляет логикой "всплывания" шторки (меняет состояние шторки (спрятана/открыта) из кода).
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инициализируем библиотеку. Ключ уже стоит (пока что) в MainActivity, тут просто "подключаемся".
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Создаем вью из XML
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем ссылку на MapView
        mapView = binding.mapContainer.mapview

        // Инициализируем BottomSheet.
        // Мы ищем вью с id "bottomSheet" внутри контейнера mapContainer.
        val bottomSheet = binding.mapContainer.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // По умолчанию прячем шторку
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

        // Создаем SearchManager (для того, чтобы искать данные по нажатию на карту)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        // Запускаем базовую настройку камеры
        setupMap()
    }

    // ФУНКЦИЯ setupMap: Начальная настройка положения карты
    private fun setupMap() {
        val map = mapView.mapWindow.map

        // Ставим камеру на Томск (ТГУ) TODO Сделать запрос на местоположение пользователя и двигать карту туда
        map.move(
            com.yandex.mapkit.map.CameraPosition(
                Point(56.471116, 84.946636),
                16.5f, 0.0f, 0.0f
            )
        )

        // Создаем объект слушателя
        mapInputListener = object : com.yandex.mapkit.map.InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                // Очищаем шторку
                binding.mapContainer.placeNameText.text = ""
                binding.mapContainer.placeAddressText.text = ""
                // Скрываем шторку перед новым поиском
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

                android.util.Log.d("MapStep", "ЭТАП 2: Запускаю поиск для ${point.latitude}")

                // Передаем координаты в поиск
                startSearch(point)
            }
            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {} //для корректного наследования
        }
        // Привязываем его к карте
        map.addInputListener(mapInputListener!!)
    }

    private fun startSearch(point: Point) {
        // Настройки поиска: ищем все организации в одной точке
        val searchOptions = com.yandex.mapkit.search.SearchOptions().apply {
            // Ищем только бизнесы, а не адреса (пока что) TODO Поиграться с поиском у Яндекс карт и настроить список организаций, при выборе дома
            searchTypes = SearchType.BIZ.value
            resultPageSize = 1
        }

        // Сохраняем в поле класса searchSession (чтобы не было finalized!)
        searchSession = searchManager.submit(
            point,
            16,
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: com.yandex.mapkit.search.Response) {
                    // Достаем метаданные бизнеса из первого результата
                    val bizMetadata = response.collection.children.firstOrNull()?.obj
                        ?.metadataContainer
                        ?.getItem(com.yandex.mapkit.search.BusinessObjectMetadata::class.java)

                    if (bizMetadata != null) {
                        val name = bizMetadata.name
                        val address = bizMetadata.address.formattedAddress
                        val category = bizMetadata.categories.firstOrNull()?.name ?: ""

                        android.util.Log.d("MapStep", "ЭТАП 2 ПРОЙДЕН: Найдена организация: $name")

                        // Обновляем текст в шторке
                        binding.mapContainer.placeNameText.text = name
                        binding.mapContainer.placeAddressText.text = address
                        binding.mapContainer.categoryText.text = category

                        // Выезжаем!
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        // Если в этой точке вообще нет бизнеса (например, газон или пустая дорога)
                        android.util.Log.d("MapStep", "В этой точке нет организаций")
                    }
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    android.util.Log.e("MapStep", "Ошибка поиска: $error")
                }
            }
        )
    }

    // ЭТО КРИТИЧЕСКИ ВАЖНО ДЛЯ ЯНДЕКСА:
    // Если не вызвать onStart/onStop у MapKit и MapView, карта будет черной или зависнет.

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

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем ссылку на биндинг, чтобы не было утечек памяти
        _binding = null
    }
}
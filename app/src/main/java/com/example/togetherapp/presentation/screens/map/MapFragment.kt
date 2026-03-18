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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.togetherapp.R
import com.example.togetherapp.domain.models.Place
import com.example.togetherapp.presentation.viewmodel.SharedMapViewModel

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

    // Shared ViewModel для связи с коллекциями
    private lateinit var sharedViewModel: SharedMapViewModel

    // Текущее выбранное место
    private var currentPlace: Place? = null

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

        // ✅ НОВОЕ: Инициализация SharedViewModel
        sharedViewModel = ViewModelProvider(requireActivity())[SharedMapViewModel::class.java]

        // ✅ НОВОЕ: Наблюдение за результатом добавления в коллекцию
        observeNavigationEvents()

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

        // ✅ НОВОЕ: Добавляем обработчик кнопки в карточке места
        setupAddToCollectionButton()
    }

    // ✅ НОВЫЙ МЕТОД: Наблюдение за результатом добавления в коллекцию
    private fun observeNavigationEvents() {
        sharedViewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is SharedMapViewModel.NavigationEvent.BackToMapWithResult -> {
                    // Показываем уведомление
                    android.widget.Toast.makeText(
                        requireContext(),
                        "✅ Место добавлено в «${event.collectionName}»",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    // Здесь можно обновить карточку места (например, подсветить, что уже добавлено)
                    binding.mapContainer.addToCollectionButton.text = "Добавлено ✓"
                    binding.mapContainer.addToCollectionButton.isEnabled = false

                    // Сбрасываем событие
                    sharedViewModel.consumeNavigationEvent()
                }
                null -> {}
            }
        }
    }

    // ✅ НОВЫЙ МЕТОД: Настройка кнопки "Добавить в коллекцию"
    private fun setupAddToCollectionButton() {
        binding.mapContainer.addToCollectionButton.setOnClickListener {
            // Проверяем, есть ли выбранное место
            val place = currentPlace
            if (place != null) {
                // Сохраняем место в SharedViewModel
                sharedViewModel.setSelectedPlace(place)

                // Переходим на экран коллекций
                findNavController().navigate(R.id.collectionsFragment)
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Сначала выберите место на карте",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
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

                        // ✅ НОВОЕ: Создаем объект Place и сохраняем
                        currentPlace = Place(
                            id = (point.latitude * point.longitude).toInt(), // Временный ID
                            name = name,
                            latitude = point.latitude,
                            longitude = point.longitude
                        )

                        // ✅ НОВОЕ: Сбрасываем текст кнопки
                        binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
                        binding.mapContainer.addToCollectionButton.isEnabled = true
                        // Обновляем текст в шторке
                        binding.mapContainer.placeNameText.text = name
                        binding.mapContainer.placeAddressText.text = address
                        binding.mapContainer.categoryText.text = category

                        // Выезжаем!
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        // Если в этой точке вообще нет бизнеса (например, газон или пустая дорога)
                        android.util.Log.d("MapStep", "В этой точке нет организаций")

                        // ✅ НОВОЕ: Сбрасываем текущее место
                        currentPlace = null

                        // Скрываем шторку
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    android.util.Log.e("MapStep", "Ошибка поиска: $error")
                    // ✅ НОВОЕ: Сбрасываем текущее место при ошибке
                    currentPlace = null
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

        // ✅ НОВОЕ: Очищаем текущее место
        currentPlace = null
    }
}
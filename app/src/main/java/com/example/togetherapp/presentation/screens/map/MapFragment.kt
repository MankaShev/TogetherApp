package com.example.togetherapp.presentation.screens.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.togetherapp.R
import com.example.togetherapp.databinding.FragmentMapBinding
import com.example.togetherapp.domain.models.SelectedPlace
import com.example.togetherapp.presentation.viewmodel.SharedMapViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.VisibleRegionUtils
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

    // Поле для адаптера поиска (работа с поисковой строкой)
    private lateinit var searchAdapter: SearchAdapter

    // Shared ViewModel для связи с коллекциями
    private lateinit var sharedViewModel: SharedMapViewModel

    // Текущее выбранное место
    private var currentPlace: SelectedPlace? = null

    // Создаём слой «User Location» для работы с местоположением пользователя
    private lateinit var userLocationLayer: com.yandex.mapkit.user_location.UserLocationLayer

    // Лаунчер: "слушает" ответ пользователя из системного окна (для местоположения)
    private lateinit var locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    // Отметка для выделенного места (с открытой карточкой)
    private var selectedPlacePlacemark: com.yandex.mapkit.map.PlacemarkMapObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инициализируем библиотеку. Ключ уже стоит (пока что) в MainActivity, тут просто "подключаемся".
        MapKitFactory.initialize(requireContext())
        // Инициализируем лаунчер: он "слушает" ответ пользователя из системного окна
        locationPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Если пользователь нажал "Разрешить" — летим камерой к нему
                moveToUserWithZoom()
            }
        }
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

        // Инициализация SharedViewModel
        sharedViewModel = ViewModelProvider(requireActivity())[SharedMapViewModel::class.java]

        // Наблюдение за результатом добавления в коллекцию
        observeNavigationEvents()

        // Инициализируем BottomSheet.
        // Мы ищем вью с id "bottomSheet" внутри контейнера mapContainer.
        val bottomSheet = binding.mapContainer.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // По умолчанию прячем шторку
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

        // Создаем SearchManager (для того, чтобы искать данные по нажатию на карту)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        // Инициализируем Адаптер (чтобы можно было использовать список ответов на запрос)
        searchAdapter = SearchAdapter { item ->
            val point = item.obj?.geometry?.firstOrNull()?.point
            if (point != null) {
                // Плавно перемещаем камеру к выбранному месту
                mapView.mapWindow.map.move(
                    com.yandex.mapkit.map.CameraPosition(point, 17.0f, 0.0f, 0.0f),
                    com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 1f),
                    null
                )
                // Прячем список и клавиатуру
                binding.cvSearchResults.visibility = View.GONE
                binding.etSearch.clearFocus()

                // ВМЕСТО startSearch(point) — сразу показываем данные в шторку!
                showPlaceData(item.obj, point)
            }
        }

        // Настраиваем RecyclerView
        binding.rvSearch.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        binding.etSearch.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    startTextSearch(query)
                    binding.etSearch.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Если текст стерли — прячем список
                if (newText.isNullOrEmpty()) {
                    binding.cvSearchResults.visibility = View.GONE
                }
                return true
            }
        })

        // Запускаем базовую настройку камеры
        setupMap()

        // Добавляем обработчик кнопки в карточке места
        setupAddToCollectionButton()
    }

    // Наблюдение за результатом добавления в коллекцию
    private fun observeNavigationEvents() {
        sharedViewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is SharedMapViewModel.NavigationEvent.BackToMapWithResult -> {
                    // Показываем уведомление
                    Toast.makeText(
                        requireContext(),
                        "✅ Место добавлено в «${event.collectionName}»",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Здесь можно обновить карточку места (например, подсветить, что уже добавлено)
                    binding.mapContainer.addToCollectionButton.text = "Добавлено ✓"
                    binding.mapContainer.addToCollectionButton.isEnabled = false

                    // После успешного добавления очищаем текущее выбранное место,
                    // чтобы пользователь явно выбрал новое место для следующего добавления
                    currentPlace = null

                    // Сбрасываем событие
                    sharedViewModel.consumeNavigationEvent()
                }
                null -> {}
            }
        }
    }

    // Настройка кнопки "Добавить в коллекцию"
    private fun setupAddToCollectionButton() {
        binding.mapContainer.addToCollectionButton.setOnClickListener {
            // Проверяем, есть ли выбранное место
            val place = currentPlace
            if (place != null) {
                // Сохраняем место в SharedViewModel
                sharedViewModel.setSelectedPlace(place)

                // Переходим на экран коллекций
                findNavController().navigate(
                    R.id.collectionsFragment,
                    Bundle().apply {
                        putBoolean("isAddingMode", true)
                    }
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "Сначала выберите место на карте",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ФУНКЦИЯ setupMap: Начальная настройка положения карты
    private fun setupMap() {
        val map = mapView.mapWindow.map
        val mapKit = MapKitFactory.getInstance()

        // Ставим Томск по дефолту
        map.move(com.yandex.mapkit.map.CameraPosition(Point(56.471116, 84.946636), 16.5f, 0.0f, 0.0f))

        // По умолчанию прячем шторку
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

        // Инициализируем слой
        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible = true

        // Запускаем проверку разрешений
        checkPermissionsAndMove()

        // Создаем объект слушателя
        mapInputListener = object : com.yandex.mapkit.map.InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                // При нажатии на карту все списки мест закрываются
                binding.cvSearchResults.visibility = View.GONE
                // Очищаем шторку
                binding.mapContainer.placeNameText.text = ""
                binding.mapContainer.placeAddressText.text = ""
                binding.mapContainer.categoryText.text = ""

                // Сбрасываем кнопку в исходное состояние при новом выборе места
                binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
                binding.mapContainer.addToCollectionButton.isEnabled = true

                // Скрываем шторку перед новым поиском
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

                // Безопасное удаление старого маркера
                removeSelectedMark()

                // Передаем координаты в поиск
                startSearch(point)
            }

            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {}
        }
        // Привязываем его к карте
        map.addInputListener(mapInputListener!!)
    }

    // Функция для проверки наличия разрешения на местоположение
    private fun checkPermissionsAndMove() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Разрешение уже есть - показываем устрашающее уведомление
            Toast.makeText(
                requireContext(),
                "Определяем местоположение...",
                Toast.LENGTH_LONG
            ).show()
            // и летим картой к пользователю
            moveToUserWithZoom()
        } else {
            // Разрешения нет — вызываем то самое окно (ну разреши, пж)
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun moveToUserWithZoom() {
        mapView.postDelayed({
            userLocationLayer.cameraPosition()?.target?.let { userPoint ->
                mapView.mapWindow.map.move(
                    com.yandex.mapkit.map.CameraPosition(userPoint, 16.5f, 0f, 0f),
                    com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 1.5f),
                    null
                )
            } ?: run {
                // Если мы еще не нашли пользователя — пробуем снова (т.к. GPS может дольше 2 секунд отдавать результат)
                moveToUserWithZoom()
            }
        }, 2000)
    }

    private fun showPlaceInBottomSheet(name: String, address: String, category: String, point: Point) {
        // Сохраняем данные в currentPlace для дальнейшего добавления в коллекцию
        currentPlace = SelectedPlace(
            external_id = (point.latitude * point.longitude).toInt(),
            title = name,
            latitude = point.latitude,
            longitude = point.longitude,
            description = null,
            address = address
        )

        // Удаляем старый маркер, если он был
        removeSelectedMark()

        // Создаем новый маркер
        selectedPlacePlacemark = mapView.mapWindow.map.mapObjects.addPlacemark().apply {
            geometry = point

                // Устанавливаем иконку с настройками стиля
                setIcon(
                    com.yandex.runtime.image.ImageProvider.fromResource(
                        requireContext(),
                        R.drawable.ic_selected_pin
                    ),
                    com.yandex.mapkit.map.IconStyle().apply {
                        // ставим картинку не центром, а серединой нижнего края
                        anchor = android.graphics.PointF(0.5f, 1.0f)

                        // Уменьшаем для аккуратности
                        scale = 0.75f
                    }
                )
        }

        // Двигаем карту к выбранной организации
        mapView.mapWindow.map.move(
            com.yandex.mapkit.map.CameraPosition(point, 17.0f, 0.0f, 0.0f),
            com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 0.8f),
            null
        )

        // Обновляем UI элементы в шторке
        binding.mapContainer.placeNameText.text = name
        binding.mapContainer.placeAddressText.text = address
        binding.mapContainer.categoryText.text = category

        // Сбрасываем состояние кнопки
        binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
        binding.mapContainer.addToCollectionButton.isEnabled = true

        // Показываем шторку
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun startSearch(point: Point) {
        // Настройки поиска: ищем все организации в одной точке
        val searchOptions = com.yandex.mapkit.search.SearchOptions().apply {
            // Ищем только бизнесы, а не адреса
            searchTypes = SearchType.BIZ.value
            resultPageSize = 20 // Просим 20 или меньше результатов
        }

        searchSession = searchManager.submit(
            point,
            16,
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: com.yandex.mapkit.search.Response) {
                    val results = response.collection.children

                    when {
                        results.isEmpty() -> {
                            // Ничего не нашли
                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                            binding.cvSearchResults.visibility = View.GONE
                        }

                        results.size == 1 -> {
                            // Один результат: используем функцию показа шторки сразу
                            binding.cvSearchResults.visibility = View.GONE

                            // results.first().obj — это и есть нужный нам GeoObject
                            showPlaceData(results.first().obj, point)
                        }

                        else -> {
                            // Много результатов: показываем список
                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                            searchAdapter.submitList(results)
                            binding.cvSearchResults.visibility = View.VISIBLE

                            // Если было выделено какое-то место, то снимаем выделение
                            removeSelectedMark()

                            currentPlace = null
                        }
                    }
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    android.util.Log.e("MapStep", "Ошибка: $error")
                }
            }
        )
    }

    private fun showPlaceData(obj: com.yandex.mapkit.GeoObject?, point: Point) {
        val metadata = obj?.metadataContainer?.getItem(com.yandex.mapkit.search.BusinessObjectMetadata::class.java)
        if (metadata != null) {
            val name = metadata.name
            val address = metadata.address.formattedAddress
            val category = metadata.categories.firstOrNull()?.name ?: ""

            // Вызываем функцию показа шторки
            showPlaceInBottomSheet(name, address, category, point)
        }
    }

    private fun startTextSearch(query: String) {
        val searchOptions = com.yandex.mapkit.search.SearchOptions().apply {
            searchTypes = SearchType.BIZ.value // Ищем организации
            resultPageSize = 7 //  Пока берем 7 первых результатов
        }

        // Получаем текущую видимую область карты
        val visibleRegion = mapView.mapWindow.map.visibleRegion

        // Запускаем поиск по строке
        searchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(visibleRegion),
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: com.yandex.mapkit.search.Response) {
                    val results = response.collection.children

                    if (results.isNotEmpty()) {
                        searchAdapter.submitList(results)
                        binding.cvSearchResults.visibility = View.VISIBLE
                    } else {
                        binding.cvSearchResults.visibility = View.GONE
                    }

                    if (results.isEmpty()) {
                        android.util.Log.d("MapSearch", "Ничего не найдено")
                    }
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    android.util.Log.e("MapSearch", "Ошибка текстового поиска: $error")
                }
            }
        )
    }

    private fun removeSelectedMark() {
        selectedPlacePlacemark?.let {
            if (it.isValid) {
                mapView.mapWindow.map.mapObjects.remove(it)
            }
        }
        selectedPlacePlacemark = null
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

        // Очищаем текущее место
        currentPlace = null
    }
}
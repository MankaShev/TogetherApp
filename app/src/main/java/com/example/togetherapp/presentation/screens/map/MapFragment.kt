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
    private var searchSession: Session? = null

    // bottomSheetBehavior: Управляет логикой шторки.
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    // Адаптер поиска по строке
    private lateinit var searchAdapter: SearchAdapter

    // Shared ViewModel для связи карты и подборок
    private lateinit var sharedViewModel: SharedMapViewModel

    // Текущее выбранное место
    private var currentPlace: SelectedPlace? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем ссылку на MapView
        mapView = binding.mapContainer.mapview

        // Shared ViewModel
        sharedViewModel = ViewModelProvider(requireActivity())[SharedMapViewModel::class.java]

        // Наблюдение за результатом добавления в подборку
        observeNavigationEvents()

        // BottomSheet
        val bottomSheet = binding.mapContainer.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

        // SearchManager
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        // Адаптер поиска по строке
        searchAdapter = SearchAdapter { item ->
            val point = item.obj?.geometry?.firstOrNull()?.point
            if (point != null) {
                mapView.mapWindow.map.move(
                    com.yandex.mapkit.map.CameraPosition(point, 17.0f, 0.0f, 0.0f),
                    com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 1f),
                    null
                )

                binding.cvSearchResults.visibility = View.GONE
                binding.etSearch.clearFocus()

                // Открываем карточку места через обычный поиск по точке
                startSearch(point)
            }
        }

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
                if (newText.isNullOrEmpty()) {
                    binding.cvSearchResults.visibility = View.GONE
                }
                return true
            }
        })

        setupMap()
        setupAddToCollectionButton()
    }

    private fun observeNavigationEvents() {
        sharedViewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is SharedMapViewModel.NavigationEvent.BackToMapWithResult -> {
                    Toast.makeText(
                        requireContext(),
                        "✅ Место добавлено в «${event.collectionName}»",
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.mapContainer.addToCollectionButton.text = "Добавлено ✓"
                    binding.mapContainer.addToCollectionButton.isEnabled = false

                    currentPlace = null
                    sharedViewModel.consumeNavigationEvent()
                }
                null -> {}
            }
        }
    }

    private fun setupAddToCollectionButton() {
        binding.mapContainer.addToCollectionButton.setOnClickListener {
            val place = currentPlace
            if (place != null) {
                sharedViewModel.setSelectedPlace(place)

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

    // Начальная настройка карты
    private fun setupMap() {
        val map = mapView.mapWindow.map

        map.move(
            com.yandex.mapkit.map.CameraPosition(
                Point(56.471116, 84.946636),
                16.5f, 0.0f, 0.0f
            )
        )

        mapInputListener = object : com.yandex.mapkit.map.InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                binding.mapContainer.placeNameText.text = ""
                binding.mapContainer.placeAddressText.text = ""
                binding.mapContainer.categoryText.text = ""

                binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
                binding.mapContainer.addToCollectionButton.isEnabled = true

                currentPlace = null
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

                android.util.Log.d("MapStep", "ЭТАП 2: Запускаю поиск для ${point.latitude}")
                startSearch(point)
            }

            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {}
        }

        map.addInputListener(mapInputListener!!)
    }

    private fun startSearch(point: Point) {
        val searchOptions = com.yandex.mapkit.search.SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 1
        }

        searchSession = searchManager.submit(
            point,
            16,
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: com.yandex.mapkit.search.Response) {
                    val bizMetadata = response.collection.children.firstOrNull()?.obj
                        ?.metadataContainer
                        ?.getItem(com.yandex.mapkit.search.BusinessObjectMetadata::class.java)

                    if (bizMetadata != null) {
                        val name = bizMetadata.name
                        val address = bizMetadata.address.formattedAddress
                        val category = bizMetadata.categories.firstOrNull()?.name ?: ""

                        android.util.Log.d("MapStep", "ЭТАП 2 ПРОЙДЕН: Найдена организация: $name")

                        currentPlace = SelectedPlace(
                            external_id = (point.latitude * point.longitude).toInt(),
                            title = name,
                            latitude = point.latitude,
                            longitude = point.longitude,
                            description = null,
                            address = address
                        )

                        binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
                        binding.mapContainer.addToCollectionButton.isEnabled = true

                        binding.mapContainer.placeNameText.text = name
                        binding.mapContainer.placeAddressText.text = address
                        binding.mapContainer.categoryText.text = category

                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        android.util.Log.d("MapStep", "В этой точке нет организаций")
                        currentPlace = null
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    android.util.Log.e("MapStep", "Ошибка поиска: $error")
                    currentPlace = null
                }
            }
        )
    }

    private fun startTextSearch(query: String) {
        val searchOptions = com.yandex.mapkit.search.SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 7
        }

        val visibleRegion = mapView.mapWindow.map.visibleRegion

        searchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(visibleRegion),
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: com.yandex.mapkit.search.Response) {
                    val results = response.collection.children
                    android.util.Log.d("MapSearch", "Найдено объектов: ${results.size}")

                    if (results.isNotEmpty()) {
                        searchAdapter.submitList(results)
                        binding.cvSearchResults.visibility = View.VISIBLE
                    } else {
                        binding.cvSearchResults.visibility = View.GONE
                    }

                    results.forEachIndexed { index, searchResult ->
                        val obj = searchResult.obj
                        val metadata = obj?.metadataContainer
                            ?.getItem(com.yandex.mapkit.search.BusinessObjectMetadata::class.java)

                        if (metadata != null) {
                            val name = metadata.name
                            val address = metadata.address.formattedAddress
                            val point = obj.geometry.firstOrNull()?.point

                            android.util.Log.d(
                                "MapSearch",
                                "#$index: $name | Адрес: $address | Координаты: ${point?.latitude}, ${point?.longitude}"
                            )
                        }
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
        _binding = null
        currentPlace = null
    }
}